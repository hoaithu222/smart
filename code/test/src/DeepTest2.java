import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import javacard.security.AESKey;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import sinhvien.host.ApduChannel;
import sinhvien.host.CardClient;
import sinhvien.host.CardCrypto;
import sinhvien.host.ProfileTlv;

/**
 * Kiểm thử chuyên sâu PHẦN 2 - bổ sung cho DeepTest (không thay thế). Phủ các vùng DeepTest chưa có:
 *  1. reset nguồn (không qua deselect)         5. cô lập 3 đối tượng, ghi dở dang, độ dài chặt
 *  2. dò PIN/PUK offline từ bản sao EEPROM     6. phía máy tính nhận phản hồi bị cắt / dài sai
 *  3. PIN trùng PUK                            7. kiểm thử theo MÔ HÌNH: hàng nghìn thao tác ngẫu nhiên,
 *  4. toàn vẹn dữ liệu mã hóa (không có MAC)      mỗi thao tác đối chiếu với mô hình Java viết độc lập.
 * Dùng lại thẻ giả lập, hàm phụ của DeepTest. "OK" = đúng; "SAI" = vi phạm (làm test thất bại);
 * "PHAT HIEN" = điểm yếu được chứng minh (không làm test thất bại).
 */
public class DeepTest2 {

    static int pass, fail;
    static final List<String> failures = new ArrayList<String>();
    static final List<String> findings = new ArrayList<String>();
    static final String PIN = DeepTest.PIN, PUK = DeepTest.PUK, ADMIN = DeepTest.ADMIN;

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

    static void section(String name, DeepTest.Op body) {
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

    static String hx(int sw) {
        return DeepTest.hx(sw);
    }

    static byte[] cat(byte[]... parts) {
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream();
        for (byte[] p : parts) {
            o.write(p, 0, p.length);
        }
        return o.toByteArray();
    }

    // ================================================================== 1. reset nguồn

    static void secReset() throws Exception {
        final DeepTest.Card k = DeepTest.issued();
        k.c.writeObject(2, "bi-mat".getBytes(StandardCharsets.UTF_8));
        k.c.topUp(PIN, 700);
        CardClient.CardStatus before = k.c.status();
        check("Trước reset: đang có phiên PIN và phiên admin", before.pinVerified && before.adminVerified);

        k.sim.reset();                                   // rút nguồn / cắm lại: KHÔNG gọi deselect()
        k.sim.selectAppletWithResult(DeepTest.APP);
        CardClient.CardStatus after = k.c.status();
        boolean simClears = !after.pinVerified && !after.adminVerified;
        boolean dekLeft = ((AESKey) DeepTest.field(k.applet(), "dek")).isInitialized();
        defect("SIM-02", "GIOI HAN GIA LAP", !simClears,
                "jCardSim.reset() KHÔNG xóa phiên: sau reset + chọn lại applet, cờ PIN (pinVerified=" + after.pinVerified + ") và admin (adminVerified=" + after.adminVerified
                        + ") vẫn còn, khóa DEK " + (dekLeft ? "vẫn khởi tạo" : "đã xóa") + ", đọc/ghi đối tượng không bị chặn. Trên thẻ thật JCRE xóa OwnerPIN và khóa "
                        + "TRANSIENT khi reset và applet dựa hoàn toàn vào cơ chế đó (không tự xóa trong process()), nên đây là giới hạn của jCardSim chứ không phải lỗi applet. "
                        + "HỆ QUẢ: không chứng minh được 'rút nguồn thì mất phiên' bằng giả lập (đường deselect() đã đạt ở DeepTest mục 6) - phải thử trên JCIDE/thẻ thật.");
        if (simClears) {
            check("Sau reset nguồn: đọc đối tượng -> 6982", k.raw(0x41, 2, 0, null) == 0x6982);
            check("Sau reset nguồn: ghi đối tượng -> 6982", k.raw(0x40, 2, 0, new byte[16]) == 0x6982);
            check("Sau reset nguồn: đọc số dư -> 6982", k.raw(0x50, 0, 0, null) == 0x6982);
            check("Sau reset nguồn: xem nhật ký -> 6982", k.raw(0x53, 0, 0, null) == 0x6982);
            check("Sau reset nguồn: lệnh quản trị (P1=2 không phá hủy) -> 6982", k.raw(0x33, 2, 0, null) == 0x6982);
        }
        k.c.verifyPin(PIN);
        CardClient.CardStatus s2 = k.c.status();
        check("Sau reset nguồn: dữ liệu EEPROM và số dư còn nguyên",
                new String(k.c.readObject(2, s2.contactLen), StandardCharsets.UTF_8).equals("bi-mat") && k.c.balance()[0] == 700);
    }

    // ================================================================== 2. dò PIN/PUK offline

    /** Dò PIN/PUK toàn số từ bản sao EEPROM (salt + giá trị băm). Trả về chuỗi tìm được, hoặc null. */
    static String bruteForce(byte[] salt, byte[] target, int digits, int maxTries) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] in = new byte[1 + 16 + digits];
        in[0] = 0x50;
        System.arraycopy(salt, 0, in, 1, 16);
        for (int n = 0; n < maxTries; n++) {
            int v = n;
            for (int i = digits - 1; i >= 0; i--) {
                in[17 + i] = (byte) ('0' + v % 10);
                v /= 10;
            }
            md.update(in);
            if (Arrays.equals(md.digest(), target)) {
                return new String(in, 17, digits, StandardCharsets.US_ASCII);
            }
        }
        return null;
    }

    static void secOffline() throws Exception {
        final DeepTest.Card k = DeepTest.issued();
        Map<Integer, String> m = new LinkedHashMap<Integer, String>();
        m.put(ProfileTlv.MA_SV, "CT070254");
        m.put(ProfileTlv.HO_TEN, "Vũ Thị Hoài Thu");
        byte[] infoPlain = ProfileTlv.encode(m);
        k.c.writeObject(1, infoPlain);

        // "bản sao EEPROM" = những gì kẻ lấy được thẻ đọc ra bằng thiết bị chuyên dụng
        Object ap = k.applet();
        byte[] salt = ((byte[]) DeepTest.field(ap, "salt")).clone();
        byte[] pinHash = Arrays.copyOf((byte[]) DeepTest.field(DeepTest.field(ap, "pin"), "pinValue"), 32);
        byte[] pukHash = Arrays.copyOf((byte[]) DeepTest.field(DeepTest.field(ap, "puk"), "pinValue"), 32);
        byte[] wrappedByPin = ((byte[]) DeepTest.field(ap, "dekByPin")).clone();
        byte[] ivAll = ((byte[]) DeepTest.field(ap, "objIv")).clone();

        long t = System.nanoTime();
        String pin = bruteForce(salt, pinHash, 6, 1000000);
        long ms = (System.nanoTime() - t) / 1000000;
        check("Dò PIN 6 chữ số từ bản sao EEPROM: tìm ra '" + (pin == null ? "?" : pin) + "' (đối chiếu đúng PIN đã đặt)", PIN.equals(pin));
        info("Dò toàn bộ 10^6 PIN 6 số tốn tối đa " + Math.max(ms, 1) + " ms (mỗi lần thử 1 phép SHA-256, không có làm chậm)");

        // từ PIN tìm được -> KEK -> DEK -> giải mã INFO, hoàn toàn không cần thẻ
        byte[] kek = Arrays.copyOf(DeepTest.sha256(0x4B, salt, pin), 16);
        byte[] dek = DeepTest.aes("AES/ECB/NoPadding", javax.crypto.Cipher.DECRYPT_MODE, kek, null, wrappedByPin);
        boolean plainOk = DeepTest.decryptObject(ap, "infoData", 1, ivAll, dek, CardCrypto.pad(infoPlain));
        defect("F-11", "CAO (thiết kế, cần chạm EEPROM)", plainOk,
                "Kẻ có bản sao EEPROM của thẻ (không cần biết PIN) dò hết 10^6 PIN 6 số trong ~" + Math.max(ms, 1) + " ms rồi giải mã được họ tên/ảnh: "
                        + "bí mật chỉ được che bằng 1 lần SHA-256 có salt, KHÔNG có hàm làm chậm (PBKDF2/lặp), PIN 6 số chỉ ~20 bit. Tính bảo mật dữ liệu khi mất thẻ "
                        + "= độ mạnh của PIN, không phải AES-128. Giảm nhẹ: thẻ thật khó đọc EEPROM; yêu cầu PIN dài/chữ-số-hỗn-hợp; ghi rõ trong báo cáo.");

        // PUK 8 số: tính tốc độ rồi ước lượng; PUK của thẻ test là 12345678 nên dò thật được trong vài giây
        t = System.nanoTime();
        String puk = bruteForce(salt, pukHash, 8, 12345679);
        long pms = (System.nanoTime() - t) / 1000000;
        check("Dò PUK 8 chữ số từ bản sao EEPROM: tìm ra PUK đã đặt", PUK.equals(puk));
        double rate = 12345679.0 / Math.max(pms, 1);
        info(String.format("Tốc độ dò ~%.0f nghìn khóa/s trên 1 luồng PC -> toàn bộ 10^8 PUK 8 số cỡ %.0f giây", rate, 1e8 / Math.max(rate, 1) / 1000.0));
    }

    // ================================================================== 3. PIN trùng PUK / PIN yếu

    static void secWeakSecrets() throws Exception {
        final DeepTest.Card b = new DeepTest.Card(ADMIN);
        b.c.verifyAdmin(ADMIN);
        int sw = DeepTest.swOf(() -> b.c.initCard("CT1", "123456", "123456"));
        boolean same = false;
        if (sw == 0x9000) {
            Object ap = b.applet();
            same = Arrays.equals((byte[]) DeepTest.field(ap, "dekByPin"), (byte[]) DeepTest.field(ap, "dekByPuk"));
        }
        defect("F-12", "THAP", sw == 0x9000,
                "Thẻ chấp nhận PIN trùng PUK (123456 / 123456)" + (same ? ": hai bản bọc DEK giống hệt nhau, PUK không còn là đường khôi phục độc lập" : "")
                        + ". Ngoài ra thẻ chỉ kiểm độ dài 4..16, không chặn PIN yếu (0000, 1234, 111111). Chặn ở phần mềm/UI là đủ nhưng applet không tự bảo vệ.");
        for (String weak : new String[] {"0000", "1111", "1234"}) {
            final DeepTest.Card w = new DeepTest.Card(ADMIN);
            w.c.verifyAdmin(ADMIN);
            final String wp = weak;
            check("PIN yếu '" + weak + "' được thẻ chấp nhận (chính sách PIN nằm ở phần mềm)", DeepTest.swOf(() -> w.c.initCard("CT1", wp, "99887766")) == 0x9000);
        }
    }

    // ================================================================== 4. toàn vẹn dữ liệu

    static void secIntegrity() throws Exception {
        final DeepTest.Card k = DeepTest.issued();
        byte[] data = new byte[100];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ('A' + i % 26);
        }
        k.c.writeObject(1, data);
        int len = k.c.status().infoLen;                       // 112 = 7 khối AES
        byte[] ee = (byte[]) DeepTest.field(k.applet(), "infoData");
        byte[] clean = k.c.readObject(1, len);
        check("Đọc lại đúng trước khi sửa EEPROM", Arrays.equals(clean, data));

        ee[0] ^= 0x01;                                         // kẻ có quyền ghi EEPROM lật 1 bit bản mã khối 0
        byte[] bad = null;
        String thrown = null;
        try {
            bad = k.c.readObject(1, len);
        } catch (Exception e) {
            thrown = e.toString();
        }
        boolean silent = bad != null;
        boolean controlledFlip = silent && bad.length == data.length && bad[16] == (byte) (data[16] ^ 0x01);
        boolean b0garbage = silent && !Arrays.equals(Arrays.copyOf(bad, 16), Arrays.copyOf(data, 16));
        defect("F-13", "THAP (cần ghi được EEPROM)", silent,
                "Dữ liệu mã hóa AES-CBC KHÔNG có MAC/kiểm toàn vẹn: lật 1 bit bản mã khối 0 -> thẻ vẫn trả dữ liệu, không báo lỗi; khối 0 thành rác"
                        + (b0garbage ? "" : "(?)") + " và khối 1 bị lật ĐÚNG bit đó" + (controlledFlip ? " (kẻ tấn công kiểm soát được)" : "")
                        + ". Chỉ phát hiện khi byte đệm cuối bị phá. Hoán đổi/ghép khối giữa các đối tượng cũng không bị phát hiện." + (thrown != null ? " [đã bị báo: " + thrown + "]" : ""));
        ee[0] ^= 0x01;                                         // trả lại
        check("Trả bit về: đọc lại đúng như cũ", Arrays.equals(k.c.readObject(1, len), data));
    }

    // ================================================================== 5. cô lập đối tượng + ghi dở dang + độ dài chặt

    static byte[] lv(byte[] v) {
        return cat(new byte[] {(byte) v.length}, v);
    }

    static void secIsolation() throws Exception {
        final DeepTest.Card k = DeepTest.issued();
        byte[] info = DeepTest.rnd(300, 31), contact = DeepTest.rnd(100, 32), photo = DeepTest.rnd(3000, 33);
        k.c.writeObject(1, info);
        k.c.writeObject(2, contact);
        k.c.writeObject(3, photo);
        Object ap = k.applet();
        byte[] eInfo = ((byte[]) DeepTest.field(ap, "infoData")).clone();
        byte[] ePhoto = ((byte[]) DeepTest.field(ap, "photoData")).clone();
        boolean intact = true;
        for (int round = 0; round < 8; round++) {
            byte[] c2 = DeepTest.rnd(1 + round * 29, 40 + round);        // 1..204 byte, nhiều độ dài khác nhau
            k.c.writeObject(2, c2);
            intact &= Arrays.equals(eInfo, (byte[]) DeepTest.field(ap, "infoData")) && Arrays.equals(ePhoto, (byte[]) DeepTest.field(ap, "photoData"));
            CardClient.CardStatus s = k.c.status();
            intact &= Arrays.equals(k.c.readObject(2, s.contactLen), c2);
        }
        check("Ghi đè CONTACT 8 lần với độ dài khác nhau: bản mã INFO/PHOTO trong EEPROM không đổi 1 byte", intact);
        CardClient.CardStatus fin = k.c.status();
        check("Sau đó INFO và PHOTO vẫn đọc ra đúng từng byte", Arrays.equals(k.c.readObject(1, fin.infoLen), info) && Arrays.equals(k.c.readObject(3, fin.photoLen), photo));

        // nối khối sai đối tượng / nhảy khối
        check("Ghi CONTACT khối 0 rồi ghi PHOTO khối 1 (khác đối tượng) -> 6B00", k.raw(0x40, 2, 0, new byte[16]) == 0x9000 && k.raw(0x40, 3, 1, new byte[16]) == 0x6B00);
        check("Ghi INFO khối 0 rồi nhảy sang khối 2 -> 6B00", k.raw(0x40, 1, 0, new byte[240]) == 0x9000 && k.raw(0x40, 1, 2, new byte[16]) == 0x6B00);
        check("Ghi INFO khối 1 lặp lại (khối 1 hai lần liên tiếp) -> 6B00", k.raw(0x40, 1, 1, new byte[16]) == 0x9000 && k.raw(0x40, 1, 1, new byte[16]) == 0x6B00);

        // ghi dở dang: mới ghi khối 0 của ảnh (240 byte) rồi dừng, phần mềm đọc ra
        k.c.writeObject(3, DeepTest.rnd(2000, 50));
        check("Ghi dở: chỉ ghi khối 0 của PHOTO", k.raw(0x40, 3, 0, new byte[240]) == 0x9000);
        CardClient.CardStatus half = k.c.status();
        check("Ghi dở: thẻ báo độ dài PHOTO = 240 (đối tượng 'trông hợp lệ' nhưng thiếu đệm)", half.photoLen == 240);
        String res;
        try {
            k.c.readObject(3, half.photoLen);
            res = "đọc được (không đệm?)";
        } catch (IllegalArgumentException e) {
            res = "IllegalArgumentException: " + e.getMessage();
        } catch (Exception e) {
            res = e.toString();
        }
        info("readObject trên đối tượng ghi dở -> " + res + " (phần mềm phải bắt IllegalArgumentException để báo 'dữ liệu hỏng, ghi lại')");

        // độ dài chặt của lệnh giao dịch / điểm danh; lỗi độ dài không được trừ lượt PIN
        byte[] tn = DeepTest.rnd(12, 60);
        byte[] good = cat(lv(PIN.getBytes(StandardCharsets.US_ASCII)), new byte[] {0, 1}, tn);
        check("TOPUP đúng độ dài -> 9000", k.raw(0x51, 0, 0, good) == 0x9000);
        check("TOPUP thừa 1 byte -> 6700", k.raw(0x51, 0, 0, cat(good, new byte[] {0})) == 0x6700);
        check("TOPUP thiếu 1 byte -> 6700", k.raw(0x51, 0, 0, Arrays.copyOf(good, good.length - 1)) == 0x6700);
        byte[] wrongPin = cat(lv("000000".getBytes(StandardCharsets.US_ASCII)), new byte[] {0, 1}, tn);
        check("PAY sai PIN nhưng thừa byte -> 6700 và KHÔNG trừ lượt PIN", k.raw(0x52, 0, 0, cat(wrongPin, new byte[] {0})) == 0x6700 && k.c.status().pinTries == 3);
        check("CHECK_IN 13 byte -> 6700", k.raw(0x60, 0, 0, new byte[13]) == 0x6700);
        check("CHECK_IN 14 byte -> 9000", k.raw(0x60, 0, 0, new byte[14]) == 0x9000);
        check("CHECK_IN 15 byte -> 6700", k.raw(0x60, 0, 0, new byte[15]) == 0x6700);
        check("Trường LV của PIN dài 17 (vượt 16) trong TOPUP -> 6A80", k.raw(0x51, 0, 0, cat(new byte[] {17}, new byte[17], new byte[14])) == 0x6A80);
        check("Trường LV của PIN dài 3 (dưới 4) trong TOPUP -> 6A80", k.raw(0x51, 0, 0, cat(new byte[] {3}, new byte[3], new byte[14])) == 0x6A80);
    }

    // ================================================================== 6. phía máy tính nhận phản hồi sai

    interface Call {
        void run(CardClient c) throws Exception;
    }

    /** Kênh bọc thẻ thật nhưng cắt / kéo dài phần dữ liệu trả về (mô phỏng đầu đọc lỗi, thẻ lỗi, kẻ chen giữa). */
    static class FaultChannel implements ApduChannel {
        final DeepTest.Card k;
        int len = -1;          // -1 = nguyên vẹn
        int realLen = -1;
        final Random rnd = new Random(77);

        FaultChannel(DeepTest.Card k) {
            this.k = k;
        }

        public ResponseAPDU transmit(CommandAPDU c) {
            ResponseAPDU r = k.sim.transmitCommand(c);
            if (r.getSW() != 0x9000) {
                return r;
            }
            byte[] d = r.getData();
            realLen = d.length;
            if (len < 0) {
                return r;
            }
            byte[] out = Arrays.copyOf(d, len);
            if (len > d.length) {
                byte[] g = new byte[len - d.length];
                rnd.nextBytes(g);
                System.arraycopy(g, 0, out, d.length, g.length);
            }
            return new ResponseAPDU(cat(out, new byte[] {(byte) 0x90, 0x00}));
        }
    }

    static void secHostFault() throws Exception {
        final DeepTest.Card k = DeepTest.issued();
        k.c.writeObject(2, "du-lieu".getBytes(StandardCharsets.UTF_8));
        k.c.topUp(PIN, 100);                                      // chừa chỗ cho ~140 lần nạp thử mà không chạm trần 30000
        final PublicKey pub = k.c.publicKey();
        final FaultChannel fc = new FaultChannel(k);
        final CardClient c = new CardClient(fc);
        Map<String, Call> calls = new LinkedHashMap<String, Call>();
        calls.put("status", x -> x.status());
        calls.put("publicKey", x -> x.publicKey());
        calls.put("balance", x -> x.balance());
        calls.put("topUp", x -> x.topUp(PIN, 1));
        calls.put("pay", x -> x.pay(PIN, 1));
        calls.put("checkIn", x -> x.checkIn(0x0203));
        calls.put("log", x -> x.log());
        calls.put("readObject", x -> x.readObject(2, 16));
        calls.put("signChallenge", x -> x.signChallenge(new byte[16]));
        calls.put("authenticate", x -> x.authenticate(pub, "CT070254"));

        List<String> offenders = new ArrayList<String>();
        for (Map.Entry<String, Call> e : calls.entrySet()) {
            fc.len = -1;
            e.getValue().run(c);                                  // chạy nguyên vẹn để biết độ dài thật
            int real = fc.realLen;
            Map<String, Integer> hist = new TreeMap<String, Integer>();
            int silent = 0, total = 0;
            for (int len = 0; len <= 140; len++) {
                if (len == real) {
                    continue;
                }
                fc.len = len;
                total++;
                String cls;
                try {
                    e.getValue().run(c);
                    cls = "OK";
                    if (len < real) {
                        silent++;
                    }
                } catch (CardClient.CardSwException x) {
                    cls = "CardSw";
                } catch (CardException x) {
                    cls = "CardException";
                } catch (GeneralSecurityException x) {
                    cls = "GeneralSecurity";
                } catch (IllegalArgumentException x) {
                    cls = x.getMessage() != null && x.getMessage().contains("đệm") ? "IAE(đệm,có chủ đích)" : "IllegalArgumentException";
                } catch (RuntimeException x) {
                    cls = x.getClass().getSimpleName();
                }
                Integer old = hist.get(cls);
                hist.put(cls, old == null ? 1 : old + 1);
            }
            fc.len = -1;
            int uncontrolled = 0;
            for (Map.Entry<String, Integer> h : hist.entrySet()) {
                if (!h.getKey().equals("OK") && !h.getKey().equals("CardSw") && !h.getKey().equals("CardException")
                        && !h.getKey().equals("GeneralSecurity") && !h.getKey().startsWith("IAE(")) {
                    uncontrolled += h.getValue();
                }
            }
            info(String.format("%-14s dài thật %3d byte; thử %d độ dài sai -> %s; nhận cụt mà không báo lỗi: %d", e.getKey(), real, total, hist, silent));
            if (uncontrolled > 0) {
                offenders.add(e.getKey() + "(" + uncontrolled + "/" + total + ")");
            }
        }
        defect("F-14", "THAP", !offenders.isEmpty(),
                "CardClient không kiểm độ dài phản hồi của thẻ: khi đầu đọc/thẻ trả dữ liệu ngắn (SW vẫn 9000) các hàm " + offenders
                        + " ném RuntimeException (ArrayIndexOutOfBounds / StringIndexOutOfBounds) thay vì CardException. Giao diện KHÔNG sập vì mọi lệnh thẻ đi qua Task.run "
                        + "(bắt Exception chung) nhưng người dùng thấy thông báo kỹ thuật kiểu 'ArrayIndexOutOfBoundsException: Index 5...'. "
                        + "Cũng nhận receipt có chữ ký cụt và publicKey cụt (đệm 0) mà không báo lỗi - chỉ bị chặn muộn ở bước kiểm chữ ký. Nên kiểm r.length trước khi đọc.");
    }

    // ================================================================== 7. kiểm thử theo mô hình

    static class Model {
        int bal, ctr, tries = 3, pukTries = 5;
        boolean dek = true, val = true;
        String pin = PIN;
        final LinkedList<int[]> log = new LinkedList<int[]>();      // {loại, giá trị, số dư sau, bộ đếm}

        boolean session() {
            return dek && val;
        }

        void append(int type, int value) {
            log.addFirst(new int[] {type, value & 0xFFFF, bal, ctr});
            if (log.size() > 10) {
                log.removeLast();
            }
        }

        /** Kết quả mong đợi của một lệnh có kiểm PIN (VERIFY / TOPUP / PAY); cập nhật mô hình. */
        int checkPin(String given) {
            if (tries == 0) {
                return 0x6983;
            }
            if (!given.equals(pin)) {
                tries--;
                val = false;
                return tries == 0 ? 0x6983 : (0x63C0 | tries);
            }
            tries = 3;
            val = true;
            return 0x9000;
        }

        int transfer(boolean topup, String given, int amount) {
            int sw = checkPin(given);
            if (sw != 0x9000) {
                return sw;
            }
            short a = (short) amount;
            if (a <= 0 || a > 30000) {
                return 0x6A83;
            }
            if (topup) {
                if (bal > 30000 - a) {
                    return 0x6A84;
                }
                bal += a;
            } else {
                if (a > bal) {
                    return 0x6A85;
                }
                bal -= a;
            }
            ctr++;
            append(topup ? 1 : 2, a);
            return 0x9000;
        }
    }

    static final int[] AMOUNTS = {1, 5, 100, 5000, 29999, 30000, 30001, 0, 65535, 40000, 32768, 32767, 2, 777};

    static void modelRun(long seed, int ops, int[] totals) throws Exception {
        final Random rnd = new Random(seed);
        final DeepTest.Card k = DeepTest.issued();
        final Model m = new Model();
        final PublicKey pub = k.c.publicKey();
        final List<String> bad = new ArrayList<String>();
        int receiptsChecked = 0, blockedHits = 0, unblocks = 0;
        for (int i = 0; i < ops; i++) {
            int op = rnd.nextInt(100);
            if (m.tries == 0 && rnd.nextInt(10) < 3) {
                op = 97;                                             // thỉnh thoảng mở khóa PIN bằng PUK
            }
            final String given = rnd.nextInt(100) < 80 ? m.pin : (m.pin.equals("000000") ? "111111" : "000000");
            final int amt = rnd.nextInt(4) == 0 ? 1 + rnd.nextInt(2000) : AMOUNTS[rnd.nextInt(AMOUNTS.length)];
            String desc;
            int exp, act;
            if (op < 50) {
                final boolean topup = op < 25;
                desc = (topup ? "TOPUP " : "PAY ") + amt + " pin=" + (given.equals(m.pin) ? "đúng" : "sai") + " (còn " + m.tries + ")";
                final int bBal = m.bal;
                exp = m.transfer(topup, given, amt);
                CardClient.Receipt[] rc = new CardClient.Receipt[1];
                act = DeepTest.swOf(() -> rc[0] = topup ? k.c.topUp(given, amt) : k.c.pay(given, amt));
                if (exp == 0x9000 && act == 0x9000) {
                    boolean ok = rc[0].balance == m.bal && rc[0].counter == m.ctr && rc[0].type == (topup ? 1 : 2) && DeepTest.verify(pub, "CT070254", rc[0]);
                    receiptsChecked++;
                    if (!ok) {
                        bad.add("#" + i + " biên nhận " + desc + ": thẻ số dư " + rc[0].balance + "/bộ đếm " + rc[0].counter + " <> mô hình " + m.bal + "/" + m.ctr + " (trước " + bBal + ")");
                    }
                }
            } else if (op < 60) {
                final int room = rnd.nextInt(65536);
                desc = "CHECK_IN " + room;
                exp = 0x9000;
                m.ctr++;
                m.append(3, room);
                CardClient.Receipt[] rc = new CardClient.Receipt[1];
                act = DeepTest.swOf(() -> rc[0] = k.c.checkIn(room));
                if (act == 0x9000) {
                    receiptsChecked++;
                    if (rc[0].counter != m.ctr || !DeepTest.verify(pub, "CT070254", rc[0])) {
                        bad.add("#" + i + " biên nhận điểm danh sai (bộ đếm " + rc[0].counter + " <> " + m.ctr + ")");
                    }
                }
            } else if (op < 75) {
                desc = "VERIFY_PIN pin=" + (given.equals(m.pin) ? "đúng" : "sai") + " (còn " + m.tries + ")";
                exp = m.checkPin(given);
                if (exp == 0x9000) {
                    m.dek = true;
                }
                act = DeepTest.swOf(() -> k.c.verifyPin(given));
            } else if (op < 85) {
                desc = "BALANCE (phiên " + m.session() + ")";
                exp = m.session() ? 0x9000 : 0x6982;
                int[][] out = new int[1][];
                act = DeepTest.swOf(() -> out[0] = k.c.balance());
                if (act == 0x9000 && exp == 0x9000 && (out[0][0] != m.bal || out[0][1] != m.ctr || out[0][2] != m.log.size())) {
                    bad.add("#" + i + " BALANCE thẻ " + Arrays.toString(out[0]) + " <> mô hình [" + m.bal + ", " + m.ctr + ", " + m.log.size() + "]");
                }
            } else if (op < 90) {
                desc = "GET_LOG (phiên " + m.session() + ")";
                exp = m.session() ? 0x9000 : 0x6982;
                List<CardClient.LogEntry> out = new ArrayList<CardClient.LogEntry>();
                act = DeepTest.swOf(() -> out.addAll(k.c.log()));
                if (act == 0x9000 && exp == 0x9000) {
                    boolean same = out.size() == m.log.size();
                    for (int j = 0; same && j < out.size(); j++) {
                        int[] w = m.log.get(j);
                        CardClient.LogEntry e = out.get(j);
                        same = e.type == w[0] && e.value == w[1] && e.balance == w[2] && e.counter == w[3];
                    }
                    if (!same) {
                        bad.add("#" + i + " nhật ký thẻ (" + out.size() + " bản ghi) <> mô hình (" + m.log.size() + " bản ghi)");
                    }
                }
            } else if (op < 94) {
                desc = "NGẮT PHIÊN (select applet khác rồi chọn lại)";
                exp = 0x9000;
                m.dek = false;
                m.val = false;
                k.endSession();
                act = 0x9000;
            } else if (op < 97) {
                desc = "GET_STATUS";
                exp = 0x9000;
                CardClient.CardStatus[] st = new CardClient.CardStatus[1];
                act = DeepTest.swOf(() -> st[0] = k.c.status());
                if (act == 0x9000 && (st[0].pinTries != m.tries || st[0].pinVerified != m.session() || st[0].pukTries != m.pukTries)) {
                    bad.add("#" + i + " STATUS thẻ PIN còn " + st[0].pinTries + " / phiên " + st[0].pinVerified + " <> mô hình " + m.tries + " / " + m.session());
                }
            } else {
                final String np = String.format("%06d", rnd.nextInt(1000000));
                desc = "UNBLOCK bằng PUK -> PIN mới " + np + " (PIN còn " + m.tries + ")";
                exp = 0x9000;
                m.tries = 3;
                m.val = false;
                m.dek = false;
                m.pin = np;
                m.pukTries = 5;
                unblocks++;
                act = DeepTest.swOf(() -> k.c.unblockPin(PUK, np));
            }
            if (exp == 0x6983 && act == 0x6983) {
                blockedHits++;
            }
            if (exp != act) {
                bad.add("#" + i + " " + desc + ": mong đợi " + hx(exp) + ", thẻ trả " + hx(act));
            }
        }
        totals[0] += ops;
        totals[1] += bad.size();
        totals[2] += receiptsChecked;
        totals[3] += blockedHits;
        totals[4] += unblocks;
        check("Mô hình, seed " + seed + ": " + ops + " thao tác ngẫu nhiên khớp 100% (" + receiptsChecked + " biên nhận ký đúng, " + blockedHits + " lần chặn 6983, "
                + unblocks + " lần mở PIN bằng PUK, bộ đếm cuối " + m.ctr + ")" + (bad.isEmpty() ? "" : " - LỆCH: " + bad.subList(0, Math.min(6, bad.size()))), bad.isEmpty());
    }

    static void secModel() throws Exception {
        int[] totals = new int[5];
        for (long seed = 1; seed <= 6; seed++) {
            modelRun(seed, 700, totals);
        }
        info("Tổng: " + totals[0] + " thao tác, " + totals[1] + " lệch mô hình, " + totals[2] + " biên nhận được kiểm chữ ký, " + totals[3] + " lần chặn 6983, " + totals[4] + " lần mở PIN");
    }

    // ================================================================== main

    public static void main(String[] args) throws Exception {
        long t0 = System.currentTimeMillis();
        section("1. Reset nguồn (không qua deselect)", () -> secReset());
        section("2. Dò PIN/PUK offline từ bản sao EEPROM", () -> secOffline());
        section("3. PIN trùng PUK, PIN yếu", () -> secWeakSecrets());
        section("4. Toàn vẹn dữ liệu mã hóa (sửa bit trong EEPROM)", () -> secIntegrity());
        section("5. Cô lập đối tượng, ghi dở dang, độ dài chặt", () -> secIsolation());
        section("6. Phía máy tính: phản hồi của thẻ bị cắt/dài sai", () -> secHostFault());
        section("7. Kiểm thử theo mô hình (thao tác ngẫu nhiên có đối chiếu)", () -> secModel());

        System.out.println();
        System.out.println("==================== TỔNG KẾT (PHẦN 2) ====================");
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
