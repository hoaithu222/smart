package bai3_Transaction;

import javacard.framework.*;

/**
 * TH3 muc 3.2 - Vi du 1: chep mang u sang mang t trong mot giao dich
 * ("hoac chep het, hoac khong chep gi").
 *
 * (De in "for(int i=0; i++; i<t.length)": dieu kien va phan tang bi doi cho;
 *  Java Card cung khong dung int. Dang dung: for (short i = 0; i < t.length; i++).)
 *
 *   00 00 00 00   chep u -> t, commit
 *   00 01 00 00   chep u -> t roi abort (t giu nguyen)
 *   00 02 00 00   xoa t ve 0
 *   00 03 00 00 08   doc t
 */
public class Applet3 extends Applet {

    private static final byte INS_CHEP = (byte) 0x00;
    private static final byte INS_CHEP_ROI_HUY = (byte) 0x01;
    private static final byte INS_XOA = (byte) 0x02;
    private static final byte INS_DOC = (byte) 0x03;

    private byte[] u;
    private byte[] t;

    private Applet3() {
        u = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        t = new byte[8];
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet3().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_CHEP:
            JCSystem.beginTransaction();
            chep();
            JCSystem.commitTransaction();
            return;
        case INS_CHEP_ROI_HUY:
            JCSystem.beginTransaction();
            chep();
            JCSystem.abortTransaction();
            return;
        case INS_XOA:
            Util.arrayFillNonAtomic(t, (short) 0, (short) t.length, (byte) 0);
            return;
        case INS_DOC:
            apdu.setOutgoing();
            apdu.setOutgoingLength((short) t.length);
            apdu.sendBytesLong(t, (short) 0, (short) t.length);
            return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void chep() {
        for (short i = 0; i < (short) t.length; i++) {
            t[i] = u[i];
        }
    }
}
