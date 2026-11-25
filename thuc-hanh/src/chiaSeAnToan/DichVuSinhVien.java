package chiaSeAnToan;

import javacard.framework.Shareable;

/**
 * TH5 mo rong - giao dien chia se co xac thuc: phai tra loi dung thu thach thi
 * moi doc duoc du lieu. Chi truyen du lieu qua MANG TOAN CUC (bo dem APDU).
 */
public interface DichVuSinhVien extends Shareable {

    /** Ghi 16 byte thu thach moi vao buf, tra ve 16. */
    short layThuThach(byte[] buf, short off);

    /** Kiem 16 byte phan hoi = AES(khoa chung, thu thach). Dung: duoc doc MOT lan. */
    boolean traLoi(byte[] buf, short off);

    /** Ghi ma sinh vien vao buf, tra ve do dai. Chua duoc cap quyen: 69 82. */
    short docMaSV(byte[] buf, short off);
}
