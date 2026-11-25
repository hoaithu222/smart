package khachLa;

import javacard.framework.*;
import chiaSeAnToan.DichVuSinhVien;

/**
 * TH5 mo rong - applet la (khong co trong danh sach cua applet chu).
 *
 *   00 00 00 00   xin doi tuong chia se  -> 69 85 (applet chu tra null)
 *
 * Biet AID va giao dien van khong du: applet chu quyet dinh ai duoc nhan.
 * AID: 11 22 33 44 55 05 04 01
 */
public class KeXamNhap extends Applet {

    private static final byte[] AID_CHU = {
        0x11, 0x22, 0x33, 0x44, 0x55, 0x05, 0x02, 0x01
    };

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new KeXamNhap().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }
        byte[] buf = apdu.getBuffer();
        AID chu = JCSystem.lookupAID(AID_CHU, (short) 0, (byte) AID_CHU.length);
        if (chu == null) {
            ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
        }
        DichVuSinhVien dv = (DichVuSinhVien) JCSystem.getAppletShareableInterfaceObject(chu, (byte) 0);
        if (dv == null) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
        apdu.setOutgoingAndSend((short) 0, dv.docMaSV(buf, (short) 0));
    }
}
