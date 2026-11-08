package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.security.PublicKey;
import java.security.SecureRandom;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import sinhvien.host.CardClient;
import sinhvien.host.CardCrypto;

/**
 * Trình diễn phần mật mã: khóa công khai RSA của thẻ so với CSDL, xác thực challenge–response
 * từng bước (challenge, chữ ký, kết quả), tóm tắt cách thẻ giữ PIN và khóa AES.
 * Tiêu chí chấm: "Tạo được cặp khóa RSA", "Xác thực được theo chức năng".
 */
public class SecurityPage extends Page {

    private static final SecureRandom RND = new SecureRandom();

    private final JLabel cardState = new JLabel(" ");
    private final JTextArea modulus = area(4);
    private final JLabel exponent = Ui.mono("—");
    private final Ui.Chip keyMatch = new Ui.Chip("Chưa đọc khóa");
    private final JTextArea challenge = area(2);
    private final JTextArea signature = area(4);
    private final Ui.Chip authResult = new Ui.Chip("Chưa xác thực");
    private final JLabel timing = Ui.muted(" ");

    public SecurityPage(Ctx ctx) {
        super(ctx);
        cardState.setVerticalAlignment(JLabel.TOP);
        JButton readKey = Ui.button("Đọc khóa công khai", Ui.Kind.DEFAULT, Icons.of(Icons.Kind.KEY, 16));
        readKey.addActionListener(e -> readKey());
        JPanel kb = Ui.transparent(new BorderLayout(0, 8));
        JPanel kt = Ui.transparent(new BorderLayout(0, 4));
        kt.add(Ui.muted("Modulus n (lấy từ thẻ bằng lệnh B0 31):"), BorderLayout.NORTH);
        kt.add(Ui.scroll(modulus), BorderLayout.CENTER);
        kb.add(kt, BorderLayout.NORTH);
        JPanel ke = Ui.transparent(new BorderLayout(8, 0));
        ke.add(Ui.muted("Số mũ e:"), BorderLayout.WEST);
        ke.add(exponent, BorderLayout.CENTER);
        kb.add(ke, BorderLayout.CENTER);
        kb.add(Ui.row(10, readKey, keyMatch), BorderLayout.SOUTH);
        Ui.RoundPanel keyCard = Ui.card("Cặp khóa RSA-1024 của thẻ",
                "Sinh trên thẻ lúc phát hành (KeyPair.genKeyPair) – khóa bí mật không có lệnh nào đọc ra", kb);

        JButton auth = Ui.button("Xác thực thẻ (challenge–response)", Ui.Kind.PRIMARY, Icons.of(Icons.Kind.SHIELD, 16, Color.WHITE));
        auth.addActionListener(e -> authenticate());
        JPanel ab = Ui.transparent(new BorderLayout(0, 8));
        ab.add(Ui.vstack(8, labeled("1. Phần mềm sinh challenge ngẫu nhiên 16 byte:", challenge),
                labeled("2. Thẻ ký 'A' ‖ mã SV ‖ challenge bằng khóa bí mật (lệnh B0 32):", signature),
                Ui.row(10, auth, authResult), timing), BorderLayout.CENTER);
        Ui.RoundPanel authCard = Ui.card("Xác thực thẻ thật",
                "3. Phần mềm kiểm chữ ký bằng khóa công khai LƯU TRONG CSDL lúc phát hành (SHA1withRSA)", ab);

        JLabel design = new JLabel("<html><table cellpadding='4'>"
                + "<tr><td><b>PIN</b></td><td>Lưu SHA-256('P' ‖ salt ‖ PIN) trong OwnerPIN, sai 3 lần khóa</td></tr>"
                + "<tr><td><b>Dữ liệu</b></td><td>AES-128 CBC bằng DEK ngẫu nhiên; DEK bọc bằng KEK = SHA-256('K' ‖ salt ‖ PIN)</td></tr>"
                + "<tr><td><b>Quên PIN</b></td><td>Bản DEK thứ hai bọc bằng KEK từ PUK → đặt PIN mới, không mất dữ liệu</td></tr>"
                + "<tr><td><b>Giao dịch</b></td><td>PIN trong lệnh + thẻ ký biên nhận + bộ đếm chống lặp + giao dịch nguyên tử</td></tr>"
                + "<tr><td><b>Phiên</b></td><td>Khóa DEK nằm ở RAM (CLEAR_ON_DESELECT): rút thẻ là mất</td></tr>"
                + "</table></html>");
        design.setFont(Theme.font(Font.PLAIN, 13));
        design.setVerticalAlignment(JLabel.TOP);
        JPanel right = Ui.transparent(new BorderLayout(0, 16));
        right.add(Ui.card("Thẻ đang cắm", null, cardState), BorderLayout.NORTH);
        right.add(Ui.card("Thiết kế bảo mật", null, design), BorderLayout.CENTER);

        JPanel left = Ui.transparent(new BorderLayout(0, 16));
        left.add(keyCard, BorderLayout.NORTH);
        left.add(authCard, BorderLayout.CENTER);

        JPanel body = Ui.transparent(new BorderLayout(16, 0));
        body.add(left, BorderLayout.CENTER);
        body.add(Ui.fixedWidth(right, 400), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    private static JTextArea area(int rows) {
        JTextArea a = new JTextArea(rows, 20);
        a.setEditable(false);
        a.setLineWrap(true);
        a.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return a;
    }

    private static JPanel labeled(String label, JTextArea a) {
        JPanel p = Ui.transparent(new BorderLayout(0, 4));
        p.add(Ui.muted(label), BorderLayout.NORTH);
        p.add(Ui.scroll(a), BorderLayout.CENTER);
        return p;
    }

    public String title() {
        return "Bảo mật thẻ";
    }

    public String subtitle() {
        return "Khóa công khai RSA, xác thực challenge–response, cách thẻ giữ PIN và khóa AES";
    }

    public Icons.Kind icon() {
        return Icons.Kind.SHIELD;
    }

    public void onShow() {
        onCardChanged();
    }

    public void onCardChanged() {
        CardSession s = ctx.session;
        CardClient.CardStatus st = s.status();
        if (!s.isConnected() || st == null) {
            cardState.setText("Chưa kết nối thẻ");
            return;
        }
        cardState.setText("<html><table cellpadding='3'>"
                + "<tr><td>Chế độ</td><td>" + s.modeLabel() + "</td></tr>"
                + "<tr><td>Mã SV</td><td><b>" + (st.cardId.isEmpty() ? "—" : st.cardId) + "</b></td></tr>"
                + "<tr><td>Trạng thái</td><td>" + (st.state == 0 ? "BLANK (thẻ trắng)" : st.state == 1 ? "ACTIVE" : "LOCKED") + "</td></tr>"
                + "<tr><td>RSA</td><td><b>" + (s.isGenuine() ? "Thẻ thật" : "Không đạt") + "</b></td></tr>"
                + "<tr><td>PIN / PUK còn</td><td>" + st.pinTries + " / " + st.pukTries + "</td></tr>"
                + "<tr><td>Phiên PIN</td><td>" + (st.pinVerified ? "Đang mở" : "Đóng") + "</td></tr>"
                + "<tr><td>Dữ liệu mã hóa</td><td>INFO " + st.infoLen + " · CONTACT " + st.contactLen + " · ẢNH " + st.photoLen + " byte</td></tr>"
                + "</table></html>");
    }

    void readKey() {
        try {
            ctx.session.requireConnected();
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        Task.run(this, "Đang đọc khóa công khai…", () -> {
            byte[][] card = ctx.session.client.publicKeyParts();
            byte[][] db = ctx.repo.activePublicKeyParts(ctx.session.cardId());
            return new byte[][][] {card, db};
        }, o -> {
            byte[][] card = o[0];
            byte[][] db = o[1];
            modulus.setText(CardCrypto.hex(card[0]));
            modulus.setCaretPosition(0);
            exponent.setText(CardCrypto.hex(card[1]) + "  (= " + new java.math.BigInteger(1, card[1]) + ")");
            if (db == null) {
                keyMatch.set("CSDL không có khóa ACTIVE cho thẻ này", Theme.DANGER);
            } else if (new java.math.BigInteger(1, db[0]).equals(new java.math.BigInteger(1, card[0]))) {
                keyMatch.set("Trùng với khóa lưu trong CSDL", Theme.SUCCESS);
            } else {
                keyMatch.set("KHÁC khóa trong CSDL", Theme.DANGER);
            }
        });
    }

    void authenticate() {
        try {
            ctx.session.requireConnected();
            if (ctx.session.status() == null || ctx.session.status().state != 1) {
                throw new Task.UserError("Thẻ chưa phát hành");
            }
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        final byte[] ch = new byte[ctx.cfg.rsaChallengeBytes()];
        RND.nextBytes(ch);
        Task.run(this, "Đang xác thực thẻ…", () -> {
            String id = ctx.session.cardId();
            long t0 = System.nanoTime();
            byte[] sig = ctx.session.client.signChallenge(ch);
            long ms = (System.nanoTime() - t0) / 1000000L;
            PublicKey key = ctx.repo.activePublicKey(id);
            boolean ok = key != null && CardCrypto.verify(key, CardCrypto.authMessage(id, ch), sig);
            return new Object[] {sig, ok, ms, key != null};
        }, o -> {
            challenge.setText(CardCrypto.hex(ch));
            signature.setText(CardCrypto.hex((byte[]) o[0]));
            signature.setCaretPosition(0);
            boolean ok = (Boolean) o[1];
            if (!(Boolean) o[3]) {
                authResult.set("CSDL không có khóa của thẻ này", Theme.DANGER);
            } else {
                authResult.set(ok ? "Chữ ký hợp lệ → THẺ THẬT" : "Chữ ký sai → THẺ GIẢ", ok ? Theme.SUCCESS : Theme.DANGER);
            }
            timing.setText("Thẻ ký mất " + o[2] + " ms · " + ((byte[]) o[0]).length + " byte");
        });
    }

}
