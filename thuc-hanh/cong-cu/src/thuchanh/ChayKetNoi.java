package thuchanh;

import java.nio.file.Paths;

/** Mo chuong trinh Connect_Ex_GUI tren the gia lap (da cai san applet cnt). */
public final class ChayKetNoi {

    private ChayKetNoi() {
    }

    public static void main(String[] args) throws Exception {
        CauHinh c = CauHinh.doc(Paths.get("goi.cfg"));
        CauHinh.Lop l = c.tim("cnt.cnt");
        TheGiaLap the = new TheGiaLap();
        the.cai(l, l.instance, l.param);
        GiaLapProvider.batDau(the);
        connect_ex_gui.GUI.main(args);
    }
}
