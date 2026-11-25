package thuchanh;

import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.TerminalFactory;

/** The that hoac the ao cua JCIDE qua PC/SC. Cai applet bang JCIDE, khong cai tu day. */
public final class ThePcsc implements KenhThe {

    private final CardTerminal dauDoc;
    private Card the;

    public ThePcsc(String tenDauDoc) throws CardException {
        List<CardTerminal> ds;
        try {
            ds = TerminalFactory.getDefault().terminals().list();
        } catch (CardException e) {
            throw new CardException("Không liệt kê được đầu đọc PC/SC. Kiểm tra dịch vụ Smart Card (SCardSvr) "
                    + "và đã Run thẻ ảo trong JCIDE chưa. (" + e.getMessage() + ")", e);
        }
        if (ds.isEmpty()) {
            throw new CardException("Không có đầu đọc PC/SC nào. Mở JCIDE, bật Enable PCSC Interface rồi Run.");
        }
        // uu tien dau doc khop ten VA dang co the (JCIDE co hai dau doc ao)
        CardTerminal chon = null;
        for (CardTerminal t : ds) {
            boolean khopTen = tenDauDoc == null || t.getName().toLowerCase().contains(tenDauDoc.toLowerCase());
            if (khopTen && t.isCardPresent()) {
                chon = t;
                break;
            }
        }
        for (CardTerminal t : ds) {
            if (chon == null && tenDauDoc != null && t.getName().toLowerCase().contains(tenDauDoc.toLowerCase())) {
                chon = t;
            }
        }
        if (chon == null) {
            StringBuilder s = new StringBuilder("Không tìm thấy đầu đọc phù hợp. Đang có:");
            for (CardTerminal t : ds) {
                s.append("\n   ").append(t.getName()).append(t.isCardPresent() ? " (có thẻ)" : " (trống)");
            }
            throw new CardException(s.toString());
        }
        dauDoc = chon;
        the = dauDoc.connect("*");
        System.out.println("Đã kết nối: " + dauDoc.getName() + ", giao thức " + the.getProtocol()
                + ", ATR " + Hex.in(the.getATR().getBytes()));
    }

    public byte[] gui(byte[] apdu) throws CardException {
        return the.getBasicChannel().transmit(new CommandAPDU(apdu)).getBytes();
    }

    public void reset() throws CardException {
        the.disconnect(true);
        the = dauDoc.connect("*");
    }

    /** Ket noi lai sau khi nguoi dung Run lai the ao trong JCIDE. */
    public void ketNoiLai() throws CardException {
        try {
            the.disconnect(false);
        } catch (Exception e) {
            // ket noi cu co the da mat khi JCIDE chay lai, bo qua
        }
        the = dauDoc.connect("*");
    }

    public boolean boChon() {
        return false;
    }

    public boolean theMoi() {
        return false;
    }

    public boolean cai(CauHinh.Lop lop, byte[] instanceAid, byte[] param) {
        return false;
    }

    public boolean laGiaLap() {
        return false;
    }

    public String ghiChu() {
        return null;
    }
}
