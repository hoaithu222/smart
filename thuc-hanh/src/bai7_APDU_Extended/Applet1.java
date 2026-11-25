package bai7_APDU_Extended;

import javacard.framework.*;
import javacardx.apdu.ExtendedLength;

/**
 * APDU mo rong (tai lieu "apdu-mo-rong"): nhap va xuat toi 1024 byte bang
 * MOT lenh.
 *
 *   00 01 00 00 Lc <du lieu>              nhap (Lc ngan, <= 255)
 *   00 01 00 00 00 LcHi LcLo <du lieu>    nhap (Lc mo rong)
 *   00 02 00 00 00                        xuat (toi 256 byte)
 *   00 02 00 00 00 00 00                  xuat (Le mo rong)
 *
 * Can giao thuc T = 1 va the ho tro Java Card 2.2.2 tro len.
 * jCardSim 2.2.2 chi chay dung voi du lieu ngan (Lc <= 127, xuat <= 255 byte);
 * phan mo rong phai thu tren JCIDE / the that.
 *
 * Sua so voi de: cap phat trong ham tao; chi ghi do dai sau khi nhan du du lieu;
 * xuat bang mot lan sendBytesLong (vong lap cua de lap mai neu le = 0).
 */
public class Applet1 extends Applet implements ExtendedLength {

    private final static short MAX_SIZE = (short) 1024;
    private final static byte INS_NHAP = (byte) 0x01;
    private final static byte INS_XUAT = (byte) 0x02;

    private byte[] temp;
    private short dataLen;

    private Applet1() {
        temp = new byte[MAX_SIZE];
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet1().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buf = apdu.getBuffer();

        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_NHAP:
            nhap(apdu);
            break;
        case INS_XUAT:
            if (dataLen == 0) {
                ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            }
            apdu.setOutgoing();
            apdu.setOutgoingLength(dataLen);
            apdu.sendBytesLong(temp, (short) 0, dataLen);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void nhap(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short recvLen = apdu.setIncomingAndReceive();
        short tong = apdu.getIncomingLength();
        if (tong <= 0 || tong > MAX_SIZE) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short dataOffset = apdu.getOffsetCdata();
        short pointer = 0;
        dataLen = 0;
        // doc tung doan vua voi bo dem APDU
        while (recvLen > 0) {
            Util.arrayCopy(buf, dataOffset, temp, pointer, recvLen);
            pointer += recvLen;
            recvLen = apdu.receiveBytes(dataOffset);
        }
        dataLen = pointer;
    }
}
