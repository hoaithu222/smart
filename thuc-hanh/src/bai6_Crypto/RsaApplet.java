package bai6_Crypto;

import javacard.framework.*;
import javacard.security.KeyBuilder;
import javacard.security.KeyPair;
import javacard.security.RSAPublicKey;
import javacard.security.Signature;
import javacardx.crypto.Cipher;

/**
 * TH6 slide 6-10: mat ma khoa cong khai RSA 1024 bit.
 *
 *   00 20 00 00                    sinh cap khoa
 *   00 21 00 00 80                 lay modulus (128 byte)
 *   00 21 01 00 03                 lay so mu cong khai (01 00 01)
 *   00 22 00 00 Lc <ro> 00         ma hoa bang khoa CONG KHAI (ro <= 117 byte)
 *   00 23 00 00 80 <ma 128> 00     giai ma bang khoa BI MAT
 *   00 24 00 00 Lc <thong diep> 00 ky so SHA-1 + PKCS#1 -> chu ky 128 byte
 *   00 25 00 00 Lc <Lm> <thong diep> <chu ky 128>   kiem chu ky -> 01 dung / 00 sai
 *
 * Xac thuc challenge-response (slide 10): may tinh gui so ngau nhien bang lenh 24,
 * the ky; may tinh kiem chu ky bang khoa cong khai lay o lenh 21.
 *
 * Khoa bi mat khong bao gio ra khoi the. Chua sinh khoa -> 69 85.
 */
public class RsaApplet extends Applet {

    private static final byte INS_SINH_KHOA = (byte) 0x20;
    private static final byte INS_KHOA_CONG_KHAI = (byte) 0x21;
    private static final byte INS_MA_HOA = (byte) 0x22;
    private static final byte INS_GIAI_MA = (byte) 0x23;
    private static final byte INS_KY = (byte) 0x24;
    private static final byte INS_KIEM_CHU_KY = (byte) 0x25;

    private static final short DO_DAI = (short) 128;      // 1024 bit
    private static final short RO_TOI_DA = (short) 117;   // 128 - 11 byte dem PKCS#1

    private KeyPair capKhoa;
    private Signature chuKy;
    private Cipher rsa;
    private byte[] tam;

    private RsaApplet() {
        capKhoa = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
        chuKy = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
        rsa = Cipher.getInstance(Cipher.ALG_RSA_PKCS1, false);
        tam = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_DESELECT);
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new RsaApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        byte ins = buf[ISO7816.OFFSET_INS];
        if (ins == INS_SINH_KHOA) {
            capKhoa.genKeyPair();
            return;
        }
        if (ins < INS_KHOA_CONG_KHAI || ins > INS_KIEM_CHU_KY) {
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
        // jCardSim tra null truoc khi sinh khoa; the that tra khoa chua khoi tao
        if (capKhoa.getPublic() == null || !capKhoa.getPublic().isInitialized()) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        switch (ins) {
        case INS_KHOA_CONG_KHAI:
            khoaCongKhai(apdu);
            return;
        case INS_MA_HOA:
            maHoa(apdu);
            return;
        case INS_GIAI_MA:
            giaiMa(apdu);
            return;
        case INS_KY:
            ky(apdu);
            return;
        default:
            kiemChuKy(apdu);
            return;
        }
    }

    private short nhan(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short lc = (short) (buf[ISO7816.OFFSET_LC] & 0x00FF);
        short n = apdu.setIncomingAndReceive();
        while (n < lc) {
            n += apdu.receiveBytes((short) (ISO7816.OFFSET_CDATA + n));
        }
        return lc;
    }

    private void khoaCongKhai(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        RSAPublicKey k = (RSAPublicKey) capKhoa.getPublic();
        byte p1 = buf[ISO7816.OFFSET_P1];
        short n = 0;
        if (p1 == 0) {
            n = k.getModulus(tam, (short) 0);
        } else if (p1 == 1) {
            n = k.getExponent(tam, (short) 0);
        } else {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
        short dau = 0;
        // jCardSim tra modulus kem 1 byte 00 o dau; the that tra dung 128 byte
        if (p1 == 0 && n > DO_DAI) {
            dau = (short) (n - DO_DAI);
        }
        short gui = (short) (n - dau);
        Util.arrayCopyNonAtomic(tam, dau, buf, (short) 0, gui);
        apdu.setOutgoingAndSend((short) 0, gui);
    }

    private void maHoa(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short n = nhan(apdu);
        if (n == 0 || n > RO_TOI_DA) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        rsa.init(capKhoa.getPublic(), Cipher.MODE_ENCRYPT);
        short ra = rsa.doFinal(buf, ISO7816.OFFSET_CDATA, n, tam, (short) 0);
        Util.arrayCopyNonAtomic(tam, (short) 0, buf, (short) 0, ra);
        apdu.setOutgoingAndSend((short) 0, ra);
    }

    private void giaiMa(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short n = nhan(apdu);
        if (n != DO_DAI) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        rsa.init(capKhoa.getPrivate(), Cipher.MODE_DECRYPT);
        short ra = 0;
        try {
            ra = rsa.doFinal(buf, ISO7816.OFFSET_CDATA, n, tam, (short) 0);
        } catch (Exception e) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        Util.arrayCopyNonAtomic(tam, (short) 0, buf, (short) 0, ra);
        apdu.setOutgoingAndSend((short) 0, ra);
    }

    private void ky(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short n = nhan(apdu);
        if (n == 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        chuKy.init(capKhoa.getPrivate(), Signature.MODE_SIGN);
        short ra = chuKy.sign(buf, ISO7816.OFFSET_CDATA, n, tam, (short) 0);
        Util.arrayCopyNonAtomic(tam, (short) 0, buf, (short) 0, ra);
        apdu.setOutgoingAndSend((short) 0, ra);
    }

    private void kiemChuKy(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short n = nhan(apdu);
        short lm = (short) (buf[ISO7816.OFFSET_CDATA] & 0x00FF);
        if (n < (short) (1 + DO_DAI) || lm == 0 || n != (short) (1 + lm + DO_DAI)) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short dauThongDiep = (short) (ISO7816.OFFSET_CDATA + 1);
        chuKy.init(capKhoa.getPublic(), Signature.MODE_VERIFY);
        boolean dung = chuKy.verify(buf, dauThongDiep, lm,
                buf, (short) (dauThongDiep + lm), DO_DAI);
        buf[0] = dung ? (byte) 1 : (byte) 0;
        apdu.setOutgoingAndSend((short) 0, (short) 1);
    }
}
