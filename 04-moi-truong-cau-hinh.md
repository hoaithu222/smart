# 04 · Cấu hình môi trường và phần mềm, từng bước

> Đã chốt ngày 08/11/2026: **CSDL SQL Server**, **giao diện đẹp bằng FlatLaf** (vẫn là Java Swing), thiết kế khóa DEK/KEK được duyệt, **báo cáo trên trình giả lập JCIDE**.
> Công cụ đúng tài liệu của cô: JCIDE (Java Card 2.2.2) cho applet + PyApduTool gửi APDU tay + NetBeans (Java Swing, `javax.smartcardio`) cho phần mềm máy tính. Phần mềm máy tính đã viết xong, nằm ở [code/host/](code/host/).
> Thông số trích từ [huong-dan-ket-noi.md](../huong-dan-ket-noi.md), [th2](../th2-cac-doi-tuong-trong-java-card.md) Hình 2.2–2.5, [th4](../th4-phuong-thuc-install.md) Hình 4.1–4.3. Tên menu JCIDE ghi đúng như ảnh trong tài liệu; chỗ tài liệu không có ảnh (tạo project) ghi "≈".

Thứ tự làm lần đầu: **mục 1** (cài) → **mục 2** (JCIDE) → **mục 5** (SQL Server) → **mục 4** (NetBeans) → chạy theo **mục 7**. Muốn xem phần mềm chạy ngay trong 2 phút mà chưa cài gì khác ngoài JDK: [mục 4.4](#44-chạy-nhanh-không-cần-netbeans--jcide--sql-server).

## 0. Bảng phiên bản thống nhất cả nhóm

| Thành phần | Phiên bản / giá trị | Ghi chú |
|---|---|---|
| JCIDE | Bản trong bộ `C:\JavaCardKit` cô phát | Dùng JDK riêng `C:\JavaCardKit\JDK`, biên dịch `-source 1.5 -target 1.5` |
| Java Card API | 2.2.2 | Không có `int` bắt buộc, không AES tự đệm, không RSA-SHA256 |
| PyApduTool | Đi kèm bộ JavaCardKit | Đầu đọc ảo `JAVACOS Virtual Contact Reader 0`, T=0 |
| JDK cho phần mềm | **JDK 8** | NetBeans 8.2 (cô dùng 8.2 RC) chỉ chạy ổn với JDK 8. Mã phần mềm viết ở mức Java 8 (có lambda) |
| NetBeans | 8.2 | Apache NetBeans mới hơn + JDK 11/17 cũng chạy được |
| CSDL | **SQL Server 2019/2022** (Express hoặc Developer) + SSMS | Driver `mssql-jdbc-12.8.1.jre8.jar` (bản cho Java 8) |
| Giao diện | FlatLaf 3.5.4 (`flatlaf-3.5.4.jar`) | Sáng/tối, bo góc; thiếu jar thì tự lùi về giao diện Windows |
| Thẻ giả lập trong phần mềm (tùy chọn) | jCardSim 2.2.2 | Chỉ dùng khi tự thử / bộ test; **báo cáo dùng JCIDE** |
| AID | Package `4B 4D 41 53 56 01` · Applet `4B 4D 41 53 56 01 01` | "KMASV" + 01 |
| CLA | `B0` | Như ví dụ kết nối |
| PIN quản trị (tham số cài đặt) | `87654321` → hex `38 37 36 35 34 33 32 31` | Đổi khi demo thật |

## 1. Cài đặt (mỗi máy làm một lần)

1. **JCIDE**: chép/giải nén bộ `JavaCardKit` vào đúng `C:\JavaCardKit` (đường dẫn trong tài liệu của cô). Mở JCIDE, build thử project mẫu của TH1.
2. **PyApduTool**: mở từ bộ JavaCardKit. Nếu Windows chặn, chạy bằng quyền Administrator.
3. **JDK 8 + NetBeans 8.2**: cài JDK 8 trước, rồi NetBeans 8.2 (bản "Java SE" là đủ). Kiểm tra: `Tools → Java Platforms` có JDK 1.8.
4. **SQL Server + SSMS**: làm theo [mục 5](#5-cài-và-cấu-hình-sql-server).
5. **Thư viện của phần mềm**: chạy [code/host/tai-thu-vien.cmd](code/host/tai-thu-vien.cmd) (tải 4 file jar vào `code\host\lib\`). Máy không có mạng thì chép thư mục `lib` từ máy bạn khác.
6. **Git** (khuyên dùng): repo riêng của nhóm; không đưa `lib/`, `build/`, `app.properties` có mật khẩu thật lên repo.

## 2. Cấu hình JCIDE

### 2.1 Tạo project và khai báo AID (≈)

1. `File → New → Project` (≈), đặt tên `TheSinhVien`, thư mục `C:\workspace\TheSinhVien` (cô dùng `C:\workspace\...`).
2. Tạo package `sinhvien`, Package AID `4B 4D 41 53 56 01`.
3. Tạo applet lớp `StudentCard` trong package đó, Applet AID `4B 4D 41 53 56 01 01`.
4. Sai AID thì sửa bằng chuột phải lên package/applet trong **User Package View → Change AID** (menu này có trong Hình 4.1 của TH4).
5. Bước 1 (TH1) chỉ cần khung applet; mã đầy đủ thêm dần theo `th/`. Bản cuối: chép nội dung [code/applet/src/sinhvien/StudentCard.java](code/applet/src/sinhvien/StudentCard.java).

### 2.2 Build

1. `Build → Build` (hoặc nút build). Khung **Output** phải có:

   ```text
   Feitian Java Card 2.2.2 Class File Converter, Version 1.3
   conversion completed with 0 errors and 0 warnings.
   ```

2. Lỗi của bộ chuyển đổi gần như luôn do vi phạm giới hạn Java Card (bài 3): dùng `int`/`long`, thiếu ép kiểu `(short)` / `(byte)`, dùng `String`, mảng 2 chiều, `@Override`, generics, vòng lặp `for (x : arr)`. Sửa theo bảng ở [th/th1-applet-apdu.md](th/th1-applet-apdu.md#5-lỗi-biên-dịch-hay-gặp).

### 2.3 Bật giao tiếp PC/SC

`Tool → IDE Options → Run/Debug → Configuration` (Hình 2.2 TH2):

| Mục | Đặt |
|---|---|
| JCVM Started by IDE | ✔ |
| Server IP | `127.0.0.1` |
| Used Port | để trống (CB 9025, Break 9550, CL 9035, Command 9551, Reader 5000 – mặc định) |
| **Enable PCSC Interface** | ✔ – bắt buộc để PyApduTool và NetBeans nhìn thấy thẻ ảo |

### 2.4 Tham số cài đặt applet (PIN quản trị)

`Debug → Run or Debug Configuration` (hoặc chuột phải applet → `Run or Debug Configuration`) → tab **Package Upload** → chọn dòng `StudentCard` → **Edit** (Hình 4.2, 4.3 TH4):

| Ô | Giá trị |
|---|---|
| Instance AID | `4B 4D 41 53 56 01 01` (7 byte, trong khoảng 5–16) |
| Application Specific Parameters | `38 37 36 35 34 33 32 31` (= chuỗi ASCII `87654321`, PIN quản trị) |
| Application Privileges | để trống |

Không nhập tham số → applet dùng PIN quản trị mặc định `12345678` (chỉ để chạy thử). Cách applet đọc tham số: [th/th4-install-khoi-tao-the.md](th/th4-install-khoi-tao-the.md).

### 2.5 Chạy thẻ ảo

1. `Debug → Run` (Ctrl+F8). Khung **Debug Shell** chạy một loạt lệnh dò của JCIDE (các dòng `6A 82`, `6A 86` là bình thường) và kết thúc bằng:

   ```text
   Disconnect CB port
   You can send apdu with PC/SC interface now
   ```

2. Từ lúc này mới mở PyApduTool hoặc chạy phần mềm NetBeans.
3. Gõ thử ngay trong Debug Shell theo cú pháp của tài liệu:

   ```text
   /select 4B4D4153560101
   /send B0100000
   ```

   Thẻ mới cài trả `00 03 05 00 00 00 00 00 00 00 00 90 00` (BLANK, PIN còn 3, PUK còn 5, chưa có mã SV).

> Mỗi lần **Run lại** là cài lại applet → thẻ về BLANK, mất dữ liệu đã phát hành. Muốn giữ thẻ giữa các lần chạy phần mềm thì để JCIDE chạy liên tục, chỉ tắt/mở phần mềm NetBeans.

## 3. Kiểm thử tay bằng PyApduTool

1. Tab **Reader**: chọn `JAVACOS Virtual Contact Reader 0`, Protocol `T = 0`, Connect Mode `Exclusive Mode` → **Connect** → Information hiện `Connect successful.`
2. Tab **Manager**: chọn applet AID `4B 4D 41 53 56 01 01` → **Select** → `Select Applet successful.`
3. Tab **Apdu**: nhập CLA/INS/P1/P2/LC/Data hoặc dán cả dòng ở chế độ Binary → **Send**.
4. **Xong thì Disconnect.** PyApduTool giữ thẻ ở chế độ *Exclusive*: còn kết nối thì phần mềm NetBeans không vào được (Debug Shell báo kiểu `Connect CL port failed. Please disconnect JAVACOS Vi…`).

Kịch bản kiểm nhanh – **kết quả đã chạy thật trên trình giả lập** (chữ ký RSA thay đổi mỗi lần nên chỉ ghi độ dài):

| # | Gửi | Nhận | Ý nghĩa |
|---|---|---|---|
| 1 | `00 A4 04 00 07 4B 4D 41 53 56 01 01` | `90 00` | Chọn applet |
| 2 | `B0 10 00 00 00` | `00 03 05 00 00 00 00 00 00 00 00 90 00` | BLANK |
| 3 | `B0 20 00 00 08 38 37 36 35 34 33 32 30` | `63 C4` | PIN quản trị sai, còn 4 |
| 4 | `B0 20 00 00 08 38 37 36 35 34 33 32 31` | `90 00` | PIN quản trị đúng |
| 5 | `B0 30 00 00 19 08 43 54 30 37 30 32 35 34 06 31 32 33 34 35 36 08 31 32 33 34 35 36 37 38` | `90 00` | Phát hành: mã SV `CT070254`, PIN `123456`, PUK `12345678` |
| 6 | `B0 10 00 00 00` | `01 03 05 01 00 00 00 00 00 00 08 43 54 30 37 30 32 35 34 90 00` | ACTIVE, cờ QT, mã SV |
| 7 | `B0 31 00 00 00` | `00 80 …` modulus 128 byte, `00 03 01 00 01`, `90 00` | Khóa công khai (jCardSim trả `00 81` vì thêm byte 00 đầu) |
| 8 | `B0 50 00 00 00` | `69 82` | Chưa nhập PIN |
| 9 | `B0 21 00 00 06 30 30 30 30 30 30` | `63 C2` | PIN sai, còn 2 |
| 10 | `B0 21 00 00 06 31 32 33 34 35 36` | `90 00` | PIN đúng |
| 11 | `B0 40 02 00 10 11 06 48 61 20 4E 6F 69 80 00 00 00 00 00 00 00` | `90 00` | Ghi CONTACT: TLV quê quán "Ha Noi" + đệm `80 00…` |
| 12 | `B0 41 02 00 00` | `11 06 48 61 20 4E 6F 69 80 00 00 00 00 00 00 00 90 00` | Đọc lại (thẻ giải mã) |
| 13 | `B0 51 00 00 15 06 31 32 33 34 35 36 01 F4 00 00 00 00 01 02 03 04 05 06 07 08` | `01 F4 00 01` + 128 byte chữ ký + `90 00` | Nạp 500 (nghìn), bộ đếm 1 |
| 14 | `B0 52 00 00 15 06 31 32 33 34 35 36 00 78 00 00 00 00 01 02 03 04 05 06 07 09` | `01 7C 00 02` + 128 byte + `90 00` | Trả 120 → còn 380 |
| 15 | `B0 52 00 00 15 06 31 32 33 34 35 36 03 E8 00 00 00 00 01 02 03 04 05 06 07 0A` | `6A 85` | Trả 1000: không đủ tiền |
| 16 | `B0 53 00 00 00` | `02 00 00 78 01 7C 00 02 00 00 00 00 01 00 01 F4 01 F4 00 01 00 00 00 00 90 00` | Nhật ký: trả 120, nạp 500 |
| 17 | `B0 60 00 00 0E 02 03 00 00 00 00 0A 0B 0C 0D 0E 0F 10 11` | `01 7C 00 03` + 128 byte + `90 00` | Điểm danh phòng 0203 |
| 18 | `B0 32 00 00 10 00 11 22 33 44 55 66 77 88 99 AA BB CC DD EE FF` | 128 byte + `90 00` | Thẻ ký challenge |
| 19 | `B0 22 00 00 0E 06 31 32 33 34 35 36 06 36 35 34 33 32 31` | `90 00` | Đổi PIN 123456 → 654321 |
| 20 | `B0 99 00 00 00` / `00 10 00 00 00` | `6D 00` / `6E 00` | Sai INS / sai CLA |

Cách tự ghép một lệnh: `CLA INS P1 P2 Lc Data`. Ví dụ PIN `123456` → ASCII `31 32 33 34 35 36`, Lc = `06`. Số tiền 500 → `01 F4` (500 = 0x01F4).

## 4. Cấu hình project NetBeans

Phần mềm đã có sẵn đủ mã ([code/host/src/](code/host/src/)): giao diện 10 màn hình (`sinhvien.ui`), lớp giao tiếp thẻ + CSDL (`sinhvien.host`). Mở bằng NetBeans như sau – **không phải dựng form bằng GUI Builder**.

### 4.1 Tạo project từ mã có sẵn

1. `File → New Project → Java → Java Project with Existing Sources` → Next.
2. Project Name: `TheSinhVienApp`. **Project Folder: chọn thư mục `code\host`** (để file `app.properties` nằm ngay thư mục chạy) → Next.
3. Source Package Folders → **Add Folder** 2 lần:
   - `code\host\src` (phần mềm)
   - `code\applet\src` (applet – dùng chung đúng một file `StudentCard.java` với JCIDE, để chạy chế độ thẻ giả lập)
4. Test Package Folders (tùy chọn): `code\test\src` → Finish.

### 4.2 Thuộc tính project (chuột phải project → Properties)

| Mục | Đặt |
|---|---|
| Sources → Source/Binary Format | **JDK 8** |
| Sources → Encoding | **UTF-8** (không đặt thì chữ tiếng Việt trong mã thành `?`) |
| Libraries → Compile → Add JAR/Folder | chọn cả 4 file trong `code\host\lib\`: `flatlaf-3.5.4.jar`, `mssql-jdbc-12.8.1.jre8.jar`, `sqlite-jdbc-3.41.2.2.jar`, `jcardsim-2.2.2.jar` |
| Run → Main Class | `sinhvien.ui.App` |
| Run → Arguments | `app.properties` |
| Run → Working Directory | để trống (= thư mục project `code\host`) |

Bấm **F6** (Run Project). File SQL tạo bảng (`src/sinhvien/host/db/*.sql`) NetBeans tự chép vào bản build.

### 4.3 Trong phần mềm có gì

| Thành phần | File | Vai trò |
|---|---|---|
| Điểm chạy | [App.java](code/host/src/sinhvien/ui/App.java) | Đọc cấu hình → giao diện → mở CSDL (tự tạo) → phiên thẻ → đăng nhập |
| Khung cửa sổ | [MainView.java](code/host/src/sinhvien/ui/MainView.java) | Thanh bên theo vai trò, header có chip trạng thái thẻ + nút Kết nối/Rút thẻ, khung **Nhật ký APDU** (nút ⌨), nút sáng/tối |
| Phiên thẻ | [CardSession.java](code/host/src/sinhvien/ui/CardSession.java) | Cắm thẻ = SELECT → GET_STATUS → tra khóa công khai CSDL → xác thực RSA |
| 10 màn hình | `*Page.java` | Tổng quan, Phát hành thẻ, Sinh viên & thẻ, Thông tin thẻ, Mã PIN, Ví & lịch sử, Thu ngân, Điểm danh, Bảo mật thẻ, Thống kê |
| Giao tiếp thẻ | [CardClient.java](code/host/src/sinhvien/host/CardClient.java) | Mỗi lệnh APDU = 1 hàm |
| CSDL | [CardRepository.java](code/host/src/sinhvien/host/CardRepository.java), [DatabaseBootstrap.java](code/host/src/sinhvien/host/DatabaseBootstrap.java) | SQL chung cho SQL Server/SQLite; tự `CREATE DATABASE` + bảng + dữ liệu mẫu |

Ảnh từng màn hình: [images/giao-dien/](images/giao-dien/) (chụp tự động bằng bộ test, xem [th8 mục 3](th/th8-ket-noi-phan-mem-csdl.md#3-các-màn-hình)).

### 4.4 Chạy nhanh (không cần NetBeans / JCIDE / SQL Server)

Để xem phần mềm chạy trước khi cài đủ: sửa `code\host\app.properties`:

```properties
card.mode=jcardsim
db.url=jdbc:sqlite:the_sinh_vien.db
```

rồi chạy [code/host/chay-phan-mem.cmd](code/host/chay-phan-mem.cmd) (tự biên dịch + chạy, chỉ cần JDK). Thẻ giả lập chạy đúng applet `StudentCard` trong phần mềm; tắt phần mềm là về thẻ trắng. **Trước khi báo cáo đổi lại `card.mode=pcsc` và `db.url` SQL Server.**

## 5. Cài và cấu hình SQL Server

### 5.1 Cài

1. Tải **SQL Server 2022 Express** (hoặc Developer) từ trang Microsoft → chọn kiểu cài **Basic**. Ghi lại tên instance: Express mặc định `SQLEXPRESS`, Developer mặc định `MSSQLSERVER`.
2. Tải và cài **SQL Server Management Studio (SSMS)**.

### 5.2 Bật đăng nhập bằng tài khoản SQL (`sa`)

Phần mềm Java đăng nhập bằng `sa` + mật khẩu (cách đơn giản nhất với driver JDBC).

1. SSMS → Server name `localhost\SQLEXPRESS` (Developer: `localhost`) → Authentication **Windows Authentication** → Connect.
2. Chuột phải tên server → **Properties → Security** → chọn **SQL Server and Windows Authentication mode** → OK.
3. `Security → Logins → sa` → chuột phải **Properties**: tab General đặt **Password**; tab Status chọn **Login: Enabled** → OK.
4. Chuột phải tên server → **Restart**.

### 5.3 Bật TCP/IP cổng 1433

Driver JDBC nối qua TCP/IP, bản Express mặc định **tắt** TCP/IP.

1. Mở **SQL Server Configuration Manager** (Windows 11 không thấy trong Start thì `Win + R` → `SQLServerManager16.msc` cho bản 2022, `SQLServerManager15.msc` cho bản 2019).
2. `SQL Server Network Configuration → Protocols for SQLEXPRESS → TCP/IP` → **Enabled**.
3. Chuột phải TCP/IP → Properties → tab **IP Addresses** → kéo xuống **IPAll**: `TCP Dynamic Ports` = *(xóa trống)*, `TCP Port` = **1433** → OK.
4. `SQL Server Services → SQL Server (SQLEXPRESS)` → chuột phải **Restart**.
5. Thử lại trong SSMS: Server name `localhost,1433`, Authentication **SQL Server Authentication**, `sa` + mật khẩu → Connect được là xong.

### 5.4 Điền vào `app.properties`

```properties
db.url=jdbc:sqlserver://localhost:1433;databaseName=the_sinh_vien;encrypt=false
db.user=sa
db.password=<mật khẩu sa>
db.autoCreate=true
```

- `encrypt=false` bắt buộc với driver 12.x khi SQL Server chưa có chứng chỉ (không có thì lỗi SSL).
- Không mở cổng 1433 theo 5.3 được thì dùng `jdbc:sqlserver://localhost\\SQLEXPRESS;databaseName=the_sinh_vien;encrypt=false` và bật thêm dịch vụ **SQL Server Browser**.

### 5.5 Database và bảng: phần mềm tự tạo

Lần chạy đầu, [DatabaseBootstrap](code/host/src/sinhvien/host/DatabaseBootstrap.java):

1. Nối vào `the_sinh_vien`; chưa có (lỗi 4060) thì nối vào `master` và chạy `CREATE DATABASE the_sinh_vien`.
2. Chạy [schema-sqlserver.sql](code/host/src/sinhvien/host/db/schema-sqlserver.sql): 5 bảng (`sinh_vien`, `the`, `giao_dich`, `nguoi_dung`, `phong`), chỉ mục, tài khoản mẫu, 3 phòng mẫu. Mọi câu có kiểm tra "chưa có mới tạo" nên chạy mỗi lần khởi động không sao.

Kiểm tra trong SSMS: `Databases → the_sinh_vien → Tables` có 5 bảng; `SELECT * FROM nguoi_dung` có 3 dòng.

Tài khoản đăng nhập mẫu (mật khẩu băm `SHA-256(salt + mật khẩu)` trong bảng `nguoi_dung`):

| Tài khoản | Mật khẩu | Vai trò | Thấy màn hình |
|---|---|---|---|
| `admin` | `admin@123` | Phòng Đào tạo | cả 10 màn |
| `thungan` | `thungan@123` | Thu ngân | Thu ngân, Ví & lịch sử, Bảo mật thẻ |
| `gv` | `gv@123` | Giảng viên | Điểm danh, Bảo mật thẻ |
| *(nút "Sinh viên – dùng thẻ")* | PIN của thẻ | Sinh viên | Thông tin thẻ, Mã PIN, Ví & lịch sử, Bảo mật thẻ |

Chữ tiếng Việt: mọi cột chữ là `NVARCHAR`, dữ liệu mẫu viết `N'...'`; driver JDBC gửi chuỗi dạng Unicode sẵn.

## 6. File cấu hình phần mềm `app.properties`

Phần mềm đọc file này lúc khởi động ([AppConfig](code/host/src/sinhvien/host/AppConfig.java)). Thiếu file hoặc thiếu khóa → dùng mặc định. Mẫu đầy đủ có chú thích: [code/host/app.properties](code/host/app.properties).

| Khóa | Mặc định | Ý nghĩa | Khi nào đổi |
|---|---|---|---|
| `card.mode` | `pcsc` | `pcsc` = JCIDE / đầu đọc thật; `jcardsim` = thẻ giả lập trong phần mềm | Tự thử khi chưa mở JCIDE → `jcardsim`; **báo cáo → `pcsc`** |
| `card.reader` | *(rỗng = tự chọn)* | Một phần tên đầu đọc | JCIDE: `JAVACOS Virtual` |
| `card.protocol` | `T=0` | Giao thức `terminal.connect(...)` | Không chắc → `*` |
| `card.aid` | `4B 4D 41 53 56 01 01` | Applet AID cần chọn | Đổi AID trong JCIDE thì đổi ở đây |
| `card.challenge.bytes` | `16` | Độ dài challenge xác thực thẻ (8–64) | |
| `card.sim.adminPin` | `87654321` | PIN quản trị nạp vào thẻ giả lập | Chỉ dùng với `jcardsim` |
| `db.url` | SQL Server `localhost:1433`, DB `the_sinh_vien`, `encrypt=false` | Chuỗi kết nối JDBC | Express có tên instance; chạy thử SQLite |
| `db.user` / `db.password` | `sa` / *(rỗng)* | Tài khoản SQL Server | Luôn điền mật khẩu `sa` |
| `db.autoCreate` | `true` | Tự `CREATE DATABASE` nếu chưa có | Tài khoản không có quyền tạo DB → `false`, tạo tay trong SSMS |
| `app.pin.length` / `app.puk.length` | `6` / `8` | Độ dài PIN/PUK giao diện bắt nhập | Thẻ nhận 4–16 ký tự |
| `app.topup.max` | `5000` | Nạp tối đa mỗi lần (nghìn đồng) | Thẻ còn chặn tổng số dư ở 30 000 |
| `app.photo.maxBytes` / `width` / `height` | `7600` / `120` / `160` | Thu nhỏ + nén ảnh cho vừa ô nhớ thẻ | |
| `app.checkin.room` | `0x0203` | Phòng mặc định của máy điểm danh | Mỗi máy điểm danh một giá trị |
| `ui.theme` | `light` | `light` / `dark` / `system` (giao diện Windows) | Trình chiếu phòng tối → `dark`; trong phần mềm có nút đổi nhanh |
| `ui.font` / `ui.fontSize` | `Segoe UI` / `14` | Phông chữ | Màn chiếu → 16 |
| `log.apdu` | `true` | In APDU ra cửa sổ Output của NetBeans (che PIN) | Khung Nhật ký APDU trong phần mềm luôn có |

## 7. Chạy demo đúng chuẩn báo cáo (JCIDE + SQL Server)

1. JCIDE: mở project `TheSinhVien`, kiểm tra tham số cài đặt (mục 2.4) → **Run** → chờ `You can send apdu with PC/SC interface now`.
2. PyApduTool đang mở thì **Disconnect**.
3. `app.properties`: `card.mode=pcsc`, `db.url` SQL Server, mật khẩu `sa`.
4. NetBeans **F6** → đăng nhập `admin / admin@123`.
5. Bấm **Kết nối thẻ** → chip "Thẻ trắng – chưa phát hành" → làm theo kịch bản demo ở [th8 mục 6](th/th8-ket-noi-phan-mem-csdl.md#6-kịch-bản-demo-quay-video).
6. Bật khung **Nhật ký APDU** (nút ⌨ trên header) khi quay video để thấy lệnh gửi xuống thẻ (PIN đã che).

## 8. Bộ test tự động

[code/run-test.cmd](code/run-test.cmd): không cần JCIDE, không cần SQL Server.

1. `SimTest` – 49 kịch bản: applet + thư viện máy tính + CSDL.
2. `UiShots` – chạy đủ luồng qua các màn hình (phát hành, giao dịch, điểm danh, đọc thông tin, xác thực RSA) và chụp 14 ảnh vào `code\test\anh-giao-dien\`.

Sửa applet hay phần mềm xong đều chạy lại; dòng cuối mỗi phần phải là `TẤT CẢ … ĐỀU ĐÚNG`.

## 9. Lỗi hay gặp

| Hiện tượng | Nguyên nhân thường gặp | Cách sửa |
|---|---|---|
| Khởi động báo "Không kết nối được CSDL … TCP/IP connection … port 1433 has failed" | SQL Server chưa bật TCP/IP / chưa đặt cổng 1433 / dịch vụ chưa chạy | Mục 5.3 |
| "Login failed for user 'sa'" | Chưa bật chế độ SQL Server Authentication, `sa` bị Disabled, sai mật khẩu | Mục 5.2 |
| Lỗi SSL / certificate / encrypt | Driver 12.x mặc định mã hóa kết nối | Thêm `;encrypt=false` vào `db.url` |
| "CREATE DATABASE permission denied" | Tài khoản không phải `sa` | Tạo DB tay trong SSMS, đặt `db.autoCreate=false` |
| "No suitable driver" | Thiếu jar trong Libraries / `lib` | Chạy `tai-thu-vien.cmd`, thêm jar theo mục 4.2 |
| Chữ tiếng Việt thành `?` trong giao diện | Encoding project | NetBeans Encoding UTF-8 (mục 4.2) |
| Phần mềm báo "Không thấy đầu đọc" / `SCARD_E_NO_SERVICE` | JCIDE chưa Run, hoặc chưa bật Enable PCSC Interface | Mục 2.3, 2.5; dịch vụ Windows "Smart Card" phải đang chạy |
| "Thẻ không có applet thẻ sinh viên (SELECT trả 6A82)" | Sai AID / applet chưa cài xong | So `card.aid` với Instance AID trong Run Configuration |
| Debug Shell: `Connect CL port failed. Please disconnect JAVACOS Vi…` | PyApduTool đang giữ thẻ (Exclusive) | Disconnect PyApduTool |
| Chip đỏ "Không xác thực được" sau khi Run lại JCIDE | Run lại = cài lại applet = thẻ mới với khóa RSA mới, CSDL còn khóa cũ | Phát hành lại thẻ (màn Phát hành tự thu hồi khóa cũ) |
| Chip đỏ "Đã báo mất" | Thẻ bị khóa trong CSDL | Màn Sinh viên & thẻ → Mở khóa trong CSDL |
| `6E 00` | Gửi CLA `00` cho lệnh của applet | Lệnh applet dùng `B0`; chỉ SELECT dùng `00` |
| `6F 00` | Applet ném lỗi không bắt (`ArrayIndexOutOfBounds`, `NullPointer`) | Xem Debug Shell; kiểm độ dài dữ liệu |
| Bộ chuyển đổi JCIDE báo lỗi kiểu `int` | Phép tính chưa ép kiểu | `(short)(a + b)`; bài 3 slide 4–9 |
| Bấm "Phát hành" giao diện chờ vài giây | Sinh khóa RSA trên thẻ chậm | Bình thường; header hiện "Đang phát hành thẻ…" |

## Nhật ký

| Ngày | Máy của ai | Kết quả |
|---|---|---|
| 07/11/2026 | (máy soạn notes) | Bộ test jCardSim 49/49; **chưa chạy JCIDE** |
| 08/11/2026 | (máy soạn notes) | Phần mềm đủ 10 màn hình; `run-test.cmd` qua 49 + 8 kiểm tra; driver SQL Server nạp được và báo lỗi đúng khi chưa có server. **Chưa chạy trên SQL Server thật và JCIDE** – người đầu tiên chạy được ghi lại ở đây |
| | | |
