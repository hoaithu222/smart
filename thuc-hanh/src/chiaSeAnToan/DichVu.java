package chiaSeAnToan;

import javacard.framework.*;
import javacard.security.AESKey;
import javacard.security.KeyBuilder;
import javacard.security.RandomData;
import javacardx.crypto.Cipher;

/**
 * Doi tuong chia se (SIO). Cac ham nay chay trong BOI CANH CUA APPLET CHU, con
 * applet duoc chon van la applet khach.
 *
 * Vi vay trang thai tam phai la CLEAR_ON_RESET: mang CLEAR_ON_DESELECT cua chu
 * KHONG truy cap duoc khi applet dang duoc chon thuoc boi canh khac
 * (SecurityException tren the that).
 */
class DichVu implements DichVuSinhVien {

    static final short DO_DAI = (short) 16;
    private static final short CO_THU_THACH = (short) 0;
    private static final short CO_QUYEN = (short) 1;

    private static final byte[] KHOA_MAU = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };
    private static final byte[] MA_SV_MAU = {
        'C', 'T', '0', '0', '0', '0', '0', '1'
    };

    private final AESKey khoa;
    private final Cipher aes;
    private final RandomData ngauNhien;
    private final byte[] maSV;
    private final byte[] thuThach;
    private final byte[] tam;
    private final byte[] co;

    DichVu(byte[] bArray, short off, short la) {
        khoa = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        aes = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
        ngauNhien = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        maSV = new byte[(short) MA_SV_MAU.length];
        Util.arrayCopy(MA_SV_MAU, (short) 0, maSV, (short) 0, (short) MA_SV_MAU.length);
        thuThach = JCSystem.makeTransientByteArray(DO_DAI, JCSystem.CLEAR_ON_RESET);
        tam = JCSystem.makeTransientByteArray(DO_DAI, JCSystem.CLEAR_ON_RESET);
        co = JCSystem.makeTransientByteArray((short) 2, JCSystem.CLEAR_ON_RESET);
        if (la == 0) {
            khoa.setKey(KHOA_MAU, (short) 0);
        } else {
            khoa.setKey(bArray, off);
        }
    }

    /** Lop bao ve 2: chi dung applet khach moi goi duoc, ke ca khi tham chieu bi chuyen tay. */
    private static void kiemNguoiGoi() {
        AID goi = JCSystem.getPreviousContextAID();
        if (goi == null || !goi.equals(ChuApplet.AID_KHACH, (short) 0, (byte) ChuApplet.AID_KHACH.length)) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
    }

    public short layThuThach(byte[] buf, short off) {
        kiemNguoiGoi();
        ngauNhien.generateData(thuThach, (short) 0, DO_DAI);
        co[CO_THU_THACH] = 1;
        co[CO_QUYEN] = 0;
        Util.arrayCopyNonAtomic(thuThach, (short) 0, buf, off, DO_DAI);
        return DO_DAI;
    }

    public boolean traLoi(byte[] buf, short off) {
        kiemNguoiGoi();
        if (co[CO_THU_THACH] != 1) {
            return false;
        }
        co[CO_THU_THACH] = 0; // moi thu thach chi tra loi mot lan
        aes.init(khoa, Cipher.MODE_ENCRYPT);
        aes.doFinal(thuThach, (short) 0, DO_DAI, tam, (short) 0);
        boolean dung = Util.arrayCompare(tam, (short) 0, buf, off, DO_DAI) == 0;
        Util.arrayFillNonAtomic(tam, (short) 0, DO_DAI, (byte) 0);
        co[CO_QUYEN] = dung ? (byte) 1 : (byte) 0;
        return dung;
    }

    public short docMaSV(byte[] buf, short off) {
        kiemNguoiGoi();
        if (co[CO_QUYEN] != 1) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
        co[CO_QUYEN] = 0; // lop bao ve 3: moi lan cap quyen chi doc mot lan
        Util.arrayCopyNonAtomic(maSV, (short) 0, buf, off, (short) maSV.length);
        return (short) maSV.length;
    }
}
