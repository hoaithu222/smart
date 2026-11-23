# Bài 3: Giới thiệu về ngôn ngữ Java trong Java Card

> Nguồn: `631616381-1793636955951-bai3-gioithieuvengonngujavatrongjavacard.pdf` · 11 slide · ThS. Cao Vinh, Khoa Công nghệ thông tin, Học viện Kỹ thuật Mật mã
> Chuyển từ PDF sang Markdown ngày 03/11/2026.

## Slide 1 · Trang bìa

- HỌC VIỆN KỸ THUẬT MẬT MÃ
- KHOA CÔNG NGHỆ THÔNG TIN
- Bài 3: Giới thiệu về ngôn ngữ Java trong Java Card
- ThS. Cao Vinh
- caovinh226@gmail.com
- “Tri thức là chìa khóa mở cánh cửa tương lai"

## Slide 2 · Nội dung chính

- Các kiểu dữ liệu cơ bản
- Ép kiểu
- Mảng
- Đặc điểm của nền tảng Java Card

## Slide 3 · Các kiểu dữ liệu cơ bản

| Kiểu dữ liệu | Độ dài | Phạm vi biểu diễn |
|---|---|---|
| byte | 8 bit | -128 → 127 |
| short | 16 bit | -32768 → 32767 |
| boolean | 8 bit | true hoặc false |
| int (hỗ trợ trên một số nền tảng) | 32 bit | -2147483648 → 2147483647 |

## Slide 4 · Ép kiểu

***Ý nghĩa***

Các phép tính trung gian trong Java Card luôn cho giá trị int.

Trong Java Card không phải nền tảng nào cũng hỗ trợ kiểu int

→ Lỗi

→ Luôn phải ép kiểu sang byte hoặc short.

## Slide 5 · Ép kiểu

***Ví dụ***

```java
byte b;
short a;
byte array[] = new byte[10];
b = (byte)((a+1)/2);
b = (byte)((byte)(a+1)/2);
b = (byte)((short)(a+1)/2);
b = array[a+1];
b = array[(byte)(a+1)];
b = array[(short)(a+1)];
```

## Slide 6 · Danh sách các phép toán trung gian hoặc chưa được gán

| Phép toán | Toán tử |
|---|---|
| Phép cộng | +, ++ |
| Phép trừ | -, -- |
| Phép phủ định một ngôi | - |
| Phép dịch trái | << |
| Phép nhân | * |
| Phép chia | / |

## Slide 7 · Danh sách các phép toán cần ép kiểu các toán hạng đầu vào

| Phép toán | Toán tử |
|---|---|
| Phép lấy số dư (cả hai toán hạng) | % |
| Phép phủ định một ngôi | - |
| Phép dịch phải (toán hạng được dịch chuyển) | >> |
| Phép dịch phải không nhận dấu (toán hạng đang được dịch chuyển) | >>> |
| Phép chia (cả hai toán hạng) | / |

## Slide 8 · Danh sách các trường hợp sử dụng kết quả số học không được gán

| STT | Trường hợp |
|---|---|
| 1 | Là phần tử được tính khi tạo một mảng mới |
| 2 | Là một chỉ số mảng |
| 3 | Như điều kiện của biểu thức if |
| 4 | Là tham số cho một phương thức (cũng được yêu cầu với công nghệ Java) |
| 5 | Là giá trị quyết định trong câu lệnh switch |

## Slide 9 · Danh sách các trường hợp sử dụng kết quả số học không được gán

```java
short s=2, c=4, d=5;
byte a=1, b=3;
byte array[] = new byte[(byte)(a*b)];
s = (short) ( (byte)(a/b) >> 2);
s = (short) ( b >> (a*b));
s = (short) ((byte) (a*b)/c);
s = (short) ((short) (a*b)/(short)(c*d));
s = (short) array[(short)(a/b)];
s = (short) ((short)((c*c) & d)/a);
if ((short)(a*b*c) < 10) { … }
switch ((byte) (c >> b)) { … }
```

## Slide 10 · Mảng

- Trong Java Card chỉ hỗ trợ mảng một chiều

***Ví dụ***

```java
byte a[] = new byte[10];
AID aid[] = new AID[5];
```

- Mảng là đối tượng → Mảng sẽ kế thừa các phương thức của Lớp `Object`
- Một số hàm hay dùng:
  - `Util.arrayCompare()`.
  - `Util.arrayCopy()`.
  - `Util.arrayCopyNonAtomic()`.

## Slide 11 · Đặc điểm của nền tảng Java Card

- Trong Java Card không hỗ trợ các luồng → không hỗ trợ từ khóa `thread`
- Không hỗ trợ trình gom rác → phương thức `finalize()` không được hỗ trợ
- Tính kế thừa được hỗ trợ y như trong Java
- Tất cả các tính năng bảo mật biên dịch và ngôn ngữ trong Java cũng đều được hỗ trợ bởi Java Card
- Tính linh động (API)
- Ngoại lệ
- Các lớp lõi của API Java Card đơn giản và gọn nhẹ hơn trong Java
