# 02 · Tham khảo chức năng các nhóm khác (sheet L02)

> Nguồn: Google Sheet "Danh sách sinh viên đăng kí đề tài Phát triển phần mềm trong thẻ thông minh L02", tab gid=1879152089, xuất CSV ngày **07/11/2026**. Sheet còn thay đổi – xuất lại trước khi chốt.
> Cách xuất lại (sheet đang để chế độ ai có link cũng xem được): mở link dưới trong trình duyệt sẽ tải về file CSV.
> `https://docs.google.com/spreadsheets/d/1_SiRNh7bW6Wht2VJpqG5Y67bDbQYMssW/export?format=csv&gid=1879152089`

## 1. Bảng tổng hợp

| Nhóm | Đề tài | Chức năng đã ghi (rút gọn) |
|---|---|---|
| **1 (mình)** | Thẻ sinh viên sử dụng Smart Card | *(chưa ghi – dán đoạn ở [01-de-tai.md mục 8](01-de-tai.md#8-đoạn-mô-tả-đề-tài-để-dán-vào-google-sheet))* |
| 2 | *(chưa đăng ký)* | – |
| 3 | Thẻ thư viện thông minh | *(chưa ghi mô tả)* |
| 4 | Thẻ bỏ phiếu điện tử | Lưu cử tri + ảnh; AES; khóa phiên RSA/ECDH; **xác thực hai chiều**; PIN + khóa; mỗi thẻ bỏ phiếu 1 lần/cuộc; **giao dịch nguyên tử**; nhiều cuộc bỏ phiếu; **toàn vẹn + chống phát lại APDU**; Java Desktop + CSDL; kiểm thử tình huống tấn công |
| 5 | *(chưa đăng ký)* | – |
| 6 | Thẻ thành viên sân | PIN; lưu + mã hóa thông tin; nạp tiền, thanh toán dịch vụ, **tích điểm, đổi quà** |
| 7 | Thẻ ký túc xá | PIN sai 3 lần khóa, **mở bằng PUK hoặc admin**; maSV, phòng, tòa, hạn hợp đồng, ảnh; AES khóa từ PIN; **trường quan trọng chỉ admin sửa**; ra vào KTX + **nhật ký vào/ra trên thẻ** + cảnh báo giờ giới nghiêm; nạp/thanh toán điện nước, RSA, **counter chống lặp**; web quản trị + app Android + jCardSim; phân quyền 4 vai trò |
| 8 | Thẻ định danh đa tầng & kiểm soát truy cập doanh nghiệp | Nhân viên, phòng ban, quyền, **vector khuôn mặt**, ảnh; ví căng tin offline; chấm công thẻ + nhận diện khuôn mặt; khu vực nhạy cảm cần thẻ + PIN + khuôn mặt |
| 9 | Java Card định danh, ký số lệnh điều khiển, vườn cây thông minh | Staff_ID, Role, Zone_ID; **ký lệnh IoT bằng AES-CMAC hoặc RSA/ECDSA** trên [Action + Zone + Timestamp] chống giả mạo MQTT và phát lại; **bộ đệm vòng** 10–20 thao tác gần nhất trên EEPROM |
| 10 | Thẻ thành viên quán game | Thông tin cá nhân; tích điểm quy đổi giờ chơi; nạp tiền, vào phòng VIP |
| 11 | *(chưa đăng ký)* | – |
| 12 | Thẻ hội viên phòng gym | maHV, họ tên, ngày sinh, SĐT, gói tập, hạn gói, ảnh; quẹt vào phòng; kiểm hạn gói; nạp tiền, mua/gia hạn gói, thuê PT, mua nước |
| 13 | Thẻ công viên trò chơi | Thông tin + ảnh; trò đã chơi, lịch sử, đánh giá; PIN, **trạng thái khóa và lý do khóa** |
| 14 | Ví điện tử sử dụng thẻ thông minh | PIN, đổi PIN, giới hạn sai; mã người dùng, họ tên, mã thẻ, số dư mã hóa AES; **nạp tiền cần PIN + chữ ký số của thiết bị/phần mềm quản lý**; thanh toán kiểm số dư; lịch sử; **chống một giao dịch bị thực hiện nhiều lần** |
| 15 | Thẻ giao thông công cộng | Thông tin cá nhân; nạp tiền / gia hạn vé; ghi nhận lượt sử dụng |
| 16 | Thẻ quản lý xe điện & trạm đổi pin | Thông tin cá nhân; mượn/trả xe; nạp tiền, thanh toán cước; đổi pin tại trạm |

## 2. Nhận xét chung

- **Khung bắt buộc ai cũng làm** (theo đề mẫu của cô): PIN có giới hạn sai → lưu thông tin mã hóa AES → nạp/thanh toán có PIN + RSA → phần mềm máy tính có CSDL. Nhóm mình cũng phải có đủ, không bỏ được mục nào vì mỗi mục là 0.5–1 điểm.
- Điểm phân biệt giữa các nhóm nằm ở **chức năng nghiệp vụ riêng** (bỏ phiếu 1 lần, tích điểm, giờ giới nghiêm…) và **độ sâu bảo mật** (chống phát lại, xác thực hai chiều, nhật ký vòng).
- Nhóm 7 dự định web + Android + jCardSim, nhóm 8 nhận diện khuôn mặt: vượt khỏi công cụ cô dạy, rủi ro cao. Nhóm mình **giữ đúng công cụ của môn** (JCIDE + NetBeans/Swing + `javax.smartcardio`) và đầu tư vào phần thẻ.

## 3. Ý tưởng lấy về (đã đưa vào thiết kế)

| Ý tưởng | Lấy từ | Nhóm mình làm thế nào | Ở đâu |
|---|---|---|---|
| Mở khóa PIN bằng PUK / admin | 7 | PUK riêng (5 lần thử), **mở xong dữ liệu vẫn còn** nhờ DEK bọc 2 lần | F4, [03 mục 5.2](03-thiet-ke.md#52-khóa-dữ-liệu-aes-sinh-từ-pin-dek--kek) |
| Trường quan trọng chỉ admin sửa | 7 | Tách 2 đối tượng INFO (admin) và CONTACT (sinh viên), **thẻ tự chặn** chứ không chỉ chặn trên giao diện | F6, lệnh `40` |
| Bộ đếm chống lặp | 7, 14, 4 | `txCounter` trong thẻ + cột `bo_dem_cuoi` + `UNIQUE(ma_sv, bo_dem)` trong CSDL | F8, F9 |
| Bộ đệm vòng nhật ký trên EEPROM | 9 | 10 bản ghi × 12 byte, ghi trong giao dịch nguyên tử | lệnh `53`, [th3](th/th3-giao-dich.md) |
| Ký dữ liệu có timestamp + nonce chống phát lại | 9, 4 | Biên nhận ký `[loại, mã SV, giá trị, số dư, bộ đếm, thời gian, nonce]` | F8, F9 |
| Chữ ký số kiểm ở phía phần mềm | 14 | Phần mềm kiểm chữ ký bằng khóa công khai **trong CSDL** (không tin khóa thẻ tự đưa) | `CardRepository.recordReceipt` |
| Trạng thái khóa + lý do khóa | 13 | Thẻ: `STATE_LOCKED` trả `6400`; CSDL: `the.trang_thai` (có thể thêm cột `ly_do_khoa`) | F10 |
| Giao dịch nguyên tử | 4 | `beginTransaction/commitTransaction` quanh số dư + bộ đếm + nhật ký; quanh đổi PIN | [th3](th/th3-giao-dich.md) |

Ý tưởng có thể thêm nếu dư thời gian (không bắt buộc): nạp tiền cần **chữ ký của máy thu ngân** mà thẻ kiểm (nhóm 14) – tức xác thực hai chiều thật sự; hạn thẻ kiểm khi quẹt (nhóm 12).

## 4. Chỗ nhóm mình khác biệt (nói khi thuyết trình)

1. **Điểm danh chống điểm danh hộ** (F9): không nhóm nào có. Thẻ ký thời điểm + mã phòng + nonce, không cần PIN để quẹt nhanh nhưng vẫn phải có thẻ thật.
2. **Đổi PIN / quên PIN không mất dữ liệu**: đề mẫu nói "AES với khóa tạo từ PIN" – làm thẳng thì đổi PIN phải mã lại toàn bộ dữ liệu, quên PIN là mất dữ liệu. Nhóm mình dùng khóa dữ liệu ngẫu nhiên (DEK) bọc bằng khóa sinh từ PIN (KEK) và từ PUK.
3. **Biên nhận có chữ ký đối soát được trong CSDL**: sửa một byte số tiền là phần mềm từ chối (đã có kịch bản test).
4. **Phân quyền làm ở trên thẻ**, không chỉ ẩn nút trên giao diện.

## 5. Tránh trùng

- Không làm mượn/trả sách (nhóm 3) và ra vào KTX/giờ giới nghiêm (nhóm 7) thành chức năng chính.
- Ví của nhóm mình là "ví sinh viên trong trường" (căng tin, gửi xe, lệ phí) – khác ví đa năng của nhóm 14 ở chỗ gắn với định danh sinh viên và điểm danh.

## Nhật ký

| Ngày | Ai | Thay đổi |
|---|---|---|
| 07/11/2026 | Thu | Tổng hợp từ CSV xuất cùng ngày (13/16 nhóm đã có đề tài) |
| | | |
