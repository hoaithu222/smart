-- ================================================================
-- CSDL Thẻ sinh viên (Nhóm 1 - L02) - SQL Server 2016 trở lên (kể cả Express)
-- Phần mềm TỰ CHẠY file này mỗi lần khởi động (mọi câu đều có kiểm tra tồn tại).
-- Muốn chạy tay trong SSMS: tạo database trước
--     IF DB_ID(N'the_sinh_vien') IS NULL CREATE DATABASE the_sinh_vien;
--     USE the_sinh_vien;
-- rồi chạy toàn bộ file. Quy ước: mỗi câu kết thúc bằng dấu chấm phẩy,
-- không có dấu chấm phẩy bên trong câu (phần mềm tách câu theo dấu này).
-- Chữ tiếng Việt: luôn NVARCHAR và chuỗi N'...'.
-- ================================================================

IF OBJECT_ID(N'dbo.sinh_vien', N'U') IS NULL
CREATE TABLE dbo.sinh_vien (
    ma_sv        NVARCHAR(16)  NOT NULL PRIMARY KEY,
    ho_ten       NVARCHAR(100) NOT NULL,
    ngay_sinh    NVARCHAR(10)  NULL,
    gioi_tinh    NVARCHAR(5)   NULL,
    que_quan     NVARCHAR(200) NULL,
    lop          NVARCHAR(20)  NULL,
    khoa_nganh   NVARCHAR(100) NULL,
    khoa_hoc     NVARCHAR(30)  NULL,
    han_the      NVARCHAR(10)  NULL,
    sdt          NVARCHAR(15)  NULL,
    email        NVARCHAR(100) NULL,
    anh          VARBINARY(MAX) NULL
);

IF OBJECT_ID(N'dbo.the', N'U') IS NULL
CREATE TABLE dbo.the (
    id              INT IDENTITY(1,1) PRIMARY KEY,
    ma_sv           NVARCHAR(16)  NOT NULL REFERENCES dbo.sinh_vien(ma_sv),
    rsa_modulus     VARCHAR(600)  NOT NULL,
    rsa_exponent    VARCHAR(16)   NOT NULL,
    trang_thai      VARCHAR(10)   NOT NULL DEFAULT 'ACTIVE',
    ly_do_khoa      NVARCHAR(200) NULL,
    ngay_phat_hanh  DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    bo_dem_cuoi     INT           NOT NULL DEFAULT 0
);

IF OBJECT_ID(N'dbo.giao_dich', N'U') IS NULL
CREATE TABLE dbo.giao_dich (
    id          BIGINT IDENTITY(1,1) PRIMARY KEY,
    ma_sv       NVARCHAR(16)  NOT NULL REFERENCES dbo.sinh_vien(ma_sv),
    loai        VARCHAR(8)    NOT NULL,
    gia_tri     INT           NOT NULL,
    so_du_sau   INT           NOT NULL,
    bo_dem      INT           NOT NULL,
    thoi_gian   BIGINT        NOT NULL,
    nonce       VARCHAR(16)   NOT NULL,
    chu_ky      VARCHAR(512)  NOT NULL,
    dich_vu     NVARCHAR(50)  NULL,
    ghi_luc     DATETIME2     NOT NULL DEFAULT SYSDATETIME(),
    CONSTRAINT uq_giao_dich_sv_bo_dem UNIQUE (ma_sv, bo_dem)
);

IF NOT EXISTS (SELECT 1 FROM sys.indexes WHERE name = N'idx_giao_dich_sv')
CREATE INDEX idx_giao_dich_sv ON dbo.giao_dich (ma_sv, thoi_gian);

IF OBJECT_ID(N'dbo.nguoi_dung', N'U') IS NULL
CREATE TABLE dbo.nguoi_dung (
    ten_dang_nhap NVARCHAR(50)  NOT NULL PRIMARY KEY,
    mat_khau_bam  VARCHAR(100)  NOT NULL,
    salt          VARCHAR(32)   NOT NULL,
    vai_tro       VARCHAR(12)   NOT NULL,
    ho_ten        NVARCHAR(100) NULL
);

IF OBJECT_ID(N'dbo.phong', N'U') IS NULL
CREATE TABLE dbo.phong (
    ma_phong  INT           NOT NULL PRIMARY KEY,
    ten_phong NVARCHAR(50)  NOT NULL
);

-- ---------- Dữ liệu mẫu (chỉ thêm nếu chưa có) ----------
-- Mật khẩu: admin@123, thungan@123, gv@123  (băm SHA-256(salt + mật khẩu))
IF NOT EXISTS (SELECT 1 FROM dbo.nguoi_dung WHERE ten_dang_nhap = N'admin')
INSERT INTO dbo.nguoi_dung VALUES (N'admin', 'b253d3ffe28af6f64febd4600b0a66cf8405a1b3eb3f1a4c8d52ab03aa607c09', 'a1b2c3d4e5f60718a1b2c3d4e5f60718', 'ADMIN', N'Phòng Đào tạo');
IF NOT EXISTS (SELECT 1 FROM dbo.nguoi_dung WHERE ten_dang_nhap = N'thungan')
INSERT INTO dbo.nguoi_dung VALUES (N'thungan', '8d928baf3b37e1970dcc22faa740f8f80a26d528e2a5e0503e0c75c4a1bf3729', 'a1b2c3d4e5f60718a1b2c3d4e5f60718', 'THU_NGAN', N'Căng tin A1');
IF NOT EXISTS (SELECT 1 FROM dbo.nguoi_dung WHERE ten_dang_nhap = N'gv')
INSERT INTO dbo.nguoi_dung VALUES (N'gv', '1de896693fa203d02629ca99f140be7100422f796487a55ff34fdb4d57f47338', 'a1b2c3d4e5f60718a1b2c3d4e5f60718', 'GIANG_VIEN', N'Giảng viên');
IF NOT EXISTS (SELECT 1 FROM dbo.phong WHERE ma_phong = 515)
INSERT INTO dbo.phong VALUES (515, N'P203 - Tầng 2');
IF NOT EXISTS (SELECT 1 FROM dbo.phong WHERE ma_phong = 769)
INSERT INTO dbo.phong VALUES (769, N'P301 - Tầng 3');
IF NOT EXISTS (SELECT 1 FROM dbo.phong WHERE ma_phong = 1025)
INSERT INTO dbo.phong VALUES (1025, N'P401 - Phòng máy');
