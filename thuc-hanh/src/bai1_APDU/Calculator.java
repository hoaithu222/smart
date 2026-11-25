package bai1_APDU;

import javacard.framework.*;

/**
 * TH1 - "May tinh bo tui" (bai huong dan tren lop; bo tai lieu khong co ban in,
 * day la ban viet theo yeu cau chung: 4 phep tinh).
 *
 *   00 01 a b 02   a + b      -> 2 byte
 *   00 02 a b 02   a - b      -> 2 byte co dau (vi du 3 - 5 = FF FE)
 *   00 03 a b 02   a x b      -> 2 byte khong dau (toi da 65025)
 *   00 04 a b 02   a : b      -> byte 1 = thuong, byte 2 = du
 *
 * a = P1, b = P2, deu la so khong dau 0..255. Chia cho 0 -> 6A 86.
 */
public class Calculator extends Applet {

    private static final byte INS_CONG = (byte) 0x01;
    private static final byte INS_TRU = (byte) 0x02;
    private static final byte INS_NHAN = (byte) 0x03;
    private static final byte INS_CHIA = (byte) 0x04;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Calculator().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        short a = (short) (buf[ISO7816.OFFSET_P1] & 0x00FF);
        short b = (short) (buf[ISO7816.OFFSET_P2] & 0x00FF);
        short ketQua = 0;
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_CONG:
            ketQua = (short) (a + b);
            break;
        case INS_TRU:
            ketQua = (short) (a - b);
            break;
        case INS_NHAN:
            ketQua = (short) (a * b);
            break;
        case INS_CHIA:
            if (b == 0) {
                ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
            }
            ketQua = (short) ((short) ((short) (a / b) << 8) | (short) (a % b));
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        Util.setShort(buf, (short) 0, ketQua);
        apdu.setOutgoingAndSend((short) 0, (short) 2);
    }
}
