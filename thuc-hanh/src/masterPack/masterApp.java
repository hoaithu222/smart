package masterPack;

import javacard.framework.*;

/**
 * TH5 muc 5.4 - applet CHU: chia se mang testArray cho applet khac.
 *
 * AID applet: 11 22 33 44 55 05 00 00 (applet khach tim theo AID nay).
 * Phai cai applet chu TRUOC khi goi applet khach.
 */
public class masterApp extends Applet implements masterInterface {

    private byte[] testArray;

    private masterApp() {
        testArray = new byte[] {0x01, 0x02, 0x03, 0x04, 0x05};
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new masterApp().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case (byte) 0x00:
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    // JCRE goi ham nay khi applet khac xin doi tuong chia se
    public Shareable getShareableInterfaceObject(AID clientAID, byte parameter) {
        // xac thuc nguoi dung
        if (parameter != (byte) 0x00) {
            return null;
        }
        return this;
    }

    public short getArray(byte[] buf) {
        short len = (short) testArray.length;
        Util.arrayCopy(testArray, (short) 0, buf, (short) 0, len);
        return len;
    }

    public byte tinhDiem(byte diemToan, byte diemVan) {
        byte diemTB = (byte) ((byte) ((byte) (6 * diemToan) + (byte) (4 * diemVan)) / 10);
        return diemTB;
    }
}
