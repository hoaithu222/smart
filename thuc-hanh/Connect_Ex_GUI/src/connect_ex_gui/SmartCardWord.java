package connect_ex_gui;

import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;
import javax.swing.JOptionPane;

/** Cac ham giao tiep voi the (huong dan ket noi, buoc 4). */
public class SmartCardWord {

    public static final byte[] AID_APPLET = {
        (byte) 0x99, (byte) 0x88, (byte) 0x77, (byte) 0x66, (byte) 0x55, (byte) 0x00
    };

    private Card card;
    private TerminalFactory factory;
    private CardChannel channel;
    private CardTerminal terminal;
    private List<CardTerminal> terminals;
    private ResponseAPDU response;
    private String loi;

    public SmartCardWord() {
    }

    public boolean connectCard() {
        try {
            factory = TerminalFactory.getDefault();
            terminals = factory.terminals().list();
            if (terminals.isEmpty()) {
                loi = "Không có đầu đọc thẻ. Hãy Run thẻ ảo trong JCIDE (bật Enable PCSC Interface).";
                return false;
            }
            // JCIDE co 2 dau doc ao: chon cai dang co the, neu khong co thi cai dau tien
            terminal = terminals.get(0);
            for (CardTerminal t : terminals) {
                if (t.isCardPresent()) {
                    terminal = t;
                    break;
                }
            }
            try {
                card = terminal.connect("T=0");
            } catch (CardException e) {
                card = terminal.connect("*"); // the chi ho tro T=1
            }
            channel = card.getBasicChannel();
            if (channel == null) {
                return false;
            }
            response = channel.transmit(new CommandAPDU(0x00, (byte) 0xA4, 0x04, 0x00, AID_APPLET));
            String check = Integer.toHexString(response.getSW());
            if (check.equals("9000")) {
                return true;
            } else if (check.equals("6400")) {
                JOptionPane.showMessageDialog(null, "Thẻ đã bị vô hiệu hóa");
                return true;
            }
            loi = "Lệnh chọn applet trả về " + check.toUpperCase() + " (applet chưa được cài trên thẻ?)";
            return false;
        } catch (Exception ex) {
            loi = ex.getMessage();
            System.err.println("Lỗi kết nối: " + ex);
        }
        return false;
    }

    /** Gui lenh B0 11 00 00, tra ve chuoi luu tren the; null neu loi (xem getLoi()). */
    public String getText() {
        try {
            response = channel.transmit(new CommandAPDU(0xB0, 0x11, 0x00, 0x00, 256));
            if (response.getSW() != 0x9000) {
                loi = "Thẻ trả về " + Integer.toHexString(response.getSW()).toUpperCase();
                return null;
            }
            return text.thanhChuoi(response.getData());
        } catch (Exception ex) {
            loi = ex.getMessage();
            return null;
        }
    }

    public boolean disconnect() {
        try {
            card.disconnect(false);
            return true;
        } catch (Exception e) {
            System.out.println("Lỗi :" + e);
        }
        return false;
    }

    public String getLoi() {
        return loi;
    }
}
