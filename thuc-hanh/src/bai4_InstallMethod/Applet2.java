package bai4_InstallMethod;

import javacard.framework.*;

/**
 * TH4 muc 4.4 - Vi du: doc tham so cai dat.
 *
 * JCIDE (Debug > Run or Debug Configuration > Edit):
 *   Instance AID:                    44 49 45 4D 5F 54 48 49      ("DIEM_THI")
 *   Application Specific Parameters: 53 56 30 31 09               ("SV01", 9 mon)
 *
 *   00 00 01 02   ->  53 56 30 31 09
 *
 * Luu y de: phan chu cua muc 4.4 ghi mac dinh 9 mon, nhung ma trong de gan 0x08
 * khi khong co tham so. Giu dung ma cua de.
 *
 * Hai cho khac de (nen hieu de tra loi van dap):
 *   1. De khai bao sinhVien, soLuongMonThi la "static": cai hai the hien thi
 *      dung chung, cai sau de len cai truoc. O day bo static.
 *   2. De goi register() truoc khi doc tham so. Neu loi xay ra sau register(),
 *      JCRE van coi nhu da cai. O day register() dat cuoi, sau khi kiem tra va
 *      cap phat xong.
 */
public class Applet2 extends Applet {

    final static byte SV_ID_LENGTH = (byte) 0x04;

    private byte[] sinhVien;
    private byte soLuongMonThi;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet2(bArray, bOffset, bLength);
    }

    private Applet2(byte[] bArray, short bOffset, byte bLength) {
        short dau = bOffset; // nho vi tri byte Li de register o cuoi
        // kiem tra du lieu den
        byte iLen = bArray[bOffset]; // do dai AID
        bOffset = (short) (bOffset + iLen + 1);
        byte cLen = bArray[bOffset]; // do dai thong tin dieu khien
        bOffset = (short) (bOffset + cLen + 1);
        byte aLen = bArray[bOffset]; // do dai du lieu Applet
        bOffset = (short) (bOffset + 1);
        // doc du lieu Applet
        if (aLen != 0) {
            if (aLen < (byte) (SV_ID_LENGTH + 1)) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            // gan ID cua sinh vien
            sinhVien = new byte[SV_ID_LENGTH];
            Util.arrayCopy(bArray, bOffset, sinhVien, (short) 0, SV_ID_LENGTH);
            bOffset = (short) (bOffset + SV_ID_LENGTH);
            // gan so mon thi
            soLuongMonThi = bArray[bOffset];
        } else {
            sinhVien = new byte[] {'S', 'V', '0', '1'};
            soLuongMonThi = (byte) 0x08;
        }

        if (iLen == 0) {
            // dang ky Applet voi JCRE su dung AID mac dinh
            register();
        } else {
            // dang ky Applet voi AID duoc chi dinh trong tham so cai dat
            register(bArray, (short) (dau + 1), iLen);
        }
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case (byte) 0x00:
            // in ra ID cua sinh vien va so luong mon thi
            apdu.setOutgoing();
            apdu.setOutgoingLength((short) 5);
            apdu.sendBytesLong(sinhVien, (short) 0, SV_ID_LENGTH);
            buf[0] = soLuongMonThi;
            apdu.sendBytes((short) 0, (short) 1);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
