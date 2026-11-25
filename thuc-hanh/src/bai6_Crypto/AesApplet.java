package bai6_Crypto;

import javacard.framework.*;
import javacard.security.AESKey;
import javacard.security.KeyBuilder;
import javacardx.crypto.Cipher;

/**
 * TH6 slide 11-13: mat ma khoa bi mat AES (khoi 128 bit, khoa 128/192/256 bit).
 *
 *   00 10 00 00 Lc <khoa>           nap khoa: 16, 24 hoac 32 byte
 *   00 11 P1 P2 Lc <ro> 00          ma hoa
 *   00 12 P1 P2 Lc <ma> 00          giai ma
 *      P1 = 00 ECB, 01 CBC (IV = 16 byte 00)
 *      P2 = 00 khong dem (do dai phai la boi cua 16)
 *           01 dem ISO 9797-1 cach 2: them 80 roi 00 cho du khoi
 *
 * Vi du chuan FIPS-197: khoa 00..0F, ro 00112233...EEFF
 *      -> 69C4E0D86A7B0430D8CDB78070B4C55A
 *
 * Loi: 69 85 chua nap khoa, 67 00 sai do dai, 6A 80 du lieu dem sai.
 * Ghi chu: ECB va IV co dinh chi de hoc; san pham that dung CBC voi IV ngau nhien.
 */
public class AesApplet extends Applet {

    private static final byte INS_NAP_KHOA = (byte) 0x10;
    private static final byte INS_MA_HOA = (byte) 0x11;
    private static final byte INS_GIAI_MA = (byte) 0x12;

    private static final short KHOI = (short) 16;
    private static final short RO_TOI_DA = (short) 223;   // sau khi dem <= 224
    private static final short MA_TOI_DA = (short) 240;

    private AESKey aes128;
    private AESKey aes192;
    private AESKey aes256;
    private AESKey khoa;       // khoa dang dung
    private Cipher ecb;
    private Cipher cbc;
    private byte[] tam;

    private AesApplet() {
        aes128 = taoKhoa(KeyBuilder.LENGTH_AES_128);
        aes192 = taoKhoa(KeyBuilder.LENGTH_AES_192);
        aes256 = taoKhoa(KeyBuilder.LENGTH_AES_256);
        ecb = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
        cbc = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
        tam = JCSystem.makeTransientByteArray((short) 256, JCSystem.CLEAR_ON_DESELECT);
    }

    private static AESKey taoKhoa(short soBit) {
        try {
            return (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, soBit, false);
        } catch (Exception e) {
            return null; // the khong ho tro do dai nay
        }
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new AesApplet().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_NAP_KHOA:
            napKhoa(apdu);
            return;
        case INS_MA_HOA:
            maHoa(apdu, true);
            return;
        case INS_GIAI_MA:
            maHoa(apdu, false);
            return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
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

    private void napKhoa(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short n = nhan(apdu);
        AESKey moi = null;
        if (n == 16) {
            moi = aes128;
        } else if (n == 24) {
            moi = aes192;
        } else if (n == 32) {
            moi = aes256;
        } else {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        if (moi == null) {
            ISOException.throwIt(ISO7816.SW_FUNC_NOT_SUPPORTED);
        }
        moi.setKey(buf, ISO7816.OFFSET_CDATA);
        khoa = moi;
        Util.arrayFillNonAtomic(buf, ISO7816.OFFSET_CDATA, n, (byte) 0);
    }

    private void maHoa(APDU apdu, boolean laMaHoa) {
        byte[] buf = apdu.getBuffer();
        byte cheDo = buf[ISO7816.OFFSET_P1];
        byte dem = buf[ISO7816.OFFSET_P2];
        if ((cheDo != 0 && cheDo != 1) || (dem != 0 && dem != 1)) {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
        if (khoa == null || !khoa.isInitialized()) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        short n = nhan(apdu);
        if (n == 0 || n > MA_TOI_DA) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        Util.arrayCopyNonAtomic(buf, ISO7816.OFFSET_CDATA, tam, (short) 0, n);

        if (laMaHoa && dem == 1) {
            if (n > RO_TOI_DA) {
                ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
            }
            // them 80 00 00 ... cho tron khoi (luon them it nhat 1 byte)
            tam[n] = (byte) 0x80;
            n++;
            while ((short) (n % KHOI) != 0) {
                tam[n] = 0;
                n++;
            }
        } else if ((short) (n % KHOI) != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }

        Cipher c = (cheDo == 0) ? ecb : cbc;
        c.init(khoa, laMaHoa ? Cipher.MODE_ENCRYPT : Cipher.MODE_DECRYPT);
        short ra = c.doFinal(tam, (short) 0, n, buf, (short) 0);

        if (!laMaHoa && dem == 1) {
            // bo phan dem: tu cuoi lui qua cac byte 00 toi byte 80
            short i = (short) (ra - 1);
            while (i >= 0 && buf[i] == 0) {
                i--;
            }
            if (i < 0 || buf[i] != (byte) 0x80 || (short) (ra - i) > KHOI) {
                ISOException.throwIt(ISO7816.SW_WRONG_DATA);
            }
            ra = i;
        }
        apdu.setOutgoingAndSend((short) 0, ra);
    }
}
