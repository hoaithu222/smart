package masterPack;

import javacard.framework.Shareable;

/** TH5 - giao dien co the chia se: chi cac phuong thuc o day applet khac goi duoc. */
public interface masterInterface extends Shareable {

    /** Chep mang testArray cua applet chu vao 'array', tra ve do dai. */
    public short getArray(byte[] array);

    /** Muc 5.5: tham so va gia tri tra ve deu la kieu co ban. */
    public byte tinhDiem(byte diemToan, byte diemVan);
}
