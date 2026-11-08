package sinhvien.host;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Tầng CSDL (JDBC thuần). Mọi câu SQL ở đây viết theo cú pháp chung, chạy được trên
 * SQL Server và SQLite (không dùng TOP/LIMIT - giới hạn số dòng bằng setMaxRows;
 * nhóm theo ngày bằng phép chia số nguyên trên cột thoi_gian).
 * Giao diện gọi CardClient để nói chuyện với thẻ, gọi CardRepository để ghi sổ.
 */
public class CardRepository {

    /** Kết quả ghi một biên nhận. */
    public enum RecordResult { OK, BAD_SIGNATURE, REPLAY, UNKNOWN_CARD }

    // ------------------------------------------------------------ kiểu dữ liệu

    public static class UserAccount {
        public String username, role, fullName;
    }

    public static class StudentRow {
        public String maSv, hoTen, lop, khoaNganh, trangThaiThe, lyDoKhoa;
        public int soDu;          // số dư theo biên nhận gần nhất (nghìn đồng), -1 nếu chưa có
    }

    public static class TxRow {
        public String maSv, hoTen, loai, dichVu;
        public int giaTri, soDuSau, boDem;
        public long thoiGian;
    }

    public static class Room {
        public int code;
        public String name;

        public String toString() {
            return name + String.format("  (%04X)", code);
        }
    }

    public static class DayTotal {
        public long dayStart;     // giây
        public long topup, pay;
        public int checkins;
    }

    private final Connection conn;

    public CardRepository(Connection conn) {
        this.conn = conn;
    }

    public Connection connection() {
        return conn;
    }

    public String productName() throws SQLException {
        return conn.getMetaData().getDatabaseProductName();
    }

    /** Chạy file .sql: bỏ dòng chú thích --, tách câu theo dấu ;. */
    public void runScript(InputStream sql) throws IOException, SQLException {
        BufferedReader r = new BufferedReader(new InputStreamReader(sql, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) {
            String t = line.trim();
            if (t.startsWith("--") || t.isEmpty()) {
                continue;
            }
            sb.append(line).append('\n');
        }
        r.close();
        try (Statement st = conn.createStatement()) {
            for (String stmt : sb.toString().split(";")) {
                if (!stmt.trim().isEmpty()) {
                    st.execute(stmt);
                }
            }
        }
    }

    // ------------------------------------------------------------ đăng nhập

    /** Trả về tài khoản nếu đúng mật khẩu, null nếu sai. Mật khẩu băm SHA-256(salt + mật khẩu). */
    public UserAccount login(String username, String password) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT mat_khau_bam, salt, vai_tro, ho_ten FROM nguoi_dung WHERE ten_dang_nhap = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                String expect = rs.getString(1);
                String actual = sha256Hex(rs.getString(2) + password);
                if (!MessageDigest.isEqual(expect.getBytes(StandardCharsets.US_ASCII),
                        actual.getBytes(StandardCharsets.US_ASCII))) {
                    return null;
                }
                UserAccount u = new UserAccount();
                u.username = username;
                u.role = rs.getString(3);
                u.fullName = rs.getString(4);
                return u;
            }
        }
    }

    // ------------------------------------------------------------ sinh viên

    /** Thêm mới hoặc cập nhật hồ sơ (UPDATE trước, không có dòng nào thì INSERT - chạy được mọi CSDL). */
    public void upsertStudent(Map<Integer, String> f, byte[] photo) throws SQLException {
        String maSv = f.get(ProfileTlv.MA_SV);
        String[] cols = {"ho_ten", "ngay_sinh", "gioi_tinh", "que_quan", "lop", "khoa_nganh", "khoa_hoc",
            "han_the", "sdt", "email"};
        int[] tags = {ProfileTlv.HO_TEN, ProfileTlv.NGAY_SINH, ProfileTlv.GIOI_TINH, ProfileTlv.QUE_QUAN,
            ProfileTlv.LOP, ProfileTlv.KHOA_NGANH, ProfileTlv.KHOA_HOC, ProfileTlv.HAN_THE, ProfileTlv.SDT,
            ProfileTlv.EMAIL};
        StringBuilder set = new StringBuilder();
        for (String c : cols) {
            set.append(c).append(" = ?, ");
        }
        int updated;
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sinh_vien SET " + set + "anh = ? WHERE ma_sv = ?")) {
            int i = 1;
            for (int t : tags) {
                ps.setString(i++, f.get(t));
            }
            ps.setBytes(i++, photo);
            ps.setString(i, maSv);
            updated = ps.executeUpdate();
        }
        if (updated > 0) {
            return;
        }
        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO sinh_vien (ma_sv, " + String.join(", ", cols)
                + ", anh) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)")) {
            ps.setString(1, maSv);
            int i = 2;
            for (int t : tags) {
                ps.setString(i++, f.get(t));
            }
            ps.setBytes(i, photo);
            ps.executeUpdate();
        }
    }

    public void updateContact(String maSv, String queQuan, String sdt, String email) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE sinh_vien SET que_quan = ?, sdt = ?, email = ? WHERE ma_sv = ?")) {
            ps.setString(1, queQuan);
            ps.setString(2, sdt);
            ps.setString(3, email);
            ps.setString(4, maSv);
            ps.executeUpdate();
        }
    }

    /** Hồ sơ theo TLV tag (ProfileTlv.*), null nếu không có. */
    public Map<Integer, String> findStudent(String maSv) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT ho_ten, ngay_sinh, gioi_tinh, que_quan, lop,"
                + " khoa_nganh, khoa_hoc, han_the, sdt, email FROM sinh_vien WHERE ma_sv = ?")) {
            ps.setString(1, maSv);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Map<Integer, String> m = new LinkedHashMap<Integer, String>();
                m.put(ProfileTlv.MA_SV, maSv);
                int[] tags = {ProfileTlv.HO_TEN, ProfileTlv.NGAY_SINH, ProfileTlv.GIOI_TINH, ProfileTlv.QUE_QUAN,
                    ProfileTlv.LOP, ProfileTlv.KHOA_NGANH, ProfileTlv.KHOA_HOC, ProfileTlv.HAN_THE, ProfileTlv.SDT,
                    ProfileTlv.EMAIL};
                for (int i = 0; i < tags.length; i++) {
                    String v = rs.getString(i + 1);
                    if (v != null) {
                        m.put(tags[i], v);
                    }
                }
                return m;
            }
        }
    }

    public byte[] findPhoto(String maSv) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT anh FROM sinh_vien WHERE ma_sv = ?")) {
            ps.setString(1, maSv);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getBytes(1) : null;
            }
        }
    }

    /** Danh sách sinh viên + trạng thái thẻ mới nhất + số dư theo biên nhận gần nhất. */
    public List<StudentRow> listStudents(String keyword) throws SQLException {
        String k = keyword == null ? "" : keyword.trim();
        String sql = "SELECT s.ma_sv, s.ho_ten, s.lop, s.khoa_nganh,"
                + " (SELECT t.trang_thai FROM the t WHERE t.ma_sv = s.ma_sv AND t.id ="
                + "    (SELECT MAX(t2.id) FROM the t2 WHERE t2.ma_sv = s.ma_sv)) AS trang_thai,"
                + " (SELECT t.ly_do_khoa FROM the t WHERE t.ma_sv = s.ma_sv AND t.id ="
                + "    (SELECT MAX(t2.id) FROM the t2 WHERE t2.ma_sv = s.ma_sv)) AS ly_do,"
                + " (SELECT g.so_du_sau FROM giao_dich g WHERE g.ma_sv = s.ma_sv AND g.loai <> 'CHECKIN' AND g.bo_dem ="
                + "    (SELECT MAX(g2.bo_dem) FROM giao_dich g2 WHERE g2.ma_sv = s.ma_sv AND g2.loai <> 'CHECKIN')) AS so_du"
                + " FROM sinh_vien s"
                + " WHERE ? = '' OR s.ma_sv LIKE ? OR s.ho_ten LIKE ? OR s.lop LIKE ?"
                + " ORDER BY s.ma_sv";
        List<StudentRow> out = new ArrayList<StudentRow>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + k + "%";
            ps.setString(1, k);
            ps.setString(2, like);
            ps.setString(3, like);
            ps.setString(4, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    StudentRow r = new StudentRow();
                    r.maSv = rs.getString(1);
                    r.hoTen = rs.getString(2);
                    r.lop = rs.getString(3);
                    r.khoaNganh = rs.getString(4);
                    r.trangThaiThe = rs.getString(5) == null ? "CHƯA CẤP" : rs.getString(5);
                    r.lyDoKhoa = rs.getString(6);
                    int soDu = rs.getInt(7);
                    r.soDu = rs.wasNull() ? -1 : soDu;
                    out.add(r);
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------ thẻ

    /** Ghi khóa công khai của thẻ vừa phát hành; thẻ cũ (nếu có) chuyển REVOKED. */
    public void saveIssuedCard(String maSv, byte[] modulus, byte[] exponent) throws SQLException {
        boolean oldAuto = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            try (PreparedStatement old = conn.prepareStatement(
                    "UPDATE the SET trang_thai = 'REVOKED' WHERE ma_sv = ? AND trang_thai <> 'REVOKED'")) {
                old.setString(1, maSv);
                old.executeUpdate();
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO the (ma_sv, rsa_modulus, rsa_exponent, trang_thai) VALUES (?,?,?,'ACTIVE')")) {
                ps.setString(1, maSv);
                ps.setString(2, hexNoSpace(modulus));
                ps.setString(3, hexNoSpace(exponent));
                ps.executeUpdate();
            }
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAuto);
        }
    }

    /** Khóa công khai của thẻ ACTIVE, null nếu không có (chưa phát hành / đã khóa / đã thu hồi). */
    public PublicKey activePublicKey(String maSv) throws SQLException, GeneralSecurityException {
        byte[][] p = activePublicKeyParts(maSv);
        return p == null ? null : CardCrypto.rsaPublicKey(p[0], p[1]);
    }

    public byte[][] activePublicKeyParts(String maSv) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT rsa_modulus, rsa_exponent FROM the WHERE ma_sv = ? AND trang_thai = 'ACTIVE'")) {
            ps.setString(1, maSv);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new byte[][] {AppConfig.hex(rs.getString(1)), AppConfig.hex(rs.getString(2))};
            }
        }
    }

    /** Trạng thái thẻ mới nhất trong CSDL: ACTIVE / LOCKED / REVOKED, null nếu chưa cấp. */
    public String cardStatus(String maSv) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT trang_thai FROM the WHERE ma_sv = ? AND id ="
                + " (SELECT MAX(id) FROM the WHERE ma_sv = ?)")) {
            ps.setString(1, maSv);
            ps.setString(2, maSv);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString(1) : null;
            }
        }
    }

    /** Khóa (báo mất) / mở khóa trong CSDL - không cần có thẻ. */
    public void setCardStatus(String maSv, String status, String reason) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE the SET trang_thai = ?, ly_do_khoa = ? WHERE ma_sv = ? AND trang_thai <> 'REVOKED'")) {
            ps.setString(1, status);
            ps.setString(2, reason);
            ps.setString(3, maSv);
            ps.executeUpdate();
        }
    }

    public void setCardStatus(String maSv, String status) throws SQLException {
        setCardStatus(maSv, status, null);
    }

    // ------------------------------------------------------------ biên nhận

    /**
     * Kiểm chữ ký bằng khóa công khai trong CSDL, kiểm bộ đếm tăng dần rồi mới ghi.
     * Biên nhận giả / phát lại đều bị từ chối.
     */
    public RecordResult recordReceipt(String maSv, CardClient.Receipt r, String service)
            throws SQLException, GeneralSecurityException {
        PublicKey key = activePublicKey(maSv);
        if (key == null) {
            return RecordResult.UNKNOWN_CARD;
        }
        if (!CardCrypto.verify(key, CardCrypto.receiptMessage(maSv, r), r.signature)) {
            return RecordResult.BAD_SIGNATURE;
        }
        boolean oldAuto = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            if (r.counter <= lastCounter(maSv)) {
                conn.rollback();
                return RecordResult.REPLAY;
            }
            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO giao_dich (ma_sv, loai, gia_tri,"
                    + " so_du_sau, bo_dem, thoi_gian, nonce, chu_ky, dich_vu) VALUES (?,?,?,?,?,?,?,?,?)")) {
                ps.setString(1, maSv);
                ps.setString(2, typeName(r.type));
                ps.setInt(3, r.value);
                ps.setInt(4, r.balance);
                ps.setInt(5, r.counter);
                ps.setLong(6, r.time());
                ps.setString(7, hexNoSpace(Arrays.copyOfRange(r.timeAndNonce, 4, 12)));
                ps.setString(8, hexNoSpace(r.signature));
                ps.setString(9, service);
                ps.executeUpdate();
            }
            try (PreparedStatement up = conn.prepareStatement(
                    "UPDATE the SET bo_dem_cuoi = ? WHERE ma_sv = ? AND trang_thai = 'ACTIVE'")) {
                up.setInt(1, r.counter);
                up.setString(2, maSv);
                up.executeUpdate();
            }
            conn.commit();
            return RecordResult.OK;
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAuto);
        }
    }

    public int lastCounter(String maSv) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT bo_dem_cuoi FROM the WHERE ma_sv = ? AND trang_thai = 'ACTIVE'")) {
            ps.setString(1, maSv);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** Lịch sử của một sinh viên (maSv = null: toàn trường), mới nhất trước. */
    public List<TxRow> history(String maSv, int limit) throws SQLException {
        String sql = "SELECT g.ma_sv, s.ho_ten, g.loai, g.dich_vu, g.gia_tri, g.so_du_sau, g.bo_dem, g.thoi_gian"
                + " FROM giao_dich g JOIN sinh_vien s ON s.ma_sv = g.ma_sv"
                + (maSv == null ? "" : " WHERE g.ma_sv = ?")
                + " ORDER BY g.thoi_gian DESC, g.id DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            if (maSv != null) {
                ps.setString(1, maSv);
            }
            ps.setMaxRows(limit);
            return readTx(ps);
        }
    }

    /** Lượt điểm danh của một phòng từ thời điểm fromSec. */
    public List<TxRow> checkins(int room, long fromSec) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT g.ma_sv, s.ho_ten, g.loai, g.dich_vu, g.gia_tri, g.so_du_sau, g.bo_dem, g.thoi_gian"
                + " FROM giao_dich g JOIN sinh_vien s ON s.ma_sv = g.ma_sv"
                + " WHERE g.loai = 'CHECKIN' AND g.gia_tri = ? AND g.thoi_gian >= ?"
                + " ORDER BY g.thoi_gian DESC")) {
            ps.setInt(1, room);
            ps.setLong(2, fromSec);
            return readTx(ps);
        }
    }

    private static List<TxRow> readTx(PreparedStatement ps) throws SQLException {
        List<TxRow> out = new ArrayList<TxRow>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TxRow t = new TxRow();
                t.maSv = rs.getString(1);
                t.hoTen = rs.getString(2);
                t.loai = rs.getString(3);
                t.dichVu = rs.getString(4);
                t.giaTri = rs.getInt(5);
                t.soDuSau = rs.getInt(6);
                t.boDem = rs.getInt(7);
                t.thoiGian = rs.getLong(8);
                out.add(t);
            }
        }
        return out;
    }

    // ------------------------------------------------------------ thống kê

    public List<Room> rooms() throws SQLException {
        List<Room> out = new ArrayList<Room>();
        try (Statement st = conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT ma_phong, ten_phong FROM phong ORDER BY ma_phong")) {
            while (rs.next()) {
                Room r = new Room();
                r.code = rs.getInt(1);
                r.name = rs.getString(2);
                out.add(r);
            }
        }
        return out;
    }

    public int countStudents() throws SQLException {
        return scalarInt("SELECT COUNT(*) FROM sinh_vien");
    }

    public int countActiveCards() throws SQLException {
        return scalarInt("SELECT COUNT(*) FROM the WHERE trang_thai = 'ACTIVE'");
    }

    public int countCheckinsSince(long fromSec) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM giao_dich WHERE loai = 'CHECKIN' AND thoi_gian >= ?")) {
            ps.setLong(1, fromSec);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Tổng tiền theo loại giao dịch (nghìn đồng). */
    public long sumByType(String type) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COALESCE(SUM(gia_tri), 0) FROM giao_dich WHERE loai = ?")) {
            ps.setString(1, type);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    /** Tổng chi theo dịch vụ. */
    public Map<String, Long> payByService() throws SQLException {
        Map<String, Long> out = new LinkedHashMap<String, Long>();
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(
                "SELECT COALESCE(dich_vu, 'Khác') AS dv, SUM(gia_tri) AS tong FROM giao_dich"
                + " WHERE loai = 'PAY' GROUP BY COALESCE(dich_vu, 'Khác') ORDER BY tong DESC")) {
            while (rs.next()) {
                out.put(rs.getString(1), rs.getLong(2));
            }
        }
        return out;
    }

    /**
     * Tổng nạp / chi / lượt điểm danh theo ngày (giờ Việt Nam, UTC+7) cho `days` ngày gần nhất.
     * Gộp trong Java để không phụ thuộc hàm ngày giờ riêng của từng CSDL.
     */
    public List<DayTotal> dailyTotals(int days, long nowSec) throws SQLException {
        long tz = 7 * 3600L;
        long today = ((nowSec + tz) / 86400L) * 86400L - tz;
        long from = today - (days - 1) * 86400L;
        DayTotal[] arr = new DayTotal[days];
        for (int i = 0; i < days; i++) {
            arr[i] = new DayTotal();
            arr[i].dayStart = from + i * 86400L;
        }
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT loai, gia_tri, thoi_gian FROM giao_dich WHERE thoi_gian >= ?")) {
            ps.setLong(1, from);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idx = (int) ((rs.getLong(3) - from) / 86400L);
                    if (idx < 0 || idx >= days) {
                        continue;
                    }
                    String loai = rs.getString(1);
                    if ("TOPUP".equals(loai)) {
                        arr[idx].topup += rs.getInt(2);
                    } else if ("PAY".equals(loai)) {
                        arr[idx].pay += rs.getInt(2);
                    } else {
                        arr[idx].checkins++;
                    }
                }
            }
        }
        return Arrays.asList(arr);
    }

    private int scalarInt(String sql) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    public static String typeName(int type) {
        return type == CardClient.TX_TOPUP ? "TOPUP" : type == CardClient.TX_PAY ? "PAY" : "CHECKIN";
    }

    static String hexNoSpace(byte[] b) {
        return CardCrypto.hex(b).replace(" ", "");
    }

    public static String sha256Hex(String s) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            return hexNoSpace(h).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
