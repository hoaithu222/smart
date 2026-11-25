package bai1_APDU;

import javacard.framework.*;

/**
 * TH1 - Bai 1: in ra chu "Xin chao, KMA".
 *
 * Lenh:  00 00 00 00 0D   (INS = 00, Le = 13)
 * Tra:   58 69 6E 20 63 68 61 6F 2C 20 4B 4D 41  90 00
 */
public class Applet1 extends Applet {

    private static final byte INS_CHAO = (byte) 0x00;

    private static final byte[] LOI_CHAO = {
        'X', 'i', 'n', ' ', 'c', 'h', 'a', 'o', ',', ' ', 'K', 'M', 'A'
    };

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet1().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_CHAO:
            apdu.setOutgoing();
            apdu.setOutgoingLength((short) LOI_CHAO.length);
            apdu.sendBytesLong(LOI_CHAO, (short) 0, (short) LOI_CHAO.length);
            return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
