# Thư mục bài thực hành Java Card (TH1 – TH6, APDU mở rộng, Kết nối)

Mã viết theo tài liệu của cô trong [smart/docs](../smart/docs/README.md), giữ đúng tên gói, AID và lệnh APDU ở chỗ tài liệu có nêu.
Mọi bài chạy được ngay trên **thẻ giả lập**, không cần JCIDE.

**Học và ôn:** [phân tích chuyên sâu từng buổi](phan-tich/README.md) – kiến thức, phân tích mã, luồng dữ liệu, cấu hình, toàn bộ câu hỏi.

## 1. Chạy nhanh

Mở PowerShell:

```powershell
cd D:\sbc\smart\thuc-hanh
```

```powershell
.\kiem-tra.cmd
```

Kết quả đúng ở cuối màn hình (đã chạy ngày 25/11/2026):

```text
16 kịch bản: 308 đạt, 0 sai, 4 chỉ kiểm được trên JCIDE / thẻ thật
Mật mã TH6: 27 đạt, 0 sai
Kịch bản APDU    : ĐẠT
Mật mã TH6       : ĐẠT
Chương trình PC  : ĐẠT
Chuyển đổi CAP   : ĐẠT
```

| Lệnh | Việc làm |
|---|---|
| `.\kiem-tra.cmd` | Biên dịch, chạy mọi kịch bản, đối chiếu mật mã, thử chương trình kết nối, tạo file CAP |
| `.\chay.cmd bai2_Object.Applet2` | Cài, chọn applet rồi mở shell gõ APDU (giống Debug Shell) |
| `.\chay.cmd kich-ban\th3.apdu` | Chạy một bài, so kết quả từng lệnh |
| `.\chay.cmd` | Shell với thẻ trắng (`/danhsach`, `/cai`, `/help`) |
| `.\chay-ket-noi.cmd` | Mở chương trình giao diện của bài Kết nối |
| `.\build-cap.cmd` | Chỉ tạo file CAP vào `build\cap\` |
| `.\kiem-mat-ma.cmd [--pcsc]` | Đối chiếu mật mã TH6 trên thẻ giả lập hoặc JCIDE |
| `.\dong-goi.cmd` | Nén thành `build\thuc-hanh-may-jcide.zip` để mang sang [máy có JCIDE](MAY-JCIDE.md) |

### Trong shell

```text
00 01 05 03 02                          gửi APDU
00 01 00 00 LC "CT01|An|1/1/2004|Hue"   chuỗi trong ngoặc kép, LC tự tính độ dài
00 00 00 00 0D => "Xin chao, KMA" 90 00 gửi và kiểm kết quả
/select bai1_APDU.Applet1    /deselect    /reset    /moi    /thoat
```

## 2. Cấu trúc

```text
thuc-hanh\
  src\<gói>\*.java            mã applet (chép vào JCIDE)
  kich-ban\*.apdu             lệnh + kết quả mong đợi của từng bài
  kich-ban\thi-nghiem\        thí nghiệm thêm: lệnh sai, giới hạn, tham số lỗi
  phan-tich\                  phân tích chuyên sâu từng buổi, luồng dữ liệu, câu hỏi
  Connect_Ex_GUI\src\         chương trình Java trên máy tính (bài Kết nối)
  cong-cu\                    công cụ: build, thẻ giả lập, shell, kiểm mật mã
  goi.cfg                     Package AID, Applet AID, tham số cài đặt
  TH4-cau-hoi-install.md      câu hỏi và trả lời bài 4
  MAY-JCIDE.md                phiếu chạy bài trên máy đã có JCIDE
  build\                      sinh ra khi chạy (không đưa vào git)
```

## 3. Yêu cầu cài đặt

| Thành phần | Dùng cho |
|---|---|
| JDK 8 (tối thiểu JDK 7), có `javac` trong PATH hoặc đặt `JAVA_HOME` | Tất cả |
| jCardSim 2.2.2 (`..\code\host\lib\`) | Thẻ giả lập |
| Java Card Development Kit Tools v26.0 của Oracle (thư mục `java_card_devkit_tools-…` cạnh `thuc-hanh`), chạy được bằng JDK 8 | File CAP, đích **3.0.4** |
| JCIDE (JavaCardKit) | Thẻ ảo PC/SC, nộp bài đúng môi trường cô dùng |
| NetBeans (không bắt buộc) | Mở `Connect_Ex_GUI` bằng IDE |

Lưu ý về file CAP: môn học dùng **Java Card 2.2.2**, còn bộ công cụ v26.0 chỉ tạo được CAP từ 3.0.4 trở lên.
Bước này để **kiểm mã theo luật Java Card** (không `int`, đúng API, 0 lỗi 0 cảnh báo). CAP nạp lên thẻ ảo phải build lại trong JCIDE.

Dùng bộ công cụ ở chỗ khác thì đặt biến `JC_TOOLS` trỏ tới thư mục đó.

## 4. Cài và chạy trên JCIDE

Các bước dưới lấy theo tài liệu của cô; **chưa được chạy thử với bộ công cụ này**.

> Dùng máy đã có sẵn JCIDE thì làm theo phiếu [MAY-JCIDE.md](MAY-JCIDE.md) và bỏ qua mục 4.1.

### 4.1 Cài đặt

1. Lấy bộ cài JCKit (gồm JCIDE) từ cô hoặc nguồn tin cậy. Nên so mã `Get-FileHash <file>` với người gửi.
2. Chuột phải vào file → **Run as administrator**.
3. Nếu Windows chặn, **đọc đúng thông báo** vì mỗi loại xử lý khác nhau:

   | Thấy gì | Là gì | Làm gì |
   |---|---|---|
   | Nhắc tới *Smart App Control* | Chặn ứng dụng không chữ ký số; [Microsoft cho biết](https://support.microsoft.com/en-us/windows/smart-app-control-faq-285ea03d-fa88-4d56-882e-6698afdb7003) không thể cho phép riêng một ứng dụng | Ưu tiên dùng máy đã có JCIDE hoặc máy ảo. Tự tắt Smart App Control thì Windows không còn chặn **mọi** ứng dụng lạ không chữ ký |
   | Khung xanh *Windows protected your PC*, có chữ *More info* | SmartScreen (file tải từ mạng) | So mã băm với người gửi; khớp thì *More info → Run anyway* |
   | *Threats found*, file bị đưa đi cách ly | Phần mềm diệt virus phát hiện mã độc | **Không chạy, không cho phép.** Xin người gửi bản khác |
   | Thông báo khác | Chưa xác định | Chụp màn hình |

4. Chọn thư mục cài `C:\JavaCardKit` (đúng đường dẫn trong tài liệu của cô). Đọc điều khoản; nếu được hỏi cài driver đầu đọc ảo thì xem tên nhà phát hành trước khi đồng ý.

Cài xong, cả ba lệnh nên trả `True` (đường dẫn theo tài liệu của cô, có thể khác ở phiên bản khác):

```powershell
Test-Path C:\JavaCardKit\SDK\Tools\JCIDE\JCIDE.exe
```

```powershell
Test-Path C:\JavaCardKit\JDK\bin\javac.exe
```

```powershell
Test-Path C:\JavaCardKit\SDK\Tools\JCIDE\lib\java_card_kit\2_2_2\lib\api.jar
```

### 4.2 Cấu hình JCIDE (th2 Hình 2.2)

`Tool → IDE Options → Run/Debug → Configuration`

| Mục | Giá trị |
|---|---|
| JCVM Started by IDE | đánh dấu |
| Server IP | `127.0.0.1` |
| Used Port | không đánh dấu |
| Enable PCSC Interface | **đánh dấu** |

### 4.3 Tạo project

1. Tạo project `ThucHanhJavaCard` tại `C:\workspace\ThucHanhJavaCard`.
2. Mỗi thư mục trong `src\` là một **package**. Đặt Package AID theo bảng mục 5 (chuột phải → `Change AID`).
3. Chép nội dung từng file `.java` vào lớp cùng tên. `masterInterface`, `sinhVien`, `sinhVienThuong` là **lớp thường**, không phải applet.
4. Đặt Applet AID theo bảng mục 5.
5. `Build`. Khung Output phải báo `conversion completed with 0 errors and 0 warnings`.
6. `slavePack` dùng `masterPack`: build `masterPack` trước.

Mã applet viết **không dấu** có chủ ý: JCIDE biên dịch bằng `windows-1252`, chữ tiếng Việt có dấu sẽ bị đọc sai.

### 4.4 Chạy và gửi lệnh

1. `Debug → Run or Debug Configuration`, chọn applet → `Edit`: nhập **Instance AID** và **Application Specific Parameters** nếu bảng mục 5 có (th4 Hình 4.1–4.3).
2. `Debug → Run` (Ctrl+F8). Đợi Debug Shell báo `You can send apdu with PC/SC interface now`.
3. Gửi lệnh bằng Debug Shell (`/select <AID>`, `/send <APDU>`), PyApduTool (đầu đọc `JAVACOS Virtual Contact Reader 0`, T = 0), hoặc công cụ ở đây:

```powershell
.\chay.cmd --pcsc=JAVACOS kich-ban\th3.apdu
```

Ở chế độ PC/SC công cụ **không cài applet được**: nó liệt kê applet và tham số cần cài, chờ bạn cài trong JCIDE rồi bấm Enter. Kịch bản có nhiều phần (th2, th4, th5) sẽ dừng lại trước mỗi phần.
Nhớ **Disconnect PyApduTool** trước khi dùng công cụ khác.

### 4.5 Thử lần đầu bằng applet `cnt`

1. Trong JCIDE: package `cnt` (AID `99 88 77 66 55`), lớp `cnt` (AID `99 88 77 66 55 00`), chép mã từ `src\cnt\cnt.java` → `Build` → `Run`.
2. Debug Shell: `/select 998877665500`, rồi `/send B011000000` → mong đợi `48 45 4C 4C 4F 90 00`.
3. Từ thư mục `thuc-hanh`, kết quả đúng là `4 đạt, 0 sai`:

```powershell
.\chay.cmd --pcsc=JAVACOS kich-ban\cnt.apdu
```

4. Mở chương trình giao diện, bấm **Connect** → **Show Text** → `HELLO`:

```powershell
.\chay-ket-noi.cmd pcsc
```

Không thấy đầu đọc `JAVACOS Virtual Contact Reader 0`: kiểm tra đã đánh dấu Enable PCSC Interface và đã Run; xem `Get-Service SCardSvr`; thử mở JCIDE bằng quyền Administrator.

## 5. Bảng AID và tham số cài đặt

| Gói | Package AID | Applet | Applet AID | Instance AID / tham số |
|---|---|---|---|---|
| `bai1_APDU` | `11 22 33 44 55 01` | Applet1, 2, 3, 4, Calculator | `…01 01` đến `…01 05` | |
| `bai2_Object` | `11 22 33 44 55 02` | Applet1, 2, 3 | `…02 01` đến `…02 03` | Applet2: tham số = PIN 4–8 byte (bỏ trống: `1234`) |
| `bai3_Transaction` | `11 22 33 44 55 03` | Applet1, 2, 3 | `…03 01` đến `…03 03` | |
| `bai4_InstallMethod` | `11 22 33 44 55 04` | Applet1, 2, 3 | `…04 01` đến `…04 03` | Applet2, 3: `44 49 45 4D 5F 54 48 49` / `53 56 30 31 09` (cài từng cái một) |
| `masterPack` | `11 22 33 44 55 05 00` | masterApp | `11 22 33 44 55 05 00 00` | |
| `slavePack` | `11 22 33 44 55 05 01` | slaveApp, slaveAppMang, slaveAppDiem, slaveApp1, slaveAppNonStatic | `…05 01 01` đến `…05 01 05` | |
| `chiaSeAnToan` | `11 22 33 44 55 05 02` | ChuApplet | `…05 02 01` | khóa chung 16 byte (tùy chọn) |
| `khachAnToan` | `11 22 33 44 55 05 03` | KhachApplet | `…05 03 01` | cùng khóa với ChuApplet |
| `khachLa` | `11 22 33 44 55 05 04` | KeXamNhap | `…05 04 01` | |
| `bai6_Crypto` | `11 22 33 44 55 06` | HashApplet, AesApplet, RsaApplet | `…06 01` đến `…06 03` | |
| `bai7_APDU_Extended` | `11 22 33 44 55 07` | Applet1 | `…07 01` | |
| `cnt` | `99 88 77 66 55` | cnt | `99 88 77 66 55 00` | |

`…` là phần đầu `11 22 33 44 55`.

## 6. Từng bài

### TH1 – APDU cơ bản ([đề](../smart/docs/th1-bai-tap.md), `kich-ban\th1.apdu`)

| Bài | Applet | Lệnh | Kết quả |
|---|---|---|---|
| 1: in "Xin chao, KMA" | Applet1 | `00 00 00 00 0D` | chuỗi + `90 00` |
| 2: họ tên / ngày sinh / cả hai | Applet2 | `00 01…`, `00 02…`, `00 03…` | chuỗi |
| 3: diện tích vuông, chữ nhật | Applet3 | `00 01 cạnh 00 02`, `00 02 dài rộng 02` | 2 byte |
| 4: nhập, in thông tin SV | Applet4 | `00 01 00 00 Lc "mã\|tên\|ngày\|quê"`, `00 02 P1` | cả chuỗi (P1=0) hoặc từng trường (1–4) |
| Máy tính bỏ túi | Calculator | `00 01..04 a b 02` | cộng, trừ, nhân, chia (thương, dư) |

- **Việc cần làm:** Applet2 đang dùng dữ liệu mẫu `Nguyen Van A`, `01/01/2004`. Sửa thành thông tin của bạn, rồi sửa 2 dòng kết quả tương ứng trong `th1.apdu`.
- Tài liệu không có bản hướng dẫn "máy tính bỏ túi"; `Calculator` viết theo 4 phép tính cơ bản. Nếu cô dạy định dạng lệnh khác thì báo để sửa.

### TH2 – Đối tượng bền và tạm ([đề](../smart/docs/th2-cac-doi-tuong-trong-java-card.md), `th2.apdu`)

| Thời điểm | Trả về (x, buffer, buffer1, buffer2) |
|---|---|
| Sau khi cập nhật | `09 01 02 03 04 05 06` |
| Sau khi bỏ chọn applet | `09 01 02 00 00 05 06` (mất `CLEAR_ON_DESELECT`) |
| Sau khi reset | `09 01 02 00 00 00 00` (mất cả `CLEAR_ON_RESET`) |

**Bài tập 2, bài 1** (Applet2, [đề](../smart/docs/bai-tap-2.md))

1. *`wrongTryCounter` nên là loại đối tượng nào?* Đối tượng **bền** (trường của applet, nằm trong EEPROM). Nếu để trong RAM, kẻ tấn công reset thẻ sau mỗi lần sai là được thử vô hạn.
2. *Reset giữa chừng có mất không?* **Không**, vì ở bộ nhớ bền; cập nhật một trường là nguyên tử. Lệnh `00 30 00 00 02` trả thêm một bộ đếm trong RAM để thấy sự khác nhau: sau reset nó về `00`.

Mã tăng bộ đếm **trước** khi so PIN, để rút thẻ đúng lúc cũng không được thử miễn phí. Thẻ thật dùng `OwnerPIN` có giới hạn số lần sai.

**Bài tập 2, bài 2** (Applet3): `tempBuffer` nên là mảng tạm **`CLEAR_ON_DESELECT`**: chỉ cần khi applet đang được chọn, tự xóa khi chọn applet khác hoặc reset, không hao mòn EEPROM. Biến độ dài cũng phải nằm trong RAM. "Giải phóng" = ghi 0 và đặt độ dài về 0.

### TH3 – Giao dịch ([đề](../smart/docs/th3-thao-tac-nguyen-to-va-giao-dich.md), `th3.apdu`)

| Applet | Lệnh | Kết quả | Ý nghĩa |
|---|---|---|---|
| Applet1 (ví dụ 2) | `00 00 01 02` rồi `00 01 01 02` | `00 00 00 00 00 00 06 07 08 09` | Phần i ≤ 5 bị hủy |
| Applet2 (mục 3.3) | `00 00 01 02` | `00 00 00 02 04 06 01` | Chỉ đối tượng bền được hoàn tác; RAM và biến cục bộ thì không |
| Applet3 (ví dụ 1) | `00 00`, `00 01`, `00 03 00 00 08` | đủ 8 byte hoặc toàn 0 | Tất cả hoặc không gì cả |

### TH4 – Phương thức install ([đề](../smart/docs/th4-phuong-thuc-install.md), `th4.apdu`)

- **Câu hỏi lý thuyết:** [TH4-cau-hoi-install.md](TH4-cau-hoi-install.md).
- Applet2 (ví dụ 4.4): `00 00 01 02` → `53 56 30 31 09`.
- Applet3 (bài tập 4.5):

| Lệnh | Chức năng |
|---|---|
| `00 00 00 00 00` | Mã SV, số môn tối đa, số môn đã nhập |
| `00 01 00 00 Lc [môn điểm]…` | Nhập (một hoặc nhiều môn) |
| `00 02 00 00 00` | In điểm |
| `00 03 00 00 Lc [môn điểm]…` | Sửa |
| `00 04 00 00 Lc [môn]…` | Xóa |

Sau nhập, sửa, xóa thẻ trả lại toàn bộ bảng điểm. Lỗi: `6A 89` trùng môn, `6A 84` vượt số môn, `6A 83` không có môn, `6A 80` điểm ngoài 0–10 hoặc thiếu byte. Một lệnh có một mục sai thì không mục nào được ghi.

### TH5 – Chia sẻ đối tượng ([đề](../smart/docs/th5-chia-se-doi-tuong-giua-cac-applet.md), `th5.apdu`)

- **Câu hỏi lý thuyết:** [TH5-cau-hoi-chia-se.md](TH5-cau-hoi-chia-se.md).

Cài `masterApp` trước. Chọn applet khách rồi gửi `00 00 01 02`:

| Applet khách | Kết quả | Vì sao |
|---|---|---|
| slaveApp | `01 02 03 04 05` | Truyền bộ đệm APDU (mảng toàn cục) |
| slaveAppDiem | `08` | Tham số kiểu cơ bản |
| slaveApp1 | `08` | Trường `static` |
| slaveAppNonStatic | `6F 00` | Đối tượng chưa `new` (null) |
| slaveAppMang | `6F 00` **chỉ trên JCIDE** | Tường lửa chặn mảng riêng của applet khách |

Khi vấn đáp về `slaveAppNonStatic`: trong mã của đề, biến `sv1` không hề được `new`. Trường `static` vẫn chạy vì thuộc về lớp; trường thường thì ném `NullPointerException`. Nếu tạo đối tượng trong hàm tạo thì trường không `static` cũng dùng được, vì giá trị truyền sang applet chủ vẫn chỉ là `byte`.

**Mở rộng – chia sẻ có xác thực** (`th5-an-toan.apdu`, giải thích ở [phan-tich/th5.md](phan-tich/th5.md#3-phần-mở-rộng-chia-sẻ-an-toàn-theo-ghi-chú-của-nhóm)): applet chủ `chiaSeAnToan` chỉ trả đối tượng cho đúng AID, kiểm lại người gọi ở mỗi phương thức và bắt trả lời thử thách AES trước khi cho đọc dữ liệu.

### TH6 – Mật mã ([slide](../smart/docs/th6-mat-ma-trong-java-card.md), `th6.apdu`)

Slide chỉ có lý thuyết, không có đề mã. Ba applet dưới đây minh họa đúng các mục trong slide.

| Applet | Lệnh chính |
|---|---|
| HashApplet | `00 01/02/03` = MD5 / SHA-1 / SHA-256 |
| AesApplet | `00 10` nạp khóa 16/24/32 byte; `00 11` mã hóa; `00 12` giải mã; P1: ECB/CBC; P2: không đệm/đệm ISO 9797-1 cách 2 |
| RsaApplet | `00 20` sinh khóa; `00 21` khóa công khai; `00 22/23` mã hóa/giải mã; `00 24` ký (challenge-response); `00 25` kiểm chữ ký |

`.\kiem-tra.cmd` so kết quả của thẻ với thư viện mật mã Java trên máy tính (kể cả giá trị `SHA-1("Fox")` in trên slide 3 và ví dụ chuẩn AES).

### APDU mở rộng ([tài liệu](../smart/docs/apdu-mo-rong.md), `th7.apdu`)

`00 01` nhập, `00 02` xuất, tối đa 1024 byte. Thẻ giả lập chỉ thử được 100 byte; phần 600 byte phải chạy trên JCIDE với **T = 1**.

### Kết nối ([tài liệu](../smart/docs/huong-dan-ket-noi.md), `cnt.apdu`)

```powershell
.\chay-ket-noi.cmd
```

Bấm **Connect** → **Show Text** → hiện `HELLO`. Với thẻ ảo JCIDE (đã cài `cnt` và Run):

```powershell
.\chay-ket-noi.cmd pcsc
```

Mở bằng NetBeans: tạo *Java Application* tên `Connect_Ex_GUI` (bỏ chọn tạo lớp main), chép thư mục `Connect_Ex_GUI\src\connect_ex_gui` vào `src`, chạy `GUI.java`. Giao diện viết tay bằng Swing nên không mở được ở chế độ Design.

## 7. Chỗ viết khác mã trong tài liệu

| Bài | Khác | Lý do |
|---|---|---|
| TH2 Applet1, TH3 Applet1 | Bỏ `setIncomingAndReceive()` | Lệnh không có dữ liệu gửi xuống |
| TH3 Applet2, TH5 slaveAppMang | `new` chuyển vào hàm tạo | Không cấp phát lại mỗi lệnh |
| TH3 ví dụ 1 | Sửa vòng `for`, dùng `short` | Đề in đổi chỗ điều kiện và phần tăng |
| TH4 Applet2 | Bỏ `static`; `register()` đặt cuối | Hai thể hiện không đè dữ liệu nhau; lỗi thì không để lại applet dở |
| TH5 slaveApp1 | `new slaveApp1()` | Đề gọi nhầm tên lớp |
| APDU mở rộng | Cấp phát trong hàm tạo; xuất bằng một lần `sendBytesLong` | Vòng lặp của đề lặp mãi nếu `le = 0` |
| Kết nối | Dùng `selectingApplet()`; chọn đầu đọc đang có thẻ; thử `*` nếu T=0 lỗi | JCIDE có hai đầu đọc ảo |

## 8. Thẻ giả lập khác thẻ thật ở đâu

Công cụ đã **bổ sung** những gì jCardSim 2.2.2 thiếu: lệnh SELECT, xóa bộ nhớ tạm khi bỏ chọn và reset, hoàn tác khi `abortTransaction`, và đúng AID người gọi (`getAID`, `getPreviousContextAID`) trong phương thức chia sẻ.
(Giới hạn của phần hoàn tác: cập nhật bằng hàm `…NonAtomic` trong giao dịch cũng bị hoàn tác; mảng bền chỉ được giữ bởi biến cục bộ thì không.)

Những gì **vẫn khác**, chỉ tin kết quả trên JCIDE:

- Không có tường lửa giữa các applet.
- APDU dữ liệu dài và APDU mở rộng không xử lý đúng.
- Giải mã RSA trả đủ 117 byte (thêm `00`) thay vì đúng độ dài.
- `register()` không tham số vẫn đăng ký theo Instance AID được cấp.
- Cài đặt lỗi chỉ báo mã chung, không đúng mã applet ném ra.
- Không mô phỏng giới hạn bộ nhớ; số "ngẫu nhiên" có thể lặp lại giữa các thẻ mới.

## 9. Chưa kiểm được

JCIDE và thẻ ảo qua PC/SC; build bằng bộ chuyển đổi 2.2.2; APDU mở rộng; tường lửa applet; mở project trong NetBeans.
