-- ================================================================
-- CSDL Thẻ sinh viên - bản SQLite (dùng cho bộ test tự động và chạy thử không cần SQL Server).
-- Cùng bảng, cùng cột với schema-sqlserver.sql. Phần mềm tự chạy file này khi db.url là jdbc:sqlite.
-- ================================================================

CREATE TABLE IF NOT EXISTS sinh_vien (
    ma_sv        VARCHAR(16)  PRIMARY KEY,
    ho_ten       VARCHAR(100) NOT NULL,
    ngay_sinh    VARCHAR(10),
    gioi_tinh    VARCHAR(5),
    que_quan     VARCHAR(200),
    lop          VARCHAR(20),
    khoa_nganh   VARCHAR(100),
    khoa_hoc     VARCHAR(30),
    han_the      VARCHAR(10),
    sdt          VARCHAR(15),
    email        VARCHAR(100),
    anh          BLOB
);

CREATE TABLE IF NOT EXISTS the (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    ma_sv           VARCHAR(16)  NOT NULL REFERENCES sinh_vien(ma_sv),
    rsa_modulus     VARCHAR(600) NOT NULL,
    rsa_exponent    VARCHAR(16)  NOT NULL,
    trang_thai      VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE',
    ly_do_khoa      VARCHAR(200),
    ngay_phat_hanh  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    bo_dem_cuoi     INTEGER      NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS giao_dich (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    ma_sv       VARCHAR(16) NOT NULL REFERENCES sinh_vien(ma_sv),
    loai        VARCHAR(8)  NOT NULL,
    gia_tri     INTEGER     NOT NULL,
    so_du_sau   INTEGER     NOT NULL,
    bo_dem      INTEGER     NOT NULL,
    thoi_gian   BIGINT      NOT NULL,
    nonce       VARCHAR(16) NOT NULL,
    chu_ky      VARCHAR(512) NOT NULL,
    dich_vu     VARCHAR(50),
    ghi_luc     TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (ma_sv, bo_dem)
);

CREATE INDEX IF NOT EXISTS idx_giao_dich_sv ON giao_dich (ma_sv, thoi_gian);

CREATE TABLE IF NOT EXISTS nguoi_dung (
    ten_dang_nhap VARCHAR(50)  PRIMARY KEY,
    mat_khau_bam  VARCHAR(100) NOT NULL,
    salt          VARCHAR(32)  NOT NULL,
    vai_tro       VARCHAR(12)  NOT NULL,
    ho_ten        VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS phong (
    ma_phong  INTEGER PRIMARY KEY,
    ten_phong VARCHAR(50) NOT NULL
);

INSERT OR IGNORE INTO nguoi_dung VALUES ('admin', 'b253d3ffe28af6f64febd4600b0a66cf8405a1b3eb3f1a4c8d52ab03aa607c09', 'a1b2c3d4e5f60718a1b2c3d4e5f60718', 'ADMIN', 'Phòng Đào tạo');
INSERT OR IGNORE INTO nguoi_dung VALUES ('thungan', '8d928baf3b37e1970dcc22faa740f8f80a26d528e2a5e0503e0c75c4a1bf3729', 'a1b2c3d4e5f60718a1b2c3d4e5f60718', 'THU_NGAN', 'Căng tin A1');
INSERT OR IGNORE INTO nguoi_dung VALUES ('gv', '1de896693fa203d02629ca99f140be7100422f796487a55ff34fdb4d57f47338', 'a1b2c3d4e5f60718a1b2c3d4e5f60718', 'GIANG_VIEN', 'Giảng viên');
INSERT OR IGNORE INTO phong VALUES (515, 'P203 - Tầng 2');
INSERT OR IGNORE INTO phong VALUES (769, 'P301 - Tầng 3');
INSERT OR IGNORE INTO phong VALUES (1025, 'P401 - Phòng máy');
