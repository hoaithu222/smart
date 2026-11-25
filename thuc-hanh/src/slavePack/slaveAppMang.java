package slavePack;

import javacard.framework.*;
import masterPack.masterInterface;

/**
 * TH5 muc 5.5 - truong hop LOI: truyen mang RIENG cua applet khach lam tham so.
 * Tuong lua khong cho applet chu ghi vao mang nay -> SecurityException.
 *
 *   00 00 01 02   -> 6F 00  tren the that / JCIDE
 *
 * Luu y: jCardSim KHONG mo phong tuong lua nen tren gia lap lai tra ve du lieu.
 *
 * (De tao "new byte[5]" trong process(); o day tao 1 lan trong ham tao.)
 */
public class slaveAppMang extends Applet {

    final static byte[] serverAID = new byte[] {
        0x11, 0x22, 0x33, 0x44, 0x55, 0x05, 0x00, 0x00
    };

    private byte[] array;

    private slaveAppMang() {
        array = new byte[5];
    }

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new slaveAppMang().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
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
            // su dung mang cua minh lam doi so cua giao dien chia se
            short len = sio.getArray(array);
            Util.arrayCopy(array, (short) 0, buf, (short) 0, len);
            apdu.setOutgoingAndSend((short) 0, len);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
