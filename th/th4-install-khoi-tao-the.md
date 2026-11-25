# Bước 4 · TH4 → Tham số cài đặt (PIN quản trị) và lệnh phát hành thẻ

> Tài liệu: [th4-phuong-thuc-install.md](../../th4-phuong-thuc-install.md).
> Mốc: **Lần 1 (30/11)** – "Triển khai lưu thông tin lên thẻ"; tiêu chí chấm "Khởi tạo thẻ (0.5)". Người phụ trách: ……

## 1. Bài học cần nắm

- `install()` giống `main()`: JCRE gọi một lần, tạo thể hiện applet bằng `new`, thể hiện phải `register()`.
- Cấp phát **mọi** đối tượng trong hàm tạo: thiếu bộ nhớ thì JCRE hủy cài đặt sạch sẽ, không có applet "dở dang"; về sau không bao giờ bị hết bộ nhớ giữa lúc chạy.
- Định dạng tham số cài đặt (từ Java Card 2.1.2):

  ```text
  bArray[bOffset]            = Li  độ dài AID
  tiếp theo Li byte          = AID thể hiện
  tiếp theo                  = Lc  độ dài thông tin điều khiển
  tiếp theo Lc byte          = thông tin điều khiển
  tiếp theo                  = La  độ dài dữ liệu applet
  tiếp theo La byte          = dữ liệu applet  ← ô "Application Specific Parameters" trong JCIDE
  ```

## 2. Áp dụng vào đề tài

Thẻ có hai giai đoạn, giống thẻ thật:

| Giai đoạn                                                       | Ai làm                  | Cách                                      | Ghi gì                                                                                                                                                                     |
| --------------------------------------------------------------- | ----------------------- | ----------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Cài applet** (nhà sản xuất / trường nạp applet vào thẻ trắng) | JCIDE `Run`             | Tham số cài đặt                           | **PIN quản trị** (`38 37 36 35 34 33 32 31` = `87654321`; thêm `00` + PUK quản trị nếu muốn có đường khôi phục), salt ngẫu nhiên, cấp phát toàn bộ bộ nhớ, `state = BLANK` |
| **Phát hành (khởi tạo) thẻ** cho từng sinh viên                 | Phòng Đào tạo, phần mềm | Lệnh `INIT_CARD 30` sau `VERIFY_ADMIN 20` | Mã SV, PIN ban đầu, PUK, (bước 6: sinh DEK, cặp khóa RSA) → `state = ACTIVE`                                                                                               |

Vì sao tách: PIN quản trị là bí mật của trường, có trước khi biết thẻ phát cho ai; mã SV/PIN thì mỗi thẻ một khác. Lệnh `INIT_CARD` chỉ chạy được **một lần** (thẻ phải đang BLANK) và **phải có quyền quản trị** – sinh viên không tự "phát hành lại" thẻ của mình được.

## 3. Việc cần làm

- [ ] Hàm tạo đọc tham số theo đúng định dạng mục 1 (giống `Applet2` của TH4 và `cnt` của tài liệu kết nối). Không có tham số hoặc sai độ dài → **từ chối cài** (`SW_WRONG_DATA`, kiểm trước khi cấp phát; trước 24/11 là PIN mặc định `12345678`, bỏ vì ai cũng biết, F-01). Dữ liệu = `PIN quản trị` hoặc `PIN quản trị | 00 | PUK quản trị (12–16 ký tự)`; PUK quản trị là đường khôi phục khi PIN quản trị bị khóa (`UNBLOCK_ADMIN 24`, F-02).
- [ ] Gọi `register(...)` **cuối** hàm tạo.
- [ ] `VERIFY_ADMIN 20`, `INIT_CARD 30` (data `LV(mã SV) LV(PIN) LV(PUK)`), `state` BLANK → ACTIVE.
- [ ] Bản mốc 1: lưu thêm thông tin sinh viên **dạng rõ** bằng `WRITE_OBJ 40`/`READ_OBJ 41` (bước 6 thay bằng mã hóa, giữ nguyên định dạng lệnh).
- [ ] JCIDE: nhập Instance AID + tham số theo [04 mục 2.4](../04-moi-truong-cau-hinh.md#24-tham-số-cài-đặt-applet-pin-quản-trị).
- [ ] Phần mềm: màn "Phát hành thẻ" (nhập PIN quản trị, mã SV, PIN ban đầu; sinh PUK ngẫu nhiên 8 số và in ra cho sinh viên).

## 4. Mã (trích bản cuối)

```java
private StudentCard(byte[] bArray, short bOffset, byte bLength) {
    // 0. đọc + KIỂM tham số cài đặt (TH4 mục 4.3) trước khi cấp phát: sai thì ném 6A80, không để lại gì
    short blen = (short) (bLength & 0x00FF);
    short end = (short) (bOffset + blen);
    short p = (short) (bOffset + (bArray[bOffset] & 0x00FF) + 1);   // qua AID
    p = (short) (p + (bArray[p] & 0x00FF) + 1);                     // qua thông tin điều khiển
    short la = (short) (bArray[p] & 0x00FF);
    p = (short) (p + 1);
    // ... kiểm p < end, p + la <= end; tách PIN | 00 | PUK quản trị; PIN 4–16, PUK 12–16 ...
    // 1. cấp phát hết (TH4 mục 4.1)
    cardId = new byte[CARD_ID_MAX];
    ...
    pin = new OwnerPIN(PIN_TRY_LIMIT, HASH_MAX);
    admin = new OwnerPIN(ADMIN_TRY_LIMIT, HASH_MAX);
    tmp = JCSystem.makeTransientByteArray(TMP_LEN, JCSystem.CLEAR_ON_RESET);
    ...
    // 2. nạp PIN quản trị (và PUK quản trị nếu có) dưới dạng băm
    setSecret(admin, bArray, p, (byte) pinLen);
    if (hasPuk) { /* băm PUK vào adminPukHash, adminPukSet = true */ }
    state = STATE_BLANK;
    // 3. đăng ký cuối cùng
    byte li = bArray[bOffset];
    if (li == 0) {
        register();
    } else {
        register(bArray, (short) (bOffset + 1), li);
    }
}
```

`initCard` (bản cuối): `requireState(STATE_BLANK)` → `requireAdmin()` → tách 3 trường LV → chép mã SV → sinh DEK, bọc bằng KEK(PIN), KEK(PUK) → lưu băm PIN/PUK → `rsaPair.genKeyPair()` → đặt số dư, nhật ký về 0 → **`state = STATE_ACTIVE` sau cùng**.

## 5. Kiểm thử (đã chạy trên giả lập)

```text
>> B0 30 00 00 19 08 43 54 30 37 30 32 35 34 06 31 32 33 34 35 36 08 31 32 33 34 35 36 37 38
<< 69 82                                   chưa đăng nhập quản trị
>> B0 20 00 00 08 38 37 36 35 34 33 32 30 << 63 C4   PIN quản trị sai
>> B0 20 00 00 08 38 37 36 35 34 33 32 31 << 90 00
>> B0 30 00 00 19 08 43 54 30 37 30 32 35 34 06 31 32 33 34 35 36 08 31 32 33 34 35 36 37 38
<< 90 00
>> B0 10 00 00 00 << 01 03 05 01 00 00 00 00 00 00 08 43 54 30 37 30 32 35 34 90 00
>> B0 30 00 00 19 …  (gửi lại)            << 69 85   thẻ đã phát hành, không khởi tạo lại được
```

Giải mã lệnh `30`: `08` + `43 54 30 37 30 32 35 34` ("CT070254") · `06` + `31…36` ("123456") · `08` + `31…38` ("12345678"); Lc = 1+8+1+6+1+8 = 25 = `0x19`.

## 6. Câu hỏi có thể bị hỏi

1. _Vì sao tạo hết đối tượng trong hàm tạo?_ – TH4 mục 4.1.
2. _Tham số cài đặt gồm những phần nào, đọc thế nào?_ – mục 1; chỉ ra dòng tương ứng trong hàm tạo.
3. _Vì sao `register` đặt cuối?_ – nếu cấp phát lỗi trước đó, applet chưa đăng ký → JCRE dọn sạch.
4. _Ai được khởi tạo thẻ? Khởi tạo hai lần được không?_ – chỉ quản trị; không (`69 85`).
5. _Rút thẻ giữa lúc khởi tạo?_ – `state` ghi cuối nên vẫn BLANK, phát hành lại được.

## Nhật ký

| Ngày | Ai  | Làm được | Vướng |
| ---- | --- | -------- | ----- |
|      |     |          |       |

Phương thức install dùng để làm gì?
Phương thức để khởi tạo đối tượng với hệ điểu hành như thế nào?
Dối tượng và mảng có thể tạo bất kì thời điểm nào trong lúc khởi tạo aplet, nhưng tốt nhất nên tạo tròn quá trình khởi tạo aplet
Trong aplet thì contructor có tham số hay không có tham số nếu có tham số thì người ta sẽ truyền vào đây những tham số gì
Phương thưc register dùng để làm gì?
Javacrad cung cấp mấy phương thức register?
Mỗi aplet được định danh bởi iede định danh, sau này sẽ có iede khác với giá trị ban đầu tại sao lại thế?
Tại sao cần có sự thay đổi trong qua trình hoạt động
