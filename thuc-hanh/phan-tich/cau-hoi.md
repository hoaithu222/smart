# Toàn bộ câu hỏi các buổi thực hành

> Gom từ: câu hỏi bạn gửi (bài 4, bài 5), [bài tập 2](../../smart/docs/bai-tap-2.md), mục "Câu hỏi có thể bị hỏi" trong [th/](../../th/), và [05-on-tap-van-dap.md](../../05-on-tap-van-dap.md). Trả lời ngắn để nói được; cột "Chi tiết" dẫn tới phần giải thích.

## 1. Nền tảng (APDU, Java Card, ngôn ngữ, API)

| # | Câu hỏi | Trả lời ngắn |
|---|---|---|
| 1 | Thẻ thông minh khác thẻ từ? | Có chip xử lý dữ liệu tại chỗ, không chỉ lưu |
| 2 | Phân loại thẻ? | Chip nhớ / vi xử lý; tiếp xúc / không tiếp xúc / hai giao diện |
| 3 | Các loại bộ nhớ chứa gì? | ROM: hệ điều hành; EEPROM: applet, dữ liệu bền; RAM: tạm |
| 4 | APDU lệnh / phản hồi gồm gì? | `CLA INS P1 P2 [Lc Data] [Le]` / `[Data] SW1 SW2` |
| 5 | `90 00`, `6A 82`, `6D 00`, `6E 00`, `69 82`, `63 Cx`? | Thành công / không tìm thấy / INS lạ / CLA lạ / chưa đủ điều kiện bảo mật / sai mã còn x lần |
| 6 | T = 0 và T = 1? | Theo byte (lấy dữ liệu bằng `61 xx`) / theo khối |
| 7 | Máy ảo Java Card gồm mấy phần? | Ngoài thẻ (converter), trên thẻ (interpreter) |
| 8 | Các bước tạo applet? | Viết → `javac` → converter ra CAP → verifier → cài lên thẻ |
| 9 | JCRE là gì, vòng đời? | Môi trường thực thi trên thẻ; sống cùng thẻ; mất điện VM dừng, không khôi phục luồng |
| 10 | Ba tính năng JCRE bổ sung? | Đối tượng bền/tạm; giao dịch; tường lửa và chia sẻ |
| 11 | AID là gì? | Mã 5–16 byte = RID (5) + PIX |
| 12 | Kiểu dữ liệu hỗ trợ? | `byte`, `short`, `boolean`; `int` tùy thẻ; không `String`, mảng nhiều chiều, luồng |
| 13 | Vì sao ép `(short)(a + b)`? | Phép tính trung gian ra `int` |
| 14 | Các gói API chính? | `java.lang`, `javacard.framework`, `javacard.security`, `javacardx.crypto` |
| 15 | Phương thức lớp `Applet`? | `install`, `register`, `select`, `process`, `deselect`, `getShareableInterfaceObject` |
| 16 | `setIncomingAndReceive`, `setOutgoing`, `sendBytes`, `sendBytesLong`, `setOutgoingAndSend`? | Nhận đợt đầu / chuyển chiều gửi / gửi từ buffer / gửi từ mảng khác / gộp ba bước |
| 17 | `arrayCopy` khác `arrayCopyNonAtomic`? | Bản đầu nguyên tử khi đích bền; bản sau không |

Chi tiết: [luồng dữ liệu](luong-du-lieu.md).

## 2. Bài 1

| # | Câu hỏi | Trả lời ngắn |
|---|---|---|
| 1 | APDU gồm gì, `Lc`/`Le` để làm gì? | Lc: byte gửi xuống; Le: byte muốn nhận |
| 2 | Vì sao SELECT phải `return` sớm? | JCRE đưa cả SELECT vào `process`; không bỏ qua sẽ trả `6D 00` |
| 3 | `register()` khác `register(bArray, …)`? | AID mặc định trong CAP / AID chỉ định |
| 4 | Vì sao ép `(short)` khắp nơi? | Phép trung gian ra `int` |

Thêm 10 câu: [TH1 mục 9](th1.md#9-câu-hỏi-vấn-đáp).

## 3. Bài 2

| # | Câu hỏi | Trả lời ngắn |
|---|---|---|
| BT1-a | `wrongTryCounter` loại gì, vì sao? | Bền; để RAM thì reset là dò PIN vô hạn |
| BT1-b | Reset giữa chừng có mất? | Không; nằm EEPROM, ghi một trường là nguyên tử |
| BT2 | `tempBuffer` loại gì? | Mảng tạm `CLEAR_ON_DESELECT`; độ dài cũng để RAM |
| 1 | `CLEAR_ON_RESET` khác `CLEAR_ON_DESELECT`? | Xóa khi reset / xóa khi bỏ chọn và reset; DESELECT có thêm tường lửa |
| 2 | Vì sao cờ "đã nhập PIN" là tạm? | Rút thẻ, đổi applet là mất quyền |
| 3 | `OwnerPIN` khác tự viết? | Bộ đếm, cờ do JCRE quản; bộ đếm không bị giao dịch hoàn tác |
| 4 | Vì sao tăng bộ đếm trước khi so? | Rút thẻ lúc biết sai cũng không né được lần đếm |
| 5 | Thí nghiệm TH2 cho gì? | Bỏ chọn: mảng DESELECT về 0; reset: cả hai về 0; bền giữ nguyên |

Chi tiết: [TH2](th2.md).

## 4. Bài 3

| # | Câu hỏi | Trả lời ngắn |
|---|---|---|
| 1 | Thao tác nguyên tử? | Một lần ghi bền: xong hẳn hoặc giữ giá trị cũ |
| 2 | Giao dịch dùng làm gì, ba phương thức? | Gom nhiều cập nhật; `begin`, `commit`, `abort` |
| 3 | Biến cục bộ, mảng tạm khi abort? | Giữ giá trị mới |
| 4 | Rút thẻ sau khi trừ tiền, trước khi ghi nhật ký? | Cùng giao dịch nên được hoàn tác |
| 5 | Thẻ đã trừ tiền, máy tính chưa nhận biên nhận? | Giao dịch đã commit. Phần mềm **chỉ phát hiện** ("Thẻ có N giao dịch chưa ghi sổ"), không tự bổ sung vì nhật ký thẻ không có chữ ký |
| 6 | Vì sao `arrayCopy` khi ghi nhật ký? | Cần nguyên tử trong giao dịch |
| 7 | Vì sao không dùng `int` cho tiền? | Không phải thẻ nào cũng có |
| 8 | Đề tài dùng giao dịch ở đâu? | Nạp, trả, điểm danh; đổi PIN, mở bằng PUK |

> Câu 5: ghi chú [th/th3](../../th/th3-giao-dich.md) viết "bổ sung (đối soát)", **không khớp** mã hiện tại. Trả lời theo bảng trên.

Chi tiết: [TH3](th3.md).

## 5. Bài 4

### 5.1 Câu hỏi bạn gửi

| # | Câu hỏi | Trả lời ngắn |
|---|---|---|
| 1 | `install` để làm gì? | Tạo thể hiện applet; JCRE gọi ở bước cuối cài đặt |
| 2 | Khởi tạo và đăng ký với hệ điều hành thế nào? | `install` → `new` → hàm tạo → `register` |
| 3 | Đối tượng tạo lúc nào, tốt nhất lúc nào? | Lúc nào cũng được; nên trong hàm tạo |
| 4 | Hàm tạo có tham số gì? | `bArray, bOffset, bLength` = `Li AID Lc ctrl La data` |
| 5 | `register` để làm gì? | Lưu tham chiếu vào JCRE và gán AID |
| 6 | Mấy phương thức `register`? | Hai |
| 7 | Vì sao có AID khác ban đầu? | Instance AID cấp lúc cài để định danh duy nhất |
| 8 | Vì sao cần thay đổi trong quá trình hoạt động? | Dữ liệu đổi qua APDU; AID và cấu hình đổi lúc cài |

Chi tiết: [TH4-cau-hoi-install.md](../TH4-cau-hoi-install.md).

### 5.2 Trong ghi chú nhóm

| # | Câu hỏi | Trả lời ngắn |
|---|---|---|
| 1 | Vì sao tạo hết đối tượng trong hàm tạo? | Thiếu bộ nhớ thì lỗi lúc cài, được dọn sạch |
| 2 | Tham số cài đặt gồm gì? | Như câu 4 ở trên |
| 3 | Vì sao `register` đặt cuối? | Lỗi trước đó thì applet chưa đăng ký |
| 4 | Ai khởi tạo thẻ, hai lần được không? | Chỉ quản trị; không (`69 85`) |
| 5 | Rút thẻ giữa lúc khởi tạo? | Trạng thái ghi cuối nên vẫn thẻ trắng |

Mã và cấu hình: [TH4](th4.md).

## 6. Bài 5

### 6.1 Câu hỏi bạn gửi

| # | Câu hỏi / ý | Trả lời ngắn |
|---|---|---|
| 1 | Bối cảnh là gì? | Vùng đối tượng được tường lửa bảo vệ; theo package |
| 2 | Cùng package chung bối cảnh, không tường lửa | Đúng |
| 3 | Tường lửa ngăn các nhóm khác nhau | Đúng ý |
| 4 | Khác bối cảnh phải qua Shareable | Đúng cho gọi phương thức; còn mảng toàn cục, điểm nhập JCRE, static |
| 5 | Có 5, đặt 3 vào giao diện | Đúng |
| 6 | 5 phương thức chia a 2, b 3, c 1 | Tổng 6 nên có trùng; mỗi khách một đối tượng riêng + kiểm AID |
| 7 | Cứ chia sẻ là nhận được? | Không |

Chi tiết: [TH5-cau-hoi-chia-se.md](../TH5-cau-hoi-chia-se.md).

### 6.2 Câu hỏi mới trong ghi chú của bạn

**a. "Có phải cứ chia sẻ là nhận được? Cần biết tôi là ai, kiểm AID, mỗi lần dùng phương thức lại kiểm AID."**
Đúng. Hai chỗ kiểm: `getShareableInterfaceObject` kiểm AID người xin; **mỗi** phương thức chia sẻ kiểm `JCSystem.getPreviousContextAID()`. Chỗ thứ hai cần vì tham chiếu có thể bị chuyển cho applet khác.

**b. "Làm thế nào chỉ chia sẻ cho a?"**
So `clientAID.equals(AID_A, 0, độ dài)` rồi mới trả đối tượng, ngược lại trả `null`; trả **đối tượng riêng** cho a; kiểm lại trong mỗi phương thức. Dùng `equals`, không dùng `partialEquals` (chỉ so phần đầu).

**c. "Xác thực bổ sung thử thách và phản hồi mới cấp quyền."**
Chủ sinh số ngẫu nhiên → khách tính AES(khóa chung, số) → chủ tự tính và so → đúng thì cấp quyền **một lần**. Thử thách dùng một lần nên phát lại không được. Mã chạy được: [chiaSeAnToan](../src/chiaSeAnToan/DichVu.java).

**d. "Quan trọng khi nhiều applet có nhiều dữ liệu riêng tư nhưng chia sẻ ít."**
Nguyên tắc quyền tối thiểu: giao diện chỉ chứa phương thức cần thiết, mỗi loại khách một giao diện và một đối tượng, chỉ trả đúng dữ liệu cần (không trả tham chiếu mảng).

**e. "Bên chia sẻ và bên nhận phải làm gì?"**
Bảng việc của hai bên: [TH5 mục 3.5](th5.md#35-mỗi-bên-phải-làm-gì).

**f. "Dữ liệu phải mã hóa trên thẻ, xác thực thẻ làm thế nào" (buổi TH6).**
Mã hóa: AES với khóa được bảo vệ bằng PIN; xác thực: thử thách – phản hồi RSA với khóa công khai lưu sẵn. [TH6 mục 5–6](th6.md#5-xác-thực-thẻ-bằng-thử-thách--phản-hồi-slide-10).

### 6.3 Trong ghi chú nhóm

| # | Câu hỏi | Trả lời ngắn |
|---|---|---|
| 1 | Tường lửa applet là gì, vì sao cần? | Chặn truy cập đối tượng giữa các bối cảnh, để applet không đọc dữ liệu nhau |
| 2 | Muốn applet khác dùng dữ liệu của mình? | Giao diện `Shareable`, ghi đè `getShareableInterfaceObject`, khách xin SIO |
| 3 | Sao đề tài chỉ một applet? | Một phiên PIN chung, không phải mở lỗ qua tường lửa |
| 4 | SIO là gì? | Đối tượng cài giao diện chia sẻ; bối cảnh khác chỉ thấy phương thức của giao diện |

## 7. Bài 6

| # | Câu hỏi | Trả lời ngắn |
|---|---|---|
| 1 | Tính chất hàm băm? | Một chiều, đầu ra cố định, có thể va chạm |
| 2 | Đối xứng khác khóa công khai? | Một khóa / cặp khóa; AES, DES / RSA, ECC |
| 3 | Quy trình chữ ký số? | Băm → ký bằng khóa bí mật; kiểm bằng khóa công khai |
| 4 | Challenge–response? | Số ngẫu nhiên → thẻ ký → máy tính kiểm bằng khóa đã lưu |
| 5 | AES khối, khóa, đệm? | 128 bit; 128/192/256; đệm `80 00…` |
| 6 | Vì sao băm PIN mà không mã hóa? | Chỉ cần so sánh |
| 7 | Salt để làm gì? | Cùng PIN ra giá trị khác nhau |
| 8 | Khóa AES lấy từ đâu, đổi PIN thì sao? | Khóa dữ liệu ngẫu nhiên, bọc bằng khóa từ PIN; đổi PIN chỉ bọc lại |
| 9 | Vì sao CBC mà không ECB? | ECB lộ cấu trúc |
| 10 | IV là gì, để đâu? | Vector khởi tạo, không cần bí mật, lưu cạnh dữ liệu |
| 11 | Khóa bí mật RSA ở đâu, lấy ra được? | Chỉ trong thẻ, không lệnh nào đọc |
| 12 | Vì sao không lấy khóa công khai từ thẻ khi xác thực? | Thẻ giả tự đưa khóa của nó |
| 13 | Chữ ký bảo đảm gì? | Toàn vẹn, nguồn gốc, chống chối bỏ |
| 14 | Vì sao thử thách phải ngẫu nhiên? | Chống phát lại |
| 15 | Vì sao SHA-1 trong chữ ký? | 2.2.2 chỉ có `ALG_RSA_SHA_PKCS1` |
| 16 | Vì sao RSA 1024? | Nhanh trên giả lập; đổi được sang 2048 nếu thẻ hỗ trợ |

Chi tiết: [TH6](th6.md).

## 8. APDU mở rộng và kết nối

| # | Câu hỏi | Trả lời ngắn |
|---|---|---|
| 1 | APDU mở rộng khác gì, cần gì? | Lc/Le dài (ISO tới 65 535; Java Card giới hạn theo `short`); `implements ExtendedLength`, ≥ 2.2.2, nên T = 1 |
| 2 | `setIncomingAndReceive` khác `receiveBytes`? | Nhận đợt đầu / đợt tiếp |
| 3 | Sao đề tài không dùng APDU mở rộng cho ảnh? | Chia khối chạy được trên mọi thẻ |
| 4 | Ảnh tốn bao nhiêu bộ nhớ? | Ô ảnh 7 680 byte EEPROM |
| 5 | Phần mềm nói chuyện với thẻ qua gì? | PC/SC, `javax.smartcardio` |
| 6 | CSDL lưu gì, sao không lưu PIN? | Hồ sơ, khóa công khai, biên nhận; PIN chỉ thẻ biết |
| 7 | CSDL đã có thông tin rõ, mã hóa trên thẻ làm gì? | Bảo vệ khi mất thẻ |
| 8 | Sửa CSDL để tăng số dư? | Số dư thật ở trên thẻ; CSDL chỉ đối soát |
| 9 | Vì sao cột chữ là `NVARCHAR`? | Lưu tiếng Việt đúng |
| 10 | Thẻ giả thì phát hiện thế nào? | Chữ ký thử thách không khớp khóa đã lưu |

Chi tiết: [APDU mở rộng](apdu-mo-rong.md), [Kết nối](ket-noi.md).

## 9. Câu hỏi về chính đề tài

Xem [05-on-tap-van-dap.md](../../05-on-tap-van-dap.md) mục K, L, M, N (câu 39–74): thiết kế PIN, khóa, giao dịch, phân quyền, điểm yếu đã biết.
