# TH3 – Thao tác nguyên tử và giao dịch: phân tích chi tiết

> Đề: [th3-thao-tac-nguyen-to-va-giao-dich.md](../../smart/docs/th3-thao-tac-nguyen-to-va-giao-dich.md). API: [bài 4 slide 19, 22–23](../../smart/docs/bai4-java-card-api.md).
> Mã: [src/bai3_Transaction](../src/bai3_Transaction/). Kết quả từ [th3.apdu](../kich-ban/th3.apdu) và [tn3.apdu](../kich-ban/thi-nghiem/tn3.apdu). **[Thêm]** = ngoài tài liệu.
> **Lưu ý quan trọng:** jCardSim không tự hoàn tác giao dịch; công cụ ở đây mô phỏng việc hoàn tác. Kết quả cần xác nhận trên JCIDE.

## 1. Vấn đề cần giải quyết

Người dùng có thể rút thẻ bất cứ lúc nào (đề gọi là "xé thẻ"), thẻ mất điện giữa lúc đang ghi. Nếu một nghiệp vụ gồm nhiều lần ghi (trừ tiền, tăng bộ đếm, ghi nhật ký), dữ liệu có thể nửa cũ nửa mới.

## 2. Hai cấp bảo đảm

| Cấp | Bảo đảm | Phạm vi |
|---|---|---|
| Nguyên tử (mục 3.1) | Một trường hoặc một phần tử mảng bền: hoặc ghi xong, hoặc giữ giá trị cũ | Một lần ghi |
| Giao dịch (mục 3.2) | Cả nhóm cập nhật: tất cả hoặc không gì | Nhiều lần ghi |

```java
JCSystem.beginTransaction();
// các cập nhật vào bộ nhớ bền
JCSystem.commitTransaction();     // hoặc JCSystem.abortTransaction();
```

**Chỉ bộ nhớ bền tham gia.** Mảng tạm, biến cục bộ, tham số **không** được hoàn tác (mục 3.3).

### Kiến thức thêm **[Thêm]**

| Tình huống | Chuyện gì xảy ra |
|---|---|
| Mất điện trong giao dịch | Lần cấp điện sau, JCRE hoàn tác |
| `process` trả về khi giao dịch còn mở | JCRE tự hủy giao dịch |
| Gọi `beginTransaction` lồng nhau | `TransactionException.IN_PROGRESS` (không lồng được) |
| `commit`/`abort` khi không có giao dịch | `TransactionException.NOT_IN_PROGRESS` |
| Ghi quá nhiều trong một giao dịch | `TransactionException.BUFFER_FULL`; xem `JCSystem.getMaxCommitCapacity()` |
| `Util.arrayCopyNonAtomic`, `arrayFillNonAtomic` trong giao dịch | **Không** được hoàn tác |
| Bộ đếm sai của `OwnerPIN` trong giao dịch | Không bị hoàn tác (chống lợi dụng abort để thử PIN) |

## 3. Ví dụ 1 – chép mảng ([Applet3](../src/bai3_Transaction/Applet3.java#L43))

Đề in `for(int i=0; i++; i<t.length)` (điều kiện và phần tăng bị đổi chỗ, và Java Card không dùng `int`). Bản đúng:

```java
JCSystem.beginTransaction();
for (short i = 0; i < (short) t.length; i++) t[i] = u[i];
JCSystem.commitTransaction();
```

| Lệnh | `t` sau lệnh |
|---|---|
| `00 03 00 00 08` (đọc) | toàn `00` |
| `00 01` chép rồi **abort** | toàn `00` |
| `00 00` chép rồi **commit** | `01 02 03 04 05 06 07 08` |
| reset thẻ | giữ `01 … 08` |
| `00 01` lần nữa (abort) | giữ `01 … 08` (về giá trị trước giao dịch) |

## 4. Ví dụ 2 – hủy giữa vòng lặp ([Applet1](../src/bai3_Transaction/Applet1.java#L43))

```java
beginTransaction();
for (i = 0; i < 10; i++) {
    buffer2[i] = buffer1[i];
    if (i == 5) { abortTransaction(); beginTransaction(); }
}
commitTransaction();
```

| `i` | Giao dịch | Số phận |
|---|---|---|
| 0–5 | thứ nhất, bị abort **sau khi** đã chép phần tử 5 | phần tử 0..5 về giá trị cũ |
| 6–9 | thứ hai, commit | giữ giá trị mới |

Trước: `00 00 00 00 00 00 00 00 00 00`. Sau: `00 00 00 00 00 00 06 07 08 09` (đúng tài liệu). Chạy lại lần nữa kết quả không đổi; reset thẻ vẫn giữ.

Câu hỏi gài: *vì sao phần tử 5 cũng bị hủy?* Vì lệnh chép nằm **trước** câu `if` trong cùng vòng lặp.

## 5. Mục 3.3 – cái gì được hoàn tác ([Applet2](../src/bai3_Transaction/Applet2.java#L42))

```java
beginTransaction();
for (i = 0; i < 3; i++) { buffer1[i] = i+1; buffer2[i] = 2*(i+1); }
byte a_local = 1;
abortTransaction();
```

| Biến | Loại | Sau abort |
|---|---|---|
| `buffer1` | mảng bền | `00 00 00` (hoàn tác) |
| `buffer2` | mảng tạm | `02 04 06` (giữ) |
| `a_local` | biến cục bộ (ngăn xếp, RAM) | `01` (giữ) |

Phản hồi: `00 00 00 02 04 06 01 90 00` (đúng tài liệu).

Khác mã đề: đề tạo `new byte[3]` **ngay trong `process`**, mỗi lần gọi tốn thêm EEPROM. Ở đây tạo một lần trong hàm tạo và xóa `buffer1` về 0 trước giao dịch.

## 6. Giao dịch trong đề tài nhóm

Trong [StudentCard.java](../../code/applet/src/sinhvien/StudentCard.java#L627):

| Chỗ | Nhóm cập nhật phải đi cùng nhau |
|---|---|
| `transfer` (dòng 659–663) | Số dư, bộ đếm, nhật ký |
| `checkIn` (dòng 680–683) | Bộ đếm, nhật ký |
| `rewrapDekWithNewPin` (dòng 436–439, dùng khi đổi PIN và mở bằng PUK) | Khóa dữ liệu bọc lại, PIN mới |
| `unblockAdmin` (dòng 363–365) | PIN quản trị mới |

Nếu thẻ đã commit mà máy tính chưa nhận được biên nhận: phần mềm chỉ **phát hiện** ("Thẻ có N giao dịch chưa ghi sổ"), không tự bổ sung vào CSDL vì nhật ký trên thẻ không có chữ ký.

## 7. Luồng dữ liệu

```text
beginTransaction ──► JCRE bắt đầu ghi nhớ giá trị cũ của mọi ô bền bị sửa
   ghi EEPROM ...
commitTransaction ─► bỏ giá trị cũ, thay đổi thành vĩnh viễn
abortTransaction / mất điện ─► chép giá trị cũ trở lại
RAM (mảng tạm, biến cục bộ) không đi qua cơ chế này
```

## 8. Cấu hình

| Mục | Giá trị |
|---|---|
| Package AID | `11 22 33 44 55 03` |
| Applet AID | `…03 01` (ví dụ 2), `…03 02` (mục 3.3), `…03 03` (ví dụ 1) |

Debug Shell: `/select 11223344550301`, `/send 00010102`, `/send 00000102`, `/send 00010102`.
Công cụ: `.\chay.cmd kich-ban\th3.apdu` (12 mục).

## 9. Câu hỏi vấn đáp

1. **Nguyên tử là gì?** Một cập nhật hoặc hoàn tất, hoặc không xảy ra.
2. **Khi nào cần giao dịch?** Khi nhiều cập nhật phải nhất quán với nhau.
3. **Ba phương thức?** `beginTransaction`, `commitTransaction`, `abortTransaction` (lớp `JCSystem`).
4. **Biến cục bộ có được hoàn tác?** Không.
5. **Mảng tạm có được hoàn tác?** Không.
6. **Vì sao ghi nhật ký bằng `arrayCopy` chứ không `arrayCopyNonAtomic`?** Cần nằm trong giao dịch.
7. **Rút thẻ ngay sau khi trừ tiền, trước khi ghi nhật ký?** Cùng giao dịch nên lần sau được hoàn tác, số dư như cũ.
8. **Có lồng giao dịch được không?** **[Thêm]** Không.
9. **Bỏ `beginTransaction` ở hàm trừ tiền thì sao?** Có thể trừ tiền mà không có nhật ký, hai bên lệch sổ.
10. **Vì sao không nên bọc cả vòng lặp dài trong một giao dịch?** **[Thêm]** Bộ đệm giao dịch có giới hạn.
