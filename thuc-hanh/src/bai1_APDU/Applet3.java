package bai1_APDU;

import javacard.framework.*;

/**
 * TH1 - Bai 3: tinh dien tich hinh vuong, hinh chu nhat.
 *
 *   00 01 aa 00 02   hinh vuong canh aa                -> 2 byte dien tich
 *   00 02 aa bb 02   hinh chu nhat dai aa, rong bb      -> 2 byte dien tich
 *
 * Canh la so khong dau 0..255 nen dien tich toi da 65025 (FE 01),
 * vua du 2 byte. Doc ket qua nhu so KHONG DAU.
 *
 * Vi du: 00 02 0C 05 02  -> 00 3C (12 x 5 = 60)
 */
public class Applet3 extends Applet {

    private static final byte INS_HINH_VUONG = (byte) 0x01;
    private static final byte INS_HINH_CHU_NHAT = (byte) 0x02;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet3().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        // & 0x00FF: byte cua Java co dau, phai bo dau truoc khi tinh
        short a = (short) (buf[ISO7816.OFFSET_P1] & 0x00FF);
        short b = (short) (buf[ISO7816.OFFSET_P2] & 0x00FF);
        short dienTich = 0;
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_HINH_VUONG:
            dienTich = (short) (a * a);
            break;
        case INS_HINH_CHU_NHAT:
            dienTich = (short) (a * b);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        Util.setShort(buf, (short) 0, dienTich);
        apdu.setOutgoingAndSend((short) 0, (short) 2);
    }
}
