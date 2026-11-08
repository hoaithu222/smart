package sinhvien.host;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Đóng gói thông tin sinh viên thành TLV: [tag 1 byte][độ dài 1 byte][giá trị UTF-8].
 * Thẻ không cần hiểu từng trường - thẻ chỉ mã hóa / giải mã cả khối.
 */
public final class ProfileTlv {

    // Đối tượng INFO (chỉ phòng Đào tạo / admin ghi)
    public static final int MA_SV = 0x01;
    public static final int HO_TEN = 0x02;
    public static final int NGAY_SINH = 0x03;
    public static final int GIOI_TINH = 0x04;
    public static final int LOP = 0x05;
    public static final int KHOA_NGANH = 0x06;
    public static final int KHOA_HOC = 0x07;
    public static final int HAN_THE = 0x08;
    // Đối tượng CONTACT (sinh viên tự sửa sau khi nhập PIN)
    public static final int QUE_QUAN = 0x11;
    public static final int SDT = 0x12;
    public static final int EMAIL = 0x13;

    private ProfileTlv() {
    }

    public static byte[] encode(Map<Integer, String> fields) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (Map.Entry<Integer, String> e : fields.entrySet()) {
            byte[] v = e.getValue().getBytes(StandardCharsets.UTF_8);
            if (v.length > 255) {
                throw new IllegalArgumentException("Trường " + e.getKey() + " dài quá 255 byte");
            }
            o.write(e.getKey());
            o.write(v.length);
            o.write(v, 0, v.length);
        }
        return o.toByteArray();
    }

    public static Map<Integer, String> decode(byte[] data) {
        Map<Integer, String> out = new LinkedHashMap<Integer, String>();
        int i = 0;
        while (i + 2 <= data.length) {
            int tag = data[i] & 0xFF;
            int len = data[i + 1] & 0xFF;
            if (i + 2 + len > data.length) {
                throw new IllegalArgumentException("TLV hỏng ở vị trí " + i);
            }
            out.put(tag, new String(data, i + 2, len, StandardCharsets.UTF_8));
            i += 2 + len;
        }
        return out;
    }
}
