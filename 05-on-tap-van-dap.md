# 05 · Ôn vấn đáp (4/10 điểm – trả lời kém là trượt)

> Đáp án viết ngắn để **nói được thành lời**. Mỗi câu ghi nguồn để đọc lại. Câu hỏi về chính đề tài (nhóm K, L) là chỗ dễ bị hỏi nhất – ai trong nhóm cũng phải trả lời được, không chỉ người viết phần đó.
> Cách ôn: mỗi buổi họp nhóm, một người hỏi ngẫu nhiên 5 câu, người khác trả lời không nhìn notes; câu nào vấp thì đánh dấu ☆ ở đầu dòng.

## A. Thẻ thông minh ([bai1](../bai1-tong-quan-the-thong-minh.md))

1. **Thẻ thông minh khác thẻ từ ở đâu?** – Có chip xử lý được dữ liệu tại chỗ (mã hóa, kiểm PIN ngay trên thẻ), không chỉ lưu.
2. **Phân loại?** – Theo chip: thẻ chip nhớ (chỉ lưu, phụ thuộc đầu đọc, dễ làm giả) / thẻ vi xử lý. Theo cách đọc: tiếp xúc, không tiếp xúc, giao diện kép.
3. **Các loại bộ nhớ, mỗi loại chứa gì?** – ROM: hệ điều hành, JCVM, API (ghi lúc sản xuất). EEPROM: applet và dữ liệu bền, giữ khi mất điện, ghi chậm, giới hạn số lần ghi. RAM: ngăn xếp, đối tượng tạm, mất khi mất điện.
4. **APDU lệnh / phản hồi gồm gì?** – `CLA INS P1 P2 [Lc Data] [Le]` / `[Data] SW1 SW2`.
5. **`90 00`, `6A 82`, `6D 00`, `6E 00`, `69 82`, `63 Cx` nghĩa là gì?** – thành công / không tìm thấy (applet) / INS không hỗ trợ / CLA không hỗ trợ / chưa thỏa điều kiện bảo mật / sai mã, còn x lần.
6. **T=0 và T=1?** – T=0 truyền theo byte (dữ liệu trả về lấy bằng GET RESPONSE `61 xx`), T=1 truyền theo khối.

## B. Công nghệ Java Card ([bai2](../bai2-gioi-thieu-chung-ve-java-card.md))

7. **Máy ảo Java Card gồm mấy phần?** – Ngoài thẻ: bộ chuyển đổi (class → CAP + export). Trên thẻ: trình thông dịch bytecode.
8. **Các bước làm ra một applet?** – Viết Java → `javac` ra class → bộ chuyển đổi ra CAP → kiểm tra (verifier) → bộ cài ngoài thẻ gửi CAP qua APDU → bộ cài trên thẻ cài đặt.
9. **JCRE là gì? Vòng đời?** – Môi trường thực thi trên thẻ (JCVM + API + dịch vụ hệ thống). Sinh ra lúc khởi tạo thẻ, sống bằng vòng đời của thẻ; mất điện thì VM dừng, có điện lại chạy tiếp từ đầu (không khôi phục luồng đang chạy).
10. **Ba tính năng thực thi JCRE bổ sung?** – Đối tượng bền và tạm; thao tác nguyên tử và giao dịch; tường lửa applet và cơ chế chia sẻ.
11. **AID là gì?** – Mã định danh ứng dụng 5–16 byte = RID 5 byte (nhà cung cấp) + PIX. Nhóm dùng `4B 4D 41 53 56 01 01`.

## C. Ngôn ngữ ([bai3](../bai3-ngon-ngu-java-trong-java-card.md))

12. **Kiểu dữ liệu được hỗ trợ?** – `byte`, `short`, `boolean`; `int` chỉ có trên một số thẻ. Không có `String`, `float`, `long`, mảng nhiều chiều, luồng, trình gom rác (`finalize`).
13. **Vì sao phải ép kiểu `(short)(a + b)`?** – Phép tính trung gian trong Java luôn cho `int`; thẻ không có `int` thì lỗi → phải ép về `short`/`byte`.
14. **Đề tài tính tiền bằng kiểu gì?** – `short`, đơn vị nghìn đồng, trần 30 000 (30 triệu).

## D. API ([bai4](../bai4-java-card-api.md))

15. **Các gói chính?** – `java.lang` (Object, Throwable, Exception), `javacard.framework` (Applet, APDU, ISO7816, JCSystem, Util, OwnerPIN, Shareable, AID…), `javacard.security` (khóa, băm, chữ ký, số ngẫu nhiên), `javacardx.crypto` (Cipher).
16. **Các phương thức của lớp Applet?** – `install` (static), `register`, `select`, `process` (abstract), `deselect`, `getShareableInterfaceObject`.
17. **`setIncomingAndReceive`, `setOutgoing`, `setOutgoingLength`, `sendBytes`, `sendBytesLong`, `setOutgoingAndSend`?** – nhận dữ liệu lần đầu / chuyển sang chiều gửi, lấy Le / đặt độ dài gửi / gửi từ bộ đệm APDU / gửi từ mảng khác / gộp 3 bước gửi.
18. **`Util.arrayCopy` khác `arrayCopyNonAtomic`?** – bản thường nguyên tử khi đích là mảng bền; NonAtomic không, nhanh hơn, dùng cho RAM.

## E. Đối tượng bền / tạm ([th2](../th2-cac-doi-tuong-trong-java-card.md), [bước 2](th/th2-doi-tuong-ben-va-tam.md))

19. **Đối tượng bền và tạm khác nhau thế nào?** – bảng mục 1 bước 2.
20. **`CLEAR_ON_RESET` khác `CLEAR_ON_DESELECT`?** – xóa khi reset thẻ / xóa khi applet bị bỏ chọn (và khi reset); DESELECT có thêm bảo vệ tường lửa.
21. **Bộ đếm sai PIN là loại gì? Reset có mất không?** – Bền; không mất. Để tạm thì rút thẻ là bộ đếm về 0 → dò PIN vô hạn.
22. **Vì sao tăng bộ đếm trước khi so sánh PIN?** – Chống rút thẻ ngay lúc biết sai để né lần đếm; `OwnerPIN` làm sẵn.
23. **Thí nghiệm TH2 cho kết quả gì?** – Bỏ chọn: mảng DESELECT về 0. Reset: cả DESELECT và RESET về 0. Biến/mảng bền giữ nguyên.

## F. Giao dịch ([th3](../th3-thao-tac-nguyen-to-va-giao-dich.md), [bước 3](th/th3-giao-dich.md))

24. **Thao tác nguyên tử là gì?** – Cập nhật một trường bền hoặc thành công hẳn, hoặc về giá trị cũ khi mất điện.
25. **Giao dịch dùng để làm gì? Ba phương thức?** – Gom nhiều cập nhật thành một khối nguyên tử; `beginTransaction`, `commitTransaction`, `abortTransaction`.
26. **Biến cục bộ, mảng tạm trong giao dịch bị abort thì sao?** – Giữ giá trị mới, không hoàn tác.
27. **Trong đề tài chỗ nào dùng giao dịch?** – Nạp/thanh toán/điểm danh (số dư + bộ đếm + nhật ký); đổi PIN và mở bằng PUK (DEK bọc lại + băm PIN mới).

## G. Install ([th4](../th4-phuong-thuc-install.md), [bước 4](th/th4-install-khoi-tao-the.md))

28. **`install` làm gì? Giống gì trong Java?** – Tạo thể hiện applet, đăng ký với JCRE; giống `main`.
29. **Vì sao cấp phát hết trong hàm tạo?** – Thiếu bộ nhớ thì hỏng ngay lúc cài, JCRE dọn sạch; lúc chạy không bao giờ hết bộ nhớ.
30. **Định dạng tham số cài đặt?** – `Li AID Lc ctrl La data`. Đề tài: `La data` = PIN quản trị.

## H. Chia sẻ ([th5](../th5-chia-se-doi-tuong-giua-cac-applet.md), [bước 5](th/th5-chia-se-applet.md))

31. **Tường lửa applet?** – Mỗi applet một ngữ cảnh, không truy cập được đối tượng của applet khác.
32. **Chia sẻ thế nào?** – Giao diện kế thừa `Shareable`; chủ ghi đè `getShareableInterfaceObject`; khách `lookupAID` + `getAppletShareableInterfaceObject`, ép về kiểu giao diện.
33. **Sao đề tài chỉ 1 applet?** – Bước 5 mục 2.

## I. Mật mã ([th6](../th6-mat-ma-trong-java-card.md), [bước 6](th/th6-mat-ma.md))

34. **Tính chất hàm băm?** – Một chiều; đầu ra độ dài cố định; đầu vào khác → đầu ra khác (vẫn có thể va chạm).
35. **Mật mã đối xứng và khóa công khai khác gì? Ví dụ?** – Một khóa cho cả mã và giải (AES, DES) / cặp khóa công khai–bí mật (RSA, ECC); ứng dụng: mã hóa, chữ ký số, xác thực.
36. **Quy trình chữ ký số?** – Băm thông điệp → mã bản băm bằng khóa bí mật = chữ ký. Kiểm: giải chữ ký bằng khóa công khai, băm lại thông điệp, so hai bản băm.
37. **Challenge–response?** – Máy chủ gửi số ngẫu nhiên, thẻ ký bằng khóa bí mật, máy chủ kiểm bằng khóa công khai đã lưu. Số mới mỗi lần nên không phát lại được.
38. **AES: khối, khóa, đệm?** – Khối 128 bit; khóa 128/192/256; khối cuối thiếu thì đệm. Java Card 2.2.2 chỉ có AES NOPAD → máy tính đệm `80 00…`.

## K. Thiết kế của đề tài (chắc chắn bị hỏi)

39. **PIN lưu thế nào? Có ai đọc được PIN không?** – `SHA-256('P' ‖ salt ‖ PIN)` trong `OwnerPIN`; không ai đọc được PIN, chỉ so sánh.
40. **Khóa AES lấy từ đâu?** – DEK ngẫu nhiên mã dữ liệu; DEK được bọc bằng KEK = 16 byte đầu `SHA-256('K' ‖ salt ‖ PIN)`. Không có PIN thì không mở được DEK.
41. **Đổi PIN thì dữ liệu đã mã hóa thế nào?** – Không mã lại; chỉ bọc lại DEK bằng KEK mới, trong một giao dịch.
42. **Quên PIN?** – PUK (in cho sinh viên lúc phát hành) mở bản DEK bọc bằng KEK(PUK) → đặt PIN mới, dữ liệu còn nguyên. Sai PUK 5 lần → khóa thẻ, phải phát hành lại.
43. **Vì sao giá trị kiểm PIN và KEK dùng tiền tố khác nhau (`'P'`, `'K'`)?** – Nếu giống nhau, ai đọc được giá trị kiểm PIN là có luôn khóa dữ liệu.
44. **Cặp khóa RSA sinh ở đâu? Khóa bí mật có rời thẻ không?** – Trên thẻ, lúc phát hành; không có lệnh nào đọc khóa bí mật.
45. **Phần mềm biết thẻ thật bằng cách nào?** – Challenge–response, kiểm bằng khóa công khai đã lưu CSDL lúc phát hành (không tin khóa thẻ tự đưa).
46. **"Mỗi lần nạp/thanh toán xác thực bằng PIN và khóa công khai" làm thế nào?** – PIN nằm trong chính lệnh `51/52`; thẻ ký biên nhận; phần mềm kiểm chữ ký + bộ đếm rồi mới ghi CSDL.
47. **Chống phát lại?** – Challenge ngẫu nhiên khi xác thực; nonce + thời gian + bộ đếm trong biên nhận; CSDL `UNIQUE(ma_sv, bo_dem)`.
48. **Rút thẻ giữa lúc thanh toán?** – Giao dịch nguyên tử: hoặc trừ tiền + bộ đếm + nhật ký, hoặc không gì cả. Nếu thẻ đã commit mà phần mềm chưa nhận biên nhận → đối soát bằng nhật ký trên thẻ.
49. **Vì sao mã SV không mã hóa?** – In trên mặt thẻ; phần mềm cần nó để tra khóa công khai **trước** khi có PIN.
50. **Phân quyền làm ở đâu?** – Trên thẻ: INFO/PHOTO cần PIN quản trị, CONTACT chỉ cần PIN; khởi tạo, khóa thẻ cần PIN quản trị. Giao diện chỉ ẩn nút cho tiện.
51. **Điểm danh không cần PIN thì có an toàn không?** – Mục đích là chứng minh **có thẻ thật** tại máy điểm danh (chữ ký); PIN làm chậm hàng người. Muốn chặt hơn thì bật PIN cho phòng thi.
52. **Ảnh lưu thế nào?** – JPEG ≤ 7,6 KB, đệm, chia khối 240 byte, mã AES từng khối trên thẻ.
53. **Hạn chế của hệ thống?** – [03 mục 5.6](03-thiet-ke.md#56-giới-hạn-nói-thẳng-khi-bị-hỏi): PIN ngắn nếu đọc được EEPROM; thời gian do máy tính cấp; SHA-1 trong chữ ký do giới hạn 2.2.2.

## L. Câu hỏi "nếu…" và chỉ vào mã

54. **Chỉ dòng nào giới hạn 3 lần sai.** – `new OwnerPIN(PIN_TRY_LIMIT, HASH_MAX)`, `PIN_TRY_LIMIT = 3`.
55. **Chỉ dòng nào mã hóa thông tin.** – `aesCbc.init(dek, Cipher.MODE_ENCRYPT, …)` + `doFinal` trong `writeObj`.
56. **Muốn đổi RSA 2048 sửa đâu?** – `KeyBuilder.LENGTH_RSA_1024` → `LENGTH_RSA_2048` (nếu thẻ hỗ trợ); chữ ký 256 byte vẫn vừa APDU.
57. **Muốn tăng số lần sai PIN lên 5?** – Hằng `PIN_TRY_LIMIT`; cài lại applet.
58. **Nếu bỏ `JCSystem.beginTransaction()` trong `transfer` thì sao?** – Rút thẻ giữa chừng có thể trừ tiền mà không có nhật ký / bộ đếm không tăng → biên nhận và sổ sách lệch.
59. **Nếu `tmp` là `CLEAR_ON_DESELECT`?** – Hàm tạo dùng `tmp` lúc cài đặt (applet chưa được chọn) → lỗi bảo mật của JCRE.
60. **Sao `deselect()` gọi `pin.reset()`?** – Chọn applet khác là kết thúc phiên; cờ đã nhập PIN không được sống tiếp.

## M. Phần mềm máy tính và CSDL ([bước 8](th/th8-ket-noi-phan-mem-csdl.md))

61. **Cắm thẻ vào thì phần mềm làm gì?** – SELECT applet → GET_STATUS (mã SV, lượt PIN) → lấy khóa công khai của mã SV đó trong CSDL → gửi challenge, thẻ ký, kiểm chữ ký. Đạt thì chip "Thẻ thật", không đạt thì mọi màn đều chặn.
62. **Vì sao dùng SQL Server mà vẫn chạy được SQLite?** – Câu SQL viết theo cú pháp chung (không `TOP`/`LIMIT`, giới hạn bằng `setMaxRows`); SQLite chỉ dùng cho bộ test tự động.
63. **Database chưa có thì sao?** – `DatabaseBootstrap` bắt lỗi 4060, nối vào `master`, `CREATE DATABASE`, rồi chạy file schema (mọi câu "chưa có mới tạo").
64. **Mật khẩu cán bộ lưu thế nào?** – `SHA-256(salt + mật khẩu)` trong bảng `nguoi_dung`, so sánh bằng `MessageDigest.isEqual`.
65. **Sao giao diện không bị "đơ" khi sinh khóa RSA?** – Lệnh chạy trong `SwingWorker` (lớp `Task`), luồng giao diện chỉ nhận kết quả.
66. **Chế độ `jcardsim` để làm gì? Báo cáo có dùng không?** – Thẻ giả lập chạy đúng `StudentCard` trong phần mềm để tự thử/kiểm thử khi chưa mở JCIDE. Báo cáo dùng JCIDE (`card.mode=pcsc`).
67. **Phân quyền ở phần mềm và ở thẻ khác nhau thế nào?** – Phần mềm chỉ ẩn màn hình theo vai trò cho gọn; quyền thật (ghi INFO/ảnh, khởi tạo, khóa thẻ) do thẻ kiểm bằng PIN quản trị.

## Nhật ký ôn

| Ngày | Ai hỏi | Ai trả lời | Câu vấp |
|---|---|---|---|
| | | | |
