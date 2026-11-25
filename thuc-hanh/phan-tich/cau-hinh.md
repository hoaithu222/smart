# Cấu hình các bài thực hành: kiểm tra và hướng dẫn

> Kiểm ngày 25/11/2026 trên thẻ giả lập và bộ chuyển đổi của Oracle. **Chưa kiểm trên JCIDE.**

## 1. Kết quả kiểm

| Hạng mục | Kết quả |
|---|---|
| `.\kiem-tra.cmd`: 16 kịch bản | 308 đạt, 0 sai, 4 mục chỉ kiểm được trên JCIDE |
| Mật mã TH6 so với thư viện Java | 27/27 |
| Chương trình kết nối | đạt |
| Chuyển đổi CAP, đích Java Card 3.0.4 | 12/12 gói, 0 lỗi, 0 cảnh báo |
| Biên dịch kiểu JCIDE (`-source 1.5 -encoding windows-1252`) | 33/33 lớp |
| Quy tắc AID trong `goi.cfg` | 12 gói, 28 applet: dài 5–16 byte, không trùng, RID applet khớp RID gói |
| AID đối chiếu tài liệu | TH2, TH4, TH5, Kết nối khớp |

## 2. Công cụ cần có

| Công cụ | Dùng cho | Ghi chú |
|---|---|---|
| JDK 8 (tối thiểu 7) | mọi lệnh `.cmd` | `javac` trong PATH hoặc `JAVA_HOME` |
| jCardSim 2.2.2 | thẻ giả lập | `code\host\lib` |
| Java Card Development Kit Tools v26.0 | tạo CAP | chỉ tạo CAP từ 3.0.4; môn học dùng 2.2.2 |
| JCIDE | môi trường của môn | build bằng bộ chuyển đổi 2.2.2 |
| PyApduTool | gửi APDU tay | đi kèm JavaCardKit |
| NetBeans | mở chương trình kết nối | không bắt buộc |

## 3. Cấu hình JCIDE dùng chung

| Mục | Giá trị |
|---|---|
| `Tool → IDE Options → Run/Debug` | JCVM Started by IDE ✔, Server IP `127.0.0.1`, Used Port ✘, Enable PCSC Interface ✔ |
| Đầu đọc ảo | `JAVACOS Virtual Contact Reader 0`, ATR `3B 70 13 00 00` |
| Giao thức | T = 0; riêng APDU mở rộng chọn T = 1 |
| Mã nguồn | không dấu (JCIDE đọc `windows-1252`) |

## 4. Bảng cấu hình theo bài

| Bài | Package (AID) | Applet (AID) | Tham số / Instance AID | Cần cài trước |
|---|---|---|---|---|
| TH1 | `bai1_APDU` `…01` | 5 applet `…01 01–05` | | |
| TH2 | `bai2_Object` `…02` | 3 applet `…02 01–03` | Applet2: PIN tùy chọn 4–8 byte | |
| TH3 | `bai3_Transaction` `…03` | 3 applet `…03 01–03` | | |
| TH4 | `bai4_InstallMethod` `…04` | 3 applet `…04 01–03` | Applet2, 3: instance `44 49 45 4D 5F 54 48 49`, tham số `53 56 30 31 09` | |
| TH5 | `masterPack` `…05 00` | `…05 00 00` | | |
| TH5 | `slavePack` `…05 01` | `…05 01 01–05` | | `masterPack` |
| TH5 mở rộng | `chiaSeAnToan` `…05 02` | `…05 02 01` | khóa 16 byte tùy chọn | |
| TH5 mở rộng | `khachAnToan` `…05 03` | `…05 03 01` | cùng khóa | `chiaSeAnToan` |
| TH5 mở rộng | `khachLa` `…05 04` | `…05 04 01` | | `chiaSeAnToan` |
| TH6 | `bai6_Crypto` `…06` | `…06 01–03` | | |
| APDU mở rộng | `bai7_APDU_Extended` `…07` | `…07 01` | T = 1 | |
| Kết nối | `cnt` `99 88 77 66 55` | `99 88 77 66 55 00` | | |

`…` = `11 22 33 44 55`.

## 5. Chỗ dễ cấu hình sai

| Sai | Hậu quả | Cách đúng |
|---|---|---|
| Quên đổi AID trong JCIDE | `6A 82` khi chọn | đặt đúng bảng trên |
| Cài khách trước chủ (TH5) | cài lỗi hoặc `6A 82` | build và cài gói chủ trước |
| Cài Applet2 và Applet3 (TH4) cùng lúc | trùng Instance AID | cài từng cái |
| Run lại trong JCIDE | thẻ về trắng, mất dữ liệu | chạy lại từ đầu kịch bản |
| PyApduTool chưa Disconnect | chương trình khác không nối được | Disconnect |
| Chạy APDU mở rộng với T = 0 | lỗi truyền | chọn T = 1 |
| Chép mã có dấu vào JCIDE | cảnh báo hoặc lỗi mã hóa | giữ mã không dấu |

## 6. Thẻ giả lập khác thẻ thật

| Hành vi | Trong công cụ | Trên thẻ thật |
|---|---|---|
| Xóa mảng tạm khi bỏ chọn / reset | công cụ bổ sung | JCRE |
| Hoàn tác giao dịch | công cụ bổ sung | JCRE |
| AID người gọi khi chia sẻ | công cụ bổ sung | JCRE |
| Tường lửa chặn truy cập đối tượng | **không có** | có |
| Dữ liệu > 127 byte với APDU mở rộng | sai | đúng nếu thẻ hỗ trợ |
| Giải mã RSA | trả đủ 117 byte | trả đúng độ dài |
| Lỗi cài đặt | mã chung | tùy thẻ |

Vì vậy các bài TH2, TH3, TH5 và APDU mở rộng **cần chạy lại trên JCIDE** trước khi báo cáo.
