package sinhvien.ui;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.security.PublicKey;
import sinhvien.host.ApduChannel;
import sinhvien.host.AppConfig;
import sinhvien.host.CardClient;
import sinhvien.host.CardRepository;
import sinhvien.host.LoggingChannel;
import sinhvien.host.PcscChannel;
import sinhvien.host.SimCardChannel;

/**
 * Trạng thái thẻ dùng chung cho mọi màn hình (giống một store):
 * đã cắm chưa, mã SV, thẻ thật hay giả, đã nhập PIN chưa. Đổi gì thì báo cho các màn hình đang nghe.
 *
 * Chuỗi chặn khi cắm thẻ: SELECT -> GET_STATUS -> tra khóa công khai trong CSDL -> AUTH_CARD (RSA).
 */
public class CardSession {

    public static final String PROP = "card";

    private final AppConfig cfg;
    private final CardRepository repo;
    private final PcscChannel pcsc;
    private final SimCardChannel sim;
    private final LoggingChannel log;
    public final CardClient client;
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private boolean connected;
    private boolean genuine;
    private boolean pinVerified;
    private CardClient.CardStatus status;
    private String dbStatus;

    public CardSession(AppConfig cfg, CardRepository repo) {
        this.cfg = cfg;
        this.repo = repo;
        ApduChannel base;
        if ("jcardsim".equals(cfg.cardMode())) {
            sim = new SimCardChannel(cfg.appletAid(), cfg.simAdminPin());
            pcsc = null;
            base = sim;
        } else {
            pcsc = new PcscChannel(cfg.protocol(), cfg.readerName());
            sim = null;
            base = pcsc;
        }
        log = new LoggingChannel(base, cfg.logApdu());
        client = new CardClient(log);
    }

    public LoggingChannel log() {
        return log;
    }

    public boolean isSimulator() {
        return sim != null;
    }

    public String modeLabel() {
        return sim != null ? "Giả lập jCardSim" : "PC/SC · " + (cfg.readerName().isEmpty() ? "tự chọn" : cfg.readerName());
    }

    public void addListener(PropertyChangeListener l) {
        pcs.addPropertyChangeListener(PROP, l);
    }

    private void fire() {
        pcs.firePropertyChange(PROP, null, this);
    }

    // ------------------------------------------------------------ cắm / rút

    public synchronized void connect() throws Exception {
        int sw;
        if (sim != null) {
            sw = sim.insert();
        } else {
            PcscChannel.SelectResult r = pcsc.connect(cfg.appletAid());
            sw = r == PcscChannel.SelectResult.OK ? 0x9000 : r == PcscChannel.SelectResult.CARD_LOCKED ? 0x6400 : 0x6A82;
            if (sw == 0x6A82) {
                pcsc.disconnect();
                throw new Task.UserError("Thẻ không có applet thẻ sinh viên (SELECT trả 6A82).\n"
                        + "Kiểm tra JCIDE đã Run và AID trong app.properties trùng Instance AID.");
            }
        }
        connected = true;
        pinVerified = false;
        refresh();
    }

    public synchronized void disconnect() {
        if (sim != null) {
            sim.remove();
        } else {
            pcsc.disconnect();
        }
        connected = false;
        genuine = false;
        pinVerified = false;
        status = null;
        dbStatus = null;
        fire();
    }

    /** Đọc lại trạng thái + xác thực RSA. Gọi sau mọi thao tác làm đổi thẻ. */
    public synchronized void refresh() throws Exception {
        status = client.status();
        genuine = false;
        dbStatus = null;
        if (status.state == 1 && !status.cardId.isEmpty()) {
            dbStatus = repo.cardStatus(status.cardId);
            PublicKey key = repo.activePublicKey(status.cardId);
            genuine = key != null && client.authenticate(key, status.cardId);
        }
        if (!status.pinVerified) {
            pinVerified = false;
        }
        fire();
    }

    /** Làm mới nhưng nuốt lỗi (dùng sau thao tác đã báo lỗi riêng). */
    public void refreshQuietly() {
        try {
            if (connected) {
                refresh();
            }
        } catch (Exception ignored) {
            fire();
        }
    }

    // ------------------------------------------------------------ PIN

    public void verifyPin(String pin) throws Exception {
        try {
            client.verifyPin(pin);
            pinVerified = true;
        } catch (CardClient.CardSwException e) {
            pinVerified = false;
            refreshQuietly();
            throw e;
        }
        refreshQuietly();
    }

    /** Thẻ báo 6982 (phiên PIN đã mất). */
    public void markPinLost() {
        pinVerified = false;
        fire();
    }

    public boolean isPinVerified() {
        return pinVerified;
    }

    // ------------------------------------------------------------ điều kiện cho từng màn hình

    public void requireConnected() {
        if (!connected) {
            throw new Task.UserError("Chưa kết nối thẻ. Bấm \"Kết nối thẻ\" ở góc trên.");
        }
    }

    /** Thẻ đã phát hành, không khóa, xác thực RSA thành công. */
    public void requireGenuine() {
        requireConnected();
        if (status == null || status.state == 0) {
            throw new Task.UserError("Thẻ trắng – cần phát hành trước (màn \"Phát hành thẻ\").");
        }
        if (status.state == 2) {
            throw new Task.UserError("Thẻ đã bị khóa. Liên hệ phòng Đào tạo.");
        }
        if ("LOCKED".equals(dbStatus)) {
            throw new Task.UserError("Thẻ " + status.cardId + " đã bị báo mất / khóa trong CSDL.");
        }
        if (!genuine) {
            throw new Task.UserError("Không xác thực được thẻ bằng RSA: thẻ giả, hoặc thẻ chưa đăng ký / đã thu hồi trong CSDL.");
        }
    }

    // ------------------------------------------------------------ đọc trạng thái

    public boolean isConnected() {
        return connected;
    }

    public boolean isGenuine() {
        return genuine;
    }

    public CardClient.CardStatus status() {
        return status;
    }

    public String cardId() {
        return status == null ? null : status.cardId;
    }

    public String chipText() {
        if (!connected) {
            return "Chưa kết nối thẻ";
        }
        if (status == null) {
            return "Đang đọc thẻ…";
        }
        if (status.state == 0) {
            return "Thẻ trắng – chưa phát hành";
        }
        if (status.state == 2) {
            return status.cardId + " · Thẻ bị khóa";
        }
        if ("LOCKED".equals(dbStatus)) {
            return status.cardId + " · Đã báo mất";
        }
        if (!genuine) {
            return status.cardId + " · Không xác thực được";
        }
        return status.cardId + " · Thẻ thật" + (pinVerified ? " · Đã nhập PIN" : "");
    }

    public Color chipColor() {
        if (!connected || status == null) {
            return Theme.muted();
        }
        if (status.state == 0) {
            return Theme.WARNING;
        }
        if (status.state == 2 || "LOCKED".equals(dbStatus) || !genuine) {
            return Theme.DANGER;
        }
        return Theme.SUCCESS;
    }
}
