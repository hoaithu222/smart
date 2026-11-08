# 06 · Áp dụng kinh nghiệm (CV) + phân công + lịch

> Ràng buộc: **ngôn ngữ và thư viện theo cô dạy** – Java Card 2.2.2 trên JCIDE cho thẻ; Java 8 + Swing + `javax.smartcardio` trên NetBeans cho máy tính. Kinh nghiệm React/Next/NestJS **không mang công nghệ sang**, chỉ mang **cách tổ chức, luồng và thói quen kỹ thuật** sang.

## 1. Bê kinh nghiệm nào sang, thành cái gì

| Kinh nghiệm trong CV | Ý tưởng giữ lại | Thành phần cụ thể trong đề tài |
|---|---|---|
| Feature-Sliced Design (VCI-PMS), module cắm-rút không đụng lõi | Tách **lõi dùng chung** khỏi **tính năng theo vai trò**; phụ thuộc một chiều | `sinhvien.host` (lõi, không biết Swing) ← `sinhvien.ui.admin / sv / thungan / diemdanh`. Form không tạo `CommandAPDU`, không viết SQL ([03 mục 8](03-thiet-ke.md#8-kiến-trúc-phần-mềm-máy-tính)) |
| Route Guard 4 lớp (Auth → Workspace → Project → Role) | Chuỗi chặn theo thứ tự, lớp sau chỉ chạy khi lớp trước qua | **Chuỗi chặn thẻ**: Kết nối → Thẻ thật (RSA) → Đăng nhập vai trò (CSDL) → PIN (thẻ) → PIN quản trị (thẻ). Mỗi panel khai báo cần tới lớp nào; viết một hàm `canOpen(panel)` duy nhất |
| OAuth 2.0 PKCE tự viết (zero-trust) | Bên kia phải **chứng minh** sở hữu bí mật bằng giá trị dùng một lần, không gửi bí mật | Challenge–response RSA: phần mềm sinh challenge, thẻ ký, kiểm bằng khóa đã lưu; khóa bí mật không rời thẻ. Giải thích cho cô bằng đúng phép so sánh này |
| Fintech VPS: chuyển tiền, OTP, sổ lệnh | Giao dịch **idempotent**, đối soát, biên nhận, OTP mỗi giao dịch | PIN trong từng lệnh `51/52` (≈ OTP); bộ đếm + `UNIQUE(ma_sv, bo_dem)` (≈ idempotency key); đối soát nhật ký thẻ ↔ CSDL; biên nhận có chữ ký |
| TanStack Query: nguồn dữ liệu đúng nằm ở server, cache có hạn | **Thẻ là nguồn đúng** của số dư/PIN; giao diện không tự cộng trừ | Sau mỗi lệnh ghi (`pay`, `writeObject`…) gọi lại `status()`/`balance()`; không giữ số dư trong biến giao diện lâu |
| Zustand / Redux: một kho trạng thái, nhiều nơi lắng nghe | Trạng thái kết nối thẻ dùng chung cho mọi màn | [CardSession](code/host/src/sinhvien/ui/CardSession.java) (thẻ đang cắm, mã SV, thẻ thật, đã nhập PIN) + `PropertyChangeSupport`; chip trên header và các màn đăng ký lắng nghe |
| Custom hooks / `useMutation` (loading – error – success) | Một khuôn cho mọi thao tác chạy lâu | [Task](code/host/src/sinhvien/ui/Task.java) bọc `SwingWorker`: tự báo "Đang …" trên header, tự hiện lỗi tiếng Việt, có callback `always` (giống `onSettled`) |
| Zod / Yup, React Hook Form | Kiểm dữ liệu ở biên, một chỗ | [Validators](code/host/src/sinhvien/ui/Validators.java) (PIN đúng 6 số, PUK 8 số, số tiền, ngày dd/MM/yyyy, dữ liệu vừa ô nhớ thẻ); thẻ vẫn kiểm lại (phòng thủ 2 lớp) |
| RBAC (admin portal JHipster) | Vai trò ở CSDL, menu theo vai trò | Bảng `nguoi_dung.vai_tro`; `MainFrame` chỉ dựng tab theo vai trò; quyền thật vẫn do thẻ quyết ([05 câu 50](05-on-tap-van-dap.md#k-thiết-kế-của-đề-tài-chắc-chắn-bị-hỏi)) |
| UI library (MUI, Radix, shadcn), design token | Khai báo màu/phông/khoảng cách **một lần** | [Theme](code/host/src/sinhvien/ui/Theme.java) (màu đọc lúc vẽ nên đổi sáng/tối không mở lại cửa sổ) + FlatLaf + khối dùng chung [Ui](code/host/src/sinhvien/ui/Ui.java) (thẻ bo góc, chip, ô số liệu, biểu mẫu) |
| Prisma / NestJS (thiết kế DB) | Schema rõ, ràng buộc ở DB, migration chạy được nhiều lần | `schema-sqlserver.sql` có khóa ngoại, `UNIQUE`, mọi câu "chưa có mới tạo"; [DatabaseBootstrap](code/host/src/sinhvien/host/DatabaseBootstrap.java) tự chạy lúc khởi động; `CardRepository` gom toàn bộ SQL |
| Viết quy ước code + luật cho AI assistant | Quy ước chung cho 3 người, giới hạn của nền tảng ghi thành luật | File quy ước nhóm (mục 2) + đưa bảng "lỗi biên dịch hay gặp" ([bước 1](th/th1-applet-apdu.md#5-lỗi-biên-dịch-hay-gặp)) cho AI trước khi nhờ sinh mã applet |

**Không mang sang** (lệch công cụ của môn, tốn thời gian, dễ bị hỏi "sao không dùng cái cô dạy"): React/Electron/web quản trị, TypeScript, NestJS/REST API, Socket.IO, app điện thoại.

## 2. Quy ước nhóm (dán vào đầu repo)

- Applet: hằng `INS_*`, `SW_*`, `STATE_*`, `OBJ_*`; mọi số trong `case` là hằng có tên; mọi phép tính có `(short)`/`(byte)`; không `int`, không `@Override`, không `static` cho dữ liệu của thể hiện.
- Mỗi lệnh APDU mới: thêm 1 dòng vào bảng [03 mục 3](03-thiet-ke.md#3-bảng-lệnh-apdu) → 1 hàm trong `CardClient` → 1 kịch bản trong `SimTest` → mới tới giao diện.
- Thông báo cho người dùng bằng tiếng Việt, không hiện mã hex trên giao diện.
- Không commit `the_sinh_vien.db`, `app.properties` có mật khẩu `sa` thật, thư mục `build/`, `lib/`.
- Sửa applet hay phần mềm xong: chạy `run-test.cmd` → 49/49 + 8/8 → mới đẩy lên nhánh chung.

## 3. Phân công đề xuất (nhóm chốt lại)

Cô yêu cầu nộp "Phân công công việc (cụ thể ai làm gì?)". Đề xuất theo thế mạnh – **nhóm tự sửa tên/việc**:

| Người | Phần chính | Bước | Phần báo cáo | Hỗ trợ |
|---|---|---|---|---|
| Vũ Thị Hoài Thu | Phần mềm máy tính (đã có bản đủ 10 màn): chạy với JCIDE + SQL Server, sửa theo góp ý, video demo | 8 (+ phần máy tính của 1–7) | 1.7 CSDL, 1.8 giao diện, 2.1 môi trường, 2.3 phần mềm | Review mã applet theo quy ước |
| Phạm Như Ngọc | Applet phần nền: khung, PIN, đối tượng bền/tạm, giao dịch ví, install/phát hành; kịch bản PyApduTool | 1, 2, 3, 4, 5 | 1.3 dữ liệu trên thẻ, 1.4 bảng APDU, 1.6 luồng, 2.2 (nửa đầu) | Màn PIN/Đổi PIN |
| Nguyễn Thị Phương Thảo | Applet phần bảo mật: băm PIN, AES (DEK/KEK), RSA, ảnh; bộ test | 6, 7 | 1.5 thiết kế bảo mật, 2.2 (nửa sau), 2.4 kiểm thử | Màn Thông tin + ảnh |
| Cả nhóm | Chương 1.1–1.2, slide, ôn vấn đáp chéo ([05](05-on-tap-van-dap.md)), điểm dự kiến | | | |

Mỗi người phải **giải thích được phần của người khác** – vấn đáp hỏi bất kỳ ai.

## 4. Lịch (07/11 → 28/12/2026)

| Tuần | Ngày | Việc | Kiểm tra cuối tuần |
|---|---|---|---|
| 1 | 07–13/11 | Cài môi trường, bước 1, chốt chức năng, dán mô tả vào sheet, hỏi cô 4 câu | Mọi máy `SELECT` + `GET_STATUS` được |
| 2 | 14–20/11 | Bước 2, 3, 4; khung màn hình Kết nối / PIN / Phát hành | PyApduTool chạy kịch bản bước 2–4 |
| 3 | 21–29/11 | Hoàn thiện mốc 1, ghép phần mềm với JCIDE, tập trình bày | **Báo cáo lần 1 – 30/11** |
| 4 | 30/11–06/12 | Bước 6: băm PIN, AES, RSA (kiểm thuật toán trên JCIDE trước) | Bộ test 49/49 với mã của nhóm |
| 5 | 07–13/12 | Bước 7 ảnh; xác thực thẻ trong phần mềm | **Báo cáo lần 2 – 14/12** |
| 6 | 14–20/12 | CSDL đầy đủ, Thu ngân, Điểm danh, Thống kê, làm đẹp giao diện | Chạy hết kịch bản demo [bước 8 mục 6](th/th8-ket-noi-phan-mem-csdl.md#6-kịch-bản-demo-quay-video) |
| 7 | 21–26/12 | Word ≥ 30 trang, slide, video, phân công, điểm dự kiến | **Nộp bản mềm trước 26/12** (2 ngày trước 28/12, ngày dự kiến) |

## Nhật ký

| Ngày | Ai | Thay đổi |
|---|---|---|
| 07/11/2026 | Thu | Bản đầu – phân công và lịch chờ nhóm chốt |
| | | |
