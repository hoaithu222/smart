package thuchanh;

import java.nio.file.Paths;

import connect_ex_gui.SmartCardWord;

/** Kiem tra lop SmartCardWord cua Connect_Ex_GUI tren the gia lap (khong mo cua so). */
public final class KiemTraKetNoi {

    private static int sai;

    private KiemTraKetNoi() {
    }

    public static void main(String[] args) throws Exception {
        CauHinh c = CauHinh.doc(Paths.get("goi.cfg"));
        CauHinh.Lop l = c.tim("cnt.cnt");
        TheGiaLap trang = new TheGiaLap();
        GiaLapProvider.batDau(trang);

        SmartCardWord s = new SmartCardWord();
        kiem("Thẻ chưa cài applet: connectCard() trả false", !s.connectCard());

        TheGiaLap the = new TheGiaLap();
        the.cai(l, l.instance, l.param);
        GiaLapProvider.doiThe(the);
        s = new SmartCardWord();
        kiem("connectCard() thành công (SELECT 99 88 77 66 55 00 -> 90 00)", s.connectCard());
        kiem("getText() trả \"HELLO\" (lệnh B0 11 00 00)", "HELLO".equals(s.getText()));
        kiem("disconnect() thành công", s.disconnect());
        kiem("Sau khi ngắt, getText() trả null kèm lý do", s.getText() == null && s.getLoi() != null);

        System.exit(sai == 0 ? 0 : 1);
    }

    private static void kiem(String ten, boolean ok) {
        if (!ok) {
            sai++;
        }
        System.out.println((ok ? "  [ĐẠT] " : "  [SAI] ") + ten);
    }
}
