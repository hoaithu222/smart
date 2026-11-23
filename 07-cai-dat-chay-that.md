# 07 · Cài đặt thật trên máy, và cài JCIDE chi tiết

> Ngày 22/11/2026. Bổ sung cho [04-moi-truong-cau-hinh.md](04-moi-truong-cau-hinh.md): mục 04 là cách bấm tay chung; file này ghi **những gì đã cài và kiểm chứng thật** trên máy soạn, link tải, lệnh cài nhanh cho máy mới, và hướng dẫn **JCIDE** từng bước.
> Mật khẩu `sa` không ghi trong file này (file này nằm trong git). Mật khẩu nằm ở `code\host\app.local.properties` (đã đưa vào `.gitignore`).

## 1. Ai cần cài gì

| Phần việc | Cần cài | Ghi chú |
|---|---|---|
| **Applet** (viết, build, chạy thẻ ảo, gửi APDU tay) | Chỉ bộ **JavaCardKit** (có sẵn JCIDE + PyApduTool + JDK riêng) | Giải nén đúng vào `C:\JavaCardKit`. **Không** cần cài JDK/NetBeans cho phần này |
| **Phần mềm máy tính** (Swing, nối thẻ + CSDL) | JDK 8, NetBeans, SQL Server Express, SSMS | Chạy nhanh không cần NetBeans: `code\host\chay-phan-mem.cmd` (chỉ cần JDK) |

## 2. Link tải

| Phần mềm | Link | Ghi chú |
|---|---|---|
| **JavaCardKit (JCIDE + PyApduTool)** | Trang chính thức: <https://www.javacardos.com/tools> · diễn đàn: <https://www.javacardos.com/javacardforum/viewforum.php?f=26> · mô tả: <https://github.com/Tolice/JCIDE> | Miễn phí. **Lúc soạn file này tôi không vào được javacardos.com** (server từ chối kết nối), nên chưa xác nhận được link tải trực tiếp. Cách chắc nhất: xin đúng bộ cô phát / chép từ máy bạn nhóm (mục 5.1) |
| JDK 8 (Temurin) | <https://adoptium.net/temurin/releases/?version=8> | Chọn Windows x64, `.msi` |
| NetBeans | <https://netbeans.apache.org/front/main/download/> | Từ bản 25 trở đi Apache chỉ phát hành **file zip** (không còn `.exe`): `netbeans-32-bin.zip`, chạy trên JDK 21/25/27. NetBeans 8.2 của cô chỉ còn ở kho lưu trữ, không cần thiết |
| SQL Server 2022 Express | <https://www.microsoft.com/en-us/sql-server/sql-server-downloads> (mục Express) · trực tiếp: <https://download.microsoft.com/download/29654887-7cde-4397-bba3-d7f087970845/SQL2022-SSEI-Expr.exe> | Bộ cài mỏng, tải thêm lúc chạy |
| SSMS | <https://learn.microsoft.com/sql/ssms/download-sql-server-management-studio-ssms> | Hoặc `winget install Microsoft.SQLServerManagementStudio.22` |

## 3. Đã cài trên máy soạn (kiểm chứng ngày 22/11/2026)

| Thành phần | Kết quả |
|---|---|
| JDK 8 | Temurin 8.0.512 tại `C:\Program Files\Eclipse Adoptium\jdk-8.0.512.9-hotspot`. Mã phần mềm biên dịch sạch bằng `javac` của JDK 8 (85 lớp, 0 lỗi) |
| JDK 21 | Có sẵn, vẫn là JDK mặc định trong PATH và `JAVA_HOME` |
| NetBeans | Apache NetBeans 32 giải nén tại `C:\Tools\netbeans-32`, có lối tắt "Apache NetBeans 32" trong Start Menu. Đã mở thử, khởi động bình thường |
| SQL Server 2022 Express | Instance `SQLEXPRESS` (16.0.1000.6), đăng nhập SQL (`sa`) bật, TCP/IP cố định cổng **1433**, dịch vụ tự khởi động cùng Windows |
| SSMS 22 | Đã cài |
| Dịch vụ Windows "Smart Card" (`SCardSvr`) | Đổi sang tự khởi động và đang chạy (cần cho PC/SC) |
| Phần mềm ↔ SQL Server thật | `DatabaseBootstrap` tự tạo database `the_sinh_vien`: 5 bảng, 3 tài khoản mẫu, 3 phòng. Chạy `chay-phan-mem.cmd` với `app.local.properties` mở được, không lỗi |
| **JavaCardKit 1.0.5.39** (cập nhật 23/11/2026) | Đã có ở `C:\JavaCardKit` (từ `JCKit_JAVACOS_1.0.5.39.exe`): `JCIDE.exe` 1.0.5.39 (build 2016-03-20), `JCOSPanel.exe`, 3 dự án mẫu (GPSample, wallet, WalletDemoApplet), 2 PDF. **Hai đầu đọc ảo `JAVACOS Virtual Contact Reader 0` / `Contactless Reader 1` đã đăng ký PC/SC** (Windows liệt kê được, `cardPresent=false` vì JCIDE chưa Run thẻ ảo). **Không có** thư mục `JDK`, `JCDK`, PyApduTool trong bộ này → đã bổ sung hai liên kết thư mục (mục 5.0) |

Thẻ đang ở chế độ `card.mode=jcardsim` (thẻ giả lập trong phần mềm) cho tới khi có JCIDE. Khi JCIDE chạy: đổi thành `pcsc` trong `app.local.properties` (mục 5.8).

## 4. Cài nhanh cho máy mới (PowerShell chạy bằng quyền Administrator)

**Cách nhanh nhất: một file cài trọn bộ** [cai-dat-may-moi.ps1](cai-dat-may-moi.ps1) (chuột phải PowerShell → Run as administrator):

```powershell
powershell -ExecutionPolicy Bypass -File .\cai-dat-may-moi.ps1
# Khi đã có file zip JavaCardKit và muốn tạo sẵn cấu hình phần mềm:
powershell -ExecutionPolicy Bypass -File .\cai-dat-may-moi.ps1 -JavaCardKitZip C:\Users\ban\Downloads\JCKit.zip -HostDir D:\sbc\smart\code\host
```

Script cài JDK 8, SQL Server Express (hỏi mật khẩu `sa`), SSMS, NetBeans 32, bật dịch vụ Smart Card; bước nào đã có thì bỏ qua. Có `-JavaCardKitZip` thì giải nén JavaCardKit vào `C:\JavaCardKit`. **Script không tự tải được JCIDE** (không có link tải được từ máy tôi). Từng bước trong script đã chạy thật ở máy soạn; cả file thì mới kiểm tra cú pháp, chưa chạy trọn trên máy khác. Dưới đây là các lệnh tương ứng nếu muốn chạy tay từng bước:

Đây là đúng các lệnh đã chạy thật ở máy soạn. Đặt `<MẬT_KHẨU_SA>` là mật khẩu tự chọn (≥ 8 ký tự, có hoa, thường, số, ký hiệu).

```powershell
# 1) JDK 8
winget install -e --id EclipseAdoptium.Temurin.8.JDK --silent --accept-package-agreements

# 2) SQL Server 2022 Express: tải bộ Core trước (KHÔNG thêm /IACCEPT... ở bước tải, sẽ báo lỗi)
$dir = "$env:TEMP\sqlmedia"; New-Item -ItemType Directory -Force $dir | Out-Null
Invoke-WebRequest "https://download.microsoft.com/download/29654887-7cde-4397-bba3-d7f087970845/SQL2022-SSEI-Expr.exe" -OutFile "$env:TEMP\SSEI.exe"
Start-Process "$env:TEMP\SSEI.exe" -ArgumentList '/ACTION=Download',"/MEDIAPATH=$dir",'/MEDIATYPE=Core','/QUIET' -Wait
# Cài với đủ tham số (bộ SSEI mỏng không nhận các tham số này, phải chạy SQLEXPR_x64_ENU.exe)
$me = "$env:USERDOMAIN\$env:USERNAME"
Start-Process "$dir\SQLEXPR_x64_ENU.exe" -Wait -ArgumentList '/Q','/ACTION=Install','/IACCEPTSQLSERVERLICENSETERMS','/FEATURES=SQLENGINE','/INSTANCENAME=SQLEXPRESS','/SECURITYMODE=SQL','/SAPWD=<MẬT_KHẨU_SA>','/TCPENABLED=1',"/SQLSYSADMINACCOUNTS=$me",'/UpdateEnabled=0'

# 3) Cổng TCP cố định 1433 rồi khởi động lại dịch vụ
$base = 'HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server'
$inst = (Get-ItemProperty "$base\Instance Names\SQL").SQLEXPRESS
$ip = "$base\$inst\MSSQLServer\SuperSocketNetLib\Tcp\IPAll"
Set-ItemProperty $ip TcpPort '1433'; Set-ItemProperty $ip TcpDynamicPorts ''
Restart-Service 'MSSQL$SQLEXPRESS' -Force

# 4) Dịch vụ Smart Card tự chạy
Set-Service SCardSvr -StartupType Automatic; Start-Service SCardSvr

# 5) SSMS
winget install -e --id Microsoft.SQLServerManagementStudio.22 --silent --accept-package-agreements
```

NetBeans: tải `netbeans-32-bin.zip` ở link mục 2, giải nén (ví dụ `C:\Tools\netbeans-32`), chạy `bin\netbeans64.exe`. Bản zip không cần cài.

Sau đó trong `code\host`: chép `app.properties` thành `app.local.properties`, điền `db.password=<MẬT_KHẨU_SA>`; chạy `chay-phan-mem.cmd`. Cổng 1433 chỉ mở cho máy này (tường lửa Windows chặn kết nối từ ngoài theo mặc định); không mở thêm.

### NetBeans dùng JDK 8 cho project

1. `Tools → Java Platforms → Add Platform → Java Standard Edition` → chọn thư mục `C:\Program Files\Eclipse Adoptium\jdk-8.0.512.9-hotspot`.
2. Tạo project và đặt thuộc tính theo [04 mục 4](04-moi-truong-cau-hinh.md#4-cấu-hình-project-netbeans). NetBeans 32 tự chạy bằng JDK 21, còn **project** biên dịch ở mức JDK 8 (đặt ở `Properties → Sources → Source/Binary Format`) và chạy bằng nền JDK 8 ở `Properties → Run`.
3. Chưa thử tạo project "Java Project with Existing Sources" trong NetBeans 32. Nếu không thấy mục này, vào `Tools → Plugins → Available` bật Java SE / Java with Ant; hoặc cứ dùng `chay-phan-mem.cmd`.

## 5. Cài JCIDE (trọn bộ JavaCardKit)

### 5.0 Hiện trạng trên máy soạn (23/11/2026)

Bạn đã tải `JCKit_JAVACOS_1.0.5.39.exe` (bản *Classic*, 94 MB) và nó đã nằm ở `C:\JavaCardKit`. **Bản này khác bản 7.3.0.0 trong tài liệu của cô**, nên đừng làm theo 5.2 từng chữ:

| Theo tài liệu cô (7.x) | Bộ 1.0.5.39 thực tế trên máy |
|---|---|
| `C:\JavaCardKit\JDK\bin\javac.exe` | **Không có** → đã tạo liên kết `C:\JavaCardKit\JDK` → JDK 8 Temurin |
| `…\JCIDE\lib\java_card_kit\2_2_2\lib\api.jar` | **Không có**; dự án mẫu của bộ này trỏ tới `C:\JavaCardKit\JCDK` → đã tạo liên kết `C:\JavaCardKit\JCDK` → `C:\Tools\oracle_javacard_sdks\jc222_kit` (Java Card Development Kit 2.2.2: `lib\api.jar`, `converter.jar`, `api_export_files`) |
| PyApduTool | Không thấy trong thư mục |
| Dòng build "Feitian … Class File Converter, Version 1.3" | Bộ chuyển đổi của JCDK 2.2.2 in "**Java Card 2.2.2 Class File Converter, Version 1.3**" (Oracle/Sun); cùng phiên bản 1.3, khác tên hãng |

Hai liên kết là *junction* (không sao chép file, xóa bằng `rmdir C:\JavaCardKit\JDK` không mất dữ liệu gốc). **JCDK lấy từ bản sao công khai của Oracle SDK trong repo bên thứ ba** `martinpaljak/oracle_javacard_sdks` (không phải trang Oracle, tôi không có cách đối chiếu mã băm với Oracle); nếu thầy/cô yêu cầu nguồn chính thức thì tải JCDK 2.2.2 từ trang Oracle và thay vào `C:\Tools\oracle_javacard_sdks\jc222_kit` (hoặc trỏ liên kết sang đó).

**Chưa kiểm chứng:** tôi chưa mở giao diện JCIDE (theo yêu cầu "không cần mở"), nên chưa biết JCIDE 1.0.5.39 có nhận hai liên kết này ở `Build → Property → Local JCKit configurations`, và chưa Run thẻ ảo. Đã kiểm chứng bằng dòng lệnh đúng bước Build của JCIDE: mục 8.

JCIDE là IDE viết applet Java Card, có sẵn **thẻ ảo (virtual card)** nối với Windows qua PC/SC, nên PyApduTool và phần mềm NetBeans nhìn thấy nó như một đầu đọc thật tên `JAVACOS Virtual Contact Reader 0`. Tài liệu của cô dùng bản trong bộ **JavaCardKit**, build bằng "Feitian Java Card 2.2.2 Class File Converter, Version 1.3".

### 5.1 Lấy bộ cài (chọn 1 trong 2)

- **Cách chắc nhất:** xin đúng bộ cô phát (hoặc nhờ bạn trong lớp, ví dụ Ngọc, nén cả thư mục `C:\JavaCardKit` gửi lại). Đúng bộ thì đường dẫn khớp 100% với tài liệu.
- **Tải trên mạng (nguồn duy nhất chính thức là JavaCardOS):** vào <https://www.javacardos.com/tools>, mục *Smart Card Development Kit*, chọn **JCKit Release Version v7.3.0.0** (gồm JCIDE + PyApduTool, miễn phí). Bài thông báo bản này trên diễn đàn có link Google Drive để tải: <https://javacardos.com/javacardforum/viewtopic.php?t=3309>. Trang còn liệt kê bản *Classic (1.0.5.x)*; bản 7.4.0.3 theo người dùng diễn đàn thực chất vẫn là 7.3.0.0 (ngày build 2021-07-07 trong Help của JCIDE). Có thể cần đăng ký tài khoản/đăng ký JCIDE. Cài xong mở menu Help của JCIDE xem phiên bản, rồi đối chiếu ở bước 5.5.
  - Chưa chắc bộ cô phát là bản nào: dòng "Feitian … Class File Converter 1.3" trong tài liệu và tên gói "JCKit ftsafe-7.3.0.0" gợi ý bản Release 7.3.0.0, nhưng các trang tải bên thứ ba lại liệt kê bản Classic 1.0.5.39 (~90 MB). Hỏi cô/Ngọc xem dùng bản nào; cả hai đều cùng họ JCKit (JCIDE + PyApduTool).
  - Tìm sâu thêm (22/11/2026): **không có nguồn nào khác ngoài `javacardos.com`** phát file JCKit. Các trang bên thứ ba (getwinpcsoft, Software Informer, freedownloadmanager) chỉ liệt kê bản 1.0.5.39 và trỏ về hãng; GitHub không có bản phát hành nào (repo `Tolice/JCIDE`, `KMA-JAVA-CARD/*` chỉ có mã/project). Link tải cũ của Oracle cho Java Card 2.2.2 (`download.oracle.com/otn-pub/java/java_card_kit/2.2.2/...windows.zip`) hiện chuyển về trang lỗi, phải đăng nhập tài khoản Oracle.
  - Diễn đàn có người báo file zip ghi "7.1.0.0" nhưng bên trong là bản 6.1.0.8, còn bản **Google Drive** thì đúng. Ưu tiên link Drive; tải xong kiểm tra phiên bản.
  - Các bài diễn đàn cần mở (đều ở javacardos.com; nằm trong bản lưu archive.org): 7.3.0.0 [t=3309](https://javacardos.com/javacardforum/viewtopic.php?t=3309), 7.4.0.3 [t=3569](https://www.javacardos.com/javacardforum/viewtopic.php?t=3569), 7.1.0.0 [t=2587](https://www.javacardos.com/javacardforum/viewtopic.php?t=2587), 5.0.1.7 [t=2108](https://www.javacardos.com/javacardforum/viewtopic.php?t=2108), 1.0.5.36 [t=168](https://www.javacardos.com/javacardforum/viewtopic.php?t=168).
  - Hai repo đồ án của sinh viên KMA (`vankief/smartcardJCIDE`, `huygia12/citizen-card`) đều ghi `C:\JavaCardKit\JDK` và `java_card_kit-2_2_2`: xác nhận đường dẫn trong tài liệu của cô là cấu trúc chuẩn của JCKit.
  - JCIDE **miễn phí**, không cần crack/key. Tuyệt đối không tải bản "crack", "full" từ trang lạ: rất hay kèm mã độc.
  - Tôi đã kiểm tra thêm: các file PDF của cô chỉ có link tới tài liệu Oracle và Yahoo, **không có link/file JavaCardKit**; máy bạn cũng chưa có file nào.
  - **Không tải từ các trang tải phần mềm bên thứ ba** (getwinpcsoft, freedownloadmanager, updatestar…): tôi không kiểm tra được file, và JCIDE cần chạy quyền Administrator.
  - Lúc soạn (22/11/2026) `javacardos.com` từ chối kết nối từ máy này và từ máy chủ tra cứu. Bạn thử mở bằng trình duyệt của bạn; nếu vẫn không vào được thì nhờ cô/bạn nhóm gửi bộ đã có. Hỏi đáp: <https://www.javacardos.com/javacardforum/viewforum.php?f=26>.

### 5.2 Giải nén đúng chỗ

1. Giải nén vào **`C:\JavaCardKit`** (đừng để thành `C:\JavaCardKit\JavaCardKit`). Đường dẫn không dấu, không khoảng trắng.
2. Kiểm tra bằng PowerShell:

   ```powershell
   Test-Path C:\JavaCardKit\JDK\bin\javac.exe
   Test-Path C:\JavaCardKit\SDK\Tools\JCIDE\lib\java_card_kit\2_2_2\lib\api.jar
   ```

   Cả hai phải ra `True` (đúng hai đường dẫn tài liệu của cô dùng khi build).
3. Windows Defender/antivirus đôi khi xóa hoặc chặn file của JCIDE và PyApduTool. Nếu thấy file biến mất: mở *Bảo mật Windows → Lịch sử bảo vệ*, khôi phục và cho phép; chỉ làm với bộ lấy từ cô hoặc trang chính thức.

### 5.3 Mở JCIDE

1. Trong `C:\JavaCardKit\SDK\Tools\JCIDE\` mở file chạy của JCIDE (tên file tùy bản; tôi chưa có bộ kit để xác nhận). Tạo lối tắt ra Desktop.
2. Chuột phải → **Run as administrator**. Đây là cách bạn đã khuyên Ngọc. Tôi không tìm thấy tài liệu chính thức nói bắt buộc; dùng khi JCIDE/PyApduTool không mở được đầu đọc ảo hoặc không ghi được file.
3. Lần đầu có thể hiện hộp thoại đăng ký/giấy phép: làm theo hướng dẫn trên màn hình.

### 5.4 Bật thẻ ảo: `Tool → IDE Options → Run/Debug → Configuration`

| Mục | Đặt |
|---|---|
| JCVM Started by IDE | ✔ |
| Server IP | `127.0.0.1` |
| Used Port | để trống (mặc định: CB 9025, Break 9550, CL 9035, Command 9551, Reader 5000) |
| **Enable PCSC Interface** | ✔ (bắt buộc) |

Nếu sau này không thấy đầu đọc `JAVACOS Virtual Contact Reader 0`, có thể phải cài driver đầu đọc ảo một lần: chạy `SdtPcsc_Install.bat` trong `C:\JavaCardKit\SDK\Tools\JCIDE\driver` bằng quyền Administrator (theo một bài diễn đàn JavaCardOS tôi tra được; **chưa kiểm chứng**, vì chưa có bộ kit).

Các cổng trên không được bị chương trình khác chiếm. Kiểm tra: `netstat -ano | findstr "9025 9550 9035 9551 5000"` (đang đóng JCIDE thì phải rỗng).

### 5.5 Tạo project và build

1. `File → New → Project`, tên `TheSinhVien`, thư mục `C:\workspace\TheSinhVien`.
2. Package `sinhvien`, Package AID `4B 4D 41 53 56 01`; applet `StudentCard`, Applet AID `4B 4D 41 53 56 01 01`.
3. Chép nội dung [code/applet/src/sinhvien/StudentCard.java](code/applet/src/sinhvien/StudentCard.java) vào `StudentCard`.
4. `Build → Build`. Khung **Output** phải có đúng hai dòng:

   ```text
   Feitian Java Card 2.2.2 Class File Converter, Version 1.3
   conversion completed with 0 errors and 0 warnings.
   ```

   Hiện đúng như vậy là bộ kit khớp với tài liệu của cô. Lỗi build hay gặp: dùng `int`, thiếu ép `(short)`/`(byte)`, `String`, `@Override`: xem [th/th1-applet-apdu.md](th/th1-applet-apdu.md#5-lỗi-biên-dịch-hay-gặp).

### 5.6 Đặt PIN quản trị và chạy thẻ ảo

1. `Debug → Run or Debug Configuration → Package Upload`, chọn dòng `StudentCard` → **Edit**: Instance AID `4B 4D 41 53 56 01 01`, Application Specific Parameters `38 37 36 35 34 33 32 31` (= `87654321`).
2. `Debug → Run` (Ctrl+F8). Đợi Debug Shell ra:

   ```text
   Disconnect CB port
   You can send apdu with PC/SC interface now
   ```

   Các dòng `6A 82`, `6A 86` ở giữa là bình thường.
3. Từ lúc này mới mở PyApduTool hoặc phần mềm. **Mỗi lần Run lại là cài lại applet = thẻ về trắng**, dữ liệu đã phát hành mất.

### 5.7 Thử bằng PyApduTool

1. Mở PyApduTool (trong bộ kit), tab **Reader**: chọn `JAVACOS Virtual Contact Reader 0`, Protocol `T = 0`, Exclusive Mode → **Connect** → `Connect successful.`
2. Tab **Manager**: Applet AID `4B 4D 41 53 56 01 01` → **Select** → `Select Applet successful.`
3. Tab **Apdu**: gửi `B0 10 00 00 00` → nhận `00 03 05 00 00 00 00 00 00 00 00 90 00` (thẻ trắng, PIN còn 3, PUK còn 5). Kịch bản 20 lệnh đầy đủ: [04 mục 3](04-moi-truong-cau-hinh.md#3-kiểm-thử-tay-bằng-pyapdutool).
4. **Xong thì Disconnect**: PyApduTool giữ thẻ ở chế độ Exclusive; còn kết nối thì phần mềm không vào được.

### 5.8 Nối phần mềm vào thẻ ảo

Trong `code\host\app.local.properties` đổi `card.mode=jcardsim` thành:

```properties
card.mode=pcsc
card.reader=JAVACOS Virtual
```

Thứ tự bật: JCIDE Run xong → PyApduTool Disconnect → chạy phần mềm → *Kết nối thẻ* (chip "Thẻ trắng – chưa phát hành"). Kịch bản demo: [th/th8-ket-noi-phan-mem-csdl.md](th/th8-ket-noi-phan-mem-csdl.md#6-kịch-bản-demo-quay-video).

### 5.9 Lỗi hay gặp khi cài/chạy JCIDE

| Hiện tượng | Nguyên nhân | Cách sửa |
|---|---|---|
| Thiếu file sau khi giải nén | Antivirus xóa | 5.2 bước 3 |
| Không thấy `JAVACOS Virtual Contact Reader 0` / `SCARD_E_NO_SERVICE` | Chưa Run thẻ ảo, chưa bật Enable PCSC, hoặc dịch vụ Smart Card dừng | 5.4, 5.6; `Get-Service SCardSvr` phải là Running (máy soạn đã bật tự chạy) |
| Debug Shell: `Connect CL port failed. Please disconnect JAVACOS Vi…` | PyApduTool đang giữ thẻ | Disconnect PyApduTool |
| Cổng bị chiếm | Lần chạy JCIDE trước còn treo | Tắt hẳn JCIDE (Task Manager), kiểm tra `netstat` ở 5.4 |
| Build báo lỗi `int` | Phép tính chưa ép kiểu | `(short)(a + b)` |
| Build báo không thấy JDK/`javac` | JCIDE chưa biết đường dẫn JDK riêng | Menu `Build → Property → Local JCKit configurations`, đặt JDK là `C:\JavaCardKit\JDK` (project của sinh viên KMA lưu đúng đường dẫn này trong `LocalJavahomeProperty.xml`; theo diễn đàn JavaCardOS, chưa kiểm chứng trên bộ của cô) |
| Output không có dòng "Feitian … 2.2.2 … 1.3" | Bộ kit khác bản của cô | Dùng đúng bộ cô phát; lệch phiên bản có thể làm tên menu khác tài liệu |
| Chip đỏ "Không xác thực được" sau khi Run lại | Thẻ mới, khóa RSA mới | Phát hành lại thẻ |

### 5.10 Video hướng dẫn dùng JCIDE (kênh chính thức JavaCard OS, quay 2015, mỗi video 2–4 phút)

Chỉ hướng dẫn **dùng**, không hướng dẫn cài, và không có link tải trong phần mô tả (trừ video đầu trỏ về bài diễn đàn bản 1.0.5.36).

| Video | Nội dung |
|---|---|
| [JavaCard Applet Development Guide](https://www.youtube.com/watch?v=6Lg3EVTexOo) | Tạo project, debug applet trong JCIDE. Mô tả trỏ tới [bài diễn đàn t=168](http://javacardos.com/javacardforum/viewtopic.php?f=5&t=168) (JCKit 1.0.5.36) |
| [How to use Virtual Card Reader?](https://www.youtube.com/watch?v=xX0O7PC0TIA) | Thẻ ảo: nối PyApduTool/công cụ PC/SC vào JCIDE |
| [How to use Virtual Card (Smart Card) Reader in JCIDE?](https://www.youtube.com/watch?v=B4E75JT2RVo) | Đầu đọc ảo dùng driver UMDF (khớp mục 5.4 về driver) |
| [How to download CAP file to card?](https://www.youtube.com/watch?v=9qT9Q4bt9IQ) | Build package, nạp CAP, cài và chọn applet, gửi APDU bằng PyApduTool |
| [How to import another package and debug it](https://www.youtube.com/watch?v=pAghsWxgSuc) | Import package có sẵn |

Không tìm thấy video tiếng Việt nào về cài JCIDE/JavaCardKit. Bản cũ (2015): giao diện có thể hơi khác bản 7.x.

## 6. Phương án dễ cài khi chưa có JCIDE (tìm hiểu 22/11/2026)

Báo cáo của môn vẫn yêu cầu chạy trên JCIDE, nên các phương án dưới đây chỉ để **không bị chặn việc** trong lúc chờ bộ JavaCardKit.

| # | Phương án | Độ dễ | Trạng thái trên máy bạn | Dùng để |
|---|---|---|---|---|
| 1 | **jCardSim** (thẻ giả lập chạy trong phần mềm) | Dễ nhất, không cài gì | Có sẵn (`lib\jcardsim-2.2.2.jar`), bộ test 49/49 + 8/8 | Chạy toàn bộ luồng phần mềm + applet, `card.mode=jcardsim` |
| 2 | **Java Card 2.2.2 SDK của Oracle** (bộ chuyển đổi converter + `api.jar`) | Dễ: tải 2,3 MB, chạy bằng JDK 8 | **Đã tải** vào `C:\Tools\oracle_javacard_sdks\jc222_kit` (lấy từ repo [martinpaljak/oracle_javacard_sdks](https://github.com/martinpaljak/oracle_javacard_sdks), bản sao công khai của bộ Oracle). **Đã chạy 23/11/2026: converter 0 lỗi/0 cảnh báo, Off-card Verifier 0 lỗi/0 cảnh báo** (mục 8) | Kiểm tra applet qua bộ chuyển đổi thật (giống bước Build của JCIDE) rồi ra file `.cap`. Script: [code/applet/build-cap.cmd](code/applet/build-cap.cmd) |
| 3 | **Đầu đọc PC/SC ảo + jCardSim** (Bix Virtual Smart Card Reader) | Khó hơn: phải cài driver, sửa file `.ini` | Chưa cài, chưa thử | Cho phần mềm chạy `card.mode=pcsc` mà chưa cần JCIDE. Hướng dẫn: [OpenSC wiki – Smart Card Simulation](https://www.github.com/OpenSC/OpenSC/wiki/Smart-Card-Simulation) |
| 4 | **Oracle Java Card Development Kit Simulator** (3.x, có driver PC/SC cho Windows) | Nặng nhất, tải từ Oracle | Chưa cài, chưa thử | Trình giả lập chính thức, chỉ thêm được 1 đầu đọc. [Hướng dẫn driver](https://docs.oracle.com/en/java/javacard/3.2/jcdksu/windows-install-and-set-pcsc-driver-optional.html), [trang tải Oracle](https://www.oracle.com/java/technologies/javacard-downloads.html) |

**Về phương án 2:** thư mục `jc222_kit` là file nhị phân của Oracle nằm trong repo bên thứ ba (nguồn chính thức là trang tải của Oracle ở trên, có bước đồng ý giấy phép). Đã chạy thật và báo 0 lỗi (mục 8), nên applet đã qua bộ chuyển đổi Java Card 2.2.2, đỡ rủi ro khi sang JCIDE. Dù vậy đây **không thay được** JCIDE cho phần báo cáo (Debug Shell, thẻ ảo PC/SC).

## 7. Việc còn lại của bạn

1. Mở JCIDE (chuột phải → Run as administrator): `Build → Property → Local JCKit configurations` đặt JDK = `C:\JavaCardKit\JDK`, JCDK = `C:\JavaCardKit\JCDK`; tạo project theo 5.5, Build, rồi Run thẻ ảo (5.4, 5.6). Menu của bản 1.0.5.39 có thể khác tài liệu 7.x, nếu lệch thì ghi lại để cập nhật file này.
2. Khi Debug Shell hiện "You can send apdu with PC/SC interface now": chạy `kiem-tra-moi-truong.ps1` (dòng "JCIDE đang chạy thẻ ảo?" phải ĐẠT), đổi `card.mode=pcsc` (5.8) rồi chạy demo theo [04 mục 7](04-moi-truong-cau-hinh.md#7-chạy-demo-đúng-chuẩn-báo-cáo-jcide--sql-server).
3. Ghi kết quả vào bảng *Nhật ký* ở cuối [04](04-moi-truong-cau-hinh.md).
4. Đổi mật khẩu `sa` nếu máy dùng chung (SSMS → Security → Logins → sa), nhớ sửa lại `app.local.properties`.

## 8. Kết quả kiểm thử ngày 23/11/2026

Chạy trên máy soạn; tái chạy bằng `kiem-tra-moi-truong.ps1 -ChayKiemThu`.

| Kiểm thử | Lệnh | Kết quả |
|---|---|---|
| Applet + thư viện + CSDL (jCardSim + SQLite) | `code\run-test.cmd` | **49/49 kịch bản** |
| Luồng qua giao diện (thẻ giả lập + SQLite, vẽ ra ảnh) | `code\run-test.cmd` | **8/8 kiểm tra**, 14 ảnh ở `code\test\anh-giao-dien` |
| **Cùng 49 kịch bản trên SQL Server 2022 Express thật** | `code\run-test-sqlserver.cmd` | **52/52** (49 + 3 kiểm tra CSDL thật: tạo database/schema, 3 tài khoản mẫu đăng nhập được, AUTO_CLOSE đã tắt). Dùng database riêng `the_sinh_vien_test`, tự tạo và tự xóa; từ chối chạy nếu tên không kết thúc bằng `_test` |
| Applet qua bộ chuyển đổi Java Card 2.2.2 | `code\applet\build-cap.cmd` | `conversion completed with 0 errors and 0 warnings` → `sinhvien.cap` 7.278 byte |
| Bytecode CAP qua Off-card Verifier của Oracle (đối chiếu gói `java.lang`, `javacard.framework`, `javacard.security`, `javacardx.crypto`) | `code\applet\build-cap.cmd` | `Verification completed with 0 warnings and 0 errors` |
| Dự án mẫu **Wallet** của JCKit qua cùng JDK + JCDK | (chạy tay) | 0 lỗi/0 cảnh báo → đường dẫn `C:\JavaCardKit\JDK` + `JCDK` dùng được từ dòng lệnh |
| PC/SC: `PcscChannel` với `card.reader=JAVACOS Virtual` | (chạy tay) | Liệt kê đúng 2 đầu đọc ảo, lọc đúng tên, `connect` báo `CardNotPresentException: No card present` (đúng, vì thẻ ảo chưa Run) |
| Môi trường | `kiem-tra-moi-truong.ps1` | 23 đạt, 3 cảnh báo (JCIDE chưa Run thẻ ảo, không thấy PyApduTool, `card.mode` đang `jcardsim`), 0 lỗi |

**Cấu hình tối ưu đã làm:**

- **Tắt `AUTO_CLOSE`** của database (mặc định bản Express bật: hết kết nối là database đóng, lần mở sau chậm). Làm trong `DatabaseBootstrap.tuneSqlServer`, nên database mới và cũ đều được tắt khi phần mềm mở; đã áp dụng cho `the_sinh_vien` (`autoClose=0`).
- Kiểm tra SQL Server: Express 16.0.1000.6, đăng nhập SQL bật, dịch vụ tự khởi động, dùng ~464 MB trên máy RAM 32 GB (`max server memory` để mặc định, Express tự giới hạn). SQL Server nghe trên mọi giao diện (`0.0.0.0:1433`); tôi không thấy luật tường lửa vào nào có tên chứa "sql" hoặc "1433" (chỉ tra theo tên luật), nên theo mặc định Windows Firewall chặn kết nối từ ngoài. Máy dùng chung/mạng lạ thì nên tự xác nhận.
- `.gitignore`: thêm `code/build/`, `code/applet/build/`.

**Chưa làm được / chưa chứng minh:** chạy applet trong **thẻ ảo của JCIDE qua PC/SC** (cần mở giao diện JCIDE, bạn bảo không cần mở); `card.mode=pcsc` đầu-cuối; mở cửa sổ Swing thật trên màn hình bạn (lần thử đầu cửa sổ đăng nhập hiện lệch xuống sát đáy màn hình và chưa rõ nguyên nhân: Java tính vị trí giữa đúng `(522,247)` khi thử riêng, nên có thể do cách tôi khởi chạy; bạn tự chạy `code\host\chay-phan-mem.cmd` để xem, nếu cửa sổ vẫn lệch thì báo tôi). Ảnh giao diện nằm ở `code\test\anh-giao-dien` (vẽ không cần mở cửa sổ).
