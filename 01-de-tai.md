# 01 · Đặc tả đề tài: Thẻ sinh viên sử dụng Smart Card

> Nhóm 1 · L02 · Dựa trên đề mẫu "thẻ bệnh nhân" và thang điểm trong [yeu-cau-de-tai-2026.md](../yeu-cau-de-tai-2026.md).
> Đây là bản nháp **Chương 1 – Phân tích thiết kế** của báo cáo: cô yêu cầu "trước khi phân tích thiết kế phải nêu rõ được bài toán", nên mục 1 viết sao để chép thẳng vào Word được.

## 1. Bài toán

Mỗi sinh viên của trường có một **thẻ sinh viên dạng thẻ thông minh (Java Card)**. Thẻ vừa là giấy tờ định danh, vừa là ví điện tử dùng trong trường, vừa là "chìa khóa" để điểm danh / ra vào.

Vấn đề cần giải quyết:

1. **Thẻ giấy/thẻ từ dễ làm giả, dễ đọc trộm**: ai cầm thẻ cũng xem được thông tin. → Thông tin lưu trên chip, **mã hóa AES**, chỉ đọc được khi chủ thẻ nhập **PIN**.
2. **Thẻ giả, thẻ nhân bản**: phần mềm phải phân biệt được thẻ thật do trường phát hành. → Mỗi thẻ tự sinh **cặp khóa RSA**, khóa bí mật không bao giờ rời chip; phần mềm kiểm bằng khóa công khai lưu trong CSDL của trường.
3. **Thanh toán trong trường** (căng tin, gửi xe, photo, lệ phí) cần nhanh nhưng an toàn: mỗi giao dịch phải **nhập PIN**, thẻ **ký biên nhận**, số dư và nhật ký trên thẻ phải **nhất quán** kể cả khi rút thẻ giữa chừng.
4. **Điểm danh hộ**: dùng mã sinh viên thì ai cũng điểm danh hộ được. → Điểm danh bằng quẹt thẻ, thẻ ký thời điểm, phải có thẻ thật mới tạo được chữ ký.
5. **Mất thẻ / quên PIN**: phòng Đào tạo khóa thẻ từ xa khi báo mất; sinh viên quên PIN thì mở bằng mã **PUK** mà **không mất dữ liệu**.

## 2. Tác nhân

| Tác nhân | Dùng phần mềm nào | Quyền |
|---|---|---|
| Sinh viên (chủ thẻ) | Màn hình sinh viên (kiosk) | Xem thông tin, đổi PIN, mở khóa PIN bằng PUK, sửa thông tin liên lạc, xem số dư/lịch sử |
| Phòng Đào tạo / CTSV (quản trị) | Màn hình quản trị | Phát hành thẻ, sửa thông tin học vụ + ảnh, khóa/mở khóa thẻ, quản lý CSDL, thống kê |
| Điểm thu tiền (căng tin, nhà xe, thu lệ phí) | Màn hình thu ngân | Nạp tiền, thanh toán (sinh viên nhập PIN trên máy) |
| Giảng viên / cổng | Màn hình điểm danh | Quẹt thẻ điểm danh theo phòng/môn |

Trong demo chỉ có **một ứng dụng Java Swing** với 4 nhóm màn hình trên; đăng nhập vai trò bằng tài khoản trong CSDL (bảng `nguoi_dung`).

## 3. Chức năng

Ký hiệu cột: **PIN** = thẻ đòi PIN sinh viên; **QT** = thẻ đòi PIN quản trị; **RSA** = có dùng khóa công khai.

| Mã | Chức năng | Trên thẻ (lệnh APDU) | PIN | QT | RSA | Mốc |
|---|---|---|---|---|---|---|
| F1 | **Phát hành (khởi tạo) thẻ**: ghi mã SV, PIN ban đầu, PUK, sinh cặp khóa RSA trên thẻ, lưu khóa công khai vào CSDL, ghi thông tin + ảnh | `INIT_CARD 30`, `GET_PUBKEY 31`, `WRITE_OBJ 40` | | ✔ | ✔ | Lần 1 (bản đơn giản) → Lần 2 |
| F2 | **Xác thực PIN**: sai 3 lần khóa PIN; thẻ trả số lần còn lại `63 Cx` | `VERIFY_PIN 21` | | | | Lần 1 |
| F3 | **Đổi PIN**: PIN lưu dạng **băm SHA-256 + salt**; đổi PIN không làm mất dữ liệu đã mã hóa | `CHANGE_PIN 22` | ✔ | | | Lần 1 (băm: Lần 2) |
| F4 | **Mở khóa PIN bằng PUK** (quên PIN): sai PUK 5 lần → khóa hẳn thẻ | `UNBLOCK_PIN 23` | | | | Lần 2 |
| F5 | **Xem thông tin + ảnh**: thẻ giải mã AES sau khi nhập PIN | `READ_OBJ 41` | ✔ | | | Lần 1 (rõ) → Lần 2 (mã hóa) |
| F6 | **Cập nhật thông tin**: sinh viên tự sửa quê quán/SĐT/email; thông tin học vụ (lớp, khoa, hạn thẻ, ảnh) chỉ quản trị sửa | `WRITE_OBJ 40` | ✔ | ✔ (học vụ) | | Lần 1 → Lần 2 |
| F7 | **Xác thực thẻ thật** (challenge–response RSA) mỗi lần cắm thẻ | `AUTH_CARD 32` | | | ✔ | Lần 2 |
| F8 | **Ví sinh viên**: nạp tiền, thanh toán dịch vụ, xem số dư, lịch sử; mỗi giao dịch nhập PIN + thẻ ký biên nhận; bộ đếm chống lặp; giao dịch nguyên tử | `TOPUP 51`, `PAY 52`, `GET_BALANCE 50`, `GET_LOG 53` | ✔ | | ✔ | Lần 3 (lõi ví có thể làm từ bước 3) |
| F9 | **Điểm danh / ra vào** bằng quẹt thẻ: không cần PIN, thẻ ký thời điểm → chống điểm danh hộ | `CHECK_IN 60` | | | ✔ | Lần 3 (điểm nhấn riêng) |
| F10 | **Khóa / mở khóa thẻ** khi báo mất; thẻ khóa trả `64 00` ngay lúc kết nối | `SET_LOCK 33` | | ✔ | | Lần 3 |
| F11 | **Phần mềm + CSDL**: quản lý sinh viên, khóa công khai từng thẻ, lịch sử giao dịch/điểm danh, thống kê | (máy tính) | | | | Lần 3 |

## 4. Thông tin lưu trên thẻ

| Nhóm | Trường | Ai ghi | Lưu thế nào |
|---|---|---|---|
| Định danh công khai | Mã sinh viên (vd `CT070254`) | Quản trị lúc phát hành | **Rõ** – vì in sẵn trên mặt thẻ và phần mềm cần nó để tra khóa công khai trong CSDL *trước khi* có PIN |
| INFO (học vụ) | Họ tên, ngày sinh, giới tính, lớp, khoa/ngành, khóa học, hạn thẻ | Quản trị | AES-128 (CBC) |
| CONTACT | Quê quán, SĐT, email | Sinh viên | AES-128 (CBC) |
| PHOTO | Ảnh chân dung JPEG ≤ 7,5 KB | Quản trị | AES-128 (CBC), chia khối 240 byte |
| Bảo mật | Băm PIN, băm PUK, băm PIN quản trị, salt, khóa dữ liệu đã bọc, cặp khóa RSA | Thẻ tự sinh | Không đọc ra được |
| Ví | Số dư (nghìn đồng), bộ đếm giao dịch, nhật ký 10 bản ghi | Thẻ | Bền, cập nhật trong giao dịch nguyên tử |

Trường thông tin trùng khớp với phiếu bài tập TH1 Bài 4 và Bài tập 2 ("mã SV, họ tên, ngày sinh, quê quán") – có thể kể lại trong báo cáo là phát triển tiếp từ bài thực hành.

## 5. Đối chiếu thang điểm (5 điểm phần Project)

| Tiêu chí của cô | Điểm | Đề tài đáp ứng bằng | Bằng chứng khi demo |
|---|---|---|---|
| Giao diện đẹp | 0.5 | Swing + FlatLaf: 10 màn hình theo vai trò, sáng/tối, thông báo lỗi tiếng Việt theo SW ([ảnh](images/giao-dien/)) | Quay video từng màn |
| Khởi tạo thẻ | 0.5 | F1 | Thẻ trắng → `GET_STATUS` ra `01` + mã SV |
| Thay đổi thông tin cá nhân | 0.5 | F6 | Sửa quê quán, rút thẻ, cắm lại vẫn đúng |
| Mã hóa được thông tin | **1** | AES-128 CBC trên thẻ, khóa sinh từ PIN | Mục "soi EEPROM" của bộ test / giải thích sơ đồ DEK–KEK |
| Thay đổi mã PIN, mã hóa mã PIN | 0.5 | F3, PIN lưu dạng SHA-256(salt‖PIN) | Đổi PIN xong dữ liệu cũ vẫn đọc được |
| Tạo được cặp khóa RSA | 0.5 | `KeyPair.genKeyPair()` trên thẻ, 1024 bit | Hiện modulus/exponent, lưu vào CSDL |
| Xác thực được theo chức năng | 0.5 | F7 + chữ ký biên nhận F8/F9 | Sửa 1 byte biên nhận → phần mềm báo chữ ký sai |
| Có CSDL | 0.5 | F11 – SQL Server, phần mềm tự tạo database + bảng | Mở bảng `giao_dich` trong SSMS sau khi demo |
| Có demo chức năng | 0.5 | Kịch bản demo ở [th/th8](th/th8-ket-noi-phan-mem-csdl.md#6-kịch-bản-demo-quay-video) | Video |

Phần **Trả lời câu hỏi = 4 điểm**, kém là trượt luôn → ôn theo [05-on-tap-van-dap.md](05-on-tap-van-dap.md).

## 6. Ngoài phạm vi (nói rõ trong báo cáo để không bị hỏi vặn)

- Không làm thẻ không tiếp xúc/NFC, không làm ứng dụng điện thoại: công cụ của môn là JCIDE + NetBeans.
- Không đồng bộ thời gian thật trên thẻ: thẻ không có đồng hồ, thời gian do máy tính gửi xuống và được đưa vào chữ ký.
- Mượn trả sách (nhóm 3), ký túc xá (nhóm 7) không làm sâu để tránh trùng; chỉ nêu là hướng mở rộng.
- Số tiền tính theo **nghìn đồng**, kiểu `short` (trần 30 triệu) vì Java Card 2.2.2 không đảm bảo có kiểu `int` (bài 3).

## 7. Các điểm đã chốt

| # | Câu hỏi | Chốt ngày 08/11/2026 | Ảnh hưởng |
|---|---|---|---|
| 1 | CSDL loại nào? | **SQL Server** | Driver `mssql-jdbc` bản jre8; cấu hình ở [04 mục 5](04-moi-truong-cau-hinh.md#5-cài-và-cấu-hình-sql-server) |
| 2 | Dùng FlatLaf cho giao diện đẹp? | **Có** (vẫn là Java Swing) | `flatlaf-3.5.4.jar`, nút sáng/tối trong phần mềm |
| 3 | DEK ngẫu nhiên bọc bằng KEK sinh từ PIN? | **Được** | Giữ thiết kế [03 mục 5.2](03-thiet-ke.md#52-khóa-dữ-liệu-aes-sinh-từ-pin-dek--kek) |
| 4 | Báo cáo trên giả lập hay thẻ thật? | **Giả lập** (JCIDE) | RSA-1024, ảnh ≤ 7,6 KB vẫn giữ; phần mềm còn có chế độ `jcardsim` để tự thử |

## 8. Đoạn "Mô tả đề tài" để dán vào Google Sheet

Ô F6 của nhóm 1 đang trống. Dán đoạn dưới (đã viết cùng kiểu với nhóm 7, nhóm 4):

```text
- Xác thực chủ thẻ bằng mã PIN: đổi được PIN, nhập sai 3 lần thì khóa PIN, mở khóa bằng mã PUK mà không mất dữ liệu; PIN lưu trên thẻ dưới dạng băm SHA-256 có salt.
- Lưu thông tin sinh viên trên thẻ (mã SV, họ tên, ngày sinh, giới tính, quê quán, lớp, khoa/ngành, khóa học, hạn thẻ, SĐT, ảnh); thông tin được mã hóa AES-128 với khóa sinh từ mã PIN. Sinh viên tự sửa thông tin liên lạc, thông tin học vụ và ảnh chỉ phòng Đào tạo sửa.
- Xác thực thẻ thật bằng RSA: cặp khóa sinh ngay trên thẻ, khóa bí mật không rời thẻ, khóa công khai lưu trong CSDL; phần mềm gửi chuỗi ngẫu nhiên, thẻ ký, phần mềm kiểm chữ ký.
- Ví sinh viên: nạp tiền, thanh toán căng tin / gửi xe / lệ phí, xem số dư và lịch sử. Mỗi giao dịch phải nhập PIN, thẻ ký biên nhận bằng RSA, có bộ đếm chống lặp; số dư và nhật ký cập nhật bằng giao dịch nguyên tử.
- Điểm danh / ra vào bằng quẹt thẻ: thẻ ký thời điểm điểm danh để chống điểm danh hộ, lưu 10 lượt gần nhất trên thẻ.
- Phòng Đào tạo phát hành thẻ, khóa / mở khóa thẻ khi sinh viên báo mất.
- Phần mềm Java Swing (NetBeans, javax.smartcardio) kết nối CSDL SQL Server: quản lý sinh viên, khóa công khai của từng thẻ, lịch sử giao dịch và điểm danh, thống kê; phân quyền phòng Đào tạo, thu ngân, giảng viên, sinh viên.
```

> Sheet là file chung của lớp; Claude không ghi trực tiếp vào đó. Một bạn trong nhóm dán đoạn trên vào ô "Mô tả đề tài" của dòng Nhóm 1.

## Nhật ký

| Ngày | Ai | Thay đổi |
|---|---|---|
| 07/11/2026 | Thu | Bản đầu, chờ nhóm chốt + hỏi cô mục 7 |
| 08/11/2026 | Thu | Chốt 4 điểm ở mục 7: SQL Server, FlatLaf, DEK/KEK, báo cáo trên giả lập |
| | | |
