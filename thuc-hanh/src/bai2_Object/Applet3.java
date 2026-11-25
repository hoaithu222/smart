package bai2_Object;

import javacard.framework.*;

/**
 * Bai tap TH2 - Bai 2: nhap (maSV, hotenSV, ngaysinh, quequan) vao bo dem,
 * sau khi in ra thi giai phong bo dem.
 *
 *   00 01 00 00 Lc <du lieu>   nhap vao tempBuffer (toi da 128 byte)
 *   00 02 00 00 00             in ra, roi xoa bo dem. Chua co du lieu -> 69 85
 *
 * Tra loi: tempBuffer nen la mang TAM THOI kieu CLEAR_ON_DESELECT:
 *   - chi can trong luc applet dang duoc chon;
 *   - JCRE tu xoa khi chon applet khac hoac reset, applet khac khong doc duoc;
 *   - nam trong RAM: ghi nhanh, khong hao mon EEPROM.
 * Do dai du lieu cung phai o RAM (mang short 1 phan tu), neu khong no se chi
 * vao bo dem da bi xoa.
 * "Giai phong" tren Java Card = ghi 0 va dat do dai ve 0 (khong co delete).
 */
public class Applet3 extends Applet {

    private static final byte INS_NHAP = (byte) 0x01;
    private static final byte INS_IN = (byte) 0x02;

    private static final short TOI_DA = (short) 128;

    private byte[] tempBuffer;
    private short[] doDai;

    private Applet3() {
        tempBuffer = JCSystem.makeTransientByteArray(TOI_DA, JCSystem.CLEAR_ON_DESELECT);
        doDai = JCSystem.makeTransientShortArray((short) 1, JCSystem.CLEAR_ON_DESELECT);
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
        case INS_NHAP:
            short lc = (short) (buf[ISO7816.OFFSET_LC] & 0x00FF);
            if (lc == 0 || lc > TOI_DA) {
                ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
            }
            short daNhan = apdu.setIncomingAndReceive();
            while (daNhan < lc) {
                daNhan += apdu.receiveBytes((short) (ISO7816.OFFSET_CDATA + daNhan));
            }
            Util.arrayFillNonAtomic(tempBuffer, (short) 0, TOI_DA, (byte) 0);
            Util.arrayCopyNonAtomic(buf, ISO7816.OFFSET_CDATA, tempBuffer, (short) 0, lc);
            doDai[0] = lc;
            return;
        case INS_IN:
            short n = doDai[0];
            if (n == 0) {
                ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            }
            Util.arrayCopyNonAtomic(tempBuffer, (short) 0, buf, (short) 0, n);
            // giai phong bo dem
            Util.arrayFillNonAtomic(tempBuffer, (short) 0, TOI_DA, (byte) 0);
            doDai[0] = 0;
            apdu.setOutgoingAndSend((short) 0, n);
            return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
