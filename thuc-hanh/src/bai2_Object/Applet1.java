package bai2_Object;

import javacard.framework.*;

/**
 * TH2 muc 2.3 - Vi du: doi tuong ben va doi tuong tam thoi.
 *
 *   x        bien toan cuc            ben (EEPROM)
 *   buffer   mang new                 ben (EEPROM)
 *   buffer1  CLEAR_ON_DESELECT        mat khi bo chon applet hoac reset
 *   buffer2  CLEAR_ON_RESET           mat khi reset the
 *
 *   00 00 00 00   gui len: x, buffer, buffer1, buffer2 (7 byte)
 *   00 01 00 00   cap nhat gia tri
 *
 * Khac de: de goi apdu.setIncomingAndReceive() truoc switch, nhung 2 lenh nay
 * khong co du lieu gui xuong nen bo di (goi no voi lenh co Le se loi tren T=0).
 */
public class Applet1 extends Applet {

    // khai bao cac doi tuong va bien
    private static byte[] buffer, buffer1, buffer2;
    private byte x;

    // khai bao ma INS trong tieu de cua apdu
    final static byte INS_SEND = (byte) 0x00;
    final static byte INS_UPDATE = (byte) 0x01;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new Applet1().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
        // khoi tao doi tuong lien tuc
        buffer = new byte[2];
        // khoi tao doi tuong tam thoi
        buffer1 = JCSystem.makeTransientByteArray((short) 2, JCSystem.CLEAR_ON_DESELECT);
        buffer2 = JCSystem.makeTransientByteArray((short) 2, JCSystem.CLEAR_ON_RESET);
    }

    public void process(APDU apdu) {
        if (selectingApplet()) {
            return;
        }

        byte[] buf = apdu.getBuffer();

        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_SEND:
            // gui gia tri cac doi tuong va bien len may chu
            apdu.setOutgoing();
            apdu.setOutgoingLength((short) 7);
            buf[0] = x;
            apdu.sendBytes((short) 0, (short) 1);
            apdu.sendBytesLong(buffer, (short) 0, (short) 2);
            apdu.sendBytesLong(buffer1, (short) 0, (short) 2);
            apdu.sendBytesLong(buffer2, (short) 0, (short) 2);
            break;

        case INS_UPDATE:
            // cap nhat gia tri cho cac doi tuong va bien
            x = 9;
            buffer[0] = 0x01;
            buffer[1] = 0x02;
            buffer1[0] = 0x03;
            buffer1[1] = 0x04;
            buffer2[0] = 0x05;
            buffer2[1] = 0x06;
            break;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }
}
