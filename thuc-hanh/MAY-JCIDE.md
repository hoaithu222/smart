# Chạy bài thực hành trên máy đã có JCIDE

Dùng khi chạy bài trên một máy khác đã có JCIDE. **Chưa bước nào ở đây được thử trên JCIDE thật**; chế độ PC/SC của công cụ mới được thử với đầu đọc giả lập.

## 1. Trước khi đi (trên máy mình)

```powershell
D:\sbc\smart\thuc-hanh\dong-goi.cmd
```

Chép file `thuc-hanh\build\thuc-hanh-may-jcide.zip` vào USB hoặc Drive.

## 2. Trên máy có JCIDE

1. Giải nén vào đường dẫn không dấu, không khoảng trắng, ví dụ `C:\bai`.
2. Mở PowerShell:

```powershell
cd C:\bai\thuc-hanh
```

3. Công cụ cần JDK 7 trở lên (khuyên 8). Nó tự tìm trong PATH, `JAVA_HOME`, rồi `C:\JavaCardKit\JDK`. Báo "Khong thay javac" thì máy đó cần cài JDK.
4. Thử công cụ bằng thẻ giả lập trước (không cần JCIDE). Dòng "Chuyển đổi CAP" sẽ ghi BỎ QUA, đó là bình thường:

```powershell
.\kiem-tra.cmd
```

## 3. Cấu hình JCIDE

`Tool → IDE Options → Run/Debug → Configuration`: đánh dấu **JCVM Started by IDE** và **Enable PCSC Interface**; Server IP `127.0.0.1`; không đánh dấu **Used Port**.

Tạo project `ThucHanhJavaCard`. Với mỗi bài: tạo package đúng tên, đặt Package AID và Applet AID theo [README mục 5](README.md#5-bảng-aid-và-tham-số-cài-đặt), chép mã từ `src\<gói>\`.
Chỉ làm `cnt` trước để chắc đường truyền chạy, rồi mới làm các bài khác.

## 4. Chạy từng bài

Cho mỗi dòng: Build và Run trong JCIDE, rồi chạy lệnh. Công cụ liệt kê applet cần cài và **chờ bạn bấm Enter**; bài có nhiều phần sẽ dừng lại nhiều lần để cài lại.
Mỗi lần Run phải là cài mới (thẻ trắng).

| Thứ tự | Cài trong JCIDE | Lệnh | Mong đợi | Kết quả của mình |
|---|---|---|---|---|
| 1 | `cnt` | `.\chay.cmd --pcsc=JAVACOS kich-ban\cnt.apdu` | 4 đạt, 0 sai | |
| 2 | `cnt` | `.\chay-ket-noi.cmd pcsc` | Connect → Show Text → `HELLO` | |
| 3 | 5 applet `bai1_APDU` | `.\chay.cmd --pcsc=JAVACOS kich-ban\th1.apdu` | 31 đạt | |
| 4 | `bai2_Object`: phần đầu Applet1, 2; phần sau Applet2 (tham số `36 36 36 36 36 36`) và Applet3 | `.\chay.cmd --pcsc=JAVACOS kich-ban\th2.apdu` | 22 đạt | |
| 5 | 3 applet `bai3_Transaction` | `.\chay.cmd --pcsc=JAVACOS kich-ban\th3.apdu` | 12 đạt | |
| 6 | `bai4_InstallMethod`, mỗi phần một applet theo hướng dẫn trên màn hình | `.\chay.cmd --pcsc=JAVACOS kich-ban\th4.apdu` | 25 đạt | |
| 7 | `masterApp` rồi 5 applet `slavePack`; phần cuối chỉ cài `slaveApp` | `.\chay.cmd --pcsc=JAVACOS kich-ban\th5.apdu` | 6 đạt | |
| 7b | `chiaSeAnToan`, `khachAnToan`, `khachLa` theo hướng dẫn trên màn hình (gói chủ trước) | `.\chay.cmd --pcsc=JAVACOS kich-ban\th5-an-toan.apdu` | 11 đạt | |
| 8 | 3 applet `bai6_Crypto` | `.\chay.cmd --pcsc=JAVACOS kich-ban\th6.apdu` | 13 đạt | |
| 9 | Cài mới lại 3 applet `bai6_Crypto` | `.\kiem-mat-ma.cmd --pcsc` | 0 sai | |
| 10 | `bai7_APDU_Extended`, chọn **T = 1** | `.\chay.cmd --pcsc=JAVACOS kich-ban\th7.apdu` | 5 đạt | |

**Nên chú ý** các dòng 5, 7 và 10: giao dịch, tường lửa applet và APDU mở rộng là phần thẻ giả lập không chứng minh được. Kết quả khác mong đợi thì chép nguyên màn hình về.

Thẻ ảo của JCIDE có thể không hỗ trợ một số thuật toán TH6 (khi đó lệnh trả `6A 81`) hoặc APDU mở rộng; ghi lại nếu gặp.

## 5. Lỗi hay gặp

| Hiện tượng | Cách xử lý |
|---|---|
| Không có đầu đọc PC/SC | Đã Run chưa, đã đánh dấu Enable PCSC Interface chưa; kiểm `Get-Service SCardSvr` |
| Không kết nối được thẻ | **Disconnect PyApduTool** (nó giữ thẻ độc quyền) |
| `6A 82` khi chọn applet | Chưa cài applet đó, hoặc sai AID / Instance AID |
| Nhiều dòng sai sau một phần đúng | Chưa cài mới lại trước phần đó |
| Build báo lỗi | Kiểm đã chép đúng file, đúng tên package; `slavePack` cần build `masterPack` trước |
