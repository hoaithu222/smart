import com.licel.jcardsim.io.JavaxSmartCardInterface;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javacard.framework.AID;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import sinhvien.StudentCard;
import sinhvien.host.ApduChannel;
import sinhvien.host.AppConfig;
import sinhvien.host.CardRepository;
import sinhvien.host.CardClient;
import sinhvien.host.CardCrypto;
import sinhvien.host.ProfileTlv;

/**
 * Kiểm thử đầu-cuối: applet StudentCard chạy trong jCardSim, máy tính gọi qua CardClient.
 * Chạy: xem code/README.md. Mỗi dòng "OK" là một kịch bản đã đúng.
 */
public class SimTest {

    static int passed = 0;
    static final AID OTHER_AID = new AID(new byte[] {0x11, 0x22, 0x33, 0x44, 0x55, 0x00}, (short) 0, (byte) 6);

    /** Applet thứ hai chỉ để "chọn applet khác" -> JCRE gọi deselect() của StudentCard. */
    public static class OtherApplet extends javacard.framework.Applet {
        public static void install(byte[] b, short o, byte l) {
            new OtherApplet().register();
        }

        public void process(javacard.framework.APDU apdu) {
        }
    }

    /** Kết thúc phiên giống thực tế: chọn applet khác rồi chọn lại applet thẻ sinh viên. */
    static void endSession(JavaxSmartCardInterface sim, AID aid) {
        selectSw(sim, OTHER_AID);
        selectSw(sim, aid);
    }

    public static void main(String[] args) throws Exception {
        final JavaxSmartCardInterface sim = new JavaxSmartCardInterface();
        byte[] aidBytes = CardClient.APPLET_AID;
        AID aid = new AID(aidBytes, (short) 0, (byte) aidBytes.length);

        // Tham số cài đặt giống ô "Application Specific Parameters" của JCIDE: PIN quản trị "87654321"
        byte[] adminPin = "87654321".getBytes(StandardCharsets.US_ASCII);
        byte[] params = new byte[1 + aidBytes.length + 1 + 1 + adminPin.length];
        int p = 0;
        params[p++] = (byte) aidBytes.length;
        System.arraycopy(aidBytes, 0, params, p, aidBytes.length);
        p += aidBytes.length;
        params[p++] = 0;                       // Lc: không có thông tin điều khiển
        params[p++] = (byte) adminPin.length;  // La
        System.arraycopy(adminPin, 0, params, p, adminPin.length);

        sim.installApplet(aid, StudentCard.class, params, (short) 0, (byte) params.length);
        sim.installApplet(OTHER_AID, OtherApplet.class);

        ApduChannel channel = new ApduChannel() {
            public ResponseAPDU transmit(CommandAPDU c) {
                return sim.transmitCommand(c);
            }
        };
        CardClient card = new CardClient(channel);

        // ---- Chọn applet ----
        check("SELECT applet", selectSw(sim, aid) == 0x9000);

        // ---- Trạng thái ban đầu ----
        CardClient.CardStatus st = card.status();
        check("Thẻ mới cài ở trạng thái BLANK", st.state == 0 && st.cardId.isEmpty());

        // ---- Khởi tạo thẻ cần quyền quản trị ----
        expectSw("INIT khi chưa xác thực admin -> 6982", 0x6982, new Step() {
            public void run(CardClient c) throws Exception { c.initCard("CT070254", "123456", "12345678"); }
        }, card);
        expectSw("PIN quản trị sai -> 63C4", 0x63C4, new Step() {
            public void run(CardClient c) throws Exception { c.verifyAdmin("00000000"); }
        }, card);
        card.verifyAdmin("87654321");
        check("PIN quản trị đúng (lấy từ tham số cài đặt)", card.status().adminVerified);

        long t0 = System.currentTimeMillis();
        PublicKey pub = card.initCard("CT070254", "123456", "12345678");
        long genMs = System.currentTimeMillis() - t0;
        RSAPublicKey rsa = (RSAPublicKey) pub;
        check("INIT + sinh cặp khóa RSA trên thẻ (" + rsa.getModulus().bitLength() + " bit, e="
                + rsa.getPublicExponent() + ", " + genMs + " ms)", rsa.getModulus().bitLength() == 1024);
        st = card.status();
        check("Sau INIT: ACTIVE, mã SV = CT070254, PIN còn 3 lần", st.state == 1
                && st.cardId.equals("CT070254") && st.pinTries == 3);
        expectSw("INIT lần 2 bị chặn -> 6985", 0x6985, new Step() {
            public void run(CardClient c) throws Exception { c.initCard("X", "123456", "12345678"); }
        }, card);

        // ---- Xác thực thẻ bằng RSA (challenge-response) ----
        check("Xác thực thẻ: chữ ký hợp lệ với khóa công khai trong CSDL", card.authenticate(pub, "CT070254"));
        check("Xác thực thẻ: sai mã SV thì chữ ký không khớp", !card.authenticate(pub, "CT070999"));

        // ---- PIN ----
        expectSw("Đọc thông tin khi chưa nhập PIN -> 6982", 0x6982, new Step() {
            public void run(CardClient c) throws Exception { c.balance(); }
        }, card);
        expectSw("PIN sai lần 1 -> 63C2", 0x63C2, verifyPin("000000"), card);
        expectSw("PIN sai lần 2 -> 63C1", 0x63C1, verifyPin("111111"), card);
        card.verifyPin("123456");
        check("PIN đúng: bộ đếm sai về lại 3", card.status().pinTries == 3 && card.status().pinVerified);

        // ---- Ghi thông tin (đã mã hóa AES trên thẻ) ----
        Map<Integer, String> info = new LinkedHashMap<Integer, String>();
        info.put(ProfileTlv.MA_SV, "CT070254");
        info.put(ProfileTlv.HO_TEN, "Vũ Thị Hoài Thu");
        info.put(ProfileTlv.NGAY_SINH, "09/10/2002");
        info.put(ProfileTlv.GIOI_TINH, "Nữ");
        info.put(ProfileTlv.LOP, "CT7C");
        info.put(ProfileTlv.KHOA_NGANH, "Công nghệ thông tin");
        info.put(ProfileTlv.KHOA_HOC, "CT7 (2022-2027)");
        info.put(ProfileTlv.HAN_THE, "30/06/2027");
        byte[] infoBytes = ProfileTlv.encode(info);
        card.writeObject(CardClient.OBJ_INFO, infoBytes);

        Map<Integer, String> contact = new LinkedHashMap<Integer, String>();
        contact.put(ProfileTlv.QUE_QUAN, "Tân Triều, Thanh Trì, Hà Nội");
        contact.put(ProfileTlv.SDT, "0900000000");
        card.writeObject(CardClient.OBJ_CONTACT, ProfileTlv.encode(contact));

        byte[] photo = new byte[5000];
        new Random(7).nextBytes(photo);
        card.writeObject(CardClient.OBJ_PHOTO, photo);

        st = card.status();
        Map<Integer, String> infoBack = ProfileTlv.decode(card.readObject(CardClient.OBJ_INFO, st.infoLen));
        check("Đọc lại INFO (tiếng Việt UTF-8) đúng từng trường", infoBack.equals(info));
        check("Đọc lại ảnh 5000 byte qua " + ((st.photoLen + 239) / 240) + " khối, đúng từng byte",
                Arrays.equals(card.readObject(CardClient.OBJ_PHOTO, st.photoLen), photo));

        // ---- Soi bộ nhớ EEPROM của applet: không được thấy dữ liệu rõ ----
        Object applet = appletInstance(aid);
        byte[] eepromInfo = (byte[]) field(applet, "infoData");
        byte[] eepromPhoto = (byte[]) field(applet, "photoData");
        check("EEPROM không chứa họ tên / mã SV dạng rõ (đã mã hóa AES)",
                indexOf(eepromInfo, "Vũ Thị Hoài Thu".getBytes(StandardCharsets.UTF_8)) < 0
                        && indexOf(eepromInfo, "CT7C".getBytes(StandardCharsets.US_ASCII)) < 0
                        && indexOf(eepromPhoto, Arrays.copyOf(photo, 32)) < 0);
        Object ownerPin = field(applet, "pin");
        byte[] pinValue = (byte[]) field(ownerPin, "pinValue");
        check("OwnerPIN chỉ giữ giá trị băm " + pinValue.length + " byte, không có chuỗi 123456",
                indexOf(pinValue, "123456".getBytes(StandardCharsets.US_ASCII)) < 0);

        // ---- Kết thúc phiên ----
        endSession(sim, aid);
        expectSw("Bỏ chọn applet (TH2 kịch bản 1): phiên PIN bị xóa -> phải nhập lại PIN (6982)", 0x6982, new Step() {
            public void run(CardClient c) throws Exception { c.readObject(CardClient.OBJ_INFO, 16); }
        }, card);

        // ---- Phân quyền ghi ----
        card.verifyPin("123456");
        expectSw("Sinh viên tự sửa INFO (không có admin) -> 6982", 0x6982, new Step() {
            public void run(CardClient c) throws Exception { c.writeObject(CardClient.OBJ_INFO, new byte[] {1}); }
        }, card);
        contact.put(ProfileTlv.QUE_QUAN, "Nam Định");
        card.writeObject(CardClient.OBJ_CONTACT, ProfileTlv.encode(contact));
        st = card.status();
        check("Sinh viên tự sửa CONTACT (quê quán) sau khi nhập PIN",
                ProfileTlv.decode(card.readObject(CardClient.OBJ_CONTACT, st.contactLen)).equals(contact));

        // ---- Ví: nạp / thanh toán, mỗi lần đều nhập PIN + thẻ ký RSA ----
        CardClient.Receipt r1 = card.topUp("123456", 500);
        check("Nạp 500 nghìn: số dư 500, bộ đếm 1", r1.balance == 500 && r1.counter == 1);
        check("Biên nhận nạp tiền: chữ ký RSA hợp lệ",
                CardCrypto.verify(pub, CardCrypto.receiptMessage("CT070254", r1), r1.signature));
        CardClient.Receipt r2 = card.pay("123456", 120);
        check("Thanh toán 120 nghìn: số dư 380, bộ đếm 2", r2.balance == 380 && r2.counter == 2);
        r2.value = 12; // giả mạo số tiền trên biên nhận
        check("Sửa số tiền trên biên nhận -> chữ ký không còn hợp lệ",
                !CardCrypto.verify(pub, CardCrypto.receiptMessage("CT070254", r2), r2.signature));

        // ---- CSDL (SQLite trong bộ nhớ, cùng schema với bản thật) ----
        AppConfig cfg = AppConfig.load("host/app.properties");
        check("app.properties: AID trùng applet, giao thức T=0",
                Arrays.equals(cfg.appletAid(), CardClient.APPLET_AID) && cfg.protocol().equals("T=0"));
        CardRepository repo = new CardRepository(java.sql.DriverManager.getConnection("jdbc:sqlite::memory:"));
        repo.runScript(CardRepository.class.getResourceAsStream("db/schema-sqlite.sql"));
        Map<Integer, String> all = new LinkedHashMap<Integer, String>(info);
        all.putAll(contact);
        repo.upsertStudent(all, photo);
        byte[][] parts = card.publicKeyParts();
        repo.saveIssuedCard("CT070254", parts[0], parts[1]);
        check("CSDL: ghi biên nhận nạp tiền (chữ ký đúng, bộ đếm mới)",
                repo.recordReceipt("CT070254", r1, "Quầy thu ngân") == CardRepository.RecordResult.OK);
        check("CSDL: gửi lại đúng biên nhận đó -> REPLAY",
                repo.recordReceipt("CT070254", r1, "Quầy thu ngân") == CardRepository.RecordResult.REPLAY);
        check("CSDL: biên nhận bị sửa số tiền -> BAD_SIGNATURE",
                repo.recordReceipt("CT070254", r2, "Căng tin") == CardRepository.RecordResult.BAD_SIGNATURE);
        r2.value = 120;
        check("CSDL: biên nhận thanh toán thật -> OK, tổng nạp 500, tổng chi 120",
                repo.recordReceipt("CT070254", r2, "Căng tin") == CardRepository.RecordResult.OK
                        && repo.sumByType("TOPUP") == 500 && repo.sumByType("PAY") == 120);
        repo.setCardStatus("CT070254", "LOCKED");
        check("CSDL: thẻ bị khóa trong CSDL -> không lấy được khóa công khai",
                repo.activePublicKey("CT070254") == null);
        repo.setCardStatus("CT070254", "ACTIVE");
        expectSw("Thanh toán vượt số dư -> 6A85", 0x6A85, new Step() {
            public void run(CardClient c) throws Exception { c.pay("123456", 1000); }
        }, card);
        expectSw("Nạp vượt trần 30 triệu -> 6A84", 0x6A84, new Step() {
            public void run(CardClient c) throws Exception { c.topUp("123456", 30000); }
        }, card);
        expectSw("Thanh toán sai PIN -> 63C2", 0x63C2, new Step() {
            public void run(CardClient c) throws Exception { c.pay("123456".replace('6', '0'), 10); }
        }, card);
        card.verifyPin("123456");
        check("Số dư không đổi sau các giao dịch lỗi", card.balance()[0] == 380);

        // ---- Điểm danh ----
        CardClient.Receipt r3 = card.checkIn(0x0203);
        check("Điểm danh phòng 0x0203: chữ ký hợp lệ, bộ đếm 3", r3.counter == 3
                && CardCrypto.verify(pub, CardCrypto.receiptMessage("CT070254", r3), r3.signature));

        List<CardClient.LogEntry> log = card.log();
        check("Nhật ký trên thẻ: 3 bản ghi, mới nhất trước (điểm danh, thanh toán, nạp)",
                log.size() == 3 && log.get(0).type == 3 && log.get(1).type == 2 && log.get(2).type == 1);

        // ---- Đổi PIN: dữ liệu cũ vẫn đọc được (chỉ bọc lại DEK) ----
        card.changePin("123456", "654321");
        endSession(sim, aid);
        expectSw("PIN cũ không còn dùng được -> 63C2", 0x63C2, verifyPin("123456"), card);
        card.verifyPin("654321");
        st = card.status();
        check("Đổi PIN xong vẫn giải mã được INFO cũ",
                ProfileTlv.decode(card.readObject(CardClient.OBJ_INFO, st.infoLen)).equals(info));

        // ---- Khóa PIN và mở bằng PUK ----
        endSession(sim, aid);
        expectSw("Sai PIN -> 63C2", 0x63C2, verifyPin("000000"), card);
        expectSw("Sai PIN -> 63C1", 0x63C1, verifyPin("000000"), card);
        expectSw("Sai PIN lần 3 -> 6983 (khóa PIN)", 0x6983, verifyPin("000000"), card);
        expectSw("PIN đúng nhưng đã khóa -> 6983", 0x6983, verifyPin("654321"), card);
        expectSw("PUK sai -> 63C4", 0x63C4, new Step() {
            public void run(CardClient c) throws Exception { c.unblockPin("00000000", "111111"); }
        }, card);
        card.unblockPin("12345678", "111111");
        card.verifyPin("111111");
        st = card.status();
        check("Mở khóa bằng PUK + PIN mới: dữ liệu mã hóa vẫn còn nguyên",
                st.pinTries == 3 && ProfileTlv.decode(card.readObject(CardClient.OBJ_INFO, st.infoLen)).equals(info));

        // ---- Báo mất thẻ: admin khóa ----
        card.verifyAdmin("87654321");
        card.setLocked(true);
        selectSw(sim, OTHER_AID);
        check("Thẻ bị khóa: SELECT trả 6400 (giống connectCard() của cô)", selectSw(sim, aid) == 0x6400);
        expectSw("Thẻ bị khóa: không nạp tiền được", 0x6400, new Step() {
            public void run(CardClient c) throws Exception { c.topUp("111111", 10); }
        }, card);
        check("Thẻ bị khóa: vẫn đọc được trạng thái", card.status().state == 2);
        card.verifyAdmin("87654321");
        card.setLocked(false);
        check("Admin mở khóa: thẻ hoạt động lại", card.status().state == 1);

        System.out.println();
        System.out.println("TẤT CẢ " + passed + " KỊCH BẢN ĐỀU ĐÚNG");
    }

    // ------------------------------------------------------------------

    static Object appletInstance(AID aid) throws Exception {
        java.lang.reflect.Method g = com.licel.jcardsim.base.SimulatorSystem.class.getDeclaredMethod("getRuntime");
        g.setAccessible(true);
        Object runtime = g.invoke(null);
        java.lang.reflect.Method m = runtime.getClass().getDeclaredMethod("getApplet", AID.class);
        m.setAccessible(true);
        return m.invoke(runtime, aid);
    }

    static Object field(Object o, String name) throws Exception {
        java.lang.reflect.Field f = o.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(o);
    }

    static int indexOf(byte[] hay, byte[] needle) {
        outer:
        for (int i = 0; i + needle.length <= hay.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    interface Step {
        void run(CardClient c) throws Exception;
    }

    static Step verifyPin(final String pin) {
        return new Step() {
            public void run(CardClient c) throws Exception { c.verifyPin(pin); }
        };
    }

    /** jCardSim 2.2.2 không nhận SELECT qua transmitCommand: dùng selectAppletWithResult. */
    static int selectSw(JavaxSmartCardInterface sim, AID aid) {
        byte[] r = sim.selectAppletWithResult(aid);
        return ((r[r.length - 2] & 0xFF) << 8) | (r[r.length - 1] & 0xFF);
    }

    static CommandAPDU select(byte[] aid) {
        return new CommandAPDU(0x00, 0xA4, 0x04, 0x00, aid);
    }

    static int sw(ApduChannel ch, CommandAPDU c) throws Exception {
        return ch.transmit(c).getSW();
    }

    static void expectSw(String name, int expected, Step step, CardClient c) throws Exception {
        try {
            step.run(c);
        } catch (CardClient.CardSwException e) {
            check(name + "  [" + e.getMessage() + "]", e.sw == expected);
            return;
        }
        check(name + "  [không có lỗi]", false);
    }

    static void check(String name, boolean ok) {
        if (!ok) {
            System.out.println("SAI  " + name);
            throw new AssertionError(name);
        }
        passed++;
        System.out.println("OK   " + name);
    }
}
