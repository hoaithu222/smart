package sinhvien.host;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * Cấu hình phần mềm đọc từ file app.properties đặt cạnh file .jar (hoặc thư mục project khi chạy NetBeans).
 * Không có file / thiếu khóa nào thì dùng giá trị mặc định bên dưới.
 * Bảng giải thích từng khóa: notes/04-moi-truong-cau-hinh.md, mục 6.
 */
public final class AppConfig {

    private final Properties p = new Properties();

    private AppConfig() {
    }

    public static AppConfig load(String path) {
        AppConfig c = new AppConfig();
        File f = new File(path);
        if (f.isFile()) {
            InputStream in = null;
            try {
                in = new FileInputStream(f);
                Reader r = new InputStreamReader(in, StandardCharsets.UTF_8);
                c.p.load(r);
            } catch (IOException e) {
                System.err.println("Không đọc được " + path + ": " + e.getMessage() + " -> dùng mặc định");
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException ignored) {
                        // bỏ qua
                    }
                }
            }
        }
        return c;
    }

    // ---- Thẻ ----
    /** "pcsc": JCIDE / đầu đọc thật (mặc định, dùng khi báo cáo). "jcardsim": thẻ giả lập chạy ngay trong phần mềm. */
    public String cardMode() {
        return get("card.mode", "pcsc").toLowerCase();
    }

    /** PIN quản trị nạp vào thẻ giả lập jcardsim lúc "cài applet" (giống tham số cài đặt trong JCIDE). */
    public String simAdminPin() {
        return get("card.sim.adminPin", "87654321");
    }

    /** Một phần tên đầu đọc, ví dụ "JAVACOS Virtual". Để trống = lấy đầu đọc đầu tiên có thẻ. */
    public String readerName() {
        return get("card.reader", "");
    }

    /** "T=0" (như tài liệu của cô), "T=1" hoặc "*". */
    public String protocol() {
        return get("card.protocol", "T=0");
    }

    public byte[] appletAid() {
        return hex(get("card.aid", "4B 4D 41 53 56 01 01"));
    }

    public int rsaChallengeBytes() {
        return Integer.parseInt(get("card.challenge.bytes", "16"));
    }

    // ---- CSDL ----
    public String dbUrl() {
        return get("db.url", "jdbc:sqlserver://localhost:1433;databaseName=the_sinh_vien;encrypt=false");
    }

    public String dbUser() {
        return get("db.user", "");
    }

    public String dbPassword() {
        return get("db.password", "");
    }

    /** SQL Server: database chưa có thì tự tạo (cần tài khoản có quyền CREATE DATABASE, ví dụ sa). */
    public boolean dbAutoCreate() {
        return Boolean.parseBoolean(get("db.autoCreate", "true"));
    }

    // ---- Nghiệp vụ ----
    public int pinLength() {
        return Integer.parseInt(get("app.pin.length", "6"));
    }

    public int pukLength() {
        return Integer.parseInt(get("app.puk.length", "8"));
    }

    public int maxTopUpThousand() {
        return Integer.parseInt(get("app.topup.max", "5000"));
    }

    public int photoMaxBytes() {
        return Integer.parseInt(get("app.photo.maxBytes", "7600"));
    }

    public int photoWidth() {
        return Integer.parseInt(get("app.photo.width", "120"));
    }

    public int photoHeight() {
        return Integer.parseInt(get("app.photo.height", "160"));
    }

    /** Mã phòng mặc định của máy điểm danh, ví dụ 0x0203 = tầng 2 phòng 03. */
    public int defaultRoom() {
        return Integer.decode(get("app.checkin.room", "0x0203"));
    }

    // ---- Giao diện ----
    /** "light", "dark" hoặc "system" (không dùng FlatLaf). */
    public String uiTheme() {
        return get("ui.theme", "light");
    }

    public String uiFont() {
        return get("ui.font", "Segoe UI");
    }

    public int uiFontSize() {
        return Integer.parseInt(get("ui.fontSize", "14"));
    }

    // ---- Nhật ký ----
    /** true: in mọi APDU gửi/nhận ra console (giống Debug Shell của JCIDE). */
    public boolean logApdu() {
        return Boolean.parseBoolean(get("log.apdu", "true"));
    }

    private String get(String key, String def) {
        String v = p.getProperty(key);
        return v == null || v.trim().isEmpty() ? def : v.trim();
    }

    static byte[] hex(String s) {
        String clean = s.replaceAll("[^0-9A-Fa-f]", "");
        if (clean.length() % 2 != 0) {
            throw new IllegalArgumentException("Chuỗi hex lẻ ký tự: " + s);
        }
        byte[] out = new byte[clean.length() / 2];
        for (int i = 0; i < out.length; i++) {
            out[i] = (byte) Integer.parseInt(clean.substring(2 * i, 2 * i + 2), 16);
        }
        return out;
    }
}
