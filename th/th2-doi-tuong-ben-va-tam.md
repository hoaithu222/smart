# Bước 2 · TH2 + Bài tập 2 → PIN, đếm sai, khóa PIN, phiên đăng nhập

> Tài liệu: [th2-cac-doi-tuong-trong-java-card.md](../../th2-cac-doi-tuong-trong-java-card.md), [bai-tap-2.md](../../bai-tap-2.md).
> Mốc: **Lần 1 (30/11)** – "Tạo mã PIN, thay đổi, kiểm tra mã PIN, có cơ chế giới hạn số lần nhập sai". Người phụ trách: ……

## 1. Bài học cần nắm

| | Đối tượng bền (persistent) | Đối tượng tạm (transient) |
|---|---|---|
| Tạo bằng | `new` | `JCSystem.makeTransient…Array(len, event)` |
| Nằm ở | EEPROM | RAM (tham chiếu tới nó thì nằm ở EEPROM) |
| Mất khi | Không mất (trừ khi xóa applet) | `CLEAR_ON_RESET`: reset/rút thẻ · `CLEAR_ON_DESELECT`: bỏ chọn applet hoặc reset |
| Cập nhật 1 trường | Nguyên tử | Không nguyên tử, không tham gia giao dịch |
| Ghi | Chậm, có giới hạn số lần ghi | Nhanh, không hao mòn |

Kết quả thí nghiệm TH2: bỏ chọn applet → `buffer1` (DESELECT) về 0; reset → cả `buffer1`, `buffer2` (RESET) về 0; biến `x` và `buffer` bền vẫn giữ.

## 2. Áp dụng vào đề tài (trả lời luôn Bài tập 2)

| Dữ liệu trong đề tài | Loại | Lý do |
|---|---|---|
| Bộ đếm sai PIN (`wrongTryCounter` của Bài tập 2) | **Bền** | Nếu để tạm, kẻ gian thử sai 2 lần rồi rút thẻ → bộ đếm về 0 → dò PIN vô hạn. Reset giữa chừng **không mất** vì nằm ở EEPROM |
| Giá trị PIN (dạng băm), PUK, PIN quản trị | Bền | Phải còn qua các lần cắm thẻ |
| Cờ "đã nhập PIN đúng" trong phiên | Tạm | Rút thẻ / chọn applet khác là phải đăng nhập lại |
| Khóa dữ liệu DEK sau khi mở bằng PIN (bước 6) | Tạm, `CLEAR_ON_DESELECT` | Không để khóa nằm lại khi applet khác được chọn |
| Bộ đệm `tempBuffer` nhập dữ liệu rồi giải phóng (Bài tập 2 bài 2) | Tạm | Dữ liệu trung gian, ghi nhiều lần; trong đề tài là mảng `tmp` |
| Thông tin sinh viên, số dư | Bền | |

Vì sao `tmp` là `CLEAR_ON_RESET` chứ không phải `CLEAR_ON_DESELECT`: hàm tạo cần băm PIN quản trị bằng `tmp` **khi đang cài đặt** – lúc đó applet chưa được chọn, truy cập mảng `CLEAR_ON_DESELECT` bị JCRE chặn. Đổi lại, applet tự xóa `tmp` sau mỗi thao tác nhạy cảm.

## 3. Việc cần làm

Bản **đủ mốc 1** (chưa có mật mã, bước 6 nâng cấp):

- [ ] Thêm `VERIFY_PIN 21`, `CHANGE_PIN 22`, `GET_STATUS` trả số lần còn lại.
- [ ] Sai 3 lần → khóa, trả `69 83`; sai lần 1, 2 → trả `63 C2`, `63 C1`.
- [ ] Nhập đúng → đặt lại bộ đếm.
- [ ] Lệnh cần đăng nhập (`GET_BALANCE`, `READ_OBJ`…) kiểm cờ phiên, chưa đăng nhập → `69 82`.
- [ ] `deselect()` xóa cờ phiên.
- [ ] Làm thí nghiệm TH2 trên chính applet: đăng nhập → chọn applet khác → chọn lại → lệnh cần PIN phải trả `69 82`.
- [ ] Phần mềm: màn hình nhập PIN, hiển thị "còn x lần", nút đổi PIN.

## 4. Mã

### 4.1 Bản tự viết (đúng tinh thần Bài tập 2 – nên viết bản này trước để hiểu)

```java
private byte[] pinValue;          // bền
private byte pinLen;
private byte wrongTryCounter;     // BỀN: trả lời câu 1 Bài tập 2
private boolean[] session;        // TẠM: session[0] = đã nhập PIN đúng

// trong hàm tạo
pinValue = new byte[16];
session = JCSystem.makeTransientBooleanArray((short) 1, JCSystem.CLEAR_ON_DESELECT);

private void verifyPin(APDU apdu) {
    byte[] buf = apdu.getBuffer();
    short len = apdu.setIncomingAndReceive();
    if (wrongTryCounter >= 3) {
        ISOException.throwIt((short) 0x6983);
    }
    // TĂNG BỘ ĐẾM TRƯỚC KHI SO SÁNH: rút thẻ đúng lúc so sánh cũng không né được lần đếm
    wrongTryCounter++;
    if (len == pinLen
            && Util.arrayCompare(buf, ISO7816.OFFSET_CDATA, pinValue, (short) 0, pinLen) == 0) {
        wrongTryCounter = 0;
        session[0] = true;
        return;
    }
    session[0] = false;
    ISOException.throwIt((short) (0x63C0 | (byte) (3 - wrongTryCounter)));
}
```

Thứ tự "tăng bộ đếm rồi mới so sánh" là điểm hay bị hỏi: nếu so sánh trước, kẻ gian rút thẻ ngay khi thấy sai (trước lúc ghi bộ đếm) sẽ thử được vô hạn lần. `OwnerPIN` của Java Card làm sẵn việc này.

### 4.2 Bản cuối (dùng `OwnerPIN`, PIN lưu dạng băm)

Trong [StudentCard.java](../code/applet/src/sinhvien/StudentCard.java): `pin = new OwnerPIN(PIN_TRY_LIMIT, HASH_MAX)`, các hàm `checkSecret`, `throwWrongSecret`, `verifyPin`, `changePin`, `deselect`. Phần băm và khóa AES giải thích ở [bước 6](th6-mat-ma.md).

```java
public void deselect() {
    pin.reset();      // bỏ cờ đã xác thực
    puk.reset();
    admin.reset();
    dek.clearKey();   // khóa phiên (RAM)
    kek.clearKey();
}
```

## 5. Kiểm thử

```text
>> B0 21 00 00 06 30 30 30 30 30 30     << 63 C2   sai lần 1
>> B0 21 00 00 06 31 31 31 31 31 31     << 63 C1   sai lần 2
>> B0 21 00 00 06 31 32 33 34 35 36     << 90 00   đúng → bộ đếm về 3
>> B0 10 00 00 00                        << 01 03 05 02 …   (byte thứ 2 = 03, cờ 02 = đã nhập PIN)
   (chọn applet khác trong tab Manager rồi chọn lại)
>> B0 50 00 00 00                        << 69 82   phiên đã bị xóa khi bỏ chọn
   (sai 3 lần liên tiếp)
>> B0 21 00 00 06 31 32 33 34 35 36     << 69 83   PIN đúng nhưng đã khóa
```

## 6. Câu hỏi có thể bị hỏi

1. *`wrongTryCounter` là loại đối tượng nào? Reset giữa chừng có mất không?* – Bền; không mất (mục 2).
2. *Biến `tempBuffer` nhập rồi giải phóng nên là loại gì?* – Tạm; `CLEAR_ON_DESELECT` nếu chỉ dùng trong lúc applet được chọn.
3. *Khác nhau `CLEAR_ON_RESET` và `CLEAR_ON_DESELECT`?* – mục 1; `DESELECT` còn được tường lửa bảo vệ thêm.
4. *Vì sao cờ "đã nhập PIN" phải là tạm?* – để rút thẻ / đổi applet là mất quyền.
5. *`OwnerPIN` khác tự viết ở đâu?* – bộ đếm và cờ do JCRE bảo vệ, không bị giao dịch hoàn tác, đã xử lý thứ tự giảm-trước-so-sánh.

## Nhật ký

| Ngày | Ai | Làm được | Vướng |
|---|---|---|---|
| | | | |
