package bai4_InstallMethod;

import javacard.framework.*;

/**
 * TH4 phan mo dau - ham tao cap phat moi doi tuong roi register().
 *
 * register() khong tham so: dung AID mac dinh (AID cua lop trong file CAP).
 *
 *   00 00 00 00 01   -> so luong mon hoc (0A)
 */
public class Applet1 extends Applet {

    final static byte SO_LUONG_MON_HOC = (byte) 10;
    private byte[] monhoc, diem;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet1();
    }

    // ham tao
    private Applet1() {
        // tao mang byte luu tru ID cua mon hoc
        monhoc = new byte[SO_LUONG_MON_HOC];
        // tao mang byte luu tru diem thi
        diem = new byte[SO_LUONG_MON_HOC];
        // dang ky Applet
        register();
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case (byte) 0x00:
            buf[0] = (byte) monhoc.length;
            apdu.setOutgoingAndSend((short) 0, (short) 1);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
