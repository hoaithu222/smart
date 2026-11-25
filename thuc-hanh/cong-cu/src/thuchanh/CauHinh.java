package thuchanh;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Doc goi.cfg: danh sach goi, lop applet va AID. */
public final class CauHinh {

    public static final class Goi {
        public String ten;
        public byte[] aid;
        public final List<String> can = new ArrayList<String>();
        public final List<Lop> lop = new ArrayList<Lop>();
    }

    public static final class Lop {
        public String ten;          // ten day du, vi du bai2_Object.Applet1
        public byte[] aid;          // AID cua lop
        public byte[] instance;     // AID the hien khi cai
        public byte[] param = new byte[0];
    }

    public final List<Goi> goi = new ArrayList<Goi>();

    public static CauHinh doc(Path tep) throws IOException {
        CauHinh c = new CauHinh();
        int dong = 0;
        for (String raw : Files.readAllLines(tep, Charset.forName("UTF-8"))) {
            dong++;
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith("#")) {
                continue;
            }
            String[] t = s.split("\\s+");
            if (t[0].equals("goi") && t.length >= 3) {
                Goi g = new Goi();
                g.ten = t[1];
                g.aid = Hex.doc(t[2]);
                for (int i = 4; i < t.length && t[3].equals("can"); i++) {
                    g.can.add(t[i]);
                }
                c.goi.add(g);
            } else if (t[0].equals("applet") && t.length >= 3 && !c.goi.isEmpty()) {
                Lop l = new Lop();
                l.ten = t[1];
                l.aid = Hex.doc(t[2]);
                l.instance = l.aid;
                for (int i = 3; i < t.length; i++) {
                    if (t[i].startsWith("instance=")) {
                        l.instance = Hex.doc(t[i].substring(9));
                    } else if (t[i].startsWith("param=")) {
                        l.param = Hex.doc(t[i].substring(6));
                    } else {
                        throw new IOException(tep + " dòng " + dong + ": không hiểu " + t[i]);
                    }
                }
                c.goi.get(c.goi.size() - 1).lop.add(l);
            } else {
                throw new IOException(tep + " dòng " + dong + ": không hiểu " + s);
            }
        }
        return c;
    }

    /** Tim lop theo ten day du, hoac ten ngan neu chi co mot lop trung ten. */
    public Lop tim(String ten) {
        Lop ketQua = null;
        int soTrung = 0;
        for (Goi g : goi) {
            for (Lop l : g.lop) {
                if (l.ten.equals(ten)) {
                    return l;
                }
                if (l.ten.endsWith("." + ten)) {
                    ketQua = l;
                    soTrung++;
                }
            }
        }
        if (soTrung == 1) {
            return ketQua;
        }
        throw new IllegalArgumentException(soTrung == 0
                ? "Không có applet '" + ten + "' trong goi.cfg"
                : "Tên '" + ten + "' trùng ở nhiều gói, hãy ghi đủ gói.Lớp");
    }
}
