# Notes bài tập lớn: Thẻ sinh viên sử dụng Smart Card

> Lớp L02 · **Nhóm 1**: Phạm Như Ngọc (CT070333) · Nguyễn Thị Phương Thảo (CT070351) · Vũ Thị Hoài Thu (CT070254)
> Bắt đầu: 07/11/2026. Tài liệu môn (bản Markdown) ở thư mục cha `../`; PDF gốc ở `../../`.
> Cách dùng: đọc theo thứ tự 01 → 04 một lần, sau đó **mỗi buổi thực hành mở đúng file trong `th/`**, làm phần "Việc cần làm", ghi kết quả vào "Nhật ký" cuối file và cập nhật bảng tiến độ dưới đây.

## Mục lục

| File | Nội dung | Khi nào đọc |
|---|---|---|
| [01-de-tai.md](01-de-tai.md) | Bài toán, tác nhân, 10 chức năng, đối chiếu thang điểm, **đoạn mô tả để dán vào Google Sheet** | Ngay bây giờ |
| [02-tham-khao-nhom-khac.md](02-tham-khao-nhom-khac.md) | 16 nhóm trong sheet L02 làm gì, học được gì, tránh trùng gì | Trước khi chốt chức năng |
| [03-thiet-ke.md](03-thiet-ke.md) | Bố cục bộ nhớ thẻ, bảng lệnh APDU, máy trạng thái, thiết kế bảo mật (PIN băm, AES, RSA), CSDL, kiến trúc phần mềm máy tính | Khi code |
| [04-moi-truong-cau-hinh.md](04-moi-truong-cau-hinh.md) | Từng bước cấu hình JCIDE, PyApduTool, NetBeans, CSDL, chạy thử; lỗi hay gặp | Trước buổi code đầu tiên |
| [th/](th/) | **Lộ trình cập nhật dần theo từng bài thực hành** (TH1 → TH6, APDU mở rộng, Kết nối) | Mỗi buổi thực hành |
| [05-on-tap-van-dap.md](05-on-tap-van-dap.md) | Ngân hàng câu hỏi vấn đáp kèm đáp án (mục này 4/10 điểm, trả lời kém là trượt) | Từ tuần thứ 2, ôn dần |
| [06-ap-dung-kinh-nghiem-phan-cong.md](06-ap-dung-kinh-nghiem-phan-cong.md) | Bê kinh nghiệm FE/hệ thống (CV) sang Java Swing + Java Card; phân công đề xuất | Khi chia việc |
| [code/](code/) | **Mã hoàn chỉnh**: applet `StudentCard.java` (JCIDE) + phần mềm Swing/FlatLaf 10 màn hình + SQL Server; `run-test.cmd` 49 kịch bản + 8 kiểm tra giao diện | Khi code / chạy demo |
| [images/giao-dien/](images/giao-dien/) | 14 ảnh màn hình phần mềm (chụp tự động trên thẻ giả lập) | Đưa vào báo cáo |

### Lộ trình theo bài thực hành (thư mục `th/`)

| Bước | File | Bài học áp vào đề tài | Ra được gì cho đề tài |
|---|---|---|---|
| 1 | [th/th1-applet-apdu.md](th/th1-applet-apdu.md) | TH1: Applet, `process`, CLA/INS | Khung applet, bảng lệnh, `GET_STATUS` |
| 2 | [th/th2-doi-tuong-ben-va-tam.md](th/th2-doi-tuong-ben-va-tam.md) | TH2 + Bài tập 2: đối tượng bền / tạm | PIN, đếm sai, khóa thẻ, phiên đăng nhập |
| 3 | [th/th3-giao-dich.md](th/th3-giao-dich.md) | TH3: nguyên tử, giao dịch | Ví: nạp/thanh toán, nhật ký vòng |
| 4 | [th/th4-install-khoi-tao-the.md](th/th4-install-khoi-tao-the.md) | TH4: `install`, tham số cài đặt | PIN quản trị, lệnh khởi tạo (phát hành) thẻ |
| 5 | [th/th5-chia-se-applet.md](th/th5-chia-se-applet.md) | TH5: Shareable | Quyết định 1 applet; phương án mở rộng |
| 6 | [th/th6-mat-ma.md](th/th6-mat-ma.md) | TH6: băm, AES, RSA | Băm PIN, AES khóa từ PIN, RSA xác thực + ký giao dịch |
| 7 | [th/th7-apdu-mo-rong-anh.md](th/th7-apdu-mo-rong-anh.md) | APDU mở rộng | Lưu ảnh nhiều khối |
| 8 | [th/th8-ket-noi-phan-mem-csdl.md](th/th8-ket-noi-phan-mem-csdl.md) | Hướng dẫn kết nối | Phần mềm Swing + CSDL, demo đủ luồng |

## Bảng tiến độ (cập nhật mỗi buổi)

Mốc lấy từ [yeu-cau-de-tai-2026.md](../yeu-cau-de-tai-2026.md).

| Mốc | Hạn | Yêu cầu tối thiểu của cô | Bước tương ứng | Trạng thái |
|---|---|---|---|---|
| Lần 1 | **30/11/2026** | Giao diện phần mềm; mô tả rõ chức năng; tạo/đổi/kiểm PIN, giới hạn nhập sai; lưu thông tin lên thẻ | 1, 2, 3, 4 + màn hình Swing (bước 8 phần giao diện) | ☐ |
| Lần 2 | **14/12/2026** | Lưu + mã hóa thông tin bằng AES; xác thực thẻ bằng RSA | 6, 7 | ☐ |
| Lần 3 | **28/12/2026** (dự kiến) | Phần mềm có CSDL; đủ chức năng; Word ≥ 30 trang + PowerPoint | 8 + báo cáo | ☐ |
| Nộp bản mềm | **2 ngày trước** buổi báo cáo | Project + Word + video demo + phân công + điểm dự kiến, đặt tên `L02_Nhóm 1_Thẻ sinh viên sử dụng Smart Card` | — | ☐ |

Đã chốt 08/11: **SQL Server**, giao diện **FlatLaf**, thiết kế khóa DEK/KEK, báo cáo trên **giả lập JCIDE** ([01 mục 7](01-de-tai.md#7-các-điểm-đã-chốt)).

Đã có (08/11): applet đầy đủ + phần mềm máy tính 10 màn hình + CSDL tự tạo, kiểm trên thẻ giả lập + SQLite ([code/README.md](code/README.md)). **Chưa chạy trên JCIDE và SQL Server thật.**

Việc ngay tuần này (08–14/11):

- [ ] Dán đoạn mô tả ở [01 mục 8](01-de-tai.md#8-đoạn-mô-tả-đề-tài-để-dán-vào-google-sheet) vào ô "Mô tả đề tài" của sheet.
- [ ] Mỗi người: `code\host\tai-thu-vien.cmd` → `code\run-test.cmd` ra 49/49 + 8/8 → mở phần mềm ở chế độ thử ([04 mục 4.4](04-moi-truong-cau-hinh.md#44-chạy-nhanh-không-cần-netbeans--jcide--sql-server)).
- [ ] Cài SQL Server theo [04 mục 5](04-moi-truong-cau-hinh.md#5-cài-và-cấu-hình-sql-server), tạo project NetBeans theo [04 mục 4](04-moi-truong-cau-hinh.md#4-cấu-hình-project-netbeans).
- [ ] Đưa `StudentCard.java` vào JCIDE, chạy demo thật theo [04 mục 7](04-moi-truong-cau-hinh.md#7-chạy-demo-đúng-chuẩn-báo-cáo-jcide--sql-server), ghi kết quả vào nhật ký 04.
- [ ] Mỗi người đọc các file `th/` của phần mình – vấn đáp hỏi từng dòng mã.

## Nhật ký cập nhật notes

| Ngày | Ai | Cập nhật |
|---|---|---|
| 07/11/2026 | Thu (+ Claude) | Tạo bộ notes: đặc tả, tham khảo 16 nhóm, thiết kế, cấu hình, lộ trình 8 bước, ôn vấn đáp; mã mẫu chạy qua 43 kịch bản trên jCardSim 2.2.2 |
| 08/11/2026 | Thu (+ Claude) | Chốt SQL Server / FlatLaf / DEK-KEK / giả lập. Viết xong phần mềm Swing 10 màn hình + CSDL SQL Server tự tạo + chế độ thẻ giả lập; `run-test.cmd` 49/49 + 8/8; 14 ảnh giao diện; 04 viết lại (SQL Server, NetBeans, `app.properties`) |
| | | |
