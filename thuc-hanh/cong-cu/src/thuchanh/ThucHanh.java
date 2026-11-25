package thuchanh;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Debug Shell" cho bai thuc hanh: chay kich ban .apdu (co kiem ket qua) hoac go lenh tay,
 * tren the gia lap hoac qua PC/SC (the ao JCIDE / the that).
 *
 *   java thuchanh.ThucHanh [--pcsc[=ten dau doc]] [kich_ban.apdu ... | goi.Lop]
 */
public final class ThucHanh {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern TOKEN = Pattern.compile("\"([^\"]*)\"|(\\S+)");

    private final CauHinh cauHinh;
    private final KenhThe kenh;
    private BufferedReader banPhim;
    private int dat;
    private int sai;
    private int chiTheThat;
    private final List<String> loi = new ArrayList<String>();

    private ThucHanh(CauHinh cauHinh, KenhThe kenh) {
        this.cauHinh = cauHinh;
        this.kenh = kenh;
    }

    public static void main(String[] args) throws Exception {
        CauHinh c = CauHinh.doc(Paths.get("goi.cfg"));
        boolean pcsc = false;
        String dauDoc = null;
        List<String> viec = new ArrayList<String>();
        for (String a : args) {
            if (a.equalsIgnoreCase("--pcsc") || a.equalsIgnoreCase("pcsc")) {
                pcsc = true;
            } else if (a.toLowerCase().startsWith("--pcsc=")) {
                pcsc = true;
                dauDoc = a.substring(7);
            } else {
                viec.add(a);
            }
        }
        KenhThe kenh;
        try {
            kenh = pcsc ? new ThePcsc(dauDoc) : new TheGiaLap();
        } catch (javax.smartcardio.CardException e) {
            System.out.println(e.getMessage());
            System.exit(2);
            return;
        }
        ThucHanh t = new ThucHanh(c, kenh);

        List<Path> kichBan = new ArrayList<Path>();
        String lop = null;
        for (String v : viec) {
            if (v.endsWith(".apdu")) {
                kichBan.add(Paths.get(v));
            } else {
                lop = v;
            }
        }
        if (!kichBan.isEmpty()) {
            for (Path p : kichBan) {
                t.chayTep(p);
            }
            t.tongKet(kichBan.size());
            System.exit(t.sai == 0 ? 0 : 1);
        }
        if (lop != null) {
            String ten = c.tim(lop).ten;
            t.lenh("/cai " + ten);
            t.lenh("/select " + ten);
        }
        t.tuongTac();
    }

    private void tuongTac() throws IOException {
        System.out.println();
        System.out.println("Gõ APDU (ví dụ: 00 01 00 00) hoặc /help. /thoat để thoát.");
        while (true) {
            System.out.print("APDU> ");
            System.out.flush();
            String s = banPhim().readLine();
            if (s == null) {
                break;
            }
            s = s.replace("﻿", "").trim();
            if (s.equals("/thoat") || s.equals("/quit") || s.equals("/exit")) {
                break;
            }
            lenh(s);
        }
    }

    private void chayTep(Path p) throws Exception {
        System.out.println();
        System.out.println("================ " + p.getFileName() + " (" + (kenh.laGiaLap() ? "thẻ giả lập" : "PC/SC") + ")");
        if (!Files.isRegularFile(p)) {
            System.out.println("Không thấy tệp " + p);
            sai++;
            loi.add("Không thấy tệp " + p);
            return;
        }
        List<String> dong = new ArrayList<String>();
        for (String s : Files.readAllLines(p, UTF8)) {
            dong.add(s.replace("﻿", "").trim());
        }
        if (!kenh.theMoi()) {
            choCaiTrongJcide(dong, 0);
        }
        for (int i = 0; i < dong.size(); i++) {
            String t = dong.get(i);
            if (t.equals("/moi") && !kenh.laGiaLap()) {
                choCaiTrongJcide(dong, i + 1);
            } else if (!lenh(t)) {
                loi.add(p.getFileName() + " dòng " + (i + 1) + ": " + t);
            }
        }
    }

    /** PC/SC: liet ke applet cua phan bat dau tu dong 'tu' (den /moi ke tiep) va cho nguoi dung cai xong. */
    private void choCaiTrongJcide(List<String> dong, int tu) throws Exception {
        System.out.println();
        System.out.println(">> Phần tiếp theo cần cài MỚI trong JCIDE các applet sau:");
        for (int i = tu; i < dong.size() && !dong.get(i).equals("/moi"); i++) {
            if (dong.get(i).startsWith("/cai ")) {
                lenh(dong.get(i));
            }
        }
        System.out.print(">> Cài xong, bấm Run, đợi 'You can send apdu with PC/SC interface now' rồi bấm Enter... ");
        System.out.flush();
        banPhim().readLine();
        System.out.println();
        ((ThePcsc) kenh).ketNoiLai();
    }

    private BufferedReader banPhim() {
        if (banPhim == null) {
            banPhim = new BufferedReader(new InputStreamReader(System.in, UTF8));
        }
        return banPhim;
    }

    /** Thuc hien mot dong lenh. Tra false neu lenh loi hoac ket qua khong dung. */
    private boolean lenh(String s) {
        try {
            if (s.isEmpty()) {
                return true;
            }
            if (s.startsWith("##")) {
                System.out.println();
                System.out.println("--- " + s.substring(2).trim());
                return true;
            }
            if (s.startsWith("#")) {
                System.out.println("    " + s.substring(1).trim());
                return true;
            }
            if (!s.startsWith("/")) {
                return gui(s);
            }
            String[] t = s.split("\\s+", 2);
            String doi = t.length > 1 ? t[1].trim() : "";
            String ten = t[0].toLowerCase();
            if (ten.equals("/send")) {
                return gui(doi);
            }
            if (ten.equals("/select")) {
                byte[] aid = aidCua(doi);
                return gui("00 A4 04 00 " + Hex.in(new byte[] {(byte) aid.length}) + " " + Hex.in(aid)
                        + (s.contains("=>") ? " => " + s.substring(s.indexOf("=>") + 2) : ""));
            }
            if (ten.equals("/cai")) {
                cai(doi);
            } else if (ten.equals("/cai-loi")) {
                return caiPhaiLoi(doi);
            } else if (ten.equals("/moi")) {
                System.out.println(kenh.theMoi() ? ">> lấy thẻ trắng mới" : ">> (PC/SC: Run lại trong JCIDE để có thẻ trắng)");
            } else if (ten.equals("/reset")) {
                kenh.reset();
                System.out.println(">> reset thẻ");
            } else if (ten.equals("/deselect")) {
                System.out.println(kenh.boChon() ? ">> bỏ chọn applet (chọn applet khác)"
                        : ">> (PC/SC: hãy /select một applet khác để bỏ chọn)");
            } else if (ten.equals("/file")) {
                for (String d : Files.readAllLines(Paths.get(doi), UTF8)) {
                    lenh(d.trim());
                }
            } else if (ten.equals("/danhsach")) {
                for (CauHinh.Goi g : cauHinh.goi) {
                    for (CauHinh.Lop l : g.lop) {
                        System.out.println("   " + l.ten + "   " + Hex.in(l.instance));
                    }
                }
            } else if (ten.equals("/help")) {
                giupDo();
            } else {
                System.out.println("Không hiểu lệnh " + t[0] + " (gõ /help)");
                return false;
            }
            return true;
        } catch (Exception e) {
            System.out.println("   LỖI: " + e.getMessage());
            sai++;
            return false;
        }
    }

    private static void giupDo() {
        System.out.println("  00 01 00 00             gửi APDU (hex); chèn chuỗi trong ngoặc kép, LC = tự tính độ dài");
        System.out.println("                          ví dụ: 00 01 00 00 LC \"CT01|An|01/01/2004|Ha Noi\"");
        System.out.println("  ... => 90 00            kiểm kết quả (.. = byte bất kỳ, * = đoạn bất kỳ)");
        System.out.println("  /danhsach               các applet và AID");
        System.out.println("  /cai goi.Lop [aid=..] [param=..]    cài applet lên thẻ giả lập");
        System.out.println("  /cai-loi goi.Lop param=..           kiểm tra việc cài bị từ chối");
        System.out.println("  /select goi.Lop | AID   chọn applet");
        System.out.println("  /deselect   /reset   /moi (thẻ trắng)   /file kich_ban.apdu   /thoat");
    }

    private byte[] aidCua(String doi) {
        String s = doi.contains("=>") ? doi.substring(0, doi.indexOf("=>")).trim() : doi.trim();
        if (s.matches("[0-9A-Fa-f ]+")) {
            return Hex.doc(s);
        }
        return cauHinh.tim(s).instance;
    }

    private void cai(String doi) throws Exception {
        String[] t = doi.split("\\s+");
        CauHinh.Lop l = cauHinh.tim(t[0]);
        byte[] aid = l.instance;
        byte[] param = l.param;
        for (int i = 1; i < t.length; i++) {
            if (t[i].startsWith("aid=")) {
                aid = Hex.doc(t[i].substring(4));
            } else if (t[i].startsWith("param=")) {
                param = Hex.doc(t[i].substring(6));
            } else {
                throw new IllegalArgumentException("Không hiểu tham số " + t[i]);
            }
        }
        if (kenh.cai(l, aid, param)) {
            System.out.println(">> cài " + l.ten + "   AID " + Hex.in(aid)
                    + (param.length > 0 ? "   tham số " + Hex.in(param) : ""));
        } else {
            System.out.println(">> (PC/SC: cài " + l.ten + " bằng JCIDE, Instance AID " + Hex.in(aid)
                    + (param.length > 0 ? ", tham số " + Hex.in(param) : "") + ")");
        }
    }

    /** Cai dat mong doi that bai (vi du tham so cai dat sai). */
    private boolean caiPhaiLoi(String doi) throws Exception {
        if (!kenh.laGiaLap()) {
            System.out.println(">> (PC/SC: thử cài trong JCIDE với " + doi + " - phải báo lỗi cài đặt)");
            return true;
        }
        try {
            cai(doi);
        } catch (IllegalStateException e) {
            dat++;
            System.out.println(doc("<< cài " + doi + " bị từ chối", "ĐẠT"));
            return true;
        }
        sai++;
        System.out.println(doc("<< cài " + doi + " thành công", "SAI, mong đợi bị từ chối"));
        return false;
    }

    private boolean gui(String s) throws Exception {
        String lenh = s;
        String mongDoi = null;
        int k = s.indexOf("=>");
        if (k >= 0) {
            lenh = s.substring(0, k).trim();
            mongDoi = s.substring(k + 2).trim();
        }
        byte[] apdu = docLenh(lenh);
        if (apdu.length < 4) {
            throw new IllegalArgumentException("APDU phải có ít nhất 4 byte: " + lenh);
        }
        System.out.println(">> " + rutGon(apdu));
        byte[] r = kenh.gui(apdu);
        String ketQua = "<< " + rutGon(r);
        boolean ok = true;
        if (mongDoi != null) {
            boolean chiThat = mongDoi.startsWith("[that]");
            if (chiThat) {
                mongDoi = mongDoi.substring(6).trim();
            }
            if (khop(mongDoi, r)) {
                dat++;
                ketQua = doc(ketQua, "ĐẠT");
            } else if (chiThat && kenh.laGiaLap()) {
                chiTheThat++;
                ketQua = doc(ketQua, "CHỈ THẺ THẬT: trên JCIDE phải ra " + mongDoi);
            } else {
                sai++;
                ok = false;
                ketQua = doc(ketQua, "SAI, mong đợi " + mongDoi);
            }
        }
        System.out.println(ketQua);
        String chu = r.length > 2 ? Hex.ascii(r, 0, r.length - 2) : null;
        if (chu != null) {
            System.out.println("   chữ: \"" + chu + "\"");
        }
        if (kenh.ghiChu() != null) {
            System.out.println("   ghi chú: " + kenh.ghiChu());
        }
        return ok;
    }

    private static String doc(String ketQua, String nhan) {
        StringBuilder s = new StringBuilder(ketQua);
        while (s.length() < 42) {
            s.append(' ');
        }
        return s.append("  [").append(nhan).append(']').toString();
    }

    private static String rutGon(byte[] b) {
        if (b.length <= 40) {
            return Hex.in(b);
        }
        return Hex.in(b, 0, 24) + " ... " + Hex.in(b, b.length - 8, b.length) + "  (" + b.length + " byte)";
    }

    /** Hex, "chuoi", LC (do dai phan du lieu phia sau), LE=xx (byte Le cuoi lenh). */
    private static byte[] docLenh(String s) {
        ByteArrayOutputStream truoc = new ByteArrayOutputStream();
        ByteArrayOutputStream sau = null;
        byte[] le = new byte[0];
        Matcher m = TOKEN.matcher(s);
        while (m.find()) {
            ByteArrayOutputStream dich = sau != null ? sau : truoc;
            if (m.group(1) != null) {
                byte[] b = m.group(1).getBytes(UTF8);
                dich.write(b, 0, b.length);
                continue;
            }
            String w = m.group(2);
            if (w.equalsIgnoreCase("LC")) {
                sau = new ByteArrayOutputStream();
            } else if (w.toUpperCase().startsWith("LE=")) {
                le = Hex.doc(w.substring(3));
            } else {
                byte[] b = Hex.doc(w);
                dich.write(b, 0, b.length);
            }
        }
        if (sau == null) {
            return Hex.noi(truoc.toByteArray(), le);
        }
        byte[] data = sau.toByteArray();
        if (data.length > 255) {
            throw new IllegalArgumentException("LC chỉ dùng cho dữ liệu tối đa 255 byte");
        }
        return Hex.noi(truoc.toByteArray(), new byte[] {(byte) data.length}, data, le);
    }

    /** So mau voi tra loi: hex, "chuoi", .. (1 byte bat ky), * (doan bat ky). */
    static boolean khop(String mau, byte[] r) {
        List<Integer> dau = new ArrayList<Integer>();
        List<Integer> cuoi = new ArrayList<Integer>();
        List<Integer> dich = dau;
        Matcher m = TOKEN.matcher(mau);
        while (m.find()) {
            if (m.group(1) != null) {
                for (byte b : m.group(1).getBytes(UTF8)) {
                    dich.add(b & 0xFF);
                }
                continue;
            }
            String w = m.group(2);
            if (w.equals("*")) {
                dich = cuoi;
                continue;
            }
            for (int i = 0; i + 1 < w.length(); i += 2) {
                String x = w.substring(i, i + 2);
                dich.add(x.equals("..") ? -1 : Integer.parseInt(x, 16));
            }
            if (w.length() % 2 != 0) {
                throw new IllegalArgumentException("Mẫu kết quả sai: " + w);
            }
        }
        boolean coSao = dich == cuoi;
        if (coSao ? r.length < dau.size() + cuoi.size() : r.length != dau.size()) {
            return false;
        }
        for (int i = 0; i < dau.size(); i++) {
            if (dau.get(i) >= 0 && dau.get(i) != (r[i] & 0xFF)) {
                return false;
            }
        }
        int goc = r.length - cuoi.size();
        for (int i = 0; i < cuoi.size(); i++) {
            if (cuoi.get(i) >= 0 && cuoi.get(i) != (r[goc + i] & 0xFF)) {
                return false;
            }
        }
        return true;
    }

    private void tongKet(int soTep) {
        System.out.println();
        System.out.println("================ TỔNG KẾT");
        System.out.println(soTep + " kịch bản: " + dat + " đạt, " + sai + " sai"
                + (chiTheThat > 0 ? ", " + chiTheThat + " chỉ kiểm được trên JCIDE / thẻ thật" : ""));
        for (String l : loi) {
            System.out.println("  SAI: " + l);
        }
        if (!kenh.laGiaLap()) {
            System.out.println("(PC/SC: mỗi kịch bản giả định applet vừa được cài mới trong JCIDE.)");
        }
    }
}
