package sinhvien.host;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Tầng "nói chuyện với thẻ": mỗi lệnh APDU của applet là một hàm Java.
 * Giao diện Swing gọi các hàm này, KHÔNG tự ghép byte APDU.
 */
public class CardClient {

    public static final byte[] APPLET_AID = {0x4B, 0x4D, 0x41, 0x53, 0x56, 0x01, 0x01};
    public static final int CLA = 0xB0;

    public static final int OBJ_INFO = 1;
    public static final int OBJ_CONTACT = 2;
    public static final int OBJ_PHOTO = 3;
    public static final int CHUNK = 240;

    public static final int TX_TOPUP = 1;
    public static final int TX_PAY = 2;
    public static final int TX_CHECKIN = 3;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final ApduChannel channel;

    public CardClient(ApduChannel channel) {
        this.channel = channel;
    }

    // ------------------------------------------------------------ kiểu dữ liệu

    /** Lỗi do thẻ trả SW khác 9000, kèm thông báo tiếng Việt để hiện lên giao diện. */
    public static class CardSwException extends Exception {
        public final int sw;

        public CardSwException(int sw) {
            super(describe(sw));
            this.sw = sw;
        }

        public boolean isWrongPin() {
            return (sw & 0xFFF0) == 0x63C0;
        }

        public int triesLeft() {
            return isWrongPin() ? (sw & 0x0F) : -1;
        }
    }

    public static class CardStatus {
        public int state;          // 0 chưa phát hành, 1 hoạt động, 2 bị khóa
        public int pinTries;
        public int pukTries;
        public boolean adminVerified;
        public boolean pinVerified;
        public int infoLen, contactLen, photoLen;
        public String cardId;
    }

    public static class Receipt {
        public int type;
        public int value;          // số tiền (nghìn đồng) hoặc mã phòng
        public int balance;        // số dư sau giao dịch (nghìn đồng)
        public int counter;        // bộ đếm giao dịch của thẻ (chống lặp lại)
        public byte[] timeAndNonce;
        public byte[] signature;

        /** Thời gian (giây kể từ 1970) máy tính đã gửi xuống thẻ. */
        public long time() {
            return ((long) (timeAndNonce[0] & 0xFF) << 24) | ((timeAndNonce[1] & 0xFF) << 16)
                    | ((timeAndNonce[2] & 0xFF) << 8) | (timeAndNonce[3] & 0xFF);
        }
    }

    public static class LogEntry {
        public int type, value, balance, counter;
        public long time;          // giây kể từ 1970 (máy tính gửi xuống)
    }

    // ------------------------------------------------------------ lệnh quản trị

    public CardStatus status() throws CardException, CardSwException {
        byte[] r = send(0x10, 0, 0, null, true);
        CardStatus s = new CardStatus();
        s.state = r[0];
        s.pinTries = r[1];
        s.pukTries = r[2];
        s.adminVerified = (r[3] & 0x01) != 0;
        s.pinVerified = (r[3] & 0x02) != 0;
        s.infoLen = u16(r, 4);
        s.contactLen = u16(r, 6);
        s.photoLen = u16(r, 8);
        int idLen = r[10];
        s.cardId = new String(r, 11, idLen, StandardCharsets.US_ASCII);
        return s;
    }

    public void verifyAdmin(String adminPin) throws CardException, CardSwException {
        send(0x20, 0, 0, ascii(adminPin), false);
    }

    /** Phát hành thẻ. Trả về khóa công khai để lưu vào CSDL. */
    public PublicKey initCard(String cardId, String pin, String puk)
            throws CardException, CardSwException, GeneralSecurityException {
        send(0x30, 0, 0, concat(lv(ascii(cardId)), lv(ascii(pin)), lv(ascii(puk))), false);
        return publicKey();
    }

    public byte[][] publicKeyParts() throws CardException, CardSwException {
        byte[] r = send(0x31, 0, 0, null, true);
        int modLen = u16(r, 0);
        byte[] mod = Arrays.copyOfRange(r, 2, 2 + modLen);
        int expLen = u16(r, 2 + modLen);
        byte[] exp = Arrays.copyOfRange(r, 4 + modLen, 4 + modLen + expLen);
        return new byte[][] {mod, exp};
    }

    public PublicKey publicKey() throws CardException, CardSwException, GeneralSecurityException {
        byte[][] p = publicKeyParts();
        return CardCrypto.rsaPublicKey(p[0], p[1]);
    }

    /** Khóa (báo mất) hoặc mở khóa thẻ. Cần verifyAdmin trước. */
    public void setLocked(boolean locked) throws CardException, CardSwException {
        send(0x33, locked ? 1 : 0, 0, null, false);
    }

    // ------------------------------------------------------------ PIN

    public void verifyPin(String pin) throws CardException, CardSwException {
        send(0x21, 0, 0, ascii(pin), false);
    }

    public void changePin(String oldPin, String newPin) throws CardException, CardSwException {
        send(0x22, 0, 0, concat(lv(ascii(oldPin)), lv(ascii(newPin))), false);
    }

    public void unblockPin(String puk, String newPin) throws CardException, CardSwException {
        send(0x23, 0, 0, concat(lv(ascii(puk)), lv(ascii(newPin))), false);
    }

    // ------------------------------------------------------------ xác thực thẻ bằng RSA

    /**
     * Gửi challenge ngẫu nhiên, thẻ ký, máy tính kiểm bằng khóa công khai LẤY TỪ CSDL
     * (không lấy từ chính thẻ đang cắm - thẻ giả cũng tự trả được khóa của nó).
     */
    public boolean authenticate(PublicKey keyFromDatabase, String cardIdFromDatabase)
            throws CardException, CardSwException, GeneralSecurityException {
        byte[] challenge = new byte[16];
        RANDOM.nextBytes(challenge);
        byte[] sig = send(0x32, 0, 0, challenge, true);
        return CardCrypto.verify(keyFromDatabase, CardCrypto.authMessage(cardIdFromDatabase, challenge), sig);
    }

    /** Gửi challenge tùy ý, nhận chữ ký thô (dùng ở màn "Bảo mật thẻ" để trình diễn). */
    public byte[] signChallenge(byte[] challenge) throws CardException, CardSwException {
        return send(0x32, 0, 0, challenge, true);
    }

    // ------------------------------------------------------------ thông tin đã mã hóa

    /** Ghi đối tượng: đệm -> chia khối 240 byte -> thẻ mã hóa AES rồi lưu. */
    public void writeObject(int obj, byte[] plain) throws CardException, CardSwException {
        byte[] padded = CardCrypto.pad(plain);
        for (int chunk = 0, off = 0; off < padded.length; chunk++, off += CHUNK) {
            byte[] part = Arrays.copyOfRange(padded, off, Math.min(off + CHUNK, padded.length));
            send(0x40, obj, chunk, part, false);
        }
    }

    /** Đọc đối tượng: thẻ giải mã từng khối; paddedLen lấy từ status(). */
    public byte[] readObject(int obj, int paddedLen) throws CardException, CardSwException {
        if (paddedLen == 0) {
            return new byte[0];
        }
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (int chunk = 0; chunk * CHUNK < paddedLen; chunk++) {
            byte[] part = send(0x41, obj, chunk, null, true);
            o.write(part, 0, part.length);
        }
        return CardCrypto.unpad(o.toByteArray());
    }

    // ------------------------------------------------------------ ví + điểm danh

    /** [0] số dư (nghìn đồng), [1] bộ đếm giao dịch, [2] số bản ghi nhật ký */
    public int[] balance() throws CardException, CardSwException {
        byte[] r = send(0x50, 0, 0, null, true);
        return new int[] {u16(r, 0), u16(r, 2), r[4]};
    }

    public Receipt topUp(String pin, int amountThousand) throws CardException, CardSwException {
        return transfer(0x51, TX_TOPUP, pin, amountThousand);
    }

    public Receipt pay(String pin, int amountThousand) throws CardException, CardSwException {
        return transfer(0x52, TX_PAY, pin, amountThousand);
    }

    public Receipt checkIn(int roomCode) throws CardException, CardSwException {
        byte[] tn = timeAndNonce();
        byte[] data = concat(new byte[] {(byte) (roomCode >> 8), (byte) roomCode}, tn);
        return receipt(TX_CHECKIN, roomCode, tn, send(0x60, 0, 0, data, true));
    }

    public List<LogEntry> log() throws CardException, CardSwException {
        byte[] r = send(0x53, 0, 0, null, true);
        List<LogEntry> out = new ArrayList<LogEntry>();
        for (int off = 0; off + 12 <= r.length; off += 12) {
            LogEntry e = new LogEntry();
            e.type = r[off];
            e.value = u16(r, off + 2);
            e.balance = u16(r, off + 4);
            e.counter = u16(r, off + 6);
            e.time = ((long) u16(r, off + 8) << 16) | u16(r, off + 10);
            out.add(e);
        }
        return out;
    }

    private Receipt transfer(int ins, int type, String pin, int amount) throws CardException, CardSwException {
        byte[] tn = timeAndNonce();
        byte[] data = concat(lv(ascii(pin)), new byte[] {(byte) (amount >> 8), (byte) amount}, tn);
        return receipt(type, amount, tn, send(ins, 0, 0, data, true));
    }

    private static Receipt receipt(int type, int value, byte[] tn, byte[] r) {
        Receipt rc = new Receipt();
        rc.type = type;
        rc.value = value;
        rc.balance = u16(r, 0);
        rc.counter = u16(r, 2);
        rc.timeAndNonce = tn;
        rc.signature = Arrays.copyOfRange(r, 4, r.length);
        return rc;
    }

    /** 4 byte thời gian (giây, big-endian) + 8 byte nonce ngẫu nhiên. */
    private static byte[] timeAndNonce() {
        long now = System.currentTimeMillis() / 1000L;
        byte[] tn = new byte[12];
        tn[0] = (byte) (now >> 24);
        tn[1] = (byte) (now >> 16);
        tn[2] = (byte) (now >> 8);
        tn[3] = (byte) now;
        byte[] nonce = new byte[8];
        RANDOM.nextBytes(nonce);
        System.arraycopy(nonce, 0, tn, 4, 8);
        return tn;
    }

    // ------------------------------------------------------------ tiện ích

    private byte[] send(int ins, int p1, int p2, byte[] data, boolean expectData)
            throws CardException, CardSwException {
        CommandAPDU cmd;
        if (data == null) {
            cmd = expectData ? new CommandAPDU(CLA, ins, p1, p2, 256) : new CommandAPDU(CLA, ins, p1, p2);
        } else {
            cmd = expectData ? new CommandAPDU(CLA, ins, p1, p2, data, 256) : new CommandAPDU(CLA, ins, p1, p2, data);
        }
        ResponseAPDU r = channel.transmit(cmd);
        if (r.getSW() != 0x9000) {
            throw new CardSwException(r.getSW());
        }
        return r.getData();
    }

    public static String describe(int sw) {
        if ((sw & 0xFFF0) == 0x63C0) {
            return "Sai mã, còn " + (sw & 0x0F) + " lần thử";
        }
        switch (sw) {
        case 0x6983: return "Mã đã bị khóa do nhập sai quá số lần";
        case 0x6982: return "Chưa xác thực (cần nhập PIN / PIN quản trị)";
        case 0x6985: return "Trạng thái thẻ không cho phép thao tác này";
        case 0x6400: return "Thẻ đã bị khóa";
        case 0x6A83: return "Số tiền không hợp lệ";
        case 0x6A84: return "Vượt số dư tối đa của thẻ";
        case 0x6A85: return "Số dư không đủ";
        case 0x6A80: return "Dữ liệu gửi xuống sai định dạng";
        case 0x6700: return "Sai độ dài dữ liệu (hoặc vượt dung lượng ô nhớ trên thẻ)";
        case 0x6B00: return "Sai P1/P2 (khối không nối tiếp / hết dữ liệu)";
        case 0x6D00: return "Thẻ không hỗ trợ lệnh này (INS)";
        case 0x6E00: return "Sai CLA";
        default: return String.format("Thẻ trả lỗi SW=%04X", sw);
        }
    }

    static int u16(byte[] b, int off) {
        return ((b[off] & 0xFF) << 8) | (b[off + 1] & 0xFF);
    }

    static byte[] ascii(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static byte[] lv(byte[] v) {
        byte[] out = new byte[v.length + 1];
        out[0] = (byte) v.length;
        System.arraycopy(v, 0, out, 1, v.length);
        return out;
    }

    static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] p : parts) {
            o.write(p, 0, p.length);
        }
        return o.toByteArray();
    }
}
