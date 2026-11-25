package thuchanh;

/** Mot "dau doc the": the gia lap hoac the that / the ao JCIDE qua PC/SC. */
public interface KenhThe {

    /** Gui mot APDU, tra ve du lieu + SW. */
    byte[] gui(byte[] apdu) throws Exception;

    /** Rut the / cap dien lai. */
    void reset() throws Exception;

    /** Bo chon applet hien tai (chon sang applet khac). Tra false neu kenh khong lam duoc. */
    boolean boChon() throws Exception;

    /** The trang moi. Tra false neu kenh khong lam duoc. */
    boolean theMoi() throws Exception;

    /** Cai applet. Tra false neu kenh khong lam duoc (PC/SC: cai bang JCIDE). */
    boolean cai(CauHinh.Lop lop, byte[] instanceAid, byte[] param) throws Exception;

    boolean laGiaLap();

    /** Ghi chu cua lenh vua gui (vi du: giao dich bi JCRE huy), null neu khong co. */
    String ghiChu();
}
