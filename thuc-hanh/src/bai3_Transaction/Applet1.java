package bai3_Transaction;

import javacard.framework.*;

/**
 * TH3 muc 3.2 - Vi du 2: huy giao dich giua chung.
 *
 *   00 01 01 02   gui buffer2   (luc dau: 10 byte 00)
 *   00 00 01 02   chep buffer1 sang buffer2, huy giao dich khi i = 5
 *   00 01 01 02   -> 00 00 00 00 00 00 06 07 08 09
 *
 * Phan i <= 5 nam trong giao dich bi abort nen tro ve gia tri cu.
 * (De goi setIncomingAndReceive() truoc switch; lenh khong co du lieu nen bo.)
 */
public class Applet1 extends Applet {

    // khai bao ma INS trong tieu de cua apdu
    final static byte INS_TRANSACTION = (byte) 0x00;
    final static byte INS_SEND = (byte) 0x01;

    // khai bao cac doi tuong va bien
    private static byte[] buffer1, buffer2;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet1().register(bArray, (short) (bOffset + 1), bArray[bOffset]);

        buffer1 = new byte[10];
        buffer2 = new byte[10];
        for (short i = 0; i < 10; i++) {
            buffer1[i] = (byte) i;
        }
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buf = apdu.getBuffer();

        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_TRANSACTION:
            JCSystem.beginTransaction();
            for (short i = 0; i < 10; i++) {
                buffer2[i] = buffer1[i];
                if (i == (short) 5) {
                    JCSystem.abortTransaction();
                    JCSystem.beginTransaction(); // bat dau mot giao dich moi voi i > 5
                }
            }
            JCSystem.commitTransaction();
            break;
        case INS_SEND:
            apdu.setOutgoing();
            apdu.setOutgoingLength((short) 10);
            apdu.sendBytesLong(buffer2, (short) 0, (short) 10);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
