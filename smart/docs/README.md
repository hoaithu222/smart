# Tài liệu môn Smart Card (Java Card), bản Markdown

Chuyển từ 14 file PDF ở thư mục cha (`../*.pdf`) ngày 03/11/2026. Mỗi file `.md` ghi tên PDF gốc ở dòng "Nguồn". Slide giữ đúng thứ tự (`## Slide N · tiêu đề`), tài liệu Word giữ đúng hệ thống mục của bản gốc. Hình nằm trong `images/<tên-file>/`; chữ, mã và thông số trong hình được chép lại ngay dưới hình. Chỗ bản gốc có lỗi hoặc tự mâu thuẫn được giữ nguyên và đánh dấu "Ghi chú chuyển đổi".

Đã đối chiếu: mọi từ trong lớp chữ của PDF đều có mặt trong bản `.md` với số lần không ít hơn bản gốc (trừ "1cm2" viết thành "1cm²" và tên biến bị ngắt dòng như `mast`/`erAID` đã nối lại); mã, AID, APDU và thông số cấu hình trong ảnh chụp được soi lại với ảnh gốc.

| Nhóm | File | Nội dung | Bản gốc |
|---|---|---|---|
| Lý thuyết | [bai1-tong-quan-the-thong-minh.md](bai1-tong-quan-the-thong-minh.md) | Bài giảng: Tổng quan về thẻ thông minh | 38 slide |
| Lý thuyết | [bai2-gioi-thieu-chung-ve-java-card.md](bai2-gioi-thieu-chung-ve-java-card.md) | Bài 2: Giới thiệu về công nghệ Java Card | 17 slide |
| Lý thuyết | [bai3-ngon-ngu-java-trong-java-card.md](bai3-ngon-ngu-java-trong-java-card.md) | Bài 3: Giới thiệu về ngôn ngữ Java trong Java Card | 11 slide |
| Lý thuyết | [bai4-java-card-api.md](bai4-java-card-api.md) | "Bài 3: Java Card API" (tên file gốc là bai4) | 24 slide |
| Thực hành | [th1-bai-tap.md](th1-bai-tap.md) | Bài tập bài thực hành số 1 | 1 trang |
| Thực hành | [th2-cac-doi-tuong-trong-java-card.md](th2-cac-doi-tuong-trong-java-card.md) | Bài thực hành số 2: Các đối tượng trong Java Card | 12 trang |
| Thực hành | [bai-tap-2.md](bai-tap-2.md) | Bài tập bài thực hành số 2 | 1 trang |
| Thực hành | [th3-thao-tac-nguyen-to-va-giao-dich.md](th3-thao-tac-nguyen-to-va-giao-dich.md) | Bài thực hành số 3: Thao tác nguyên tố và giao dịch | 8 trang |
| Thực hành | [th4-phuong-thuc-install.md](th4-phuong-thuc-install.md) | Bài thực hành số 4: Phương thức install | 10 trang |
| Thực hành | [th5-chia-se-doi-tuong-giua-cac-applet.md](th5-chia-se-doi-tuong-giua-cac-applet.md) | Bài thực hành số 5: Chia sẻ đối tượng giữa các Applet | 17 trang |
| Thực hành | [th6-mat-ma-trong-java-card.md](th6-mat-ma-trong-java-card.md) | Bài thực hành 6: Mật mã trong Java Card | 13 slide |
| Bổ sung | [apdu-mo-rong.md](apdu-mo-rong.md) | APDU mở rộng (`ExtendedLength`, `receiveBytes`) | 5 trang |
| Bổ sung | [huong-dan-ket-noi.md](huong-dan-ket-noi.md) | Hướng dẫn connect SmartCard sử dụng Java (JCIDE + NetBeans) | 6 trang |
| Đề tài | [yeu-cau-de-tai-2026.md](yeu-cau-de-tai-2026.md) | Ví dụ đề tài thẻ bệnh nhân, các mốc báo cáo 2026, yêu cầu báo cáo cuối kì | 4 trang |

Thư mục gốc không có file hướng dẫn của bài thực hành số 1 ("Tạo Applet máy tính bỏ túi"), chỉ có phiếu bài tập.

## Cấu hình môi trường nêu trong tài liệu

| Mục | Giá trị trong tài liệu | Ở đâu |
|---|---|---|
| Build trong JCIDE | `C:\JavaCardKit\JDK\bin\javac` với `-target 1.5 -source 1.5`; bộ chuyển đổi "Feitian Java Card 2.2.2 Class File Converter, Version 1.3"; thư viện `C:\JavaCardKit\SDK\Tools\JCIDE\lib\java_card_kit\2_2_2\lib\api.jar` | huong-dan-ket-noi, Bước 1 |
| Trình giả lập | Thanh công cụ JCIDE: `Simulator:Default`, `T=0 \| T=1` | huong-dan-ket-noi, Bước 1 |
| Tool → IDE Options → Run/Debug | JCVM Started by IDE: bật; Server IP 127.0.0.1; Used Port: tắt (CB 9025, Break 9550, CL 9035, Command 9551, Reader 5000); Enable PCSC Interface: bật | th2 Hình 2.2; huong-dan-ket-noi, Bước 6 |
| PyApduTool | Reader `JAVACOS Virtual Contact Reader 0`, Protocol T = 0, ATR `3B 70 13 00 00`; PackageAID `11 22 33 44 55 02`, Applet AID `11 22 33 44 55 02 01` | th2 Hình 2.3–2.5 |
| Tham số cài đặt applet | Debug → Run or Debug Configuration → Edit: Instance AID `44 49 45 4D 5F 54 48 49`, Application Specific Parameters `53 56 30 31 09` | th4 Hình 4.1–4.3 |
| Chương trình phía máy tính | NetBeans IDE 8.2 RC, project `Connect_Ex_GUI`, thư viện `javax.smartcardio`, `terminal.connect("T=0")` | huong-dan-ket-noi, Bước 2–4 |
| APDU của ví dụ kết nối | CLA của applet `B0`, INS in chuỗi `11`; AID_APPLET `99 88 77 66 55 00`; lệnh chọn `00 A4 04 00 06 99 88 77 66 55 00` → `90 00` | huong-dan-ket-noi, Bước 1, 4, 6 |
| APDU mở rộng | Có từ Java Card 2.2.2; applet `implements ExtendedLength` (`javacardx.apdu`); bộ đệm APDU tối thiểu 133 byte, thẻ JCOP đang dùng 261 byte | apdu-mo-rong |

## Chỗ lệch trong bản gốc (đã giữ nguyên, có ghi chú)

- bai4: trang bìa ghi "Bài 3: Java Card API" trong khi tên file là bai4; bài 3 thật là file bai3.
- bai1: slide 16 chữ trên thẻ trong ảnh mờ, chỉ chép gần đúng; slide 19 in "Itel 8051" (đúng là Intel 8051).
- th2: mục 2.2.1 có một gạch đầu dòng in "đối tượng tồn tại liên tục … không nguyên tử" trong phần đối tượng tạm thời, ngược với mục 2.1; Bảng 2.1 in kiểu `object []` thay cho `Object[]`.
- th3: Ví dụ 1 in `for(int i=0; i++; i<t.length)`, phần điều kiện và phần tăng biến bị đổi chỗ.
- th5: Kịch bản 2 khai báo lớp `slaveApp1` nhưng `install` gọi `new slaveApp()`.
- huong-dan-ket-noi: Bước 5 ghi `SmartCardWork.java`, còn Bước 2 và mọi ảnh chụp là `SmartCardWord.java`.
- Mã trong th3, th4, th5: các dòng bị Word ngắt vì tràn lề đã được nối lại; dòng `ISOException.throwIt(...)` sau `default:` được đặt lại đúng mức thụt lề.

## Notes bài tập lớn (Nhóm 1 – Thẻ sinh viên)

Đặc tả, thiết kế, cấu hình từng bước, lộ trình cập nhật theo từng bài thực hành và mã mẫu đã chạy thử: [notes/README.md](notes/README.md).
