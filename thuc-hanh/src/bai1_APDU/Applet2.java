package bai1_APDU;

import javacard.framework.*;

/**
 * TH1 - Bai 2: applet co 3 chuc nang, gia tri cac truong gan co dinh tu dau.
 *
 *   00 01 00 00 00   in ho ten
 *   00 02 00 00 00   in ngay sinh
 *   00 03 00 00 00   in ho ten va ngay sinh  ("ho ten - ngay sinh")
 *
 * HAY SUA HO_TEN va NGAY_SINH thanh thong tin cua ban (khong dau, vi la ASCII).
 */
public class Applet2 extends Applet {

    private static final byte INS_HO_TEN = (byte) 0x01;
    private static final byte INS_NGAY_SINH = (byte) 0x02;
    private static final byte INS_CA_HAI = (byte) 0x03;

    private static final byte[] HO_TEN = {
        'N', 'g', 'u', 'y', 'e', 'n', ' ', 'V', 'a', 'n', ' ', 'A'
    };
    private static final byte[] NGAY_SINH = {
        '0', '1', '/', '0', '1', '/', '2', '0', '0', '4'
    };
    private static final byte[] NOI = {
        ' ', '-', ' '
    };

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet2().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_HO_TEN:
            gui(apdu, HO_TEN);
            return;
        case INS_NGAY_SINH:
            gui(apdu, NGAY_SINH);
            return;
        case INS_CA_HAI:
            // ghep 3 doan vao bo dem APDU roi gui mot lan
            short n = Util.arrayCopyNonAtomic(HO_TEN, (short) 0, buf, (short) 0, (short) HO_TEN.length);
            n = Util.arrayCopyNonAtomic(NOI, (short) 0, buf, n, (short) NOI.length);
            n = Util.arrayCopyNonAtomic(NGAY_SINH, (short) 0, buf, n, (short) NGAY_SINH.length);
            apdu.setOutgoingAndSend((short) 0, n);
            return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void gui(APDU apdu, byte[] duLieu) {
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) duLieu.length);
        apdu.sendBytesLong(duLieu, (short) 0, (short) duLieu.length);
    }
}
