package slavePack;

import javacard.framework.*;
import masterPack.masterInterface;

/**
 * TH5 muc 5.5 - "cho mot truong cua sinhVien khong static" -> 6F 00.
 *
 *   00 00 01 02   -> 6F 00
 *
 * Giai thich khi van dap: loi xay ra vi sv chua duoc "new" (null), nen truy cap
 * truong thuong nem NullPointerException. Neu tao doi tuong trong ham tao
 * (sv = new sinhVienThuong()) thi chay binh thuong, vi gia tri truyen sang
 * applet chu van chi la byte (kieu co ban).
 */
public class slaveAppNonStatic extends Applet {

    final static byte[] serverAID = new byte[] {
        0x11, 0x22, 0x33, 0x44, 0x55, 0x05, 0x00, 0x00
    };

    sinhVienThuong sv;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new slaveAppNonStatic().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
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
            sv.diemToan = (byte) 0x08;
            sv.diemVan = (byte) 0x09;
            sv.diemTB = sio.tinhDiem(sv.diemToan, sv.diemVan);
            buf[0] = sv.diemTB;
            apdu.setOutgoingAndSend((short) 0, (short) 1);
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
