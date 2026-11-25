package bai2_Object;

import javacard.framework.*;

/**
 * Bai tap TH2 - Bai 1: xac thuc ma PIN, dem so lan nhap sai.
 *
 * Moi lan nhap sai PIN: wrongTryCounter tang 1. Nhap dung: ve 0.
 *
 *   00 20 00 00 Lc <PIN>   kiem tra PIN     90 00 dung, 63 00 sai
 *   00 30 00 00 02         doc [wrongTryCounter][ramCounter]
 *
 * PIN mac dinh "1234" (31 32 33 34); doi bang tham so cai dat (4..8 byte).
 *
 * Cau 1: wrongTryCounter la truong cua applet = doi tuong BEN (EEPROM).
 *        Neu de trong RAM, ke tan cong reset the sau moi lan sai la co so lan thu
 *        khong gioi han.
 * Cau 2: reset giua chung KHONG mat. ramCounter (CLEAR_ON_RESET) chi de minh
 *        hoa: sau reset no ve 0 con wrongTryCounter giu nguyen.
 */
public class Applet2 extends Applet {

    private static final byte INS_VERIFY = (byte) 0x20;
    private static final byte INS_DOC_DEM = (byte) 0x30;

    private static final short SW_SAI_PIN = (short) 0x6300;
    private static final byte DEM_TOI_DA = (byte) 127;
    private static final short PIN_NGAN_NHAT = (short) 4;
    private static final short PIN_DAI_NHAT = (short) 8;

    private byte[] pin;
    private byte wrongTryCounter;   // BEN
    private byte[] ramCounter;      // TAM, chi de minh hoa cau 2

    private Applet2(byte[] bArray, short bOffset, byte bLength) {
        // tham so cai dat: [Li][AID][Lc][thong tin dieu khien][La][PIN]
        short p = (short) (bOffset + (bArray[bOffset] & 0x00FF) + 1);
        p = (short) (p + (bArray[p] & 0x00FF) + 1);
        short la = (short) (bArray[p] & 0x00FF);
        p++;
        if (la == 0) {
            pin = new byte[] {'1', '2', '3', '4'};
        } else {
            if (la < PIN_NGAN_NHAT || la > PIN_DAI_NHAT) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            pin = new byte[la];
            Util.arrayCopy(bArray, p, pin, (short) 0, la);
        }
        ramCounter = JCSystem.makeTransientByteArray((short) 1, JCSystem.CLEAR_ON_RESET);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet2(bArray, bOffset, bLength).register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_VERIFY:
            verify(apdu);
            return;
        case INS_DOC_DEM:
            buf[0] = wrongTryCounter;
            buf[1] = ramCounter[0];
            apdu.setOutgoingAndSend((short) 0, (short) 2);
            return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void verify(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short n = apdu.setIncomingAndReceive();
        if (n == 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        // Tang TRUOC khi so sanh: rut the giua chung cung khong duoc thu mien phi.
        if (wrongTryCounter < DEM_TOI_DA) {
            wrongTryCounter++;
        }
        if (ramCounter[0] < DEM_TOI_DA) {
            ramCounter[0]++;
        }
        if (n != (short) pin.length
                || Util.arrayCompare(buf, ISO7816.OFFSET_CDATA, pin, (short) 0, n) != 0) {
            ISOException.throwIt(SW_SAI_PIN);
        }
        wrongTryCounter = 0;
        ramCounter[0] = 0;
    }
}
