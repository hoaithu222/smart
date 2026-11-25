package slavePack;

import javacard.framework.*;
import masterPack.masterInterface;

/**
 * TH5 muc 5.5, kich ban 2: dung truong TINH cua lop sinhVien.
 *
 *   00 00 01 02   -> 08 90 00
 *
 * (De khai bao lop slaveApp1 nhung install() goi "new slaveApp()"; o day sua lai
 *  thanh "new slaveApp1()".)
 *
 * De y: bien sv1 khong he duoc "new" (van la null). Truy cap truong static qua
 * tham chieu null van chay duoc, vi truong static thuoc ve LOP, khong thuoc
 * doi tuong.
 */
public class slaveApp1 extends Applet {

    final static byte[] serverAID = new byte[] {
        0x11, 0x22, 0x33, 0x44, 0x55, 0x05, 0x00, 0x00
    };

    sinhVien sv1;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new slaveApp1().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
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
            /* su dung cac truong cua doi tuong sinhVien */
            sv1.diemToan = (byte) 0x08;
            sv1.diemVan = (byte) 0x09;
            sv1.diemTB = sio.tinhDiem(sv1.diemToan, sv1.diemVan);
            buf[0] = sv1.diemTB;
            apdu.setOutgoingAndSend((short) 0, (short) 1);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
