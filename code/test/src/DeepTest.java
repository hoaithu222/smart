import com.licel.jcardsim.io.JavaxSmartCardInterface;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import javacard.framework.AID;
import javacard.security.AESKey;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import sinhvien.StudentCard;
import sinhvien.host.ApduChannel;
import sinhvien.host.CardClient;
import sinhvien.host.CardCrypto;
import sinhvien.host.CardRepository;
import sinhvien.host.ProfileTlv;

/**
 * Kiểm thử chuyên sâu applet StudentCard + thư viện máy tính (bổ sung cho SimTest 49 kịch bản).
 * Phủ: biên giá trị, ghi dở dang, fuzz APDU theo từng trạng thái thẻ, đối chiếu mật mã bằng cài đặt
 * độc lập (SHA-256 / AES của JDK), giả mạo biên nhận, khóa admin, bộ đếm giao dịch, CSDL.
 *
 * "OK"  = hành vi đúng đặc tả.   "SAI" = vi phạm đặc tả (làm bộ test thất bại).
 * "PHAT HIEN" = điểm yếu / lỗi thiết kế được chứng minh bằng chính lần chạy này (không làm test thất bại).
 */
public class DeepTest {

    static int pass, fail;
    static final List<String> failures = new ArrayList<String>();
    static final List<String> findings = new ArrayList<String>();
    static String lastErr = "";

    static final byte[] AIDB = CardClient.APPLET_AID;
    static final AID APP = new AID(AIDB, (short) 0, (byte) AIDB.length);
    static final AID OTHER = new AID(new byte[] {0x11, 0x22, 0x33, 0x44, 0x55, 0x00}, (short) 0, (byte) 6);
    static final String ADMIN = "87654321", PIN = "123456", PUK = "12345678";
    static final String ADMIN_PUK = "876543210987";   // PUK quản trị: >= 12 ký tự (applet từ chối ngắn hơn)
    static final int[] KNOWN = {0x10, 0x20, 0x21, 0x22, 0x23, 0x24, 0x30, 0x31, 0x32, 0x33, 0x40, 0x41, 0x50, 0x51, 0x52, 0x53, 0x60};

    public static class OtherApplet extends javacard.framework.Applet {
        public static void install(byte[] b, short o, byte l) {
            new OtherApplet().register();
        }

        public void process(javacard.framework.APDU apdu) {
        }
    }

    interface Op {
        void run() throws Exception;
    }

    // ------------------------------------------------------------------ khung báo cáo

    static void check(String name, boolean ok) {
        if (ok) {
            pass++;
            System.out.println("OK   " + name);
        } else {
            fail++;
            failures.add(name);
            System.out.println("SAI  " + name);
        }
    }

    /** Điểm yếu đã chứng minh: isDefect=true thì ghi nhận PHAT HIEN, false thì coi là đã được thẻ chặn. */
    static void defect(String id, String sev, boolean isDefect, String text) {
        if (isDefect) {
            findings.add("[" + sev + "] " + id + ": " + text);
            System.out.println("PHAT HIEN [" + sev + "] " + id + ": " + text);
        } else {
            pass++;
            System.out.println("OK   (không có lỗi) " + id + ": " + text);
        }
    }

    static void info(String s) {
        System.out.println("     " + s);
    }

    static String hx(int sw) {
        return sw < 0 ? "EXC" : String.format("%04X", sw & 0xFFFF);
    }

    static int swOf(Op op) {
        try {
            op.run();
            return 0x9000;
        } catch (CardClient.CardSwException e) {
            return e.sw;
        } catch (Exception e) {
            lastErr = e.toString();
            return -1;
        }
    }

    static void section(String name, Op body) {
        System.out.println();
        System.out.println("===== " + name);
        long t = System.currentTimeMillis();
        try {
            body.run();
        } catch (Throwable t2) {
            fail++;
            failures.add(name + ": ngoại lệ " + t2);
            System.out.println("SAI  " + name + " - ngoại lệ ngoài dự kiến: " + t2);
            t2.printStackTrace(System.out);
        }
        info("(" + (System.currentTimeMillis() - t) + " ms)");
    }

    // ------------------------------------------------------------------ một thẻ giả lập

    static class Card {
        final JavaxSmartCardInterface sim = new JavaxSmartCardInterface();
        final CardClient c;

        String installErr;

        /** Thẻ có PIN quản trị adminPin VÀ PUK quản trị mặc định ADMIN_PUK (đường khôi phục F-02). */
        Card(String adminPin) {
            this(adminPin, ADMIN_PUK);
        }

        /** adminPuk = null: cài chỉ với PIN quản trị (thẻ không có đường khôi phục). */
        Card(String adminPin, String adminPuk) {
            this(adminData(adminPin, adminPuk), true);
        }

        /** Dữ liệu tham số cài đặt tùy ý (kể cả sai định dạng) - dữ liệu = phần sau byte La. */
        Card(byte[] data, boolean rawData) {
            this(installParams(data, data.length));
        }

        /** Tham số cài đặt nguyên khối [Li][AID][Lc][ctrl][La][dữ liệu] - cho phép giả mạo cả byte La. */
        Card(byte[] params) {
            try {
                sim.installApplet(APP, StudentCard.class, params, (short) 0, (byte) params.length);
            } catch (Throwable t) {
                installErr = t.toString();
            }
            try {
                sim.installApplet(OTHER, OtherApplet.class);
            } catch (Throwable t) {
                // bỏ qua: chỉ để chọn applet khác
            }
            final JavaxSmartCardInterface s = sim;
            c = new CardClient(new ApduChannel() {
                public ResponseAPDU transmit(CommandAPDU x) {
                    return s.transmitCommand(x);
                }
            });
            try {
                sim.selectAppletWithResult(APP);
            } catch (Throwable t) {
                if (installErr == null) {
                    installErr = t.toString();
                }
            }
        }

        /** Applet có cài được và chọn được không. */
        boolean installed() {
            try {
                return installErr == null && selectSw() == 0x9000;
            } catch (Throwable t) {
                return false;
            }
        }

        void endSession() {
            sim.selectAppletWithResult(OTHER);
            sim.selectAppletWithResult(APP);
        }

        int selectSw() {
            byte[] r = sim.selectAppletWithResult(APP);
            return ((r[r.length - 2] & 0xFF) << 8) | (r[r.length - 1] & 0xFF);
        }

        int raw(int ins, int p1, int p2, byte[] data) {
            return raw(0xB0, ins, p1, p2, data);
        }

        int raw(int cla, int ins, int p1, int p2, byte[] data) {
            try {
                // Lc >= 254 kèm Le làm jCardSim tràn bộ đệm APDU (6F00 ngay cả với applet rỗng) -> bỏ Le cho các lệnh dài
                CommandAPDU cmd = (data == null || data.length == 0) ? new CommandAPDU(cla, ins, p1, p2, 256)
                        : data.length >= 254 ? new CommandAPDU(cla, ins, p1, p2, data) : new CommandAPDU(cla, ins, p1, p2, data, 256);
                return sim.transmitCommand(cmd).getSW();
            } catch (Throwable t) {
                lastErr = t.toString();
                return -1;
            }
        }

        Object applet() throws Exception {
            java.lang.reflect.Method g = com.licel.jcardsim.base.SimulatorSystem.class.getDeclaredMethod("getRuntime");
            g.setAccessible(true);
            Object runtime = g.invoke(null);
            java.lang.reflect.Method m = runtime.getClass().getDeclaredMethod("getApplet", AID.class);
            m.setAccessible(true);
            return m.invoke(runtime, APP);
        }
    }

    /** Dữ liệu tham số: PIN quản trị, hoặc PIN | 00 | PUK quản trị. */
    static byte[] adminData(String pin, String puk) {
        byte[] a = pin == null ? new byte[0] : pin.getBytes(StandardCharsets.US_ASCII);
        if (puk == null) {
            return a;
        }
        byte[] b = puk.getBytes(StandardCharsets.US_ASCII);
        byte[] r = new byte[a.length + 1 + b.length];
        System.arraycopy(a, 0, r, 0, a.length);
        System.arraycopy(b, 0, r, a.length + 1, b.length);
        return r;
    }

    /** [Li][AID][Lc=0][La][dữ liệu]; La có thể khác độ dài thật để thử tham số bị cắt. */
    static byte[] installParams(byte[] data, int la) {
        byte[] p = new byte[3 + AIDB.length + data.length];
        int i = 0;
        p[i++] = (byte) AIDB.length;
        System.arraycopy(AIDB, 0, p, i, AIDB.length);
        i += AIDB.length;
        p[i++] = 0;
        p[i++] = (byte) la;
        System.arraycopy(data, 0, p, i, data.length);
        return p;
    }

    /** Thẻ đã phát hành, admin + PIN đã xác thực (đang có phiên). */
    static Card issued(String id) throws Exception {
        Card k = new Card(ADMIN);
        k.c.verifyAdmin(ADMIN);
        k.c.initCard(id, PIN, PUK);
        k.c.verifyPin(PIN);
        return k;
    }

    static Card issued() throws Exception {
        return issued("CT070254");
    }

    static byte[] rnd(int n, long seed) {
        byte[] b = new byte[n];
        new Random(seed).nextBytes(b);
        return b;
    }

    static Object field(Object o, String name) throws Exception {
        Class<?> k = o.getClass();
        while (k != null) {
            try {
                Field f = k.getDeclaredField(name);
                f.setAccessible(true);
                return f.get(o);
            } catch (NoSuchFieldException e) {
                k = k.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }

    static byte[] sha256(int domain, byte[] salt, String secret) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update((byte) domain);
        md.update(salt);
        md.update(secret.getBytes(StandardCharsets.US_ASCII));
        return md.digest();
    }

    static byte[] aes(String mode, int opmode, byte[] key, byte[] iv, byte[] data) throws Exception {
        Cipher c = Cipher.getInstance(mode);
        if (iv == null) {
            c.init(opmode, new SecretKeySpec(key, "AES"));
        } else {
            c.init(opmode, new SecretKeySpec(key, "AES"), new IvParameterSpec(iv));
        }
        return c.doFinal(data);
    }

    static CardClient.Receipt copy(CardClient.Receipt r) {
        CardClient.Receipt c = new CardClient.Receipt();
        c.type = r.type;
        c.value = r.value;
        c.balance = r.balance;
        c.counter = r.counter;
        c.timeAndNonce = r.timeAndNonce.clone();
        c.signature = r.signature.clone();
        return c;
    }

    static boolean verify(PublicKey k, String id, CardClient.Receipt r) throws Exception {
        return CardCrypto.verify(k, CardCrypto.receiptMessage(id, r), r.signature);
    }

    // ================================================================== các phần kiểm thử

    /** 1. Cài đặt + PIN quản trị. */
    static void secAdmin() throws Exception {
        // ---- F-01 (đã sửa): tham số cài đặt bắt buộc, không còn PIN quản trị mặc định ----
        final Card d = new Card(new byte[0], true);
        boolean defaultPinWorks = d.installed() && swOf(new Op() { public void run() throws Exception { d.c.verifyAdmin("12345678"); } }) == 0x9000;
        defect("F-01", "TRUNG BINH", d.installed() || defaultPinWorks,
                "Cài applet KHÔNG có tham số -> PIN quản trị mặc định 12345678 được chấp nhận (thẻ vẫn cài thành công). "
                        + "Quên điền 'Application Specific Parameters' trong JCIDE = thẻ có PIN quản trị công khai.");
        check("Không tham số: lỗi cài đặt, thẻ không tồn tại" + (d.installErr == null ? "" : " [" + shortErr(d.installErr) + "]"), !d.installed());
        check("Tham số có PIN quản trị 3 ký tự -> từ chối cài", !new Card("abc", null).installed());
        check("Tham số có PIN quản trị 17 ký tự -> từ chối cài", !new Card("12345678901234567", null).installed());
        check("PIN quản trị rỗng + PUK (00 đứng đầu) -> từ chối cài", !new Card("", ADMIN_PUK).installed());
        check("PUK quản trị 11 ký tự (ngắn hơn 12) -> từ chối cài", !new Card(ADMIN, "12345678901").installed());
        check("PUK quản trị 17 ký tự -> từ chối cài", !new Card(ADMIN, "12345678901234567").installed());
        check("PUK quản trị rỗng (PIN | 00) -> từ chối cài", !new Card(ADMIN, "").installed());
        byte[] cut = installParams("8765".getBytes(StandardCharsets.US_ASCII), 20);
        check("Byte La lớn hơn dữ liệu thật (tham số bị cắt) -> từ chối cài, không tràn bộ đệm", !new Card(cut).installed());
        byte[] noLa = Arrays.copyOf(installParams(new byte[0], 0), 3 + AIDB.length);
        check("Tham số cụt trước byte La -> từ chối cài", !new Card(noLa).installed());
        Card pinOnly = new Card(ADMIN, null);
        check("Chỉ PIN quản trị (không PUK) vẫn cài được, PIN đúng được nhận", pinOnly.installed()
                && swOf(new Op() { public void run() throws Exception { pinOnly.c.verifyAdmin(ADMIN); } }) == 0x9000);
        Card both = new Card(ADMIN, "0123456789ABCDEF");
        check("PIN + PUK quản trị 16 ký tự (tối đa) cài được", both.installed());

        // ---- F-02 (đã sửa): PIN quản trị bị khóa vẫn khôi phục được bằng PUK quản trị, KHÔNG mất dữ liệu ----
        final Card q = issued();
        q.c.writeObject(2, "lien-he".getBytes(StandardCharsets.UTF_8));
        q.c.topUp(PIN, 200);
        final int balBefore = q.c.balance()[0];
        final int ctrBefore = q.c.balance()[1];
        q.endSession();
        for (int i = 0; i < 5; i++) {
            swOf(new Op() { public void run() throws Exception { q.c.verifyAdmin("00000000"); } });
        }
        check("Sau 5 lần sai, PIN quản trị bị khóa (PIN đúng cũng 6983)", swOf(new Op() { public void run() throws Exception { q.c.verifyAdmin(ADMIN); } }) == 0x6983);
        int worstWrong = 0;
        for (int i = 0; i < 40; i++) {
            int w = swOf(new Op() { public void run() throws Exception { q.c.unblockAdmin("000000000000", "11223344"); } });
            if (w != 0x6300) {
                worstWrong = w;
            }
        }
        check("40 lần PUK quản trị sai liên tiếp: luôn 6300, KHÔNG bị khóa (kẻ cầm thẻ không khóa được đường khôi phục)", worstWrong == 0);
        check("PUK quản trị 3 ký tự -> 6A80", swOf(new Op() { public void run() throws Exception { q.c.unblockAdmin("abc", "11223344"); } }) == 0x6A80);
        check("PIN quản trị mới 3 ký tự -> 6A80 (PIN cũ vẫn bị khóa)", swOf(new Op() { public void run() throws Exception { q.c.unblockAdmin(ADMIN_PUK, "abc"); } }) == 0x6A80
                && swOf(new Op() { public void run() throws Exception { q.c.verifyAdmin(ADMIN); } }) == 0x6983);
        check("PUK quản trị đúng + PIN quản trị mới -> 9000", swOf(new Op() { public void run() throws Exception { q.c.unblockAdmin(ADMIN_PUK, "11223344"); } }) == 0x9000);
        check("Sau mở khóa: chưa có phiên admin (phải VERIFY_ADMIN lại)", !q.c.status().adminVerified);
        check("Sau mở khóa: PIN quản trị CŨ không còn dùng được (63C4)", swOf(new Op() { public void run() throws Exception { q.c.verifyAdmin(ADMIN); } }) == 0x63C4);
        check("Sau mở khóa: PIN quản trị MỚI -> 9000, bộ đếm sai được đặt lại", swOf(new Op() { public void run() throws Exception { q.c.verifyAdmin("11223344"); } }) == 0x9000);
        q.c.verifyPin(PIN);
        check("Dữ liệu thẻ còn nguyên sau khôi phục (số dư, bộ đếm, đối tượng LIEN-HE, thẻ ACTIVE)", q.c.balance()[0] == balBefore && q.c.balance()[1] == ctrBefore
                && new String(q.c.readObject(2, q.c.status().contactLen), StandardCharsets.UTF_8).equals("lien-he") && q.c.status().state == 1);
        check("PIN/PUK sinh viên không đổi (PIN còn 3 lần, PUK còn 5 lần)", q.c.status().pinTries == 3 && q.c.status().pukTries == 5);
        defect("F-02", "CAO (thiết kế)", false, "5 lệnh VERIFY_ADMIN sai khóa PIN quản trị nhưng PUK quản trị khôi phục được, dữ liệu thẻ còn nguyên (balance " + balBefore + ", counter " + ctrBefore + ")");

        // thẻ vừa bị KHÓA vừa bị khóa admin: tổ hợp gây "thẻ chết" trước đây
        final Card lk = issued();
        lk.c.writeObject(2, "lock".getBytes(StandardCharsets.UTF_8));
        lk.c.verifyAdmin(ADMIN);
        lk.c.setLocked(true);
        lk.endSession();
        for (int i = 0; i < 5; i++) {
            swOf(new Op() { public void run() throws Exception { lk.c.verifyAdmin("00000000"); } });
        }
        check("Thẻ LOCKED + admin bị khóa: PIN quản trị đúng vẫn 6983", swOf(new Op() { public void run() throws Exception { lk.c.verifyAdmin(ADMIN); } }) == 0x6983);
        check("Thẻ LOCKED + admin bị khóa: UNBLOCK_ADMIN vẫn chạy được (không bị 6400) -> 9000", swOf(new Op() { public void run() throws Exception { lk.c.unblockAdmin(ADMIN_PUK, "55667788"); } }) == 0x9000);
        lk.c.verifyAdmin("55667788");
        lk.c.setLocked(false);
        lk.c.verifyPin(PIN);
        check("Thẻ LOCKED + admin bị khóa: khôi phục rồi mở khóa thẻ, đọc lại dữ liệu nguyên vẹn",
                new String(lk.c.readObject(2, lk.c.status().contactLen), StandardCharsets.UTF_8).equals("lock"));

        // thẻ cài chỉ có PIN quản trị: vẫn khóa vĩnh viễn như cũ (nêu rõ là điều kiện của bản sửa)
        final Card np = new Card(ADMIN, null);
        for (int i = 0; i < 5; i++) {
            swOf(new Op() { public void run() throws Exception { np.c.verifyAdmin("00000000"); } });
        }
        int npSw = swOf(new Op() { public void run() throws Exception { np.c.unblockAdmin(ADMIN_PUK, "11223344"); } });
        check("Thẻ cài KHÔNG có PUK quản trị: UNBLOCK_ADMIN -> 6985, vẫn khóa admin vĩnh viễn (điều kiện của bản sửa F-02)", npSw == 0x6985);

        final Card k = new Card(ADMIN);
        check("PIN quản trị 3 ký tự -> 6700", swOf(new Op() { public void run() throws Exception { k.c.verifyAdmin("abc"); } }) == 0x6700);
        check("PIN quản trị 17 ký tự -> 6700", swOf(new Op() { public void run() throws Exception { k.c.verifyAdmin("12345678901234567"); } }) == 0x6700);
        int[] exp = {0x63C4, 0x63C3, 0x63C2, 0x63C1, 0x6983};
        for (int i = 0; i < exp.length; i++) {
            int s = swOf(new Op() { public void run() throws Exception { k.c.verifyAdmin("00000000"); } });
            check("PIN quản trị sai lần " + (i + 1) + " -> " + hx(exp[i]) + " (thực tế " + hx(s) + ")", s == exp[i]);
        }
        int right = swOf(new Op() { public void run() throws Exception { k.c.verifyAdmin(ADMIN); } });
        k.endSession();
        int right2 = swOf(new Op() { public void run() throws Exception { k.c.verifyAdmin(ADMIN); } });
        int init = swOf(new Op() { public void run() throws Exception { k.c.initCard("CT1", PIN, PUK); } });
        check("PIN quản trị đúng sau khi bị khóa -> 6983, kể cả sau rút/cắm lại (khóa chống dò PIN vẫn còn)", right == 0x6983 && right2 == 0x6983);
        check("Thẻ trắng + admin bị khóa: chưa phát hành được (6982) cho tới khi khôi phục bằng PUK quản trị", init == 0x6982);
    }

    static String shortErr(String s) {
        return s.length() > 90 ? s.substring(0, 90) + "..." : s;
    }

    /** 2. PIN / PUK. */
    static void secPin() throws Exception {
        final Card b = new Card(ADMIN);
        b.c.verifyAdmin(ADMIN);
        check("Phát hành PIN 3 ký tự -> 6A80", swOf(new Op() { public void run() throws Exception { b.c.initCard("CT1", "123", PUK); } }) == 0x6A80);
        check("Phát hành PUK 17 ký tự -> 6A80", swOf(new Op() { public void run() throws Exception { b.c.initCard("CT1", PIN, "12345678901234567"); } }) == 0x6A80);
        check("Phát hành mã SV rỗng -> 6A80", swOf(new Op() { public void run() throws Exception { b.c.initCard("", PIN, PUK); } }) == 0x6A80);
        check("Phát hành mã SV 17 ký tự -> 6A80", swOf(new Op() { public void run() throws Exception { b.c.initCard("12345678901234567", PIN, PUK); } }) == 0x6A80);
        CardClient.CardStatus st = b.c.status();
        check("Sau 4 lần phát hành lỗi thẻ vẫn BLANK, không có mã SV dở dang", st.state == 0 && st.cardId.isEmpty());
        int sw = swOf(new Op() { public void run() throws Exception { b.c.initCard("CT1", "abcdef", "qwertyui"); } });
        info("PIN/PUK chữ cái (không phải số): thẻ " + (sw == 0x9000 ? "CHẤP NHẬN" : "từ chối " + hx(sw)) + " (chỉ phần mềm bắt nhập số)");

        // Dò PIN: đúng 3 lần thử
        final Card k = issued();
        k.c.writeObject(2, "lien-he".getBytes(StandardCharsets.UTF_8));
        k.endSession();
        int[] exp = {0x63C2, 0x63C1, 0x6983};
        for (int i = 0; i < 3; i++) {
            int s = swOf(new Op() { public void run() throws Exception { k.c.verifyPin("000000"); } });
            check("Dò PIN lần " + (i + 1) + " -> " + hx(exp[i]) + " (thực tế " + hx(s) + ")", s == exp[i]);
        }
        check("PIN đúng sau khi đã khóa -> 6983", swOf(new Op() { public void run() throws Exception { k.c.verifyPin(PIN); } }) == 0x6983);
        check("Đổi PIN khi PIN bị khóa -> 6983", swOf(new Op() { public void run() throws Exception { k.c.changePin(PIN, "654321"); } }) == 0x6983);
        check("Nạp tiền (PIN đúng) khi PIN bị khóa -> 6983", swOf(new Op() { public void run() throws Exception { k.c.topUp(PIN, 10); } }) == 0x6983);

        // PUK sai 5 lần -> khóa thẻ, admin mở
        int[] pexp = {0x63C4, 0x63C3, 0x63C2, 0x63C1, 0x6983};
        for (int i = 0; i < 5; i++) {
            int s = swOf(new Op() { public void run() throws Exception { k.c.unblockPin("00000000", "111111"); } });
            check("PUK sai lần " + (i + 1) + " -> " + hx(pexp[i]) + " (thực tế " + hx(s) + ")", s == pexp[i]);
        }
        check("Hết lượt PUK -> thẻ chuyển LOCKED", k.c.status().state == 2);
        check("Thẻ LOCKED: SELECT -> 6400", k.selectSw() == 0x6400);
        check("Thẻ LOCKED: VERIFY_PIN -> 6400", swOf(new Op() { public void run() throws Exception { k.c.verifyPin(PIN); } }) == 0x6400);
        k.c.verifyAdmin(ADMIN);
        k.c.setLocked(false);
        CardClient.CardStatus s2 = k.c.status();
        check("Admin mở khóa: ACTIVE, PUK về 5 lần, PIN vẫn còn khóa (" + s2.pinTries + ")", s2.state == 1 && s2.pukTries == 5 && s2.pinTries == 0);
        k.c.unblockPin(PUK, "111111");
        k.c.verifyPin("111111");
        CardClient.CardStatus s3 = k.c.status();
        check("Mở PIN bằng PUK: PIN về 3 lần; dữ liệu cũ vẫn giải mã được",
                s3.pinTries == 3 && new String(k.c.readObject(2, s3.contactLen), StandardCharsets.UTF_8).equals("lien-he"));

        // Bộ đếm PUK được trả lại khi nhập đúng
        final Card u = issued();
        u.endSession();
        swOf(new Op() { public void run() throws Exception { u.c.unblockPin("00000000", "222222"); } });
        swOf(new Op() { public void run() throws Exception { u.c.unblockPin("00000000", "222222"); } });
        check("PUK sai 2 lần -> còn 3", u.c.status().pukTries == 3);
        u.c.unblockPin(PUK, "222222");
        check("PUK đúng -> bộ đếm PUK về lại 5", u.c.status().pukTries == 5);

        // Bộ đếm sai dùng chung giữa VERIFY_PIN và giao dịch
        final Card w = issued();
        w.endSession();
        int a = swOf(new Op() { public void run() throws Exception { w.c.pay("000000", 1); } });
        int c2 = swOf(new Op() { public void run() throws Exception { w.c.topUp("000000", 1); } });
        int d = swOf(new Op() { public void run() throws Exception { w.c.verifyPin("000000"); } });
        check("PIN sai ở PAY, TOPUP, VERIFY dùng chung bộ đếm: 63C2, 63C1, 6983 (thực tế " + hx(a) + "," + hx(c2) + "," + hx(d) + ")",
                a == 0x63C2 && c2 == 0x63C1 && d == 0x6983);

        // Đổi PIN lỗi không được làm hỏng dữ liệu
        final Card e = issued();
        e.c.writeObject(2, "du-lieu-quan-trong".getBytes(StandardCharsets.UTF_8));
        check("Đổi PIN sang PIN 3 ký tự -> 6A80", swOf(new Op() { public void run() throws Exception { e.c.changePin(PIN, "123"); } }) == 0x6A80);
        check("Đổi PIN với PIN cũ sai -> 63C2", swOf(new Op() { public void run() throws Exception { e.c.changePin("999999", "654321"); } }) == 0x63C2);
        e.endSession();
        e.c.verifyPin(PIN);
        CardClient.CardStatus s4 = e.c.status();
        check("Sau 2 lần đổi PIN lỗi: PIN cũ còn dùng được, dữ liệu còn nguyên",
                new String(e.c.readObject(2, s4.contactLen), StandardCharsets.UTF_8).equals("du-lieu-quan-trong"));
    }

    /** 3. Đối tượng mã hóa: biên, nối khối, dở dang. */
    static void secObjects() throws Exception {
        final Card k = issued();
        byte[] photo = rnd(5000, 1);
        k.c.writeObject(3, photo);
        for (int p2 : new int[] {137, 138, 200, 255}) {
            int sw = k.raw(0x41, 3, p2, null);
            defect("F-07/P2=" + p2, "THAP", sw != 0x6B00,
                    "READ_OBJ khối P2=" + p2 + " (ngoài dữ liệu) phải trả 6B00, thực tế " + hx(sw)
                            + " (chunk*240 tràn short thành số âm -> vượt kiểm tra 'hết dữ liệu', thẻ bắn ngoại lệ mảng)");
        }
        check("READ_OBJ khối ngay sau cuối dữ liệu (P2=21) -> 6B00", k.raw(0x41, 3, 21, null) == 0x6B00);
        check("READ_OBJ khối 20 (khối cuối) -> 9000", k.raw(0x41, 3, 20, null) == 0x9000);

        // đối tượng sai
        for (int obj : new int[] {0, 4, 0x80, 0xFF}) {
            check("READ_OBJ mã đối tượng " + obj + " -> 6A86", k.raw(0x41, obj, 0, null) == 0x6A86);
            check("WRITE_OBJ mã đối tượng " + obj + " -> 6A86", k.raw(0x40, obj, 0, new byte[16]) == 0x6A86);
        }
        // độ dài sai
        check("WRITE_OBJ 17 byte (không bội 16) -> 6700", k.raw(0x40, 2, 0, new byte[17]) == 0x6700);
        check("WRITE_OBJ 0 byte -> 6700", k.raw(0x40, 2, 0, null) == 0x6700);
        check("WRITE_OBJ khối 2 khi chưa ghi khối 0,1 -> 6B00", k.raw(0x40, 1, 2, new byte[240]) == 0x6B00);
        check("WRITE_OBJ khối 200 (tràn short) khi chưa ghi -> 6B00", k.raw(0x40, 3, 200, new byte[240]) == 0x6B00);

        // dung lượng tối đa = sức chứa - 1 (đệm luôn thêm >= 1 byte)
        check("Ghi INFO 479 byte (tối đa) -> được", swOf(new Op() { public void run() throws Exception { k.c.writeObject(1, rnd(479, 2)); } }) == 0x9000);
        check("Ghi INFO 480 byte -> 6700 (phải <= 479)", swOf(new Op() { public void run() throws Exception { k.c.writeObject(1, rnd(480, 3)); } }) == 0x6700);
        check("Ghi ảnh 7679 byte (tối đa) -> được và đọc lại đúng từng byte", swOf(new Op() { public void run() throws Exception { k.c.writeObject(3, rnd(7679, 4)); } }) == 0x9000
                && Arrays.equals(k.c.readObject(3, k.c.status().photoLen), rnd(7679, 4)));
        check("Ghi ảnh 7680 byte -> 6700", swOf(new Op() { public void run() throws Exception { k.c.writeObject(3, rnd(7680, 5)); } }) == 0x6700);

        // ghi quá dung lượng làm mất dữ liệu cũ
        k.c.writeObject(2, "so-dien-thoai-cu".getBytes(StandardCharsets.UTF_8));
        check("Ghi CONTACT 240 byte -> 6700", swOf(new Op() { public void run() throws Exception { k.c.writeObject(2, rnd(240, 6)); } }) == 0x6700);
        boolean lost;
        try {
            byte[] back = k.c.readObject(2, k.c.status().contactLen);
            lost = !Arrays.equals(back, "so-dien-thoai-cu".getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            lost = true;
        }
        defect("F-06", "THAP", lost, "Ghi đối tượng quá dung lượng bị từ chối nhưng khối 0 đã ghi đè: dữ liệu CŨ mất, đối tượng ở trạng thái dở dang "
                + "(chiều dài " + k.c.status().contactLen + " byte, không giải mã/đệm được). Nên kiểm tra tổng dung lượng trước hoặc ghi vào vùng đệm rồi mới thay.");

        // rút ngắn
        k.c.writeObject(2, rnd(200, 7));
        byte[] shortData = rnd(20, 8);
        k.c.writeObject(2, shortData);
        check("Ghi đè bằng dữ liệu ngắn: độ dài 32 byte đệm, đọc lại đúng", k.c.status().contactLen == 32
                && Arrays.equals(k.c.readObject(2, 32), shortData));
        check("Khối cũ phía sau không còn đọc được (P2=1 -> 6B00)", k.raw(0x41, 2, 1, null) == 0x6B00);

        // tính chất mã hóa
        k.c.writeObject(1, new byte[400]);
        Object ap = k.applet();
        byte[] ee = ((byte[]) field(ap, "infoData")).clone();
        check("INFO 400 byte 0x00: hai khối AES liền nhau khác nhau (CBC, không phải ECB)",
                !Arrays.equals(Arrays.copyOfRange(ee, 0, 16), Arrays.copyOfRange(ee, 16, 32)));
        check("Cùng nội dung ở khối 0 và khối 1 của đối tượng: bản mã khác nhau (IV riêng từng khối)",
                !Arrays.equals(Arrays.copyOfRange(ee, 0, 16), Arrays.copyOfRange(ee, 240, 256)));
        k.c.writeObject(1, new byte[400]);
        byte[] ee2 = ((byte[]) field(ap, "infoData")).clone();
        check("Ghi lại cùng nội dung: bản mã trong EEPROM đổi (IV gốc sinh mới mỗi lần)", !Arrays.equals(Arrays.copyOf(ee, 416), Arrays.copyOf(ee2, 416)));
        byte[] snap = ((byte[]) field(ap, "infoData")).clone();
        k.c.changePin(PIN, "654321");
        check("Đổi PIN không mã hóa lại dữ liệu (bản mã giữ nguyên, chỉ bọc lại DEK)", Arrays.equals(snap, (byte[]) field(ap, "infoData")));
        k.endSession();
        check("Phiên mới: chưa nhập PIN thì đọc -> 6982", k.raw(0x41, 1, 0, null) == 0x6982);
        check("Phiên mới: chưa nhập PIN thì ghi -> 6982", k.raw(0x40, 2, 0, new byte[16]) == 0x6982);
    }

    /** 4. Đối chiếu mật mã bằng cài đặt độc lập của JDK. */
    static void secCrypto() throws Exception {
        final Card k = issued();
        Map<Integer, String> m = new LinkedHashMap<Integer, String>();
        m.put(ProfileTlv.MA_SV, "CT070254");
        m.put(ProfileTlv.HO_TEN, "Vũ Thị Hoài Thu");
        byte[] infoPlain = ProfileTlv.encode(m);
        byte[] photo = rnd(5000, 11);
        k.c.writeObject(1, infoPlain);
        k.c.writeObject(3, photo);

        Object ap = k.applet();
        byte[] salt = ((byte[]) field(ap, "salt")).clone();
        byte[] pinValue = (byte[]) field(field(ap, "pin"), "pinValue");
        byte[] pukValue = (byte[]) field(field(ap, "puk"), "pinValue");
        check("OwnerPIN(PIN) = SHA-256('P' | salt | PIN) (tính độc lập bằng JDK)",
                Arrays.equals(Arrays.copyOf(pinValue, 32), sha256(0x50, salt, PIN)));
        check("OwnerPIN(PUK) = SHA-256('P' | salt | PUK)", Arrays.equals(Arrays.copyOf(pukValue, 32), sha256(0x50, salt, PUK)));

        byte[] dek = new byte[16];
        ((AESKey) field(ap, "dek")).getKey(dek, (short) 0);
        byte[] kekPin = Arrays.copyOf(sha256(0x4B, salt, PIN), 16);
        byte[] kekPuk = Arrays.copyOf(sha256(0x4B, salt, PUK), 16);
        byte[] wPin = (byte[]) field(ap, "dekByPin");
        byte[] wPuk = (byte[]) field(ap, "dekByPuk");
        check("DEK bọc bằng KEK(PIN) = AES-ECB(SHA-256('K'|salt|PIN)[0..16], DEK)", Arrays.equals(aes("AES/ECB/NoPadding", Cipher.ENCRYPT_MODE, kekPin, null, dek), wPin));
        check("DEK bọc bằng KEK(PUK) = AES-ECB(SHA-256('K'|salt|PUK)[0..16], DEK)", Arrays.equals(aes("AES/ECB/NoPadding", Cipher.ENCRYPT_MODE, kekPuk, null, dek), wPuk));
        check("KEK(PIN) khác giá trị kiểm tra PIN (tách miền băm 'K' / 'P')", !Arrays.equals(kekPin, Arrays.copyOf(pinValue, 16)));

        // giải mã EEPROM bằng AES/CBC của JDK, IV = IV gốc xor số khối ở byte cuối
        byte[] ivBase = (byte[]) field(ap, "objIv");
        check("INFO: bản mã EEPROM giải được bằng AES-CBC độc lập ra đúng bản rõ đã đệm", decryptObject(ap, "infoData", 1, ivBase, dek, CardCrypto.pad(infoPlain)));
        check("PHOTO: 21 khối bản mã EEPROM giải được bằng AES-CBC độc lập ra đúng ảnh", decryptObject(ap, "photoData", 3, ivBase, dek, CardCrypto.pad(photo)));

        // Mỗi thẻ có salt riêng, cùng PIN cho băm khác nhau
        byte[] saltA = salt, hashA = Arrays.copyOf(pinValue, 32), dekA = dek.clone();
        PublicKey pubA = k.c.publicKey();
        Card k2 = issued("CT070999");
        Object ap2 = k2.applet();
        byte[] saltB = ((byte[]) field(ap2, "salt")).clone();
        byte[] hashB = Arrays.copyOf((byte[]) field(field(ap2, "pin"), "pinValue"), 32);
        byte[] dekB = new byte[16];
        ((AESKey) field(ap2, "dek")).getKey(dekB, (short) 0);
        PublicKey pubB = k2.c.publicKey();
        java.security.interfaces.RSAPublicKey rb = (java.security.interfaces.RSAPublicKey) pubB;
        check("Khóa RSA 1024 bit, e = 65537; hai thẻ có modulus KHÁC nhau", rb.getModulus().bitLength() == 1024
                && rb.getPublicExponent().intValue() == 65537 && !pubA.equals(pubB));
        boolean sameSalt = Arrays.equals(saltA, saltB);
        defect("SIM-01", "GIOI HAN GIA LAP", sameSalt,
                "jCardSim: RandomData.ALG_SECURE_RANDOM trả CÙNG một dãy cho mọi thẻ mới -> hai thẻ cài riêng có salt trùng (" + CardCrypto.hex(saltA)
                        + "), DEK " + (Arrays.equals(dekA, dekB) ? "CŨNG trùng" : "khác nhau") + ", băm PIN " + (Arrays.equals(hashA, hashB) ? "trùng" : "khác") + " dù PIN giống nhau. "
                        + "Không phải lỗi applet (thẻ thật dùng bộ sinh ngẫu nhiên phần cứng) nhưng KHÔNG được trích 'salt/DEK ngẫu nhiên' làm bằng chứng khi demo trên giả lập.");
        if (!sameSalt) {
            check("Hai thẻ cùng PIN '" + PIN + "': băm PIN khác nhau", !Arrays.equals(hashA, hashB));
        }
        info("Chú ý: salt sinh lúc CÀI applet và giữ nguyên suốt đời thẻ (không đổi khi phát hành lại).");
    }

    static boolean decryptObject(Object ap, String store, int obj, byte[] ivAll, byte[] dek, byte[] paddedPlain) throws Exception {
        byte[] ee = (byte[]) field(ap, store);
        for (int ch = 0; ch * 240 < paddedPlain.length; ch++) {
            int off = ch * 240, len = Math.min(240, paddedPlain.length - off);
            byte[] iv = Arrays.copyOfRange(ivAll, obj * 16, obj * 16 + 16);
            iv[15] ^= (byte) ch;
            byte[] dec = aes("AES/CBC/NoPadding", Cipher.DECRYPT_MODE, dek, iv, Arrays.copyOfRange(ee, off, off + len));
            if (!Arrays.equals(dec, Arrays.copyOfRange(paddedPlain, off, off + len))) {
                return false;
            }
        }
        return true;
    }

    /** 5. Ví, biên nhận, nhật ký, giả mạo. */
    static void secWallet() throws Exception {
        final Card k = issued();
        for (final int bad : new int[] {0, 30001, 40000, 65535}) {
            check("Số tiền " + bad + " -> 6A83", swOf(new Op() { public void run() throws Exception { k.c.topUp(PIN, bad); } }) == 0x6A83);
        }
        k.c.topUp(PIN, 30000);
        check("Nạp đúng trần 30000 -> số dư 30000", k.c.balance()[0] == 30000);
        int before = k.c.balance()[1];
        check("Nạp thêm 1 khi đầy -> 6A84", swOf(new Op() { public void run() throws Exception { k.c.topUp(PIN, 1); } }) == 0x6A84);
        k.c.pay(PIN, 30000);
        check("Trả hết 30000 -> số dư 0", k.c.balance()[0] == 0);
        check("Trả 1 khi số dư 0 -> 6A85", swOf(new Op() { public void run() throws Exception { k.c.pay(PIN, 1); } }) == 0x6A85);
        int[] bal = k.c.balance();
        check("Giao dịch lỗi không đổi bộ đếm/nhật ký (bộ đếm " + bal[1] + ", trước đó " + before + " + 1 giao dịch trả)", bal[1] == before + 1);
        check("Nạp 29999 rồi 1 -> đúng 30000, rồi nạp 1 -> 6A84",
                k.c.topUp(PIN, 29999).balance == 29999 && k.c.topUp(PIN, 1).balance == 30000
                        && swOf(new Op() { public void run() throws Exception { k.c.topUp(PIN, 1); } }) == 0x6A84);

        // PAY/TOPUP không để lại phiên giải mã
        k.endSession();
        k.c.pay(PIN, 5);
        check("Sau PAY (PIN đúng) chưa có phiên dữ liệu: cờ pinVerified = false", !k.c.status().pinVerified);
        check("Sau PAY: đọc số dư vẫn cần VERIFY_PIN (6982)", swOf(new Op() { public void run() throws Exception { k.c.balance(); } }) == 0x6982);

        // nhật ký vòng
        k.c.verifyPin(PIN);
        for (int i = 0; i < 12; i++) {
            k.c.pay(PIN, 1);
        }
        List<CardClient.LogEntry> log = k.c.log();
        boolean ordered = log.size() == 10;
        for (int i = 1; i < log.size(); i++) {
            ordered &= log.get(i - 1).counter == log.get(i).counter + 1;
        }
        check("Nhật ký vòng giữ đúng 10 bản ghi mới nhất, bộ đếm liên tiếp giảm dần", ordered && log.get(0).counter == k.c.balance()[1]);

        // biên nhận
        final Card r = issued();
        PublicKey pub = r.c.publicKey();
        CardClient.Receipt a = r.c.topUp(PIN, 700), b = r.c.pay(PIN, 200), c = r.c.checkIn(0x0203);
        check("Chữ ký RSA hợp lệ: nạp / trả / điểm danh, dài 128 byte", verify(pub, "CT070254", a) && verify(pub, "CT070254", b)
                && verify(pub, "CT070254", c) && a.signature.length == 128);
        boolean allRejected = true;
        String[] names = {"đổi loại nạp->trả", "tăng bộ đếm", "đổi số dư", "đổi số tiền", "đổi mã SV", "đổi 1 bit thời gian", "đổi 1 bit nonce", "đổi 1 bit chữ ký"};
        for (int i = 0; i < names.length; i++) {
            CardClient.Receipt x = copy(a);
            String id = "CT070254";
            switch (i) {
            case 0: x.type = 2; break;
            case 1: x.counter++; break;
            case 2: x.balance++; break;
            case 3: x.value++; break;
            case 4: id = "CT070255"; break;
            case 5: x.timeAndNonce[0] ^= 1; break;
            case 6: x.timeAndNonce[11] ^= 1; break;
            default: x.signature[10] ^= 1; break;
            }
            boolean ok = verify(pub, id, x);
            allRejected &= !ok;
            check("Giả mạo biên nhận (" + names[i] + ") -> chữ ký bị từ chối", !ok);
        }
        check("Mọi kiểu giả mạo đều bị phát hiện", allRejected);

        // Xác thực thẻ không dùng giả làm biên nhận
        for (int len : new int[] {7, 8, 64, 65}) {
            int sw = r.raw(0x32, 0, 0, new byte[len]);
            boolean okLen = len >= 8 && len <= 64;
            check("AUTH_CARD challenge " + len + " byte -> " + (okLen ? "9000" : "6700") + " (thực tế " + hx(sw) + ")", okLen ? sw == 0x9000 : sw == 0x6700);
        }
        byte[] ch = new byte[18];
        ch[0] = 0x00; ch[1] = 0x64;             // số tiền 100
        ch[2] = (byte) 0xFF; ch[3] = (byte) 0xFF; // số dư giả
        ch[5] = 99;                              // bộ đếm giả
        System.arraycopy(rnd(12, 3), 0, ch, 6, 12);
        byte[] sig = r.c.signChallenge(ch);
        CardClient.Receipt forged = new CardClient.Receipt();
        forged.type = 2; forged.value = 100; forged.balance = 0xFFFF; forged.counter = 99; forged.timeAndNonce = Arrays.copyOfRange(ch, 6, 18); forged.signature = sig;
        check("Chữ ký AUTH_CARD không dùng làm biên nhận THANH TOÁN được (tách miền byte đầu 0x41 / 0x02)", !verify(pub, "CT070254", forged));
        forged.type = 0x41;
        boolean cross = verify(pub, "CT070254", forged);
        info("Biên nhận giả loại 0x41 ghép từ chữ ký AUTH_CARD: chữ ký " + (cross ? "HỢP LỆ (thẻ ký hộ)" : "không hợp lệ"));

        // Nạp tiền chỉ cần PIN sinh viên
        final Card s = issued();
        s.endSession(); // bỏ cả phiên PIN lẫn phiên admin
        defect("F-03", "CAO (nghiệp vụ)", swOf(new Op() { public void run() throws Exception { s.c.topUp(PIN, 30000); } }) == 0x9000,
                "NẠP TIỀN chỉ cần PIN của chính sinh viên (không cần thu ngân/PIN quản trị): chủ thẻ tự nạp 30.000 nghìn đồng vào thẻ của mình, "
                        + "thẻ vẫn ký biên nhận hợp lệ. Chỉ lộ ra khi đối chiếu với tiền thu thực ở CSDL.");

        // Điểm danh không cần PIN, làm tràn nhật ký
        final Card f = issued();
        f.c.topUp(PIN, 500);
        f.c.pay(PIN, 100);
        f.endSession();
        for (int i = 0; i < 10; i++) {
            f.c.checkIn(0x0100 + i);
        }
        f.c.verifyPin(PIN);
        List<CardClient.LogEntry> l2 = f.c.log();
        boolean allCheckin = true;
        for (CardClient.LogEntry e : l2) {
            allCheckin &= e.type == 3;
        }
        defect("F-04", "TRUNG BINH", allCheckin,
                "CHECK_IN không cần PIN và không xác thực đầu đọc: bất kỳ ai quẹt thẻ (kể cả không phải chủ) ghi được 10 lần điểm danh với phòng/thời gian tùy ý "
                        + "-> nhật ký vòng 10 ô trên thẻ bị đẩy hết lịch sử nạp/trả; bộ đếm giao dịch cũng tăng theo.");
    }

    /** 6. Vòng đời, trạng thái, mã lệnh. */
    static void secLifecycle() throws Exception {
        final Card b = new Card(ADMIN);
        int[] needActive = {0x21, 0x22, 0x23, 0x31, 0x32, 0x40, 0x41, 0x50, 0x51, 0x52, 0x53, 0x60};
        StringBuilder bad = new StringBuilder();
        for (int ins : needActive) {
            int sw = b.raw(ins, 0, 0, null);
            if (sw != 0x6985) {
                bad.append(String.format(" %02X->%s", ins, hx(sw)));
            }
        }
        check("Thẻ BLANK: 12 lệnh cần thẻ ACTIVE đều trả 6985" + (bad.length() > 0 ? " (lệch:" + bad + ")" : ""), bad.length() == 0);
        check("Thẻ BLANK: SET_LOCK khi chưa admin -> 6982", b.raw(0x33, 1, 0, null) == 0x6982);
        b.c.verifyAdmin(ADMIN);
        check("Thẻ BLANK: SET_LOCK (đã admin) khi chưa phát hành -> 6985", b.raw(0x33, 1, 0, null) == 0x6985);

        final Card k = issued();
        check("INIT lần 2 trên thẻ ACTIVE -> 6985 (không phát hành lại được)", swOf(new Op() { public void run() throws Exception { k.c.initCard("CT2", PIN, PUK); } }) == 0x6985);
        check("SET_LOCK P1=2 -> 6A86", k.raw(0x33, 2, 0, null) == 0x6A86);
        k.c.setLocked(true);
        StringBuilder bad2 = new StringBuilder();
        for (int ins : KNOWN) {
            if (ins == 0x10 || ins == 0x20 || ins == 0x24 || ins == 0x33) {
                continue;
            }
            int sw = k.raw(ins, 0, 0, null);
            if (sw != 0x6400) {
                bad2.append(String.format(" %02X->%s", ins, hx(sw)));
            }
        }
        check("Thẻ LOCKED: 13 lệnh còn lại đều bị 6400" + (bad2.length() > 0 ? " (lệch:" + bad2 + ")" : ""), bad2.length() == 0);
        check("Thẻ LOCKED: GET_STATUS vẫn trả 9000", k.raw(0x10, 0, 0, null) == 0x9000);
        k.endSession();
        check("Rút/cắm lại: phiên admin bị xóa (cờ adminVerified = false)", !k.c.status().adminVerified);

        // CLA / INS
        final Card z = issued();
        for (int cla : new int[] {0x00, 0x80, 0xA0, 0xB1, 0xFF}) {
            check("CLA " + String.format("%02X", cla) + " -> 6E00", z.raw(cla, 0x10, 0, 0, null) == 0x6E00);
        }
        StringBuilder bad3 = new StringBuilder();
        for (int ins = 0; ins < 256; ins++) {
            boolean known = false;
            for (int kn : KNOWN) {
                known |= kn == ins;
            }
            if (known || ins == 0xA4) {
                continue;
            }
            int sw = z.raw(ins, 0, 0, null);
            if (sw != 0x6D00) {
                bad3.append(String.format(" %02X->%s", ins, hx(sw)));
            }
        }
        check("240 mã INS không dùng đều trả 6D00" + (bad3.length() > 0 ? " (lệch:" + bad3 + ")" : ""), bad3.length() == 0);

        // giao thức
        int s1 = z.sim.transmitCommand(new CommandAPDU(0xB0, 0x10, 0, 0)).getSW();
        info("GET_STATUS dạng case-1 (không có Le): SW " + hx(s1));
        int s2;
        try {
            s2 = z.sim.transmitCommand(new CommandAPDU(0xB0, 0x40, 3, 0, new byte[300])).getSW();
        } catch (Throwable t) {
            s2 = -1;
            lastErr = t.toString();
        }
        info("APDU mở rộng (Lc=300; Java Card 2.2.2 và jCardSim không hỗ trợ): " + (s2 < 0 ? "ngoại lệ " + lastErr : hx(s2)) + " -> ứng dụng phải giữ Lc <= 255 (đang dùng tối đa 240)");
    }

    /** 7. Fuzz APDU theo từng trạng thái. */
    static void fuzz(String label, int kind, int perIns, long seed) throws Exception {
        Random rnd = new Random(seed);
        Card k = build(kind);
        List<Integer> inses = new ArrayList<Integer>();
        for (int i : KNOWN) {
            inses.add(i);
        }
        for (int i : new int[] {0x00, 0x01, 0x25, 0x42, 0x70, 0xFF}) {
            inses.add(i);
        }
        int[] p12 = {0, 1, 2, 3, 4, 0x7F, 0x80, 0x89, 0xFF};
        Map<Integer, Integer> hist = new TreeMap<Integer, Integer>();
        List<String> bad = new ArrayList<String>();
        int total = 0, crashes = 0, rebuilds = 0;
        for (int ins : inses) {
            for (int n = 0; n < perIns; n++) {
                if (!healthy(kind, k)) {
                    k = build(kind);
                    rebuilds++;
                }
                int len = rnd.nextInt(10) < 3 ? rnd.nextInt(256) : rnd.nextInt(40);
                byte[] d = new byte[len];
                rnd.nextBytes(d);
                if (len > 0 && rnd.nextBoolean()) {
                    d[0] = (byte) rnd.nextInt(20);
                }
                int p1 = rnd.nextBoolean() ? p12[rnd.nextInt(p12.length)] : rnd.nextInt(256);
                int p2 = rnd.nextBoolean() ? p12[rnd.nextInt(p12.length)] : rnd.nextInt(256);
                int sw = k.raw(ins, p1, p2, d);
                total++;
                Integer old = hist.get(sw);
                hist.put(sw, old == null ? 1 : old + 1);
                if (sw == 0x6F00 || sw == -1) {
                    crashes++;
                    if (bad.size() < 6) {
                        bad.add(String.format("INS=%02X P1=%02X P2=%02X Lc=%d -> %s", ins, p1, p2, len, sw == -1 ? lastErr : "6F00"));
                    }
                }
            }
        }
        defect("FUZZ-" + label, "THAP", crashes > 0, total + " lệnh ngẫu nhiên (" + rebuilds + " lần dựng lại thẻ): "
                + crashes + " lệnh làm applet bắn ngoại lệ chưa bắt (6F00)" + (bad.isEmpty() ? "" : " - ví dụ: " + bad));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : hist.entrySet()) {
            sb.append(hx(e.getKey())).append('x').append(e.getValue()).append(' ');
        }
        info("phân bố SW: " + sb);
    }

    static final int B_BLANK = 0, B_BLANK_ADMIN = 1, B_ACTIVE = 2, B_SESSION = 3, B_LOCKED = 4;

    static Card build(int kind) throws Exception {
        Card k;
        switch (kind) {
        case B_BLANK:
            return new Card(ADMIN);
        case B_BLANK_ADMIN:
            k = new Card(ADMIN);
            k.c.verifyAdmin(ADMIN);
            return k;
        case B_ACTIVE:
            k = issued();
            k.endSession();
            return k;
        case B_SESSION:
            return issued();
        default:
            k = issued();
            k.c.setLocked(true);
            return k;
        }
    }

    static boolean healthy(int kind, Card k) {
        try {
            CardClient.CardStatus s = k.c.status();
            switch (kind) {
            case B_BLANK: return s.state == 0;
            case B_BLANK_ADMIN: return s.state == 0 && s.adminVerified;
            case B_ACTIVE: return s.state == 1 && s.pinTries > 0 && s.pukTries > 0;
            case B_SESSION: return s.state == 1 && s.pinTries > 0 && s.pukTries > 0 && s.pinVerified && s.adminVerified;
            default: return s.state == 2;
            }
        } catch (Exception e) {
            return false;
        }
    }

    /** 8. Phía máy tính: đệm, TLV, CSDL, bộ đếm. */
    static void secHost() throws Exception {
        Random r = new Random(5);
        boolean padOk = true;
        for (int n = 0; n <= 300; n++) {
            byte[] p = new byte[n];
            r.nextBytes(p);
            byte[] padded = CardCrypto.pad(p);
            padOk &= padded.length % 16 == 0 && padded.length > n && Arrays.equals(CardCrypto.unpad(padded), p);
        }
        check("Đệm ISO 9797-1 M2: vòng đi-về đúng cho độ dài 0..300, luôn bội 16 và dài hơn bản rõ", padOk);
        boolean threw = false;
        try {
            CardCrypto.unpad(new byte[32]);
        } catch (IllegalArgumentException e) {
            threw = true;
        }
        check("unpad dữ liệu toàn 0 (sai khóa) -> báo lỗi, không trả rác", threw);
        Map<Integer, String> m = new LinkedHashMap<Integer, String>();
        StringBuilder vn = new StringBuilder();
        for (int i = 0; i < 85; i++) {
            vn.append('ệ');
        }
        m.put(ProfileTlv.QUE_QUAN, vn.toString());
        check("TLV: giá trị UTF-8 đúng 255 byte (85 chữ 'ệ') mã hóa/giải mã đúng", ProfileTlv.decode(ProfileTlv.encode(m)).equals(m));
        m.put(ProfileTlv.QUE_QUAN, vn + "a");
        boolean tlvLong = false;
        try {
            ProfileTlv.encode(m);
        } catch (IllegalArgumentException e) {
            tlvLong = true;
        }
        check("TLV: giá trị 256 byte bị từ chối (không tràn byte độ dài)", tlvLong);
        boolean tlvTrunc = false;
        try {
            ProfileTlv.decode(new byte[] {0x01, 0x05, 0x41});
        } catch (IllegalArgumentException e) {
            tlvTrunc = true;
        }
        check("TLV: dữ liệu bị cắt cụt -> báo lỗi", tlvTrunc);
        byte[] dup = {0x01, 0x01, 0x41, 0x01, 0x01, 0x42};
        info("TLV: tag trùng lặp -> giữ giá trị sau cùng ('" + ProfileTlv.decode(dup).get(1) + "'), không báo lỗi");

        // CSDL SQLite trong bộ nhớ
        CardRepository repo = new CardRepository(java.sql.DriverManager.getConnection("jdbc:sqlite::memory:"));
        repo.runScript(CardRepository.class.getResourceAsStream("db/schema-sqlite.sql"));
        check("Đăng nhập admin / admin@123 đúng", repo.login("admin", "admin@123") != null);
        check("SQL injection ở tên đăng nhập (admin' OR '1'='1) -> từ chối", repo.login("admin' OR '1'='1", "x") == null);
        check("SQL injection ở mật khẩu (' OR '1'='1) -> từ chối", repo.login("admin", "' OR '1'='1") == null);
        boolean plain = false;
        try (java.sql.Statement st = repo.connection().createStatement(); java.sql.ResultSet rs = st.executeQuery("SELECT * FROM nguoi_dung")) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    String v = String.valueOf(rs.getObject(i));
                    plain |= v.equals("admin@123") || v.equals("thungan@123") || v.equals("gv@123");
                }
            }
        }
        check("Bảng nguoi_dung không chứa mật khẩu rõ (chỉ băm + salt)", !plain);

        // Biên nhận <-> CSDL
        final Card k = issued();
        Map<Integer, String> stu = new LinkedHashMap<Integer, String>();
        stu.put(ProfileTlv.MA_SV, "CT070254");
        stu.put(ProfileTlv.HO_TEN, "Thu");
        stu.put(ProfileTlv.NGAY_SINH, "09/10/2002");
        stu.put(ProfileTlv.GIOI_TINH, "Nu");
        stu.put(ProfileTlv.LOP, "CT7C");
        stu.put(ProfileTlv.KHOA_NGANH, "CNTT");
        stu.put(ProfileTlv.KHOA_HOC, "CT7");
        stu.put(ProfileTlv.HAN_THE, "30/06/2027");
        stu.put(ProfileTlv.QUE_QUAN, "Ha Noi");
        stu.put(ProfileTlv.SDT, "0900000000");
        repo.upsertStudent(stu, new byte[] {1, 2, 3});
        check("Tìm sinh viên bằng 'CT0702' -> thấy 1 người", repo.listStudents("CT0702").size() == 1);
        check("SQL injection ở ô tìm sinh viên (%' OR 1=1 --) -> không trả toàn bảng", repo.listStudents("%' OR 1=1 --").isEmpty());
        byte[][] pk = k.c.publicKeyParts();
        repo.saveIssuedCard("CT070254", pk[0], pk[1]);
        CardClient.Receipt c1 = k.c.topUp(PIN, 100), c2 = k.c.pay(PIN, 10), c3 = k.c.topUp(PIN, 20);
        check("Ghi biên nhận counter 1 -> OK", repo.recordReceipt("CT070254", c1, "Quầy") == CardRepository.RecordResult.OK);
        check("Ghi biên nhận counter 3 (bỏ qua counter 2) -> OK", repo.recordReceipt("CT070254", c3, "Quầy") == CardRepository.RecordResult.OK);
        CardRepository.RecordResult late = repo.recordReceipt("CT070254", c2, "Căng tin");
        defect("F-08", "THAP", late == CardRepository.RecordResult.REPLAY,
                "Biên nhận THẬT nhưng đến muộn (counter 2 sau counter 3) bị coi là REPLAY và mất vĩnh viễn: chi tiêu offline / thu ngân nhiều quầy ghi không theo thứ tự sẽ rớt giao dịch. "
                        + "Thẻ có nhật ký 10 ô nhưng phần mềm không có chức năng đồng bộ lại.");
        CardClient.Receipt other = copy(c1);
        check("Ghi cùng biên nhận lần 2 -> REPLAY", repo.recordReceipt("CT070254", other, "Quầy") == CardRepository.RecordResult.REPLAY);
        check("Biên nhận mã SV khác (CT070999) -> không có thẻ", repo.recordReceipt("CT070999", c3, "Quầy") == CardRepository.RecordResult.UNKNOWN_CARD);
        repo.setCardStatus("CT070254", "LOCKED");
        check("Thẻ báo mất trong CSDL: biên nhận mới -> bị từ chối (UNKNOWN_CARD)", repo.recordReceipt("CT070254", k.c.topUp(PIN, 5), "Quầy") == CardRepository.RecordResult.UNKNOWN_CARD);
        repo.setCardStatus("CT070254", "ACTIVE");

        // Biên nhận giả loại 0x41: ghép từ chữ ký AUTH_CARD (thẻ ký hộ, không cần PIN) rồi nộp vào CSDL
        byte[] fch = new byte[18];
        fch[0] = 0x02; fch[1] = 0x03;                                  // "mã phòng" 0x0203
        fch[4] = (byte) (60000 >> 8); fch[5] = (byte) 60000;             // bộ đếm giả 60000
        System.arraycopy(rnd(12, 21), 0, fch, 6, 12);
        CardClient.Receipt forged = new CardClient.Receipt();
        forged.type = 0x41;
        forged.value = 0x0203;
        forged.balance = 0;
        forged.counter = 60000;
        forged.timeAndNonce = Arrays.copyOfRange(fch, 6, 18);
        forged.signature = k.c.signChallenge(fch);
        int counterBefore = repo.lastCounter("CT070254");
        CardRepository.RecordResult fr = repo.recordReceipt("CT070254", forged, "Giả");
        CardClient.Receipt legit = k.c.checkIn(0x0203);
        CardRepository.RecordResult lr = repo.recordReceipt("CT070254", legit, "Quầy");
        check("F-10: biên nhận loại 0x41 -> BAD_TYPE (thực tế " + fr + "), bo_dem_cuoi giữ nguyên rồi biên nhận thật kế tiếp -> OK (thực tế " + lr + ")",
                fr == CardRepository.RecordResult.BAD_TYPE && lr == CardRepository.RecordResult.OK && repo.lastCounter("CT070254") == legit.counter
                        && counterBefore < legit.counter);
        for (int bad : new int[] {0, 4, 0x41, 0x7F, 0xFF}) {
            CardClient.Receipt t = copy(legit);
            t.type = bad;
            t.counter = legit.counter + 1;
            check("F-10: biên nhận type " + bad + " -> BAD_TYPE", repo.recordReceipt("CT070254", t, "Giả") == CardRepository.RecordResult.BAD_TYPE);
        }
        defect("F-10", "TRUNG BINH", fr == CardRepository.RecordResult.OK && lr == CardRepository.RecordResult.REPLAY,
                "recordReceipt không kiểm tra loại biên nhận thuộc {1,2,3}: chữ ký AUTH_CARD (thẻ ký challenge tùy ý, KHÔNG cần PIN) ghép thành biên nhận loại 0x41 "
                        + "được CSDL nhận (" + fr + ") và ghi như một lần ĐIỂM DANH (typeName mặc định 'CHECKIN'), đồng thời đẩy bo_dem_cuoi lên " + repo.lastCounter("CT070254")
                        + " nên biên nhận thật kế tiếp (counter " + legit.counter + ") bị " + lr + ". Cần kiểm tra type và bộ đếm tăng hợp lý.");

        // Bộ đếm 16 bit tràn
        Object ap = k.applet();
        Field f = ap.getClass().getDeclaredField("txCounter");
        f.setAccessible(true);
        f.setShort(ap, (short) 0xFFFD);
        int[] seq = new int[4];
        for (int i = 0; i < 4; i++) {
            seq[i] = k.c.checkIn(0x0203).counter;
        }
        info("Bộ đếm quanh điểm tràn: " + Arrays.toString(seq));
        CardClient.Receipt w1 = k.c.checkIn(0x0203);
        // đưa lastCounter của CSDL tới 65535 rồi thử biên nhận counter 0
        repo.connection().createStatement().executeUpdate("UPDATE the SET bo_dem_cuoi = 65535 WHERE ma_sv = 'CT070254'");
        defect("F-09", "THAP", w1.counter < 65535 && repo.recordReceipt("CT070254", w1, "Quầy") == CardRepository.RecordResult.REPLAY,
                "Bộ đếm giao dịch 16 bit quay vòng 65535 -> 0: sau đó mọi biên nhận THẬT đều bị CSDL coi là REPLAY (cần thẻ ~65.5 nghìn giao dịch, "
                        + "nhưng điểm danh/F-04 làm tăng bộ đếm nhanh hơn dự kiến). Nên dùng bộ đếm 32 bit hoặc quy tắc so sánh có tràn.");
    }

    /** 9. Thời gian xử lý (jCardSim chạy trên PC; thẻ thật chậm hơn nhiều). */
    static void secTiming() throws Exception {
        Card k = new Card(ADMIN);
        k.c.verifyAdmin(ADMIN);
        long t = System.nanoTime();
        k.c.initCard("CT070254", PIN, PUK);
        long init = (System.nanoTime() - t) / 1000000;
        k.c.verifyPin(PIN);
        t = System.nanoTime();
        for (int i = 0; i < 50; i++) {
            k.c.status();
        }
        long st = (System.nanoTime() - t) / 50000;
        t = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            k.c.topUp(PIN, 1);
        }
        long tx = (System.nanoTime() - t) / 20000000;
        t = System.nanoTime();
        k.c.writeObject(3, rnd(7000, 9));
        long wr = (System.nanoTime() - t) / 1000000;
        t = System.nanoTime();
        k.c.readObject(3, k.c.status().photoLen);
        long rd = (System.nanoTime() - t) / 1000000;
        info("Trên jCardSim/PC: INIT+sinh RSA-1024 " + init + " ms; GET_STATUS " + st + " µs; TOPUP (băm PIN + RSA ký) " + tx + " ms; ghi ảnh 7 KB " + wr + " ms; đọc ảnh " + rd + " ms");
        info("Thẻ Java Card thật: RSA-1024 sinh khóa thường 1-10 giây, ký 50-200 ms, truyền T=0 ~10 KB/s -> ghi ảnh 7 KB cỡ vài giây.");
    }

    public static void main(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();
        section("1. Cài đặt + PIN quản trị", new Op() { public void run() throws Exception { secAdmin(); } });
        section("2. PIN / PUK / khóa thẻ", new Op() { public void run() throws Exception { secPin(); } });
        section("3. Đối tượng mã hóa AES: biên, nối khối, dở dang", new Op() { public void run() throws Exception { secObjects(); } });
        section("4. Đối chiếu mật mã bằng cài đặt độc lập (JDK)", new Op() { public void run() throws Exception { secCrypto(); } });
        section("5. Ví, biên nhận RSA, nhật ký, giả mạo", new Op() { public void run() throws Exception { secWallet(); } });
        section("6. Vòng đời thẻ, mã lệnh, giao thức", new Op() { public void run() throws Exception { secLifecycle(); } });
        final String[] names = {"BLANK", "BLANK+admin", "ACTIVE-không-phiên", "ACTIVE+PIN+admin", "LOCKED"};
        for (int kind = 0; kind < names.length; kind++) {
            final int kk = kind;
            section("7." + (kind + 1) + " Fuzz APDU - thẻ " + names[kind], new Op() { public void run() throws Exception { fuzz(names[kk], kk, 40, 100 + kk); } });
        }
        section("8. Phía máy tính: đệm, TLV, CSDL, bộ đếm", new Op() { public void run() throws Exception { secHost(); } });
        section("9. Thời gian xử lý", new Op() { public void run() throws Exception { secTiming(); } });

        System.out.println();
        System.out.println("==================== TỔNG KẾT ====================");
        System.out.println("Kiểm tra đúng: " + pass + "   Kiểm tra SAI: " + fail + "   Điểm yếu phát hiện: " + findings.size()
                + "   (" + (System.currentTimeMillis() - t0) / 1000 + " s)");
        for (String s : failures) {
            System.out.println("  SAI: " + s);
        }
        for (String s : findings) {
            System.out.println("  " + s);
        }
        System.exit(fail == 0 ? 0 : 1);
    }
}
