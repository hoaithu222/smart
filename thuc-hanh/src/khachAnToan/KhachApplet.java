package khachAnToan;

import javacard.framework.*;
import javacard.security.AESKey;
import javacard.security.KeyBuilder;
import javacardx.crypto.Cipher;
import chiaSeAnToan.DichVuSinhVien;

/**
 * TH5 mo rong - applet KHACH duoc phep. Moi lenh minh hoa mot tinh huong:
 *
 *   00 00 00 00 00   dung quy trinh: thu thach -> phan hoi -> doc   -> ma SV
 *   00 01 00 00      phan hoi sai                                    -> 63 00
 *   00 02 00 00      doc ma khong tra loi thu thach                  -> 69 82
 *   00 03 00 00      dung mot lan cap quyen de doc hai lan           -> 69 82
 *   00 04 00 00 01   phat lai phan hoi cu cho thu thach moi          -> 00 (bi tu choi)
 *   00 05 00 00      xin doi tuong voi tham so sai                   -> 69 85
 *
 * Tham so cai dat (khong bat buoc): 16 byte khoa AES chung voi applet chu.
 * AID: 11 22 33 44 55 05 03 01
 */
public class KhachApplet extends Applet {

    private static final byte[] AID_CHU = {
        0x11, 0x22, 0x33, 0x44, 0x55, 0x05, 0x02, 0x01
    };
    private static final short DO_DAI = (short) 16;
    private static final byte[] KHOA_MAU = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };
    private static final short SW_PHAN_HOI_SAI = (short) 0x6300;

    private final AESKey khoa;
    private final Cipher aes;
    private final byte[] tam;

    private KhachApplet(byte[] bArray, short bOffset) {
        short p = (short) (bOffset + (bArray[bOffset] & 0x00FF) + 1);
        p = (short) (p + (bArray[p] & 0x00FF) + 1);
        short la = (short) (bArray[p] & 0x00FF);
        if (la != 0 && la != DO_DAI) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        khoa = (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        aes = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
        // khach la applet dang duoc chon khi dung mang nay, nen CLEAR_ON_DESELECT duoc
        tam = JCSystem.makeTransientByteArray((short) 32, JCSystem.CLEAR_ON_DESELECT);
        if (la == 0) {
            khoa.setKey(KHOA_MAU, (short) 0);
        } else {
            khoa.setKey(bArray, (short) (p + 1));
        }
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new KhachApplet(bArray, bOffset).register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        byte ins = buf[ISO7816.OFFSET_INS];
        if (ins > (byte) 0x05 || ins < 0) {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        byte thamSo = (ins == (byte) 0x05) ? (byte) 0x01 : (byte) 0x00;
        DichVuSinhVien dv = layDichVu(thamSo);

        switch (ins) {
        case (byte) 0x00:
            xacThuc(dv, buf);
            apdu.setOutgoingAndSend((short) 0, dv.docMaSV(buf, (short) 0));
            return;
        case (byte) 0x01:
            dv.layThuThach(buf, (short) 0);
            tinhPhanHoi(buf);
            buf[0] ^= (byte) 0x01; // lam sai 1 bit
            if (!dv.traLoi(buf, (short) 0)) {
                ISOException.throwIt(SW_PHAN_HOI_SAI);
            }
            return;
        case (byte) 0x02:
            apdu.setOutgoingAndSend((short) 0, dv.docMaSV(buf, (short) 0));
            return;
        case (byte) 0x03:
            xacThuc(dv, buf);
            dv.docMaSV(buf, (short) 0);
            apdu.setOutgoingAndSend((short) 0, dv.docMaSV(buf, (short) 0));
            return;
        default: // 0x04: phat lai
            dv.layThuThach(buf, (short) 0);
            tinhPhanHoi(buf);
            Util.arrayCopyNonAtomic(buf, (short) 0, tam, DO_DAI, DO_DAI); // "nghe trom"
            dv.traLoi(buf, (short) 0);
            dv.layThuThach(buf, (short) 0);                               // thu thach moi
            Util.arrayCopyNonAtomic(tam, DO_DAI, buf, (short) 0, DO_DAI); // gui lai cau cu
            buf[0] = dv.traLoi(buf, (short) 0) ? (byte) 1 : (byte) 0;
            apdu.setOutgoingAndSend((short) 0, (short) 1);
            return;
        }
    }

    private DichVuSinhVien layDichVu(byte thamSo) {
        AID chu = JCSystem.lookupAID(AID_CHU, (short) 0, (byte) AID_CHU.length);
        if (chu == null) {
            ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
        }
        DichVuSinhVien dv = (DichVuSinhVien) JCSystem.getAppletShareableInterfaceObject(chu, thamSo);
        if (dv == null) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        return dv;
    }

    private void xacThuc(DichVuSinhVien dv, byte[] buf) {
        dv.layThuThach(buf, (short) 0);
        tinhPhanHoi(buf);
        if (!dv.traLoi(buf, (short) 0)) {
            ISOException.throwIt(SW_PHAN_HOI_SAI);
        }
    }

    /** Thu thach o buf[0..15] -> phan hoi = AES(khoa, thu thach), ghi lai vao buf[0..15]. */
    private void tinhPhanHoi(byte[] buf) {
        Util.arrayCopyNonAtomic(buf, (short) 0, tam, (short) 0, DO_DAI);
        aes.init(khoa, Cipher.MODE_ENCRYPT);
        aes.doFinal(tam, (short) 0, DO_DAI, buf, (short) 0);
    }
}
