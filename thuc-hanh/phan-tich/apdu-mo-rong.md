# APDU mở rộng: phân tích chi tiết

> Đề: [apdu-mo-rong.md](../../smart/docs/apdu-mo-rong.md). Mã: [bai7_APDU_Extended](../src/bai7_APDU_Extended/Applet1.java). Kết quả từ [th7.apdu](../kich-ban/th7.apdu) và [tn7.apdu](../kich-ban/thi-nghiem/tn7.apdu). **[Thêm]** = ngoài tài liệu.

## 1. Vì sao cần

APDU thường gửi tối đa 255 byte và nhận tối đa 256 byte mỗi lệnh. Ảnh, chữ ký dài, sinh trắc học phải chia nhiều lệnh. APDU mở rộng gửi một lệnh dài (tài liệu: từ Java Card 2.2.2, dễ triển khai với **T = 1**).

## 2. Định dạng **[Thêm]**

| | Ngắn | Mở rộng |
|---|---|---|
| Lc | 1 byte (`01`..`FF`) | 3 byte `00 hh ll` |
| Le khi có Lc | 1 byte (`00` = 256) | 2 byte |
| Le khi không có Lc | 1 byte | 3 byte `00 hh ll` (`00 00 00` = 65536) |

Ví dụ:

```text
00 01 00 00 00 02 58 <600 byte>      nhập 600 byte (0258h = 600)
00 02 00 00 00 00 00                 xin trả về, Le mở rộng
```

## 3. Nhận dữ liệu dài

```text
setIncomingAndReceive()  → đọc đoạn đầu vào bộ đệm, trả số byte
getIncomingLength()      → tổng độ dài Lc
getOffsetCdata()         → vị trí dữ liệu (khác 5 khi Lc 3 byte)
receiveBytes(off)        → đọc đoạn tiếp, lặp tới khi trả 0
```

Bộ đệm APDU tối thiểu 133 byte (tài liệu; thẻ JCOP trong bài dùng 261 byte), nên dữ liệu lớn **luôn** đến thành nhiều đoạn: mỗi đoạn phải chép đi ngay.

## 4. Mã ([dòng 64–80](../src/bai7_APDU_Extended/Applet1.java#L64))

```java
short recvLen = apdu.setIncomingAndReceive();
short tong = apdu.getIncomingLength();
if (tong <= 0 || tong > MAX_SIZE) ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
short dataOffset = apdu.getOffsetCdata();
short pointer = 0;
dataLen = 0;
while (recvLen > 0) {
    Util.arrayCopy(buf, dataOffset, temp, pointer, recvLen);
    pointer += recvLen;
    recvLen = apdu.receiveBytes(dataOffset);
}
dataLen = pointer;              // chỉ ghi độ dài khi đã nhận đủ
```

Applet phải khai báo `implements ExtendedLength`.

Xuất: `setOutgoing` → `setOutgoingLength(dataLen)` → `sendBytesLong` (tự chia đoạn).

### Chỗ cần cẩn thận trong mã của tài liệu

| Trong tài liệu | Vấn đề | Ở đây |
|---|---|---|
| Vòng `while` chia theo `le` | Nếu `le = 0` thì lặp mãi | Một lần `sendBytesLong` |
| Ghi `dataLen` trước khi nhận | Đứt giữa chừng để lại độ dài sai | Ghi sau cùng |
| Cấp phát `temp` sau `register` | Trái mục 4.1 TH4 | Cấp trong hàm tạo |

## 5. Kết quả đã chạy trên thẻ giả lập

| Dữ liệu | Kết quả |
|---|---|
| 100, 127 byte (Lc ngắn) | đúng |
| 128 byte | `67 00`: jCardSim đọc `Lc` như số có dấu |
| 600 byte (Lc mở rộng) | không xử lý được |

Đây là **giới hạn của thẻ giả lập**, không phải của applet. Phần dài và mở rộng chỉ kiểm được trên JCIDE / thẻ thật, **chưa kiểm**.

## 6. Cách thay thế: chia khối

Đề tài nhóm gửi ảnh thành **khối 240 byte** (`P1` = đối tượng, `P2` = số khối). Chạy được cả trên thẻ không hỗ trợ APDU mở rộng và T = 0, đổi lại nhiều lệnh hơn.

## 7. Cấu hình

| Mục | Giá trị |
|---|---|
| Package / Applet AID | `11 22 33 44 55 07` / `…07 01` |
| Giao thức | **T = 1** (chọn trên thanh công cụ JCIDE) |
| Công cụ | `.\chay.cmd --pcsc=JAVACOS kich-ban\th7.apdu` (5 mục) |

## 8. Câu hỏi vấn đáp

1. **APDU mở rộng khác gì?** Lc, Le dài hơn, tới hàng chục nghìn byte.
2. **Cần gì để dùng?** Applet `implements ExtendedLength`, Java Card ≥ 2.2.2, thẻ hỗ trợ, nên T = 1.
3. **`setIncomingAndReceive` và `receiveBytes` khác gì?** Lần nhận đầu / các lần tiếp theo.
4. **Vì sao không nhận một lần?** Bộ đệm nhỏ hơn dữ liệu.
5. **`getOffsetCdata` để làm gì?** Vị trí dữ liệu thay đổi theo dạng APDU.
