package thuchanh;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/** Doi qua lai giua chuoi hex va mang byte. */
public final class Hex {

    private Hex() {
    }

    /** Nhan "00 A4 04" hoac "00A404". */
    public static byte[] doc(String s) {
        String t = s.replaceAll("\\s+", "");
        if (t.length() % 2 != 0 || !t.matches("[0-9A-Fa-f]*")) {
            throw new IllegalArgumentException("Chuỗi hex không hợp lệ: " + s);
        }
        byte[] r = new byte[t.length() / 2];
        for (int i = 0; i < r.length; i++) {
            r[i] = (byte) Integer.parseInt(t.substring(2 * i, 2 * i + 2), 16);
        }
        return r;
    }

    public static String in(byte[] b) {
        return in(b, 0, b.length);
    }

    public static String in(byte[] b, int tu, int den) {
        StringBuilder s = new StringBuilder();
        for (int i = tu; i < den; i++) {
            if (s.length() > 0) {
                s.append(' ');
            }
            s.append(String.format("%02X", b[i] & 0xFF));
        }
        return s.toString();
    }

    /** Chuoi ASCII neu toan bo la ky tu in duoc (it nhat 3 ky tu), nguoc lai null. */
    public static String ascii(byte[] b, int tu, int den) {
        if (den - tu < 3) {
            return null;
        }
        for (int i = tu; i < den; i++) {
            if (b[i] < 0x20 || b[i] > 0x7E) {
                return null;
            }
        }
        return new String(b, tu, den - tu, Charset.forName("US-ASCII"));
    }

    public static byte[] noi(byte[]... cac) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        for (byte[] c : cac) {
            o.write(c, 0, c.length);
        }
        return o.toByteArray();
    }
}
