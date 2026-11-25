package bai4_InstallMethod;

import javacard.framework.*;

/**
 * TH4 muc 4.5 - BAI TAP: luu diem thi sinh vien.
 *
 * Cai dat:
 *   Instance AID:                    44 49 45 4D 5F 54 48 49    ("DIEM_THI")
 *   Application Specific Parameters: 53 56 30 31 09             (ID "SV01", 9 mon)
 *   Bo trong tham so -> mac dinh SV01, 9 mon.
 *
 * Mang diemThi: moi mon 2 byte lien tiep [ID mon][diem], diem 0..10 (00..0A).
 *
 *   00 00 00 00 00               thong tin: ID SV (4) | so mon toi da | so da nhap
 *   00 01 00 00 Lc [id diem]...  NHAP diem (mot hoac nhieu cap)
 *   00 02 00 00 00               IN tat ca diem
 *   00 03 00 00 Lc [id diem]...  SUA diem
 *   00 04 00 00 Lc [id]...       XOA diem
 *
 * Sau NHAP, SUA, XOA the tra ve toan bo bang diem (dung yeu cau de).
 * Mot lenh nhieu mon: mot cap sai thi KHONG cap nao duoc ghi.
 *
 * Loi: 6A 80 du lieu sai    6A 89 trung ID mon    6A 84 vuot so mon thi
 *      6A 83 khong co mon do
 */
public class Applet3 extends Applet {

    private static final byte INS_THONG_TIN = (byte) 0x00;
    private static final byte INS_NHAP = (byte) 0x01;
    private static final byte INS_IN = (byte) 0x02;
    private static final byte INS_SUA = (byte) 0x03;
    private static final byte INS_XOA = (byte) 0x04;

    private static final short SV_ID_LENGTH = (short) 4;
    private static final short SO_MON_TOI_DA = (short) 60;
    private static final byte DIEM_TOI_DA = (byte) 10;
    private static final short SW_TRUNG_MON = (short) 0x6A89;

    private byte[] sinhVien;
    private byte[] diemThi;
    private short soLuongMonThi;
    private short soDaNhap;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet3(bArray, bOffset, bLength);
    }

    private Applet3(byte[] bArray, short bOffset, byte bLength) {
        short iLen = (short) (bArray[bOffset] & 0x00FF);
        short p = (short) (bOffset + iLen + 1);
        p = (short) (p + (bArray[p] & 0x00FF) + 1);
        short aLen = (short) (bArray[p] & 0x00FF);
        p++;

        sinhVien = new byte[SV_ID_LENGTH];
        if (aLen == 0) {
            sinhVien[0] = 'S';
            sinhVien[1] = 'V';
            sinhVien[2] = '0';
            sinhVien[3] = '1';
            soLuongMonThi = 9;
        } else {
            if (aLen != (short) (SV_ID_LENGTH + 1)) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            Util.arrayCopy(bArray, p, sinhVien, (short) 0, SV_ID_LENGTH);
            soLuongMonThi = (short) (bArray[(short) (p + SV_ID_LENGTH)] & 0x00FF);
            if (soLuongMonThi == 0 || soLuongMonThi > SO_MON_TOI_DA) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
        }
        // cap phat du ngay tu dau (muc 4.1)
        diemThi = new byte[(short) (soLuongMonThi * 2)];

        if (iLen == 0) {
            register();
        } else {
            register(bArray, (short) (bOffset + 1), (byte) iLen);
        }
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_THONG_TIN:
            Util.arrayCopyNonAtomic(sinhVien, (short) 0, buf, (short) 0, SV_ID_LENGTH);
            buf[4] = (byte) soLuongMonThi;
            buf[5] = (byte) soDaNhap;
            apdu.setOutgoingAndSend((short) 0, (short) 6);
            return;
        case INS_NHAP:
            nhapDiem(apdu, nhanDuLieu(apdu));
            break;
        case INS_IN:
            break;
        case INS_SUA:
            suaDiem(apdu, nhanDuLieu(apdu));
            break;
        case INS_XOA:
            xoaDiem(apdu, nhanDuLieu(apdu));
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        inDiem(apdu);
    }

    /** Nhan toan bo du lieu lenh vao bo dem APDU, tra ve do dai. */
    private short nhanDuLieu(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short lc = (short) (buf[ISO7816.OFFSET_LC] & 0x00FF);
        if (lc == 0 || lc > (short) (SO_MON_TOI_DA * 2)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short daNhan = apdu.setIncomingAndReceive();
        while (daNhan < lc) {
            daNhan += apdu.receiveBytes((short) (ISO7816.OFFSET_CDATA + daNhan));
        }
        return lc;
    }

    /** Vi tri cap diem cua mon 'id' trong diemThi, -1 neu chua co. */
    private short timMon(byte id) {
        for (short i = 0; i < (short) (soDaNhap * 2); i += 2) {
            if (diemThi[i] == id) {
                return i;
            }
        }
        return (short) -1;
    }

    /** ID tai vi tri 'cuoi' co xuat hien truoc do trong du lieu lenh khong. */
    private boolean lapTrongLenh(byte[] buf, short cuoi, short buoc) {
        for (short i = ISO7816.OFFSET_CDATA; i < cuoi; i += buoc) {
            if (buf[i] == buf[cuoi]) {
                return true;
            }
        }
        return false;
    }

    private void kiemTraDiem(byte diem) {
        if (diem < 0 || diem > DIEM_TOI_DA) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
    }

    private void nhapDiem(APDU apdu, short lc) {
        byte[] buf = apdu.getBuffer();
        if ((short) (lc & 1) != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        short het = (short) (ISO7816.OFFSET_CDATA + lc);
        // kiem tra het roi moi ghi
        for (short i = ISO7816.OFFSET_CDATA; i < het; i += 2) {
            kiemTraDiem(buf[(short) (i + 1)]);
            if (timMon(buf[i]) >= 0 || lapTrongLenh(buf, i, (short) 2)) {
                ISOException.throwIt(SW_TRUNG_MON);
            }
        }
        if ((short) (soDaNhap + (short) (lc / 2)) > soLuongMonThi) {
            ISOException.throwIt(ISO7816.SW_FILE_FULL);
        }
        JCSystem.beginTransaction();
        Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, diemThi, (short) (soDaNhap * 2), lc);
        soDaNhap = (short) (soDaNhap + (short) (lc / 2));
        JCSystem.commitTransaction();
    }

    private void suaDiem(APDU apdu, short lc) {
        byte[] buf = apdu.getBuffer();
        if ((short) (lc & 1) != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        short het = (short) (ISO7816.OFFSET_CDATA + lc);
        for (short i = ISO7816.OFFSET_CDATA; i < het; i += 2) {
            kiemTraDiem(buf[(short) (i + 1)]);
            if (timMon(buf[i]) < 0) {
                ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
            }
        }
        JCSystem.beginTransaction();
        for (short i = ISO7816.OFFSET_CDATA; i < het; i += 2) {
            diemThi[(short) (timMon(buf[i]) + 1)] = buf[(short) (i + 1)];
        }
        JCSystem.commitTransaction();
    }

    private void xoaDiem(APDU apdu, short lc) {
        byte[] buf = apdu.getBuffer();
        short het = (short) (ISO7816.OFFSET_CDATA + lc);
        for (short i = ISO7816.OFFSET_CDATA; i < het; i++) {
            if (timMon(buf[i]) < 0 || lapTrongLenh(buf, i, (short) 1)) {
                ISOException.throwIt(ISO7816.SW_RECORD_NOT_FOUND);
            }
        }
        JCSystem.beginTransaction();
        for (short i = ISO7816.OFFSET_CDATA; i < het; i++) {
            short viTri = timMon(buf[i]);
            short sau = (short) (viTri + 2);
            short conLai = (short) ((short) (soDaNhap * 2) - sau);
            // don cac cap phia sau len, roi xoa cap thua o cuoi
            Util.arrayCopy(diemThi, sau, diemThi, viTri, conLai);
            soDaNhap--;
            // gan tung phan tu (khong dung arrayFillNonAtomic) de nam trong giao dich
            short cuoi = (short) (soDaNhap * 2);
            diemThi[cuoi] = 0;
            diemThi[(short) (cuoi + 1)] = 0;
        }
        JCSystem.commitTransaction();
    }

    private void inDiem(APDU apdu) {
        short n = (short) (soDaNhap * 2);
        apdu.setOutgoing();
        apdu.setOutgoingLength(n);
        apdu.sendBytesLong(diemThi, (short) 0, n);
    }
}
