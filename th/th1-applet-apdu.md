# Bước 1 · TH1 → Khung applet, bảng lệnh APDU, `GET_STATUS`

> Tài liệu: [th1-bai-tap.md](../../th1-bai-tap.md) (phiếu bài tập; bài hướng dẫn "máy tính bỏ túi" không có trong bộ PDF), lý thuyết APDU ở [bai1 slide 30–37](../../bai1-tong-quan-the-thong-minh.md), lớp `Applet`/`APDU`/`ISO7816` ở [bai4](../../bai4-java-card-api.md), giới hạn ngôn ngữ ở [bai3](../../bai3-ngon-ngu-java-trong-java-card.md).
> Mốc: **Lần 1 (30/11)**. Người phụ trách: ……

## 1. Bài học cần nắm

- Applet kế thừa `javacard.framework.Applet`; JCRE gọi `install()` một lần lúc cài, gọi `process(APDU)` cho mỗi lệnh.
- APDU lệnh: `CLA INS P1 P2 [Lc Data] [Le]`; APDU phản hồi: `[Data] SW1 SW2`; `90 00` là thành công.
- `selectingApplet()` để bỏ qua chính lệnh SELECT; CLA sai → `SW_CLA_NOT_SUPPORTED`, INS lạ → `SW_INS_NOT_SUPPORTED`.
- Nhận dữ liệu: `apdu.setIncomingAndReceive()` trả về số byte, dữ liệu bắt đầu ở `ISO7816.OFFSET_CDATA` (= 5). Gửi dữ liệu: ghi vào `apdu.getBuffer()` rồi `apdu.setOutgoingAndSend(0, len)`.
- Java Card: chỉ `byte`, `short`, `boolean` (int tùy thẻ) → mọi phép tính phải ép kiểu.

## 2. Áp dụng vào đề tài

| Bài tập TH1 | Thành phần tương ứng trong đề tài |
|---|---|
| Bài 1 "Xin chao, KMA" | Lệnh `GET_STATUS 10` – lệnh đầu tiên phần mềm gửi khi cắm thẻ |
| Bài 2 in họ tên / ngày sinh (giá trị cố định) | Đọc thông tin (`READ_OBJ 41`) – bước này cho phép giá trị cố định |
| Bài 4 nhập chuỗi mã SV, họ tên, ngày sinh, quê quán, in riêng từng trường | Ghi/đọc thông tin dạng **TLV** (`WRITE_OBJ 40` / `READ_OBJ 41`): mỗi trường `[tag][độ dài][giá trị]`, in riêng từng trường = máy tính tách TLV |

Chốt ngay ở bước này: **AID, CLA `B0`, toàn bộ bảng INS** ([03 mục 3](../03-thiet-ke.md#3-bảng-lệnh-apdu)). Các bước sau chỉ thêm `case`, không đổi mã lệnh – phần mềm máy tính viết song song được.

## 3. Việc cần làm

- [ ] Tạo project JCIDE theo [04 mục 2](../04-moi-truong-cau-hinh.md#2-cấu-hình-jcide), AID `4B 4D 41 53 56 01` / `…01 01`.
- [ ] Gõ khung applet dưới đây, build 0 lỗi.
- [ ] Chạy, `/select 4B4D4153560101`, `/send B0100000` → nhận `00 … 90 00`.
- [ ] Thử CLA sai (`00 10 00 00`) → `6E 00`; INS sai (`B0 99 00 00`) → `6D 00`.
- [ ] (song song) Người làm phần mềm: tạo project NetBeans, chép `sinhvien.host`, nút Kết nối chạy được với `PcscChannel`.

## 4. Mã bước 1

Khung tối thiểu (bản cuối: [StudentCard.java](../code/applet/src/sinhvien/StudentCard.java) – phần `process` giữ nguyên cấu trúc này):

```java
package sinhvien;

import javacard.framework.*;

public class StudentCard extends Applet {
    static final byte CLA_SV = (byte) 0xB0;
    static final byte INS_GET_STATUS = (byte) 0x10;
    // ... khai báo đủ INS ở 03-thiet-ke.md mục 3 ngay từ đầu

    static final byte STATE_BLANK = (byte) 0x00;
    private byte state;
    private byte[] cardId;          // bước 4 mới ghi giá trị
    private byte cardIdLen;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new StudentCard(bArray, bOffset, bLength);
    }

    private StudentCard(byte[] bArray, short bOffset, byte bLength) {
        cardId = new byte[16];
        state = STATE_BLANK;
        byte li = bArray[bOffset];
        if (li == 0) {
            register();
        } else {
            register(bArray, (short) (bOffset + 1), li);
        }
    }

    public void process(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        if (selectingApplet()) {
            return;
        }
        if (buf[ISO7816.OFFSET_CLA] != CLA_SV) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }
        switch (buf[ISO7816.OFFSET_INS]) {
        case INS_GET_STATUS:
            getStatus(apdu);
            return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    private void getStatus(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short p = 0;
        buf[p++] = state;
        buf[p++] = cardIdLen;
        p = Util.arrayCopyNonAtomic(cardId, (short) 0, buf, p, cardIdLen);
        apdu.setOutgoingAndSend((short) 0, p);
    }
}
```

Khác với ví dụ trong tài liệu (để giải thích được khi bị hỏi):

- Biến lưu dữ liệu là **trường của đối tượng** (`private byte[] cardId`), không phải `static` như ví dụ TH2/TH3 – `static` thì mọi thể hiện applet dùng chung một vùng nhớ.
- Chỉ gọi `setIncomingAndReceive()` trong lệnh **có** dữ liệu gửi xuống; lệnh chỉ trả dữ liệu (`GET_STATUS`) không gọi.

## 5. Lỗi biên dịch hay gặp

| Viết | Lỗi | Sửa |
|---|---|---|
| `short c = a + b;` | int → short | `short c = (short) (a + b);` |
| `buf[off + 1]` (off là short) | chỉ số kiểu int | `buf[(short) (off + 1)]` |
| `int x = …` | thẻ không có int | dùng `short`; số tiền tính theo nghìn đồng |
| `String name = "…"` | không có `String` | mảng `byte[]` ASCII/UTF-8 |
| `@Override` | `-source 1.5` của bộ chuyển đổi | bỏ annotation |
| `byte[][]` | chỉ hỗ trợ mảng 1 chiều | nhiều mảng riêng (`infoData`, `contactData`…) |
| `x % 10` | phép `%` cần ép kiểu toán hạng | tránh, dùng so sánh + gán lại (xem `appendLog`) |

## 6. Kiểm thử

```text
>> /select 4B4D4153560101
<< 90 00
>> /send B0100000
<< 00 00 90 00            (bản bước 1: state=00, Lid=00)
>> /send 00100000
<< 6E 00
>> /send B0990000
<< 6D 00
```

## 7. Câu hỏi có thể bị hỏi

1. *APDU gồm những trường nào? `Lc`, `Le` để làm gì?* – Lc = số byte dữ liệu gửi xuống, Le = số byte mong nhận về.
2. *Vì sao lệnh SELECT phải `return` sớm trong `process`?* – JCRE chuyển cả lệnh SELECT cho applet; không bỏ qua thì applet coi `A4` là INS lạ, trả `6D 00`.
3. *`register()` khác `register(bArray, …)` thế nào?* – không tham số dùng AID mặc định trong file CAP; có tham số dùng AID trong tham số cài đặt (TH4).
4. *Vì sao phải ép kiểu `(short)` khắp nơi?* – phép tính trung gian trong Java luôn ra `int`, nhiều thẻ Java Card không hỗ trợ `int` (bài 3).

## Nhật ký

| Ngày | Ai | Làm được | Vướng |
|---|---|---|---|
| | | | |
