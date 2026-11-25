# Bài thực hành 4 – Phương thức `install`: câu hỏi và trả lời

> Nguồn chính: [th4-phuong-thuc-install.md](../smart/docs/th4-phuong-thuc-install.md) (mục 4.1 – 4.5).
> Chỗ nào ghi **[Thêm]** là kiến thức ngoài tài liệu của cô, lấy từ đặc tả Java Card, để trả lời khi bị hỏi sâu.
> Mã minh họa: [src/bai4_InstallMethod](src/bai4_InstallMethod/).
> Đã được một agent khác đối chiếu độc lập với tài liệu bài 4 và Java Card API 2.2.2 (25/11/2026).

---

## Câu 1. Phương thức `install` dùng để làm gì?

**Trả lời ngắn:** `install` dùng để **tạo một thể hiện (đối tượng) của applet** trên thẻ, tức một ứng dụng chạy được. JCRE thường gọi nó ở **bước cuối của quá trình cài đặt applet**.

**Giải thích:**

- `install` tương tự hàm `main` của chương trình Java: là điểm bắt đầu.
- Các đối số của nó chính là **tham số cài đặt**, tương tự đối số của `main`.
- Bên trong, `install` dùng `new` rồi gọi hàm tạo (constructor) của applet.

```java
public static void install(byte[] bArray, short bOffset, byte bLength)
{
    new Applet1();      // gọi hàm tạo
}
```

**[Thêm]** `install` là `public static` vì lúc JCRE gọi nó thì chưa có đối tượng applet nào.

---

## Câu 2. Applet được khởi tạo và đăng ký với hệ điều hành thẻ (JCRE) bằng cách nào?

**Trả lời ngắn:**

- **Khởi tạo:** phương thức **`install`** (gọi `new`, chạy hàm tạo).
- **Đăng ký với JCRE:** phương thức **`register`**.

| Bước | Ai làm | Việc làm |
|---|---|---|
| 1 | Trình cài đặt | Nạp file CAP xuống thẻ, gửi kèm tham số cài đặt |
| 2 | JCRE | Gọi `install(bArray, bOffset, bLength)` |
| 3 | `install` | `new` thể hiện applet → chạy hàm tạo |
| 4 | Hàm tạo | **Tạo** các đối tượng applet cần trong suốt vòng đời |
| 5 | Hàm tạo | **Khởi tạo** giá trị cho đối tượng, biến |
| 6 | Hàm tạo (hoặc `install`) | **`register(...)`** – đăng ký thể hiện với JCRE |

Nếu thiếu tài nguyên khi tạo đối tượng, JCRE **xóa applet và thu hồi toàn bộ bộ nhớ**, không để lại applet tạo dở (mục 4.1).

**[Thêm]**

- Đăng ký xong, applet mới **chọn được** bằng lệnh SELECT và nhận APDU qua `process`.
- Theo Java Card API (`Applet.install`): cài đặt được coi là **thành công khi `register()` chạy xong không lỗi**. Cài thất bại (và được dọn sạch) khi không gọi `register`, hoặc ngoại lệ ném ra **trước** khi `register` thành công. Vì vậy nên **kiểm tra tham số và cấp phát trước, gọi `register` ở cuối**.

---

## Câu 3. Đối tượng và mảng tạo lúc nào cũng được, nhưng tốt nhất nên tạo trong quá trình khởi tạo applet. Vì sao?

**Chỉnh lại câu cho đúng tài liệu:** đối tượng và mảng có thể tạo **bất kỳ lúc nào trong quá trình *thực thi* applet** (không chỉ lúc khởi tạo), nhưng khi có thể thì **nên đặt vào quá trình khởi tạo** (trong hàm tạo).

**Lý do (mục 4.1):**

1. **Applet không bao giờ lỗi vì thiếu bộ nhớ khi đang chạy:** cần gì đã giữ chỗ sẵn.
2. **Thiếu bộ nhớ thì lộ ra ngay lúc cài** và JCRE dọn sạch, không có applet dở dang (**[Thêm]** với điều kiện cấp phát trước `register`).
3. Nhưng **không tạo nhiều hơn mức cần:** bộ nhớ thừa đáng lẽ dùng được cho applet khác hoặc cho JCRE.

**[Thêm]** Thẻ không bảo đảm có bộ thu gom rác (xóa đối tượng là tính năng tùy chọn, phải gọi yêu cầu). Nếu viết `new` trong `process()`, **mỗi lần nhận lệnh lại tốn thêm bộ nhớ** và có thể cạn thẻ.

```java
// NÊN: tạo một lần trong hàm tạo
private Applet1() {
    monhoc = new byte[SO_LUONG_MON_HOC];
    diem   = new byte[SO_LUONG_MON_HOC];
    register();
}

// KHÔNG NÊN: mỗi APDU lại cấp phát
public void process(APDU apdu) {
    byte[] tam = new byte[10];
}
```

---

## Câu 4. Hàm tạo của applet có tham số hay không? Nếu có thì truyền vào những gì?

**Trả lời ngắn:** **cả hai đều được.** Hàm tạo có thể không có tham số, hoặc nhận **tham số cài đặt**:

```java
private Applet1() { ... }                                              // không tham số
private Applet1(byte[] bArray, short bOffset, byte bLength) { ... }    // có tham số
```

| Tham số | Ý nghĩa |
|---|---|
| `byte[] bArray` | Mảng chứa các tham số cài đặt |
| `short bOffset` | Vị trí bắt đầu của tham số trong `bArray` |
| `byte bLength` | Độ dài dữ liệu tham số (byte) |

### Nội dung bên trong `bArray` (định dạng từ Java Card 2.1.2)

```text
[Li] [AID thể hiện: Li byte] [Lc] [thông tin điều khiển: Lc byte] [La] [dữ liệu applet: La byte]
```

| Trường | Là gì |
|---|---|
| `Li` | Độ dài AID |
| AID | AID của thể hiện applet |
| `Lc` | Độ dài thông tin điều khiển (do trình cài đặt quy định) |
| `La` | Độ dài dữ liệu applet |
| Dữ liệu applet | Tham số do người viết applet tự định nghĩa |

`Li`, `Lc`, `La` đều **có thể bằng 0**.

**Người ta truyền gì?** Tham số **cấu hình** (kích thước file, mảng...) và **giá trị khởi tạo / cá nhân hóa**. Ví dụ trong bài: AID `"DIEM_THI"`, mã sinh viên `"SV01"`, số môn thi `9`.

> Đề có chỗ tự mâu thuẫn: phần chữ ghi mặc định **9** môn, nhưng mã mục 4.4 gán `0x08` khi không có tham số. Bài tập 4.5 ghi 9.

### Ví dụ giải mã

JCIDE: Instance AID = `44 49 45 4D 5F 54 48 49`, Application Specific Parameters = `53 56 30 31 09`. Trường hợp `Lc = 0`, applet nhận:

```text
08  44 49 45 4D 5F 54 48 49  00  05  53 56 30 31  09
Li  └──── "DIEM_THI" ─────┘  Lc  La  └ "SV01" ─┘  số môn
```

### Cách đọc

```java
byte iLen = bArray[bOffset];                      // Li
bOffset = (short) (bOffset + iLen + 1);           // nhảy qua AID
byte cLen = bArray[bOffset];                      // Lc
bOffset = (short) (bOffset + cLen + 1);           // nhảy qua thông tin điều khiển
byte aLen = bArray[bOffset];                      // La
bOffset = (short) (bOffset + 1);                  // đầu dữ liệu applet
```

**[Thêm]**

- Đừng giả định `Lc = 0`: JCRE để trình cài đặt quy định trường này; theo GlobalPlatform đó là *Application Privileges* (1 hoặc 3 byte), ví dụ `08 <AID> 01 00 05 53 56 30 31 09`. Vì vậy phải đọc `Lc` rồi nhảy qua.
- Không giữ tham chiếu tới `bArray` sau khi `install` kết thúc: dữ liệu cần dùng phải **chép ra** mảng của applet.

---

## Câu 5. Phương thức `register` dùng để làm gì?

**Trả lời ngắn:** để **đăng ký applet với JCRE**. Nó có **hai chức năng** (mục 4.2):

1. **Lưu một tham chiếu** tới thể hiện applet vào JCRE.
2. **Gán AID** cho thể hiện applet.

**[Thêm]** Không gọi `register` thì applet **không được cài thành công** và không chọn được.

---

## Câu 6. Java Card cung cấp mấy phương thức `register`?

**Trả lời: 2 phương thức**, đều định nghĩa trong lớp cơ sở `Applet`.

| Phương thức | AID dùng để đăng ký | Khi nào dùng |
|---|---|---|
| `protected final void register()` | **AID mặc định** từ file CAP | Không cần đổi AID |
| `protected final void register(byte[] bArray, short bOffset, byte bLength)` | **AID chỉ định** trong `bArray`: bắt đầu tại `bOffset`, dài `bLength` | Muốn dùng AID do tham số cài đặt cung cấp |

Cách viết hay dùng:

```java
byte iLen = bArray[bOffset];
if (iLen == 0) {
    register();                                          // AID mặc định
} else {
    register(bArray, (short) (bOffset + 1), iLen);       // AID từ tham số cài đặt
}
```

Chú ý: AID bắt đầu ở `bOffset + 1` (bỏ qua byte độ dài `Li`).

**[Thêm]** AID dài **5 đến 16 byte** (sai độ dài: `SystemException.ILLEGAL_VALUE`). Mỗi thể hiện chỉ đăng ký được một lần, trong lúc `install` đang chạy, và AID không được trùng AID đang dùng (nếu không: `SystemException.ILLEGAL_AID`).

---

## Câu 7. Mỗi applet được định danh bởi AID. Vì sao sau này applet lại có AID khác với giá trị ban đầu?

**Trả lời ngắn:** file CAP chỉ cho **AID mặc định**. Khi cài đặt, người cài có thể cung cấp **AID khác trong tham số cài đặt**, và applet đăng ký bằng AID đó nhờ `register(bArray, ...)` (mục 4.2).

**[Thêm]** Ba loại AID:

| Loại AID | Định danh cái gì | Quyết định lúc nào |
|---|---|---|
| Package AID | Gói (file CAP) | Lúc build |
| Applet AID (mặc định) | Lớp applet trong CAP | Lúc build |
| **Instance AID** | **Thể hiện** đã cài trên thẻ – dùng trong lệnh SELECT | **Lúc cài đặt** |

**Vì sao cần AID khác:**

1. Mỗi **thể hiện** applet trên thẻ được xác định **duy nhất** bởi AID (mục 4.2).
2. **[Thêm]** Cài hai thể hiện từ cùng một lớp thì phải có hai AID.
3. **[Thêm]** Đơn vị phát hành thẻ đặt AID theo hệ thống của họ, không cần sửa mã và build lại; tránh trùng AID đã có.
4. **[Thêm]** Phần mềm trên máy tính chọn applet theo AID đã thống nhất.

Ví dụ trong bài: `Applet2` có AID mặc định (đặt trong JCIDE) nhưng cài với Instance AID `44 49 45 4D 5F 54 48 49`, nên chọn bằng:

```text
00 A4 04 00 08 44 49 45 4D 5F 54 48 49   ->  90 00
```

**[Thêm]**

- Muốn dùng Instance AID người cài đưa vào thì phải gọi `register(bArray, (short)(bOffset+1), bArray[bOffset])`. `register()` không tham số lấy AID của lớp applet trong CAP. Đặc tả còn cho phép thẻ đòi AID truyền vào `register(...)` trùng Instance AID trong tham số.
- Thẻ giả lập jCardSim trong thư mục này **không** theo đúng đặc tả ở điểm này (gọi `register()` vẫn đăng ký theo AID được cấp lúc cài), nên **đừng demo ý này trên giả lập**. Hành vi trên JCIDE chưa kiểm.
- Đề khai báo các trường của `Applet2` là `static`. Khi đó hai thể hiện **dùng chung** dữ liệu (cài cái sau đè cái trước). Mã ở đây đã bỏ `static`.

---

## Câu 8. Tại sao cần có sự thay đổi trong quá trình hoạt động của applet?

> Câu hỏi gốc viết ngắn, có thể hiểu hai cách; câu trả lời dưới bao cả hai. Nếu được, hỏi lại cô ý muốn hỏi.

**Trả lời ngắn:**

- **Trong lúc hoạt động**, cái thay đổi là **dữ liệu** (điểm thi, số dư, PIN...) qua APDU, vì nghiệp vụ thay đổi theo thời gian. **AID của thể hiện không đổi.**
- **AID và cấu hình** chỉ đặt **lúc cài đặt**, để **một file CAP dùng được cho nhiều thẻ, nhiều cấu hình** mà không phải sửa mã.

| Thay đổi gì | Lúc nào | Cách | Vì sao |
|---|---|---|---|
| Instance AID | Chỉ lúc cài đặt | Tham số của `install` + `register(bArray, ...)` | Định danh duy nhất từng thể hiện |
| Giá trị khởi tạo, cấu hình | Chỉ lúc cài đặt | Dữ liệu applet trong tham số | Cá nhân hóa; cấp **đủ** bộ nhớ, không lãng phí (mục 4.3) |
| Dữ liệu applet (điểm, số dư, PIN) | Trong lúc hoạt động | Lệnh APDU gửi tới `process` | Nhập, sửa, xóa theo nhu cầu sử dụng |

Ví dụ cấu hình: bài tập 4.5 lưu mỗi môn 2 byte, nên số môn thi = 9 → mảng `diemThi` 18 byte. Đổi số môn thì đổi tham số rồi **cài lại** applet, không phải sửa mã hay build lại.

**[Thêm]** Sau khi đăng ký, AID của thể hiện không đổi được. Muốn AID hoặc cấu hình khác phải **xóa applet rồi cài lại**, và dữ liệu của thể hiện cũ mất.

---

## Tóm tắt một dòng mỗi câu (ôn trước vấn đáp)

| Câu | Ý chính |
|---|---|
| 1 | `install` tạo thể hiện applet; JCRE gọi ở bước cuối cài đặt; tương tự `main` |
| 2 | Khởi tạo: `install` → `new` → hàm tạo. Đăng ký: `register` |
| 3 | Tạo sẵn trong hàm tạo để lúc chạy không thiếu bộ nhớ; không tạo thừa |
| 4 | Có hoặc không tham số; nếu có: `bArray, bOffset, bLength` chứa `Li-AID-Lc-thông tin-La-dữ liệu` |
| 5 | Lưu tham chiếu applet vào JCRE và gán AID |
| 6 | Hai: `register()` (AID mặc định) và `register(bArray, bOffset, bLength)` (AID chỉ định) |
| 7 | AID mặc định từ CAP; lúc cài có thể cấp Instance AID khác để định danh duy nhất |
| 8 | Lúc chạy đổi dữ liệu qua APDU; AID và cấu hình đổi lúc cài để một CAP dùng nhiều nơi |

## Tự kiểm tra bằng bài thực hành

Mở PowerShell trong thư mục `thuc-hanh`:

```powershell
.\chay.cmd kich-ban\th4.apdu
```

Kịch bản cài `Applet2` với Instance AID `DIEM_THI`, tham số `53 56 30 31 09`, rồi gửi `00 00 01 02`; kết quả mong đợi `53 56 30 31 09 90 00` (đúng kết quả mục 4.4). Sau đó chạy tiếp bài tập 4.5 (nhập, in, sửa, xóa điểm).
