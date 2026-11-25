package slavePack;

import javacard.framework.*;
import masterPack.masterInterface;

/**
 * TH5 muc 5.4 - applet KHACH: goi getArray() cua applet chu.
 * Dung bo dem APDU (mang toan cuc) lam tham so nen tuong lua cho phep.
 *
 *   00 A4 04 00 08 11 22 33 44 55 05 01 01 00    chon slaveApp
 *   00 00 01 02                                  -> 01 02 03 04 05 90 00
 *
 * Them so voi de: bao 6A 82 neu chua cai applet chu, 69 85 neu bi tu choi.
 */
public class slaveApp extends Applet {

    final static byte[] serverAID = new byte[] {
        0x11, 0x22, 0x33, 0x44, 0x55, 0x05, 0x00, 0x00
    };

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new slaveApp().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case (byte) 0x00:
            AID masterAID = JCSystem.lookupAID(serverAID, (short) 0, (byte) serverAID.length);
            if (masterAID == null) {
                ISOException.throwIt(ISO7816.SW_FILE_NOT_FOUND);
            }
            masterInterface sio = (masterInterface) (JCSystem.getAppletShareableInterfaceObject(masterAID, (byte) 0x00));
            if (sio == null) {
                ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
            }
            short len = sio.getArray(buf);
            apdu.setOutgoingAndSend((short) 0, len);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
