package thuchanh;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Doi chieu ba applet TH6 voi thu vien mat ma cua Java tren may tinh
 * (chung la hai cai dat doc lap, khop nhau thi applet tinh dung).
 *
 *   java thuchanh.KiemTraMatMa            the gia lap
 *   java thuchanh.KiemTraMatMa --pcsc     JCIDE / the that (cai 3 applet bai6 truoc)
 */
public final class KiemTraMatMa {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static KenhThe the;
    private static int dat;
    private static int sai;

    private KiemTraMatMa() {
    }

    public static void main(String[] args) throws Exception {
        CauHinh c = CauHinh.doc(Paths.get("goi.cfg"));
        boolean pcsc = args.length > 0 && args[0].toLowerCase().contains("pcsc");
        the = pcsc ? new ThePcsc(null) : new TheGiaLap();
        CauHinh.Lop bam = c.tim("bai6_Crypto.HashApplet");
        CauHinh.Lop aes = c.tim("bai6_Crypto.AesApplet");
        CauHinh.Lop rsa = c.tim("bai6_Crypto.RsaApplet");
        for (CauHinh.Lop l : new CauHinh.Lop[] {bam, aes, rsa}) {
            the.cai(l, l.instance, l.param);
        }
        Random ngauNhien = new Random(2026);

        chon(bam);
        kiemBam(ngauNhien);
        chon(aes);
        kiemAes(ngauNhien);
        chon(rsa);
        kiemRsa(ngauNhien);

        System.out.println("Mật mã TH6: " + dat + " đạt, " + sai + " sai");
        System.exit(sai == 0 ? 0 : 1);
    }

    // -------------------------------------------------------------- bam

    private static void kiemBam(Random r) throws Exception {
        String[] ten = {"MD5", "SHA-1", "SHA-256"};
        byte[][] vao = {"abc".getBytes(UTF8), "Fox".getBytes(UTF8),
            "The red fox runs across the ice".getBytes(UTF8), ngau(r, 128), ngau(r, 200), ngau(r, 255)};
        for (int i = 0; i < ten.length; i++) {
            int dung = 0;
            for (byte[] m : vao) {
                byte[] tl = gui(0x00, i + 1, 0, 0, m);
                if (sw(tl) == 0x9000 && Arrays.equals(duLieu(tl), MessageDigest.getInstance(ten[i]).digest(m))) {
                    dung++;
                }
            }
            kiem(ten[i] + ": " + dung + "/" + vao.length + " đầu vào (3..255 byte) khớp máy tính", dung == vao.length);
        }
        // gia tri in tren slide 3
        kiem("SHA-1(\"Fox\") đúng giá trị trên slide 3",
                Arrays.equals(duLieu(gui(0, 2, 0, 0, "Fox".getBytes(UTF8))),
                        Hex.doc("DFCD3454BBEA788A751A696C24D97009CA992D17")));
        kiem("INS lạ -> 6D 00", sw(gui(0, 9, 0, 0, new byte[] {1})) == 0x6D00);
    }

    // -------------------------------------------------------------- AES

    private static void kiemAes(Random r) throws Exception {
        kiem("AES: chưa nạp khóa -> 69 85", sw(gui(0, 0x11, 0, 1, new byte[16])) == 0x6985);
        kiem("AES: khóa 17 byte -> 67 00", sw(gui(0, 0x10, 0, 0, new byte[17])) == 0x6700);

        // vi du FIPS-197 phu luc C
        byte[] ro = Hex.doc("00112233445566778899AABBCCDDEEFF");
        String[] mau = {"69C4E0D86A7B0430D8CDB78070B4C55A", "DDA97CA4864CDFE06EAF70A0EC0D7191",
            "8EA2B7CA516745BFEAFC49904B496089"};
        int[] doDaiKhoa = {16, 24, 32};
        for (int k = 0; k < 3; k++) {
            byte[] khoa = new byte[doDaiKhoa[k]];
            for (int i = 0; i < khoa.length; i++) {
                khoa[i] = (byte) i;
            }
            gui(0, 0x10, 0, 0, khoa);
            kiem("AES-" + (khoa.length * 8) + " khớp ví dụ chuẩn FIPS-197",
                    Arrays.equals(duLieu(gui(0, 0x11, 0, 0, ro)), Hex.doc(mau[k])));

            byte[] k2 = ngau(r, doDaiKhoa[k]);
            gui(0, 0x10, 0, 0, k2);
            int tong = 0;
            int dung = 0;
            for (int cheDo = 0; cheDo <= 1; cheDo++) {
                for (int dem = 0; dem <= 1; dem++) {
                    int[] cacDoDai = dem == 1 ? new int[] {1, 15, 16, 17, 100, 223} : new int[] {16, 32, 128, 240};
                    for (int n : cacDoDai) {
                        tong++;
                        byte[] m = ngau(r, n);
                        byte[] ma = gui(0, 0x11, cheDo, dem, m);
                        byte[] mayTinh = aesMayTinh(k2, cheDo, dem == 1 ? dem9797(m) : m);
                        if (sw(ma) != 0x9000 || !Arrays.equals(duLieu(ma), mayTinh)) {
                            continue;
                        }
                        byte[] lai = gui(0, 0x12, cheDo, dem, mayTinh);
                        if (sw(lai) == 0x9000 && Arrays.equals(duLieu(lai), m)) {
                            dung++;
                        }
                    }
                }
            }
            kiem("AES-" + (doDaiKhoa[k] * 8) + " ECB/CBC, có/không đệm: " + dung + "/" + tong
                    + " lần mã hóa + giải mã khớp máy tính", dung == tong);
        }
        byte[] khongDem = duLieu(gui(0, 0x11, 0, 0, new byte[16]));
        kiem("AES: giải mã dữ liệu đệm sai -> 6A 80", sw(gui(0, 0x12, 0, 1, khongDem)) == 0x6A80);
        kiem("AES: không đệm mà dài 15 byte -> 67 00", sw(gui(0, 0x11, 0, 0, new byte[15])) == 0x6700);
        kiem("AES: P1 sai -> 6A 86", sw(gui(0, 0x11, 2, 0, new byte[16])) == 0x6A86);
    }

    private static byte[] dem9797(byte[] m) {
        byte[] r = Arrays.copyOf(m, (m.length / 16 + 1) * 16);
        r[m.length] = (byte) 0x80;
        return r;
    }

    private static byte[] aesMayTinh(byte[] khoa, int cheDo, byte[] m) throws Exception {
        Cipher c = Cipher.getInstance(cheDo == 0 ? "AES/ECB/NoPadding" : "AES/CBC/NoPadding");
        SecretKeySpec k = new SecretKeySpec(khoa, "AES");
        if (cheDo == 0) {
            c.init(Cipher.ENCRYPT_MODE, k);
        } else {
            c.init(Cipher.ENCRYPT_MODE, k, new IvParameterSpec(new byte[16]));
        }
        return c.doFinal(m);
    }

    // -------------------------------------------------------------- RSA

    private static void kiemRsa(Random r) throws Exception {
        kiem("RSA: chưa sinh khóa -> 69 85", sw(gui(0, 0x21, 0, 0, null)) == 0x6985);
        kiem("RSA: sinh cặp khóa 1024 bit", sw(gui(0, 0x20, 0, 0, null)) == 0x9000);
        byte[] n = duLieu(gui(0, 0x21, 0, 0, null));
        byte[] e = duLieu(gui(0, 0x21, 1, 0, null));
        kiem("RSA: modulus 128 byte, số mũ 01 00 01", n.length == 128 && Arrays.equals(e, Hex.doc("010001")));
        PublicKey pub = KeyFactory.getInstance("RSA").generatePublic(
                new RSAPublicKeySpec(new BigInteger(1, n), new BigInteger(1, e)));

        // challenge-response (slide 10)
        byte[] thuThach = ngau(r, 32);
        byte[] chuKy = duLieu(gui(0, 0x24, 0, 0, thuThach));
        Signature s = Signature.getInstance("SHA1withRSA");
        s.initVerify(pub);
        s.update(thuThach);
        kiem("Challenge-response: máy tính kiểm chữ ký của thẻ -> đúng", chuKy.length == 128 && s.verify(chuKy));
        s.initVerify(pub);
        s.update(ngau(r, 32));
        kiem("Chữ ký không dùng lại được cho thử thách khác", !s.verify(chuKy));

        byte[] thongDiep = ngau(r, 100);
        byte[] ck = duLieu(gui(0, 0x24, 0, 0, thongDiep));
        byte[] goi = Hex.noi(new byte[] {(byte) thongDiep.length}, thongDiep, ck);
        kiem("Thẻ tự kiểm chữ ký đúng -> 01", Arrays.equals(gui(0, 0x25, 0, 0, goi), Hex.doc("019000")));
        goi[5] ^= 1;
        kiem("Sửa 1 bit thông điệp -> 00", Arrays.equals(gui(0, 0x25, 0, 0, goi), Hex.doc("009000")));

        byte[] ro = ngau(r, 117);
        Cipher c = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        c.init(Cipher.ENCRYPT_MODE, pub);
        byte[] giai = gui(0, 0x23, 0, 0, c.doFinal(ro));
        kiem("Máy tính mã hóa bằng khóa công khai, thẻ giải mã đúng", Arrays.equals(duLieu(giai), ro));
        byte[] chao = "Xin chao, KMA".getBytes(UTF8);
        byte[] maThe = duLieu(gui(0, 0x22, 0, 0, chao));
        byte[] lai = duLieu(gui(0, 0x23, 0, 0, maThe));
        // jCardSim 2.2.2 tra du 117 byte (them 00 o cuoi); the that tra dung do dai
        boolean khop = Arrays.equals(lai, chao);
        if (the.laGiaLap() && lai.length == 117) {
            khop = Arrays.equals(lai, Arrays.copyOf(chao, 117));
        }
        kiem("Thẻ mã hóa rồi tự giải mã đúng" + (the.laGiaLap() ? " (giả lập đệm thêm 00)" : ""),
                maThe.length == 128 && khop);
        kiem("Rõ 118 byte (quá giới hạn PKCS#1) -> 67 00", sw(gui(0, 0x22, 0, 0, new byte[118])) == 0x6700);
        kiem("Giải mã dữ liệu rác -> 6A 80", sw(gui(0, 0x23, 0, 0, ngauNhoHon(r, n.length))) == 0x6A80);
    }

    // -------------------------------------------------------------- tien ich

    private static byte[] ngau(Random r, int n) {
        byte[] b = new byte[n];
        r.nextBytes(b);
        return b;
    }

    /** Du lieu rac nho hon modulus (byte dau = 0) de phep RSA thuc hien duoc. */
    private static byte[] ngauNhoHon(Random r, int n) {
        byte[] b = ngau(r, n);
        b[0] = 0;
        return b;
    }

    private static void chon(CauHinh.Lop l) throws Exception {
        byte[] a = l.instance;
        byte[] tl = the.gui(Hex.noi(Hex.doc("00A40400"), new byte[] {(byte) a.length}, a));
        if (sw(tl) != 0x9000) {
            System.out.println("Không chọn được " + l.ten + " (SW " + Integer.toHexString(sw(tl)) + ")");
            System.exit(1);
        }
    }

    private static byte[] gui(int cla, int ins, int p1, int p2, byte[] data) throws Exception {
        byte[] dau = {(byte) cla, (byte) ins, (byte) p1, (byte) p2};
        if (data == null || data.length == 0) {
            return the.gui(Hex.noi(dau, new byte[] {0}));
        }
        return the.gui(Hex.noi(dau, new byte[] {(byte) data.length}, data, new byte[] {0}));
    }

    private static int sw(byte[] r) {
        return ((r[r.length - 2] & 0xFF) << 8) | (r[r.length - 1] & 0xFF);
    }

    private static byte[] duLieu(byte[] r) {
        return Arrays.copyOf(r, r.length - 2);
    }

    private static void kiem(String ten, boolean ok) {
        if (ok) {
            dat++;
        } else {
            sai++;
        }
        System.out.println((ok ? "  [ĐẠT] " : "  [SAI] ") + ten);
    }
}
