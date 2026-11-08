package sinhvien.host;

import java.util.List;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

/**
 * Kết nối thẻ qua PC/SC - mở rộng từ lớp SmartCardWord trong "Hướng dẫn kết nối".
 * Với JCIDE: bật Tool > IDE Options > Run/Debug > Enable PCSC Interface, chạy applet,
 * chờ dòng "You can send apdu with PC/SC interface now" rồi mới bấm Kết nối.
 */
public class PcscChannel implements ApduChannel {

    /** Kết quả chọn applet. */
    public enum SelectResult { OK, CARD_LOCKED, NOT_FOUND }

    private final String protocol;
    private final String readerFilter;
    private Card card;
    private CardChannel channel;
    private String terminalName;

    /**
     * protocol: "T=0" như tài liệu của cô, hoặc "*" để tự chọn.
     * readerFilter: một phần tên đầu đọc (vd "JAVACOS Virtual"), rỗng = tự chọn.
     * Thường tạo bằng: new PcscChannel(cfg.protocol(), cfg.readerName())
     */
    public PcscChannel(String protocol, String readerFilter) {
        this.protocol = protocol;
        this.readerFilter = readerFilter == null ? "" : readerFilter;
    }

    /** Danh sách đầu đọc, ví dụ "JAVACOS Virtual Contact Reader 0". */
    public static List<CardTerminal> listTerminals() throws CardException {
        return TerminalFactory.getDefault().terminals().list();
    }

    public SelectResult connect(byte[] appletAid) throws CardException {
        List<CardTerminal> terminals = listTerminals();
        if (terminals.isEmpty()) {
            throw new CardException("Không thấy đầu đọc nào. JCIDE đã bật PC/SC chưa?");
        }
        CardTerminal terminal = null;
        for (CardTerminal t : terminals) {
            boolean nameOk = readerFilter.isEmpty() || t.getName().contains(readerFilter);
            if (nameOk && t.isCardPresent()) {
                terminal = t;
                break;
            }
        }
        if (terminal == null) {
            terminal = terminals.get(0);
        }
        terminalName = terminal.getName();
        card = terminal.connect(protocol);
        channel = card.getBasicChannel();

        ResponseAPDU r = channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, appletAid));
        if (r.getSW() == 0x9000) {
            return SelectResult.OK;
        }
        if (r.getSW() == 0x6400) {
            return SelectResult.CARD_LOCKED;
        }
        return SelectResult.NOT_FOUND;
    }

    public void disconnect() {
        try {
            if (card != null) {
                card.disconnect(false);
            }
        } catch (CardException ignored) {
            // đã ngắt rồi
        } finally {
            card = null;
            channel = null;
        }
    }

    public boolean isConnected() {
        return channel != null;
    }

    public String getTerminalName() {
        return terminalName;
    }

    public ResponseAPDU transmit(CommandAPDU command) throws CardException {
        if (channel == null) {
            throw new CardException("Chưa kết nối thẻ");
        }
        return channel.transmit(command);
    }
}
