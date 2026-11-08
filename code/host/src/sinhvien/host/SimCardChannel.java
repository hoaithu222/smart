package sinhvien.host;

import com.licel.jcardsim.io.JavaxSmartCardInterface;
import java.nio.charset.StandardCharsets;
import javacard.framework.AID;
import javacard.framework.APDU;
import javacard.framework.Applet;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import sinhvien.StudentCard;

/**
 * Thẻ giả lập chạy NGAY TRONG phần mềm (thư viện jCardSim), dùng khi chưa mở JCIDE
 * hoặc để tự kiểm thử. Bật bằng card.mode=jcardsim trong app.properties.
 * Chạy đúng mã applet sinhvien.StudentCard như trên JCIDE.
 * Lưu ý: thẻ chỉ tồn tại trong lúc phần mềm chạy; tắt phần mềm là về thẻ trắng.
 */
public class SimCardChannel implements ApduChannel {

    private static final AID OTHER_AID = new AID(new byte[] {0x11, 0x22, 0x33, 0x44, 0x55, 0x00}, (short) 0, (byte) 6);

    /** Applet phụ, chỉ để "chọn applet khác" -> JCRE gọi deselect() của thẻ sinh viên (kết thúc phiên). */
    public static class OtherApplet extends Applet {
        public static void install(byte[] b, short o, byte l) {
            new OtherApplet().register();
        }

        public void process(APDU apdu) {
        }
    }

    private final JavaxSmartCardInterface sim = new JavaxSmartCardInterface();
    private final AID aid;
    private boolean inserted;

    public SimCardChannel(byte[] appletAid, String adminPin) {
        aid = new AID(appletAid, (short) 0, (byte) appletAid.length);
        byte[] pin = adminPin.getBytes(StandardCharsets.US_ASCII);
        // Tham số cài đặt đúng định dạng TH4: [Li][AID][Lc=0][La][PIN quản trị]
        byte[] params = new byte[3 + appletAid.length + pin.length];
        int p = 0;
        params[p++] = (byte) appletAid.length;
        System.arraycopy(appletAid, 0, params, p, appletAid.length);
        p += appletAid.length;
        params[p++] = 0;
        params[p++] = (byte) pin.length;
        System.arraycopy(pin, 0, params, p, pin.length);
        sim.installApplet(aid, StudentCard.class, params, (short) 0, (byte) params.length);
        sim.installApplet(OTHER_AID, OtherApplet.class);
    }

    /** "Cắm thẻ": chọn applet thẻ sinh viên. Trả SW của lệnh SELECT (9000 / 6400). */
    public int insert() {
        byte[] r = sim.selectAppletWithResult(aid);
        inserted = true;
        return ((r[r.length - 2] & 0xFF) << 8) | (r[r.length - 1] & 0xFF);
    }

    /** "Rút thẻ": chọn applet khác để thẻ sinh viên bị bỏ chọn - xóa phiên PIN như thẻ thật. */
    public void remove() {
        sim.selectAppletWithResult(OTHER_AID);
        inserted = false;
    }

    public boolean isInserted() {
        return inserted;
    }

    public ResponseAPDU transmit(CommandAPDU command) throws CardException {
        if (!inserted) {
            throw new CardException("Chưa cắm thẻ giả lập");
        }
        byte[] b = command.getBytes();
        if (b.length >= 4 && b[0] == 0x00 && b[1] == (byte) 0xA4) {
            int sw = insert(); // jCardSim không nhận SELECT qua transmitCommand
            return new ResponseAPDU(new byte[] {(byte) (sw >> 8), (byte) sw});
        }
        return sim.transmitCommand(command);
    }
}
