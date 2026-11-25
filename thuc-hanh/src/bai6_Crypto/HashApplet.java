package bai6_Crypto;

import javacard.framework.*;
import javacard.security.MessageDigest;

/**
 * TH6 slide 2-5: ham bam.
 *
 *   00 01 00 00 Lc <du lieu> 00   MD5      (16 byte)
 *   00 02 00 00 Lc <du lieu> 00   SHA-1    (20 byte)
 *   00 03 00 00 Lc <du lieu> 00   SHA-256  (32 byte)
 *
 * Vi du slide 3: SHA-1("Fox") = DFCD3454 BBEA788A 751A696C 24D97009 CA992D17
 *   00 02 00 00 03 46 6F 78 00
 *
 * The khong ho tro thuat toan nao thi lenh do tra 6A 81.
 */
public class HashApplet extends Applet {

    private static final byte INS_MD5 = (byte) 0x01;
    private static final byte INS_SHA1 = (byte) 0x02;
    private static final byte INS_SHA256 = (byte) 0x03;

    private MessageDigest md5;
    private MessageDigest sha1;
    private MessageDigest sha256;
    private byte[] ketQua;

    private HashApplet() {
        md5 = tao(MessageDigest.ALG_MD5);
        sha1 = tao(MessageDigest.ALG_SHA);
        sha256 = tao(MessageDigest.ALG_SHA_256);
        ketQua = JCSystem.makeTransientByteArray((short) 32, JCSystem.CLEAR_ON_DESELECT);
    }

    private static MessageDigest tao(byte thuatToan) {
        try {
            return MessageDigest.getInstance(thuatToan, false);
        } catch (Exception e) {
            return null; // the khong ho tro
        }
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new HashApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_MD5:
            bam(apdu, md5);
            return;
        case INS_SHA1:
            bam(apdu, sha1);
            return;
        case INS_SHA256:
            bam(apdu, sha256);
            return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void bam(APDU apdu, MessageDigest md) {
        if (md == null) {
            ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
        }
        byte[] buf = apdu.getBuffer();
        md.reset();
        // du lieu co the den thanh nhieu doan: dua tung doan vao update()
        short conLai = (short) (buf[ISO7816.OFFSET_LC] & 0x00FF);
        short n = apdu.setIncomingAndReceive();
        while (n < conLai) {
            md.update(buf, ISO7816.OFFSET_CDATA, n);
            conLai -= n;
            n = apdu.receiveBytes(ISO7816.OFFSET_CDATA);
        }
        short doDai = md.doFinal(buf, ISO7816.OFFSET_CDATA, n, ketQua, (short) 0);
        Util.arrayCopyNonAtomic(ketQua, (short) 0, buf, (short) 0, doDai);
        apdu.setOutgoingAndSend((short) 0, doDai);
    }
}
