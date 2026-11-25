package slavePack;

import javacard.framework.*;
import masterPack.masterInterface;

/**
 * TH5 muc 5.5, kich ban 1: tham so va gia tri tra ve la kieu co ban.
 *
 *   00 00 01 02   -> 08 90 00      (6*8 + 4*9) / 10 = 8
 */
public class slaveAppDiem extends Applet {

    final static byte[] serverAID = new byte[] {
        0x11, 0x22, 0x33, 0x44, 0x55, 0x05, 0x00, 0x00
    };

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new slaveAppDiem().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buf = apdu.getBuffer();
        switch (buf[ISO7816.OFFSET_INS]) {
        case (byte) 0x00:
            AID masterAID = JCSystem.lookupAID(serverAID, (short) 0, (byte) serverAID.length);
            masterInterface sio = (masterInterface) (JCSystem.getAppletShareableInterfaceObject(masterAID, (byte) 0x00));
            /* su dung cac kieu du lieu co ban lam tham so va gia tri tra ve */
            byte diemToan = (byte) 0x08;
            byte diemVan = (byte) 0x09;
            byte diemTB = sio.tinhDiem(diemToan, diemVan);
            buf[0] = diemTB;
            apdu.setOutgoingAndSend((short) 0, (short) 1);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
