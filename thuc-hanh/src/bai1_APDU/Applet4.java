package bai1_APDU;

import javacard.framework.*;

/**
 * TH1 - Bai 4: nhap chuoi thong tin sinh vien (ma SV, ho ten, ngay sinh, que quan)
 * va in ra: tung truong rieng le va ca chuoi.
 *
 * Du lieu nhap la ASCII, 4 truong cach nhau bang dau '|' (7C):
 *      maSV|hoTen|ngaySinh|queQuan
 *
 *   00 01 00 00 Lc <chuoi>   nhap (toi da 128 byte)
 *   00 02 00 00 00           in ca chuoi
 *   00 02 01 00 00           in ma sinh vien
 *   00 02 02 00 00           in ho ten
 *   00 02 03 00 00           in ngay sinh
 *   00 02 04 00 00           in que quan
 *
 * Loi: 67 00 sai do dai, 6A 80 sai dinh dang, 69 85 chua nhap, 6A 86 sai P1.
 */
public class Applet4 extends Applet {

    private static final byte INS_NHAP = (byte) 0x01;
    private static final byte INS_IN = (byte) 0x02;

    private static final short TOI_DA = (short) 128;
    private static final byte DAU_CACH = (byte) '|';
    private static final short SO_TRUONG = (short) 4;

    // Du lieu ben (EEPROM): con nguyen sau khi rut the
    private byte[] thongTin;
    private short doDai;

    private Applet4() {
        thongTin = new byte[TOI_DA];
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet4().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_NHAP:
            nhap(apdu);
            return;
        case INS_IN:
            in(apdu);
            return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void nhap(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short lc = (short) (buf[ISO7816.OFFSET_LC] & 0x00FF);
        if (lc == 0 || lc > TOI_DA) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short daNhan = apdu.setIncomingAndReceive();
        while (daNhan < lc) {
            daNhan += apdu.receiveBytes((short) (ISO7816.OFFSET_CDATA + daNhan));
        }
        // kiem tra: dung 4 truong, khong truong nao rong
        short soDauCach = 0;
        short doDaiTruong = 0;
        for (short i = 0; i < lc; i++) {
            if (buf[(short) (ISO7816.OFFSET_CDATA + i)] == DAU_CACH) {
                if (doDaiTruong == 0) {
                    ISOException.throwIt(ISO7816.SW_WRONG_DATA);
                }
                soDauCach++;
                doDaiTruong = 0;
            } else {
                doDaiTruong++;
            }
        }
        if (soDauCach != (short) (SO_TRUONG - 1) || doDaiTruong == 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        // chuoi va do dai phai doi cung luc (bai thuc hanh 3)
        JCSystem.beginTransaction();
        Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, thongTin, (short) 0, lc);
        doDai = lc;
        JCSystem.commitTransaction();
    }

    private void in(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short truong = (short) (buf[ISO7816.OFFSET_P1] & 0x00FF);
        if (truong > SO_TRUONG) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
        if (doDai == 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        short dau = 0;
        short cuoi = doDai;
        if (truong != 0) {
            // tim vi tri bat dau va ket thuc cua truong thu 'truong' (1..4)
            short hienTai = 1;
            short i = 0;
            while (hienTai < truong) {
                if (thongTin[i] == DAU_CACH) {
                    hienTai++;
                }
                i++;
            }
            dau = i;
            while (i < doDai && thongTin[i] != DAU_CACH) {
                i++;
            }
            cuoi = i;
        }
        apdu.setOutgoing();
        apdu.setOutgoingLength((short) (cuoi - dau));
        apdu.sendBytesLong(thongTin, dau, (short) (cuoi - dau));
    }
}
