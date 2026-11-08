# Bước 8 · Hướng dẫn kết nối → Phần mềm Java Swing + SQL Server + demo

> Tài liệu: [huong-dan-ket-noi.md](../../huong-dan-ket-noi.md) (JCIDE + NetBeans 8.2, `javax.smartcardio`, `SmartCardWord.connectCard()`, nút Connect/Disconnect).
> Mốc: giao diện ở **Lần 1 (30/11)**; CSDL + đủ chức năng ở **Lần 3 (28/12)**. Tiêu chí: giao diện đẹp, có CSDL, có demo (mỗi mục 0.5). Người phụ trách: ……
> **Trạng thái 08/11/2026: phần mềm đã viết xong** ([code/host/src/sinhvien/](../code/host/src/sinhvien/)), chạy qua 8 kiểm tra giao diện trên thẻ giả lập + SQLite. Việc còn lại: chạy với JCIDE + SQL Server thật, quay video. Cấu hình: [04 mục 4–7](../04-moi-truong-cau-hinh.md#4-cấu-hình-project-netbeans).

## 1. Bài học cần nắm

- `TerminalFactory.getDefault().terminals().list()` → chọn đầu đọc → `terminal.connect("T=0")` → `card.getBasicChannel()` → `channel.transmit(new CommandAPDU(...))` → `ResponseAPDU.getSW()`, `getData()`.
- Sau khi kết nối phải `SELECT` applet bằng AID: `00 A4 04 00 Lc AID`; `90 00` thành công, `64 00` thẻ bị vô hiệu hóa.
- Tài liệu chia 3 file: `GUI.java` (giao diện), `SmartCardWord.java` (hàm giao tiếp thẻ), nút Connect đổi thành Disconnect khi đã kết nối.

## 2. Từ ví dụ của cô sang cấu trúc của nhóm

| Ví dụ của cô | Trong phần mềm của nhóm | Thêm gì |
|---|---|---|
| `SmartCardWord.connectCard()` | [PcscChannel.connect()](../code/host/src/sinhvien/host/PcscChannel.java) | Chọn đầu đọc theo tên trong `app.properties`, trả OK / CARD_LOCKED (`64 00`) / NOT_FOUND – đúng 3 nhánh của `connectCard()` |
| `SmartCardWord.disconnect()` | `PcscChannel.disconnect()` | |
| Nút Connect / Disconnect | Nút **Kết nối thẻ / Rút thẻ** trên header ([MainView](../code/host/src/sinhvien/ui/MainView.java)) + chip trạng thái | Cắm thẻ chạy luôn chuỗi SELECT → GET_STATUS → tra khóa CSDL → xác thực RSA ([CardSession](../code/host/src/sinhvien/ui/CardSession.java)) |
| Gửi APDU trực tiếp trong nút bấm | [CardClient](../code/host/src/sinhvien/host/CardClient.java): mỗi lệnh 1 hàm, lỗi SW → `CardSwException` tiếng Việt | Màn hình không ghép byte |
| (không có) | [CardRepository](../code/host/src/sinhvien/host/CardRepository.java) + [DatabaseBootstrap](../code/host/src/sinhvien/host/DatabaseBootstrap.java) | SQL Server, tự tạo database/bảng, chỉ ghi biên nhận đã kiểm chữ ký |
| (không có) | Khung **Nhật ký APDU** (nút ⌨) | Cô thấy được lệnh thật gửi xuống thẻ khi demo, PIN đã che |
| (không có) | `card.mode=jcardsim` ([SimCardChannel](../code/host/src/sinhvien/host/SimCardChannel.java)) | Tự thử khi chưa mở JCIDE |
| `GUI.java` 1 form | `MainView` + 10 màn hình `*Page.java`, thanh bên theo vai trò | Đăng nhập theo bảng `nguoi_dung` |

## 3. Các màn hình

Ảnh chụp tự động (thẻ giả lập, dữ liệu mẫu) ở [../images/giao-dien/](../images/giao-dien/).

| Màn hình | Vai trò | Làm gì | Gọi thẻ | Gọi CSDL | Ảnh |
|---|---|---|---|---|---|
| Đăng nhập | mọi người | Tài khoản cán bộ hoặc "Sinh viên – dùng thẻ" | – | `login` | [00](../images/giao-dien/00-dang-nhap.png) |
| Tổng quan | QT | 4 số liệu, hoạt động gần đây, thẻ đang cắm | – | thống kê, `history` | [01](../images/giao-dien/01-tong-quan.png) |
| Phát hành thẻ | QT | Form 11 trường + ảnh + PIN/PUK/PIN QT, 7 bước có dấu tick | `verifyAdmin`, `initCard`, `publicKeyParts`, `verifyPin`, `writeObject` ×3 | `upsertStudent`, `saveIssuedCard` | [02](../images/giao-dien/02-phat-hanh-the.png) |
| Sinh viên & thẻ | QT | Tìm, xem hồ sơ + ảnh, báo mất (CSDL), khóa/mở thẻ đang cắm, lịch sử | `verifyAdmin`, `setLocked` | `listStudents`, `setCardStatus`, `history` | [03](../images/giao-dien/03-sinh-vien-va-the.png) |
| Thông tin thẻ | SV, QT | Nhập PIN → thẻ giải mã INFO/CONTACT/ảnh; SV sửa liên lạc; QT sửa học vụ + ảnh | `verifyPin`, `readObject`, `writeObject` | `updateContact`, `upsertStudent` | [04](../images/giao-dien/04-thong-tin-the.png) |
| Mã PIN | SV, QT | Lượt PIN/PUK còn lại, đổi PIN, mở khóa bằng PUK | `changePin`, `unblockPin` | – | [05](../images/giao-dien/05-ma-pin.png) |
| Ví & lịch sử | SV, thu ngân, QT | Số dư từ thẻ, nhật ký 10 bản ghi trên thẻ, lịch sử CSDL, **đối soát** bộ đếm | `balance`, `log` | `history`, `lastCounter` | [06](../images/giao-dien/06-vi-va-lich-su.png) |
| Thu ngân | thu ngân, QT | Dịch vụ, số tiền (nút nhanh), PIN sinh viên, Nạp / Thanh toán, biên nhận + chữ ký | `topUp`, `pay` | `recordReceipt` | [07](../images/giao-dien/07-thu-ngan.png) |
| Điểm danh | GV, QT | Chọn phòng, quẹt thẻ, ảnh + tên, danh sách hôm nay | `checkIn` | `recordReceipt`, `checkins` | [08](../images/giao-dien/08-diem-danh.png) |
| Bảo mật thẻ | mọi vai trò | Khóa công khai thẻ so với CSDL; challenge → chữ ký → kết quả; tóm tắt thiết kế PIN/AES | `publicKeyParts`, `signChallenge` | `activePublicKeyParts` | [09](../images/giao-dien/09-bao-mat-the.png) |
| Thống kê | QT | Tổng nạp/chi, biểu đồ 7 ngày, chi theo dịch vụ | – | `dailyTotals`, `payByService` | [10](../images/giao-dien/10-thong-ke.png) |

Thêm: [11 – khung Nhật ký APDU](../images/giao-dien/11-nhat-ky-apdu.png) · [12 – giao diện tối](../images/giao-dien/12-giao-dien-toi.png) · [13 – chế độ sinh viên](../images/giao-dien/13-che-do-sinh-vien.png).

## 4. Ba đoạn mã nên hiểu để trả lời vấn đáp

### 4.1 Cắm thẻ = chuỗi chặn ([CardSession](../code/host/src/sinhvien/ui/CardSession.java))

```java
public synchronized void refresh() throws Exception {
    status = client.status();                                   // B0 10
    genuine = false;
    if (status.state == 1 && !status.cardId.isEmpty()) {
        dbStatus = repo.cardStatus(status.cardId);              // ACTIVE / LOCKED / REVOKED
        PublicKey key = repo.activePublicKey(status.cardId);    // khóa lưu lúc phát hành
        genuine = key != null && client.authenticate(key, status.cardId);   // B0 32, kiểm SHA1withRSA
    }
    fire();                                                     // mọi màn hình cập nhật chip
}
```

### 4.2 Việc chậm chạy ngoài luồng giao diện ([Task](../code/host/src/sinhvien/ui/Task.java))

```java
Task.run(this, "Đang phát hành thẻ…",
        () -> issueOnCard(id, pin, puk, adminPin, info, contact),     // chạy nền: APDU + CSDL
        modulusHex -> Dialogs.info(this, "Phát hành thành công", …),  // xong: hiện kết quả
        () -> issue.setEnabled(true));                                // luôn chạy: bật lại nút
```

Lỗi thẻ (`63 C2`, `6A 85`…) được `Task` đổi thành câu tiếng Việt ("Sai mã, còn 2 lần thử", "Số dư không đủ").

### 4.3 Thanh toán: thẻ ký → CSDL kiểm rồi mới ghi ([CashierPage](../code/host/src/sinhvien/ui/CashierPage.java))

```java
CardClient.Receipt r = c.pay(pin, soTienNghin);                          // B0 52, PIN nằm trong lệnh
CardRepository.RecordResult kq = ctx.repo.recordReceipt(maSv, r, dichVu); // kiểm chữ ký + bộ đếm
// OK -> "Chữ ký RSA hợp lệ · đã ghi sổ"; BAD_SIGNATURE -> nghi thẻ giả; REPLAY -> biên nhận cũ
```

## 5. Việc cần làm

- [x] Khung các màn hình + điều hướng theo vai trò (08/11)
- [x] Phát hành, thông tin, PIN, ví, thu ngân, điểm danh, bảo mật, thống kê (08/11, kiểm trên giả lập)
- [ ] Chạy với **JCIDE + SQL Server** theo [04 mục 7](../04-moi-truong-cau-hinh.md#7-chạy-demo-đúng-chuẩn-báo-cáo-jcide--sql-server), ghi kết quả vào nhật ký dưới
- [ ] Đo thời gian sinh RSA trên JCIDE; nếu quá chậm ghi vào báo cáo
- [ ] Quay video theo mục 6
- [ ] Chụp ảnh màn hình trên JCIDE thật cho báo cáo (ảnh trong `images/giao-dien/` là giả lập)

## 6. Kịch bản demo (quay video)

Chuẩn bị: JCIDE Run (thẻ BLANK), SQL Server chạy, bật khung Nhật ký APDU.

| # | Thao tác | Điều cần thấy | Tiêu chí chấm |
|---|---|---|---|
| 1 | Đăng nhập `admin`, bấm **Kết nối thẻ** | Chip vàng "Thẻ trắng – chưa phát hành" | |
| 2 | **Phát hành thẻ**: Điền dữ liệu mẫu (hoặc nhập tay), chọn ảnh, PIN quản trị `87654321`, Phát hành | 7 bước tick xanh, hộp thoại PUK + modulus | Khởi tạo thẻ, Tạo cặp khóa RSA |
| 3 | **Rút thẻ** → **Kết nối thẻ** | Chip xanh "CT070254 · Thẻ thật" | Xác thực |
| 4 | **Bảo mật thẻ**: Đọc khóa công khai, Xác thực thẻ | "Trùng với khóa lưu trong CSDL", "Chữ ký hợp lệ → THẺ THẬT" | Tạo cặp khóa RSA, Xác thực |
| 5 | SSMS: `SELECT * FROM the` | Có modulus của thẻ | Có CSDL |
| 6 | **Thông tin thẻ**: Đọc thẻ, nhập PIN sai 1 lần rồi đúng | "Sai mã, còn 2 lần thử"; sau đó ảnh + thông tin hiện ra | Mã hóa thông tin |
| 7 | Sửa Quê quán → Lưu; Rút / cắm lại; Đọc thẻ | Giá trị mới | Thay đổi thông tin cá nhân |
| 8 | **Mã PIN**: đổi `123456` → `654321`; Thông tin thẻ đọc bằng PIN mới | Đọc được, dữ liệu không mất | Đổi PIN, mã hóa PIN |
| 9 | **Thu ngân**: Nạp 500, Thanh toán Căng tin 25 | Biên nhận "Chữ ký RSA hợp lệ · đã ghi sổ" | Xác thực theo chức năng |
| 10 | Thanh toán 10 000 | "Số dư không đủ" | |
| 11 | **Điểm danh** P203 | Ảnh + tên + "Đã điểm danh · chữ ký hợp lệ" | Chức năng riêng |
| 12 | **Ví & lịch sử** | Nhật ký thẻ khớp CSDL, chip "Khớp CSDL" | |
| 13 | **Mã PIN**: nhập PIN sai 3 lần (ở Thông tin thẻ) → khóa → Quên PIN: PUK + PIN mới → đọc lại thông tin | Dữ liệu còn nguyên | |
| 14 | **Sinh viên & thẻ**: Khóa thẻ đang cắm → Rút / cắm lại | Chip đỏ "Thẻ bị khóa" (`64 00`) | |
| 15 | **Thống kê** | Biểu đồ + chi theo dịch vụ | Có CSDL |
| 16 | Nút ☾ | Giao diện tối | Giao diện đẹp |

## 7. Báo cáo (bám yêu cầu của cô)

- Tên file: `L02_Nhóm 1_Thẻ sinh viên sử dụng Smart Card` (Word, slide, phân công, file nén). Project giữ tên gốc.
- Word ≥ 30 trang, **chỉ 2 chương**, không viết cơ sở lý thuyết:
  - **Chương 1 – Phân tích thiết kế**: 1.1 Bài toán ([01 mục 1](../01-de-tai.md#1-bài-toán)) · 1.2 Tác nhân, chức năng · 1.3 Thiết kế dữ liệu trên thẻ ([03 mục 2](../03-thiet-ke.md#2-bộ-nhớ-trên-thẻ)) · 1.4 Bảng lệnh APDU · 1.5 Thiết kế bảo mật (PIN, AES, RSA, giao dịch) · 1.6 Máy trạng thái, các luồng · 1.7 CSDL SQL Server · 1.8 Thiết kế giao diện (bảng mục 3 + ảnh).
  - **Chương 2 – Cài đặt và thực nghiệm**: 2.1 Môi trường ([04](../04-moi-truong-cau-hinh.md)) · 2.2 Cài đặt applet (trích mã từng phần như các file `th/`) · 2.3 Phần mềm máy tính (mục 2–4 file này) · 2.4 Kiểm thử: bảng APDU + 49 kịch bản + 8 kiểm tra giao diện · 2.5 Đánh giá, hạn chế ([03 mục 5.6](../03-thiet-ke.md#56-giới-hạn-nói-thẳng-khi-bị-hỏi)), hướng phát triển.
- Nộp **2 ngày trước** buổi báo cáo: Project + Word + video demo + phân công cụ thể ai làm gì + điểm dự kiến.

## 8. Câu hỏi có thể bị hỏi

1. *Phần mềm nói chuyện với thẻ qua gì?* – PC/SC, thư viện `javax.smartcardio`; JCIDE tạo đầu đọc ảo khi bật Enable PCSC Interface.
2. *T=0 và T=1 khác nhau?* – T=0 truyền theo byte, T=1 theo khối; APDU mở rộng hợp với T=1.
3. *CSDL lưu những gì, sao không lưu PIN?* – Hồ sơ, khóa công khai từng thẻ, biên nhận đã kiểm chữ ký, tài khoản cán bộ; PIN chỉ thẻ biết.
4. *CSDL đã có thông tin sinh viên rõ thì mã hóa trên thẻ làm gì?* – CSDL ở máy chủ có phân quyền; mã hóa trên thẻ bảo vệ khi **mất thẻ**.
5. *Sửa CSDL để tăng số dư thì sao?* – Số dư thật nằm trên thẻ; CSDL chỉ là sổ đối soát, mọi dòng có chữ ký thẻ.
6. *Vì sao cột chữ là NVARCHAR?* – Lưu Unicode, tiếng Việt có dấu không bị lỗi; `VARCHAR` theo bảng mã một byte sẽ thành `?`.
7. *Thẻ giả cắm vào thì phần mềm biết thế nào?* – Chữ ký challenge không kiểm được bằng khóa công khai lưu trong CSDL → chip đỏ, mọi màn đều chặn (`requireGenuine`).

## Nhật ký

| Ngày | Ai | Làm được | Vướng |
|---|---|---|---|
| 08/11/2026 | Thu (+ Claude) | Phần mềm đủ 10 màn, 8/8 kiểm tra giao diện trên giả lập + SQLite, 14 ảnh | Chưa chạy JCIDE + SQL Server thật |
| | | | |
