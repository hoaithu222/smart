package chiaSeAnToan;

import javacard.framework.*;

/**
 * TH5 mo rong - applet CHU giu du lieu rieng (ma sinh vien) va chi chia se cho
 * dung mot applet khach, sau khi khach tra loi dung thu thach.
 *
 * Ba lop bao ve:
 *   1. getShareableInterfaceObject chi tra doi tuong cho dung AID khach;
 *   2. MOI phuong thuc chia se kiem lai AID nguoi goi (getPreviousContextAID);
 *   3. doc du lieu phai qua thu thach - phan hoi, moi lan cap quyen chi doc mot lan.
 *
 * Tra ve doi tuong DichVu rieng (khong tra "this") de khach khong ep kieu sang
 * giao dien khac cua applet.
 *
 * Tham so cai dat (khong bat buoc): 16 byte khoa AES chung. Bo trong: khoa mau
 * 00 01 .. 0F (CHI DE HOC; the that phai nap khoa rieng cho tung the).
 *
 * AID: 11 22 33 44 55 05 02 01
 */
public class ChuApplet extends Applet {

    /** Applet khach duoc phep: khachAnToan.KhachApplet */
    static final byte[] AID_KHACH = {
        0x11, 0x22, 0x33, 0x44, 0x55, 0x05, 0x03, 0x01
    };

    private final DichVu dichVu;

    private ChuApplet(byte[] bArray, short bOffset) {
        short p = (short) (bOffset + (bArray[bOffset] & 0x00FF) + 1);
        p = (short) (p + (bArray[p] & 0x00FF) + 1);
        short la = (short) (bArray[p] & 0x00FF);
        if (la != 0 && la != DichVu.DO_DAI) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        dichVu = new DichVu(bArray, (short) (p + 1), la);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new ChuApplet(bArray, bOffset).register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
    }

    // Lop bao ve 1: JCRE goi ham nay, kem AID cua applet dang xin
    public Shareable getShareableInterfaceObject(AID clientAID, byte parameter) {
        if (parameter != (byte) 0x00 || clientAID == null
                || !clientAID.equals(AID_KHACH, (short) 0, (byte) AID_KHACH.length)) {
            return null;
        }
        return dichVu;
    }
}
