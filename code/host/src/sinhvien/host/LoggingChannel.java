package sinhvien.host;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Bọc một ApduChannel, ghi mọi APDU theo đúng kiểu Debug Shell của JCIDE:
 *   >> B0 21 00 00 06 ** (đã che 6 byte có PIN)
 *   << 90 00
 * Gửi tới console (nếu log.apdu=true) và tới các listener (khung "Nhật ký APDU" trên giao diện).
 */
public class LoggingChannel implements ApduChannel {

    /** Nhận từng dòng nhật ký. Được gọi trên luồng gửi APDU (không phải luồng giao diện). */
    public interface Listener {
        void onLine(String line);
    }

    private final ApduChannel inner;
    private final boolean console;
    private final List<Listener> listeners = new CopyOnWriteArrayList<Listener>();

    public LoggingChannel(ApduChannel inner) {
        this(inner, true);
    }

    public LoggingChannel(ApduChannel inner, boolean console) {
        this.inner = inner;
        this.console = console;
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public ResponseAPDU transmit(CommandAPDU command) throws CardException {
        emit(">> " + mask(command));
        ResponseAPDU r = inner.transmit(command);
        byte[] b = r.getBytes();
        String resp = CardCrypto.hex(b);
        if (b.length > 24) {
            resp = CardCrypto.hex(java.util.Arrays.copyOf(b, 12)) + " … (" + (b.length - 2) + " byte) "
                    + String.format("%02X %02X", r.getSW1(), r.getSW2());
        }
        emit("<< " + resp);
        return r;
    }

    private void emit(String line) {
        if (console) {
            System.out.println(line);
        }
        for (Listener l : listeners) {
            l.onLine(line);
        }
    }

    /** Lệnh có PIN (20, 21, 22, 23, 30, 51, 52): chỉ in phần đầu, che dữ liệu. */
    private static String mask(CommandAPDU c) {
        int ins = c.getINS();
        boolean secret = c.getCLA() == 0xB0 && (ins == 0x20 || ins == 0x21 || ins == 0x22 || ins == 0x23
                || ins == 0x30 || ins == 0x51 || ins == 0x52);
        if (!secret) {
            byte[] b = c.getBytes();
            if (b.length > 40) {
                return CardCrypto.hex(java.util.Arrays.copyOf(b, 13)) + " … (" + c.getNc() + " byte dữ liệu)";
            }
            return CardCrypto.hex(b);
        }
        return String.format("%02X %02X %02X %02X %02X ** (đã che %d byte có PIN)",
                c.getCLA(), ins, c.getP1(), c.getP2(), c.getNc(), c.getNc());
    }
}
