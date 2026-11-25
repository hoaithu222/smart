package connect_ex_gui;

import java.nio.charset.Charset;

/** Mo-dun phu: doi du lieu nhan tu the thanh chuoi de hien thi. */
public class text {

    private text() {
    }

    public static String thanhChuoi(byte[] duLieu) {
        return new String(duLieu, Charset.forName("US-ASCII"));
    }
}
