package sinhvien.host;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mở CSDL theo app.properties và chuẩn bị sẵn mọi thứ:
 * 1. SQL Server: nếu database (databaseName=...) chưa có và db.autoCreate=true -> nối vào master, CREATE DATABASE.
 * 2. Chạy file schema phù hợp (SQL Server / SQLite) - mọi câu đều "tạo nếu chưa có", chạy lại không sao.
 */
public final class DatabaseBootstrap {

    private static final Pattern DB_NAME = Pattern.compile("(?i)databaseName=([^;]+)");
    /** Mã lỗi SQL Server "Cannot open database ... requested by the login". */
    private static final int SQLSERVER_CANNOT_OPEN_DB = 4060;

    private DatabaseBootstrap() {
    }

    public static CardRepository open(AppConfig cfg) throws SQLException, IOException {
        String url = cfg.dbUrl();
        Connection c;
        try {
            c = connect(cfg, url);
        } catch (SQLException e) {
            Matcher m = DB_NAME.matcher(url);
            boolean sqlServer = url.startsWith("jdbc:sqlserver:");
            if (sqlServer && cfg.dbAutoCreate() && m.find() && e.getErrorCode() == SQLSERVER_CANNOT_OPEN_DB) {
                createSqlServerDatabase(cfg, url, m.group(1).trim());
                c = connect(cfg, url);
            } else {
                throw e;
            }
        }
        CardRepository repo = new CardRepository(c);
        String product = repo.productName().toLowerCase();
        String script = product.contains("sql server") ? "db/schema-sqlserver.sql"
                : product.contains("sqlite") ? "db/schema-sqlite.sql" : null;
        if (script == null) {
            throw new SQLException("Chưa hỗ trợ CSDL: " + repo.productName());
        }
        InputStream in = DatabaseBootstrap.class.getResourceAsStream(script);
        if (in == null) {
            throw new IOException("Không tìm thấy sinhvien/host/" + script + " trong classpath");
        }
        repo.runScript(in);
        return repo;
    }

    private static Connection connect(AppConfig cfg, String url) throws SQLException {
        return cfg.dbUser().isEmpty()
                ? DriverManager.getConnection(url)
                : DriverManager.getConnection(url, cfg.dbUser(), cfg.dbPassword());
    }

    private static void createSqlServerDatabase(AppConfig cfg, String url, String name) throws SQLException {
        if (!name.matches("[A-Za-z0-9_]+")) {
            throw new SQLException("Tên database chỉ gồm chữ không dấu, số, gạch dưới: " + name);
        }
        String masterUrl = DB_NAME.matcher(url).replaceFirst("databaseName=master");
        try (Connection m = connect(cfg, masterUrl); Statement st = m.createStatement()) {
            st.execute("IF DB_ID(N'" + name + "') IS NULL CREATE DATABASE " + name);
        }
    }

    /** Thông báo lỗi kết nối dễ hiểu cho người dùng (hiện trên hộp thoại lúc khởi động). */
    public static String explain(SQLException e, AppConfig cfg) {
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String low = msg.toLowerCase();
        StringBuilder sb = new StringBuilder("Không kết nối được CSDL.\n\n" + msg + "\n\nKiểm tra:\n");
        if (low.contains("no suitable driver")) {
            sb.append("- Thiếu file driver trong lib\\ (mssql-jdbc-12.8.1.jre8.jar hoặc sqlite-jdbc).\n");
        }
        if (low.contains("tcp/ip") || low.contains("connection refused") || low.contains("connect timed out")) {
            sb.append("- SQL Server Configuration Manager > Protocols > TCP/IP = Enabled, cổng 1433, khởi động lại dịch vụ.\n");
            sb.append("- Dùng bản Express có tên instance thì db.url = jdbc:sqlserver://localhost\\\\SQLEXPRESS;...\n");
        }
        if (low.contains("login failed")) {
            sb.append("- Sai db.user / db.password, hoặc SQL Server chưa bật chế độ đăng nhập SQL Server and Windows Authentication.\n");
        }
        if (low.contains("ssl") || low.contains("certificate") || low.contains("encrypt")) {
            sb.append("- Thêm ;encrypt=false (hoặc ;trustServerCertificate=true) vào db.url.\n");
        }
        sb.append("- db.url hiện tại: ").append(cfg.dbUrl()).append('\n');
        sb.append("- Chạy thử không cần SQL Server: đặt db.url=jdbc:sqlite:the_sinh_vien.db");
        return sb.toString();
    }
}
