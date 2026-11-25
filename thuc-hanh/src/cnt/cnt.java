package cnt;

import javacard.framework.*;

/**
 * Huong dan ket noi (buoc 1): applet tra ve chuoi luu tren the.
 *
 * AID applet: 99 88 77 66 55 00        CLA = B0, INS = 11
 *
 *   00 A4 04 00 06 99 88 77 66 55 00   -> 90 00
 *   B0 11 00 00 00                     -> 48 45 4C 4C 4F 90 00   ("HELLO")
 *
 * Chuong trinh phia may tinh: thu muc Connect_Ex_GUI.
 *
 * Khac de: de tu so CLA/INS de nhan lenh SELECT; o day dung selectingApplet()
 * (chuan cua Java Card).
 */
public class cnt extends Applet {

    public final static byte IDCARD_CLA = (byte) 0xB0;

    public final static byte INS_PRINT = (byte) 0x11;

    private static byte[] text;

    protected cnt(byte[] bArray, short bOffset, byte bLength) {
        byte aIDLen = bArray[bOffset];
        if (aIDLen == 0) {
            register();
        } else {
            register(bArray, (short) (bOffset + 1), aIDLen);
        }

        text = new byte[] {(byte) 0x48, (byte) 0x45, (byte) 0x4c, (byte) 0x4c, (byte) 0x4f};
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new cnt(bArray, bOffset, bLength);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();

        if (buf[ISO7816.OFFSET_CLA] != IDCARD_CLA) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_PRINT:
            PrintText(apdu);
            return;

        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void PrintText(APDU apdu) {
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) text.length);
        apdu.sendBytesLong(text, (short) 0, (short) text.length);
    }
}
