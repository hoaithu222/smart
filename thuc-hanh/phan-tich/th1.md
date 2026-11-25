# TH1 – Applet đầu tiên và APDU: phân tích chi tiết

> Đề: [th1-bai-tap.md](../../smart/docs/th1-bai-tap.md). Lý thuyết: [bài 1 slide 30–37](../../smart/docs/bai1-tong-quan-the-thong-minh.md) (APDU), [bài 3](../../smart/docs/bai3-ngon-ngu-java-trong-java-card.md) (ngôn ngữ), [bài 4](../../smart/docs/bai4-java-card-api.md) (API).
> Mã: [src/bai1_APDU](../src/bai1_APDU/). Kết quả trong file này lấy từ [th1.apdu](../kich-ban/th1.apdu) và [tn1.apdu](../kich-ban/thi-nghiem/tn1.apdu) chạy trên thẻ giả lập. **[Thêm]** = ngoài tài liệu của cô.

## 1. Đề yêu cầu gì

| Bài | Yêu cầu | Applet |
|---|---|---|
| 1 | In ra "Xin chao, KMA" | `Applet1` |
| 2 | 3 chức năng: họ tên, ngày sinh, cả hai (giá trị gán sẵn) | `Applet2` |
| 3 | Diện tích hình vuông, hình chữ nhật | `Applet3` |
| 4 | Nhập mã SV, họ tên, ngày sinh, quê quán; in từng trường và cả chuỗi | `Applet4` |
| Hướng dẫn | "Máy tính bỏ túi" (bộ tài liệu không có bản in) | `Calculator` |

## 2. Kiến thức nền

### 2.1 Vòng đời một applet

```text
install()  ──►  hàm tạo  ──►  register()          (một lần, lúc cài)
                                   │
   SELECT ──► select() ──► process(SELECT)          selectingApplet() = true
                                   │
   mỗi APDU ──────────────► process(apdu)           nhiều lần
                                   │
   chọn applet khác ───────► deselect()
```

| Phương thức (bài 4 slide 15) | Ai gọi | Ghi chú |
|---|---|---|
| `public static void install(byte[] bArray, short bOffset, byte bLength)` | JCRE | Tạo thể hiện applet |
| `protected final void register()` / `register(bArray, off, len)` | applet | Đăng ký, gán AID |
| `public boolean select()` | JCRE | Mặc định trả `true` |
| `public abstract void process(APDU apdu)` | JCRE | Bắt buộc viết |
| `public void deselect()` | JCRE | Dọn trạng thái phiên |

### 2.2 APDU

```text
Lệnh      : CLA  INS  P1  P2  [Lc  Data...]  [Le]
Phản hồi  : [Data...]  SW1  SW2
```

Vị trí trong bộ đệm APDU (hằng của `ISO7816`):

| Hằng | Giá trị | Byte |
|---|---|---|
| `OFFSET_CLA` | 0 | lớp lệnh |
| `OFFSET_INS` | 1 | mã lệnh |
| `OFFSET_P1`, `OFFSET_P2` | 2, 3 | tham số |
| `OFFSET_LC` | 4 | độ dài dữ liệu gửi xuống (hoặc Le nếu không có dữ liệu) |
| `OFFSET_CDATA` | 5 | dữ liệu đầu tiên |

Bốn trường hợp (bài 1 slide 36), mỗi cái có ví dụ trong bài này:

| Case | Cấu trúc | Ví dụ | Applet |
|---|---|---|---|
| 1 | header | `00 00 00 00` | Applet1 |
| 2 | header, Le | `00 00 00 00 0D` (xin 13 byte) | Applet1 |
| 3 | header, Lc, data | `00 01 00 00 05 61 7C 62 7C 63` | Applet4 nhập |
| 4 | header, Lc, data, Le | `00 03 00 00 03 61 62 63 00` | xem [TH6](th6.md) |

### 2.3 Ngôn ngữ Java trên thẻ (bài 3)

- Kiểu dùng được: `byte`, `short`, `boolean`; `int` chỉ một số thẻ.
- Phép tính trung gian ra `int`, nên **luôn ép kiểu** `(short)(a + b)`.
- Mảng một chiều; không luồng, không trình gom rác, không `String`.
- Chuỗi chữ phải viết thành mảng byte: `{'X','i','n',...}`.

### 2.4 Gửi dữ liệu trả về (bài 4 slide 13)

| Cách | Nguồn dữ liệu | Dùng khi |
|---|---|---|
| `setOutgoingAndSend(off, len)` | **bộ đệm APDU** | Kết quả ngắn đã ghi sẵn vào buffer |
| `setOutgoing()` → `setOutgoingLength(len)` → `sendBytes(off, len)` | bộ đệm APDU | Gửi nhiều đợt từ buffer |
| `setOutgoing()` → `setOutgoingLength(len)` → `sendBytesLong(mảng, off, len)` | **mảng bất kỳ** | Dữ liệu nằm trong mảng của applet |

Thứ tự bắt buộc: `setOutgoing` → `setOutgoingLength` → `send...`.

## 3. Phân tích mã

### 3.1 Phần chung

```java
public static void install(byte[] bArray, short bOffset, byte bLength) {
    new Applet1().register(bArray, (short) (bOffset + 1), bArray[bOffset]);
}
public void process(APDU apdu) {
    if (selectingApplet()) return;          // lệnh SELECT cũng đi vào đây
    byte[] buf = apdu.getBuffer();
    switch (buf[ISO7816.OFFSET_INS]) { ... default: ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED); }
}
```

- `bArray[bOffset]` là độ dài AID, AID bắt đầu ở `bOffset + 1` (xem [TH4](th4.md)).
- Bỏ `selectingApplet()` thì lệnh SELECT (`INS A4`) rơi vào `default` và trả `6D 00`.

### 3.2 Bài 1 – [Applet1](../src/bai1_APDU/Applet1.java#L23)

- `LOI_CHAO` là `static final byte[]`: hằng số nằm trong bộ nhớ bền, không cấp phát lại mỗi lệnh.
- Dữ liệu nằm trong mảng riêng nên dùng `sendBytesLong` ([dòng 30–32](../src/bai1_APDU/Applet1.java#L30)).

### 3.3 Bài 2 – [Applet2](../src/bai1_APDU/Applet2.java#L46)

- INS `01`, `02`: gửi thẳng mảng hằng.
- INS `03`: ghép 3 đoạn vào buffer. `Util.arrayCopyNonAtomic` **trả về vị trí ngay sau đoạn vừa chép**, nên nối được liên tiếp:

```java
short n = Util.arrayCopyNonAtomic(HO_TEN, 0, buf, 0, len1);     // n = 12
n = Util.arrayCopyNonAtomic(NOI, 0, buf, n, 3);                 // n = 15
n = Util.arrayCopyNonAtomic(NGAY_SINH, 0, buf, n, 10);          // n = 25
apdu.setOutgoingAndSend((short) 0, n);
```

Dùng bản `NonAtomic` vì đích là buffer trong RAM, không cần tính nguyên tử.

### 3.4 Bài 3 – [Applet3](../src/bai1_APDU/Applet3.java#L35)

- `short a = (short)(buf[ISO7816.OFFSET_P1] & 0x00FF);` : `byte` của Java **có dấu** (`80h` = −128). `& 0x00FF` đổi thành 0..255. Thí nghiệm: `00 01 80 00` trả `40 00` (128² = 16384).
- Tích lớn nhất 255² = 65025 > 32767 nên `short` hiện thành số âm, nhưng **2 byte vẫn đúng** khi đọc là số không dấu: `FE 01`.
- `Util.setShort(buf, 0, v)` ghi byte cao trước (big-endian).

### 3.5 Bài 4 – [Applet4](../src/bai1_APDU/Applet4.java#L55)

Dữ liệu: `maSV|hoTen|ngaySinh|queQuan`, ngăn bằng `|` (`7C`).

1. **Kiểm độ dài** `Lc` trong 1..128, sai trả `67 00`.
2. **Nhận đủ dữ liệu** ([dòng 65](../src/bai1_APDU/Applet4.java#L65)): `setIncomingAndReceive()` trả số byte nhận được đợt đầu; chưa đủ thì `receiveBytes` tiếp tại `OFFSET_CDATA + đã nhận`.
3. **Kiểm định dạng** trong một lượt duyệt: đúng 3 dấu `|`, không trường nào rỗng, sai trả `6A 80`.
4. **Ghi nguyên tử** ([dòng 87–90](../src/bai1_APDU/Applet4.java#L87)): chuỗi và độ dài phải đổi cùng nhau, nếu rút thẻ giữa chừng sẽ không có chuỗi mới đi với độ dài cũ ([TH3](th3.md)).
5. **In**: `P1 = 0` in cả chuỗi; `P1 = 1..4` tìm ranh giới trường thứ `P1` rồi `sendBytesLong(thongTin, dau, cuoi - dau)`.

Vì kiểm hết rồi mới ghi, một lệnh nhập sai **không làm hỏng dữ liệu cũ** (kịch bản: nhập `a|b|c` bị từ chối, in lại vẫn ra mã SV cũ).

### 3.6 Máy tính – [Calculator](../src/bai1_APDU/Calculator.java#L36)

- Toán hạng ở `P1`, `P2` (không cần dữ liệu), kết quả 2 byte.
- Trừ ra số âm theo bù hai: `3 − 5` → `FF FE`.
- Chia: byte cao là thương, byte thấp là dư: `(short)((short)(a / b) << 8) | (short)(a % b)`. Chia cho 0 trả `6A 86`.

## 4. Phân tích lệnh và phản hồi

| Lệnh | Giải mã | Phản hồi |
|---|---|---|
| `00 A4 04 00 07 11 22 33 44 55 01 01` | SELECT theo AID, Lc = 7 | `90 00` |
| `00 00 00 00 0D` | Applet1, xin 13 byte | `58 69 6E 20 63 68 61 6F 2C 20 4B 4D 41 90 00` |
| `00 03 00 00 00` | Applet2, cả hai | "Nguyen Van A - 01/01/2004" `90 00` |
| `00 02 0C 05 02` | chữ nhật 12 × 5 | `00 3C 90 00` |
| `00 01 00 00 27 43 54 … 69` | nhập 39 byte | `90 00` |
| `00 02 02 00 00` | in trường 2 | "Nguyen Van A" `90 00` |
| `00 02 05 00 00` | P1 quá 4 | `6A 86` |
| `00 04 11 05 02` | 17 : 5 | `03 02 90 00` |

## 5. Dữ liệu đi đâu trong bài này

```text
Máy tính ──APDU──► bộ đệm APDU (RAM) ──arrayCopy trong giao dịch──► thongTin (EEPROM)
Máy tính ◄─R-APDU── bộ đệm APDU (RAM) ◄──sendBytesLong────────────── thongTin / hằng số
```

Sau reset thẻ, `thongTin` còn nguyên vì là đối tượng bền (kịch bản: reset rồi in quê quán vẫn ra "Ha Noi").

## 6. Cấu hình trên JCIDE

| Mục | Giá trị |
|---|---|
| Package / AID | `bai1_APDU` / `11 22 33 44 55 01` |
| Applet AID | Applet1 `…01 01`, Applet2 `…01 02`, Applet3 `…01 03`, Applet4 `…01 04`, Calculator `…01 05` |
| Tham số cài đặt | không cần |

1. Tạo package, tạo 5 lớp, chép mã (mã không dấu để `windows-1252` đọc đúng).
2. Chuột phải → `Change AID` cho package và từng applet.
3. `Build` → `Run or Debug Configuration` chọn cả 5 → `Run`.
4. Debug Shell: `/select 11223344550101` rồi `/send 000000000D`.

Chạy bằng công cụ: giả lập `.\chay.cmd kich-ban\th1.apdu`; JCIDE `.\chay.cmd --pcsc=JAVACOS kich-ban\th1.apdu` (31 mục).

## 7. Thí nghiệm nên thử (đã chạy)

| Thử | Kết quả | Rút ra |
|---|---|---|
| SELECT AID không tồn tại | `6A 82` | Không tìm thấy applet |
| `00 00 00 00` (case 1) tới Applet1 | vẫn trả chuỗi | Applet không phụ thuộc Le |
| `80 00 00 00` tới Applet1 | vẫn trả chuỗi | Applet bài 1 **không kiểm CLA** |
| Hình vuông cạnh 0 | `00 00` | Biên dưới |
| Applet4 không có dữ liệu | `67 00` | Kiểm độ dài |
| Applet4 gửi 129 byte | `67 00` | Quá 128 |
| Applet4 gửi 128 byte không có `\|` | `6A 80` | Đúng độ dài, sai định dạng |

## 8. Mã trạng thái và lỗi hay gặp

| SW | Hằng | Khi nào trong bài này |
|---|---|---|
| `90 00` | — | Thành công |
| `6D 00` | `SW_INS_NOT_SUPPORTED` | INS lạ |
| `6E 00` | `SW_CLA_NOT_SUPPORTED` | (bài này không dùng; xem [Kết nối](ket-noi.md)) |
| `67 00` | `SW_WRONG_LENGTH` | Sai độ dài |
| `6A 80` | `SW_WRONG_DATA` | Dữ liệu sai định dạng |
| `6A 86` | `SW_INCORRECT_P1P2` | Sai P1/P2 |
| `69 85` | `SW_CONDITIONS_NOT_SATISFIED` | In khi chưa nhập |
| `6A 82` | `SW_FILE_NOT_FOUND` | Chọn applet chưa cài |
| `6F 00` | `SW_UNKNOWN` | **[Thêm]** Ngoại lệ không bắt (ví dụ vượt chỉ số mảng) |

Lỗi biên dịch hay gặp: dùng `int`, thiếu ép `(short)`, dùng `String`, quên `import javacard.framework.*`.

## 9. Câu hỏi vấn đáp

1. **APDU lệnh gồm gì?** CLA, INS, P1, P2 bắt buộc; Lc, Data, Le tùy chọn.
2. **Lc khác Le?** Lc: số byte gửi xuống. Le: số byte muốn nhận. `Le = 00` nghĩa là tối đa 256 **[Thêm]**.
3. **Vì sao `return` sớm khi `selectingApplet()`?** Lệnh SELECT cũng được đưa vào `process`.
4. **`sendBytes` khác `sendBytesLong`?** Gửi từ bộ đệm APDU / từ mảng bất kỳ.
5. **Vì sao `& 0x00FF`?** `byte` có dấu; cần giá trị 0..255.
6. **Vì sao ép `(short)`?** Phép tính trung gian ra `int`, nhiều thẻ không có `int`.
7. **`arrayCopy` khác `arrayCopyNonAtomic`?** Bản đầu nguyên tử khi đích là mảng bền; bản sau không, nhanh hơn, hợp với RAM.
8. **Vì sao dùng giao dịch ở Applet4?** Để chuỗi và độ dài luôn khớp nhau.
9. **`static final byte[]` có tốn bộ nhớ mỗi lệnh không?** Không: cấp một lần.
10. **Muốn thêm chức năng in tuổi thì làm gì?** Thêm hằng INS, thêm `case`, thêm hàm gửi dữ liệu.
