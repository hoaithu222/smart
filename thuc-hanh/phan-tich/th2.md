# TH2 – Đối tượng bền và đối tượng tạm thời: phân tích chi tiết

> Đề: [th2-cac-doi-tuong-trong-java-card.md](../../smart/docs/th2-cac-doi-tuong-trong-java-card.md) và [bài tập 2](../../smart/docs/bai-tap-2.md). Lý thuyết bộ nhớ: [bài 1 slide 21–24](../../smart/docs/bai1-tong-quan-the-thong-minh.md).
> Mã: [src/bai2_Object](../src/bai2_Object/). Kết quả từ [th2.apdu](../kich-ban/th2.apdu) và [tn2.apdu](../kich-ban/thi-nghiem/tn2.apdu). **[Thêm]** = ngoài tài liệu.

## 1. Kiến thức nền

### 1.1 Ba loại bộ nhớ (bài 1 slide 21–24)

| Bộ nhớ | Chứa gì | Mất điện | Ghi |
|---|---|---|---|
| ROM | Hệ điều hành, máy ảo, API | Giữ | Không ghi được |
| EEPROM | Applet, đối tượng bền | Giữ | Chậm hơn RAM khoảng 1000 lần, giới hạn số lần ghi **[Thêm]** |
| RAM | Đối tượng tạm, ngăn xếp, bộ đệm APDU | **Mất** | Nhanh |

### 1.2 Đối tượng bền (mục 2.1)

- Tạo bằng `new`; giữ giá trị qua các phiên.
- Cập nhật **một trường** là nguyên tử.
- Không còn tham chiếu thì không truy cập được nữa.

### 1.3 Đối tượng tạm thời (mục 2.2)

- Chỉ **mảng** mới tạm được: `boolean[]`, `byte[]`, `short[]`, `Object[]`. Một trường đơn (ví dụ `byte x`) **không** thể là tạm.
- "Tạm" là **nội dung**: tham chiếu giữ trong trường bền, tạo một lần lúc cài.
- Cập nhật không nguyên tử, không tham gia giao dịch.

| Kiểu | Xóa khi bỏ chọn applet | Xóa khi reset | Thêm |
|---|---|---|---|
| `CLEAR_ON_DESELECT` | Có | Có | Tường lửa bảo vệ thêm: chỉ truy cập được khi applet thuộc bối cảnh đó đang được chọn |
| `CLEAR_ON_RESET` | Không | Có | Dùng được qua lần chuyển applet |

```java
byte[] b = JCSystem.makeTransientByteArray((short) 2, JCSystem.CLEAR_ON_DESELECT);
byte kieu = JCSystem.isTransient(b);   // CLEAR_ON_DESELECT, CLEAR_ON_RESET hoặc NOT_A_TRANSIENT_OBJECT
```

**[Thêm]** Bỏ chọn gồm cả trường hợp **chọn lại chính applet đó**.

## 2. Ví dụ của đề – [Applet1](../src/bai2_Object/Applet1.java#L22)

| Biến | Loại | Nằm ở |
|---|---|---|
| `x` | trường `byte` | EEPROM |
| `buffer` | `new byte[2]` | EEPROM |
| `buffer1` | `CLEAR_ON_DESELECT` | RAM |
| `buffer2` | `CLEAR_ON_RESET` | RAM |

Đề tạo các mảng trong `install` **sau** `register` và để `static`. Chạy được, nhưng mục 4.1 khuyên cấp phát trong hàm tạo ([TH4](th4.md)); biến `static` thì hai thể hiện dùng chung.

`INS_SEND` ghép 7 byte bằng 4 lần gửi ([dòng 48–54](../src/bai2_Object/Applet1.java#L48)): `setOutgoingLength(7)`, rồi `x` (1) + `buffer` (2) + `buffer1` (2) + `buffer2` (2).

### Kịch bản và kết quả

| Bước | Lệnh | x, buffer, buffer1, buffer2 |
|---|---|---|
| Vừa cài | `00 00 00 00` | `00 \| 00 00 \| 00 00 \| 00 00` |
| Cập nhật rồi đọc | `00 01 00 00`, `00 00 00 00` | `09 \| 01 02 \| 03 04 \| 05 06` |
| Bỏ chọn, chọn lại | | `09 \| 01 02 \| 00 00 \| 05 06` |
| Reset | | `09 \| 01 02 \| 00 00 \| 00 00` |

Trùng khớp kết quả in trong tài liệu. Thí nghiệm thêm: **chọn lại chính applet** (không đổi sang applet khác) cũng xóa `buffer1`.

## 3. Bài tập 2, bài 1 – [Applet2](../src/bai2_Object/Applet2.java#L75)

| Lệnh | Ý nghĩa | Trả về |
|---|---|---|
| `00 20 00 00 Lc <PIN>` | Kiểm PIN | `90 00` đúng, `63 00` sai |
| `00 30 00 00 02` | Đọc bộ đếm | `[wrongTryCounter][ramCounter]` |

Tham số cài đặt: PIN 4–8 byte; bỏ trống thì PIN `1234`.

### Mã quan trọng ([dòng 83–93](../src/bai2_Object/Applet2.java#L83))

```java
if (wrongTryCounter < DEM_TOI_DA) wrongTryCounter++;   // 1. tăng TRƯỚC
if (n != pin.length || Util.arrayCompare(...) != 0)     // 2. so sánh
    ISOException.throwIt(SW_SAI_PIN);                   // 3. sai: giữ bộ đếm
wrongTryCounter = 0;                                    // 4. đúng: về 0
```

- **Vì sao tăng trước khi so?** Nếu so trước, kẻ tấn công rút nguồn ngay khi biết kết quả sai, bộ đếm không kịp tăng. Quan sát từ ngoài vẫn đúng đề: sai thì tăng 1, đúng thì về 0.
- **Vì sao kiểm độ dài trước `arrayCompare`?** Tránh đọc vượt mảng PIN.
- **Chặn ở 127:** `byte` tăng quá 127 thành −128. Thí nghiệm sai 130 lần: bộ đếm đứng ở `7F`.
- Đề chỉ yêu cầu đếm; thẻ thật giới hạn số lần (3) bằng `OwnerPIN` **[Thêm]**.

### Trả lời 2 câu của đề

1. **`wrongTryCounter` nên là loại nào? Vì sao?** Đối tượng **bền** (trường của applet). Nếu đặt trong RAM, reset thẻ là bộ đếm về 0, dò PIN vô hạn.
2. **Reset giữa chừng biến có mất không?** **Không**, vì nằm trong EEPROM và cập nhật một trường là nguyên tử.

Chứng minh bằng lệnh (`ramCounter` chỉ để so sánh):

| Bước | `00 30` trả |
|---|---|
| Vừa cài | `00 00` |
| Sai 2 lần | `02 02` |
| Reset thẻ | `02 00` ← bền còn, RAM mất |
| Nhập đúng | `00 00` |

## 4. Bài tập 2, bài 2 – [Applet3](../src/bai2_Object/Applet3.java#L45)

| Lệnh | Việc |
|---|---|
| `00 01 00 00 Lc <dữ liệu>` | Nhập vào `tempBuffer` (1–128 byte) |
| `00 02 00 00 00` | In ra rồi **giải phóng** bộ đệm |

**Câu hỏi:** `tempBuffer` nên là loại nào? **Mảng tạm `CLEAR_ON_DESELECT`**:

- Chỉ cần dữ liệu trong lúc applet đang được chọn.
- JCRE tự xóa khi bỏ chọn hoặc reset; applet khác không đọc được.
- Ghi RAM nhanh, không hao mòn EEPROM.

Điểm hay bị hỏi: **độ dài cũng phải ở RAM** (`short[1]`). Nếu để `short` bền, sau khi bỏ chọn độ dài vẫn khác 0 nhưng bộ đệm đã toàn 0.

"Giải phóng" trên Java Card = ghi 0 (`arrayFillNonAtomic`) và đặt độ dài 0; không có lệnh `delete`. Thứ tự trong mã: chép sang bộ đệm APDU → xóa bộ đệm → gửi.

## 5. Luồng dữ liệu

```text
Applet3:  APDU ─► buffer APDU ─arrayCopyNonAtomic─► tempBuffer (RAM)
          APDU ◄─ buffer APDU ◄─arrayCopyNonAtomic── tempBuffer ─► rồi ghi 0
Applet2:  APDU ─► buffer APDU ─arrayCompare─► pin (EEPROM) ; wrongTryCounter (EEPROM) ±
```

## 6. Cấu hình

| Mục | Giá trị |
|---|---|
| Package AID | `11 22 33 44 55 02` (đúng Hình 2.4 trong đề) |
| Applet AID | `…02 01`, `…02 02`, `…02 03` |
| Tham số | Applet2: PIN tùy chọn, ví dụ `36 36 36 36 36 36` |

**PyApduTool (Hình 2.2–2.5):** trong JCIDE bật `Tool → IDE Options → Run/Debug → Enable PCSC Interface`; `Run`; PyApduTool tab **Reader** chọn `JAVACOS Virtual Contact Reader 0`, T = 0, **Connect** (ATR `3B 70 13 00 00`); tab **Manager** chọn applet; tab **Apdu** gửi lệnh.

- **Kịch bản 1 (bỏ chọn):** trong tab Manager chọn applet khác rồi chọn lại.
- **Kịch bản 2 (reset):** nút **Disconnect** (reset cứng) hoặc **Reset** (reset mềm).

Công cụ: `.\chay.cmd kich-ban\th2.apdu` (22 mục); trong shell dùng `/deselect`, `/reset`.

> Thẻ giả lập jCardSim không tự xóa bộ nhớ tạm; công cụ trong thư mục này làm thay theo đúng đặc tả. **Bằng chứng cuối cùng vẫn nên chạy trên JCIDE.**

## 7. Thí nghiệm đã chạy

| Thử | Kết quả |
|---|---|
| Chọn lại chính applet | `09 01 02 00 00 05 06` |
| Sang applet khác rồi về | giống trên |
| Sai PIN 130 lần | bộ đếm `7F 7F`; nhập đúng về `00 00` |
| Nhập dữ liệu rồi reset | in: `69 85` |
| Nhập 2 lần liên tiếp | lần sau **thay** lần trước |
| Nhập 129 byte | `67 00` |

## 8. Câu hỏi vấn đáp

1. **Phân biệt đối tượng bền và tạm?** Bảng mục 1.
2. **Biến `byte x` có thể là tạm không?** Không, chỉ mảng.
3. **Tham chiếu tới mảng tạm nằm ở đâu?** Trong trường bền; nội dung ở RAM.
4. **`CLEAR_ON_DESELECT` khác `CLEAR_ON_RESET`?** Thời điểm xóa và bảo vệ tường lửa.
5. **Vì sao không để mọi thứ trong EEPROM?** Ghi chậm, hao mòn, dữ liệu nhạy cảm tồn tại lâu.
6. **Vì sao cờ "đã nhập PIN" phải tạm?** Rút thẻ hay đổi applet phải mất quyền.
7. **Khi mất điện, `buffer` (bền) đang ghi dở thì sao?** Một phần tử đang ghi được khôi phục giá trị cũ; nhiều phần tử cần giao dịch ([TH3](th3.md)).
8. **`OwnerPIN` hơn tự viết ở đâu?** **[Thêm]** Có sẵn giới hạn số lần, cờ xác thực tạm, bộ đếm không bị giao dịch hoàn tác.
