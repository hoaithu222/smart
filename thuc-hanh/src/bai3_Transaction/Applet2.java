package bai3_Transaction;

import javacard.framework.*;

/**
 * TH3 muc 3.3 - Bien cuc bo va doi tuong tam thoi trong giao dich.
 *
 *   00 00 01 02  ->  00 00 00 | 02 04 06 | 01
 *                    buffer1    buffer2    a_local
 *                    (ben:      (tam:      (cuc bo:
 *                    hoan tac)  giu)       giu)
 *
 * Khac de: de tao "new byte[3]" ngay trong process(), moi lan goi lai ton them
 * EEPROM khong thu hoi duoc. O day tao 1 lan trong ham tao (bai thuc hanh 4) va
 * dat buffer1 ve 0 truoc khi bat dau giao dich; ket qua giong het.
 */
public class Applet2 extends Applet {

    private byte[] buffer1;   // doi tuong BEN
    private byte[] buffer2;   // doi tuong TAM THOI

    private Applet2() {
        buffer1 = new byte[3];
        buffer2 = JCSystem.makeTransientByteArray((short) 3, JCSystem.CLEAR_ON_RESET);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet2().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buf = apdu.getBuffer();

        switch (buf[ISO7816.OFFSET_INS]) {
        case (byte) 0x00:
            Util.arrayFillNonAtomic(buffer1, (short) 0, (short) 3, (byte) 0);

            JCSystem.beginTransaction();
            for (byte i = 0; i < 3; i++) {
                buffer1[i] = (byte) (i + 1);
                buffer2[i] = (byte) (2 * (byte) (i + 1));
            }
            byte a_local = 1;
            JCSystem.abortTransaction();

            apdu.setOutgoing();
            apdu.setOutgoingLength((short) 7);
            apdu.sendBytesLong(buffer1, (short) 0, (short) 3);
            apdu.sendBytesLong(buffer2, (short) 0, (short) 3);
            Util.arrayFillNonAtomic(buf, (short) 0, (short) 1, a_local);
            apdu.sendBytes((short) 0, (short) 1);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
