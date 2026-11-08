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

| Giai đoạn | Ai làm | Cách | Ghi gì |
|---|---|---|---|
| **Cài applet** (nhà sản xuất / trường nạp applet vào thẻ trắng) | JCIDE `Run` | Tham số cài đặt | **PIN quản trị** (`38 37 36 35 34 33 32 31` = `87654321`), salt ngẫu nhiên, cấp phát toàn bộ bộ nhớ, `state = BLANK` |
| **Phát hành (khởi tạo) thẻ** cho từng sinh viên | Phòng Đào tạo, phần mềm | Lệnh `INIT_CARD 30` sau `VERIFY_ADMIN 20` | Mã SV, PIN ban đầu, PUK, (bước 6: sinh DEK, cặp khóa RSA) → `state = ACTIVE` |

Vì sao tách: PIN quản trị là bí mật của trường, có trước khi biết thẻ phát cho ai; mã SV/PIN thì mỗi thẻ một khác. Lệnh `INIT_CARD` chỉ chạy được **một lần** (thẻ phải đang BLANK) và **phải có quyền quản trị** – sinh viên không tự "phát hành lại" thẻ của mình được.

## 3. Việc cần làm

- [ ] Hàm tạo đọc tham số theo đúng định dạng mục 1 (giống `Applet2` của TH4 và `cnt` của tài liệu kết nối). Không có tham số → PIN quản trị mặc định `12345678`.
- [ ] Gọi `register(...)` **cuối** hàm tạo.
- [ ] `VERIFY_ADMIN 20`, `INIT_CARD 30` (data `LV(mã SV) LV(PIN) LV(PUK)`), `state` BLANK → ACTIVE.
- [ ] Bản mốc 1: lưu thêm thông tin sinh viên **dạng rõ** bằng `WRITE_OBJ 40`/`READ_OBJ 41` (bước 6 thay bằng mã hóa, giữ nguyên định dạng lệnh).
- [ ] JCIDE: nhập Instance AID + tham số theo [04 mục 2.4](../04-moi-truong-cau-hinh.md#24-tham-số-cài-đặt-applet-pin-quản-trị).
- [ ] Phần mềm: màn "Phát hành thẻ" (nhập PIN quản trị, mã SV, PIN ban đầu; sinh PUK ngẫu nhiên 8 số và in ra cho sinh viên).

## 4. Mã (trích bản cuối)

```java
private StudentCard(byte[] bArray, short bOffset, byte bLength) {
    // 1. cấp phát hết (TH4 mục 4.1)
    cardId = new byte[CARD_ID_MAX];
    ...
    pin = new OwnerPIN(PIN_TRY_LIMIT, HASH_MAX);
    admin = new OwnerPIN(ADMIN_TRY_LIMIT, HASH_MAX);
    tmp = JCSystem.makeTransientByteArray(TMP_LEN, JCSystem.CLEAR_ON_RESET);
    ...
    // 2. đọc tham số cài đặt (TH4 mục 4.3)
    byte li = bArray[bOffset];
    short p = (short) (bOffset + li + 1);
    byte lc = bArray[p];
    p = (short) (p + lc + 1);
    byte la = bArray[p];
    p = (short) (p + 1);
    if (la >= SECRET_MIN && la <= SECRET_MAX) {
        setSecret(admin, bArray, p, la);          // lưu dạng băm
    } else {
        byte[] def = {'1', '2', '3', '4', '5', '6', '7', '8'};
        setSecret(admin, def, (short) 0, (byte) def.length);
    }
    state = STATE_BLANK;
    // 3. đăng ký cuối cùng
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

1. *Vì sao tạo hết đối tượng trong hàm tạo?* – TH4 mục 4.1.
2. *Tham số cài đặt gồm những phần nào, đọc thế nào?* – mục 1; chỉ ra dòng tương ứng trong hàm tạo.
3. *Vì sao `register` đặt cuối?* – nếu cấp phát lỗi trước đó, applet chưa đăng ký → JCRE dọn sạch.
4. *Ai được khởi tạo thẻ? Khởi tạo hai lần được không?* – chỉ quản trị; không (`69 85`).
5. *Rút thẻ giữa lúc khởi tạo?* – `state` ghi cuối nên vẫn BLANK, phát hành lại được.

## Nhật ký

| Ngày | Ai | Làm được | Vướng |
|---|---|---|---|
| | | | |
