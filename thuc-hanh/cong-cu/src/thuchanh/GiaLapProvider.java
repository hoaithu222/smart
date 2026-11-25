package thuchanh;

import java.nio.ByteBuffer;
import java.security.Provider;
import java.util.Collections;
import java.util.List;

import javax.smartcardio.ATR;
import javax.smartcardio.Card;
import javax.smartcardio.CardChannel;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactorySpi;

/**
 * Dau doc the gia cho thu vien javax.smartcardio, noi vao TheGiaLap.
 * Nho no ma chuong trinh viet dung tai lieu (TerminalFactory.getDefault())
 * chay duoc khi chua co JCIDE, khong phai sua mot dong nao.
 */
public final class GiaLapProvider extends Provider {

    private static final long serialVersionUID = 1L;
    public static final String LOAI = "ThucHanhGiaLap";

    static KenhThe the;

    public GiaLapProvider() {
        super(LOAI, 1.0, "The gia lap cho bai thuc hanh");
        put("TerminalFactory." + LOAI, Nha.class.getName());
    }

    /** Goi truoc khi chuong trinh dung toi TerminalFactory. */
    public static void batDau(KenhThe kenh) {
        the = kenh;
        java.security.Security.addProvider(new GiaLapProvider());
        System.setProperty("javax.smartcardio.TerminalFactory.DefaultType", LOAI);
    }

    public static void doiThe(KenhThe kenh) {
        the = kenh;
    }

    public static final class Nha extends TerminalFactorySpi {
        public Nha(Object thamSo) {
        }

        protected CardTerminals engineTerminals() {
            return new CardTerminals() {
                public List<CardTerminal> list(State s) {
                    return Collections.<CardTerminal>singletonList(new DauDoc());
                }

                public boolean waitForChange(long ms) {
                    return false;
                }
            };
        }
    }

    private static final class DauDoc extends CardTerminal {
        public String getName() {
            return "Dau doc gia lap (jCardSim)";
        }

        public Card connect(String giaoThuc) {
            return new TheAo();
        }

        public boolean isCardPresent() {
            return true;
        }

        public boolean waitForCardPresent(long ms) {
            return true;
        }

        public boolean waitForCardAbsent(long ms) {
            return false;
        }
    }

    private static final class TheAo extends Card {
        private boolean dong;

        public ATR getATR() {
            return new ATR(Hex.doc("3B 70 13 00 00"));
        }

        public String getProtocol() {
            return "T=0";
        }

        public CardChannel getBasicChannel() {
            return new Kenh(this);
        }

        public CardChannel openLogicalChannel() throws CardException {
            throw new CardException("Không hỗ trợ kênh logic");
        }

        public void beginExclusive() {
        }

        public void endExclusive() {
        }

        public byte[] transmitControlCommand(int ma, byte[] lenh) throws CardException {
            throw new CardException("Không hỗ trợ");
        }

        public void disconnect(boolean reset) throws CardException {
            dong = true;
            if (reset) {
                try {
                    the.reset();
                } catch (Exception e) {
                    throw new CardException(e);
                }
            }
        }
    }

    private static final class Kenh extends CardChannel {
        private final TheAo card;

        Kenh(TheAo card) {
            this.card = card;
        }

        public Card getCard() {
            return card;
        }

        public int getChannelNumber() {
            return 0;
        }

        public ResponseAPDU transmit(CommandAPDU c) throws CardException {
            if (card.dong) {
                throw new CardException("Thẻ đã ngắt kết nối");
            }
            try {
                return new ResponseAPDU(the.gui(c.getBytes()));
            } catch (Exception e) {
                throw new CardException(e);
            }
        }

        public int transmit(ByteBuffer vao, ByteBuffer ra) throws CardException {
            byte[] b = new byte[vao.remaining()];
            vao.get(b);
            byte[] r = transmit(new CommandAPDU(b)).getBytes();
            ra.put(r);
            return r.length;
        }

        public void close() {
        }
    }
}
