# Bước 3 · TH3 → Ví sinh viên: nạp, thanh toán, nhật ký vòng (giao dịch nguyên tử)

> Tài liệu: [th3-thao-tac-nguyen-to-va-giao-dich.md](../../th3-thao-tac-nguyen-to-va-giao-dich.md), lớp `Util`/`JCSystem` ở [bai4 slide 19, 22–23](../../bai4-java-card-api.md).
> Mốc: lõi ví làm sớm được từ bây giờ; phần ký RSA thêm ở bước 6; tính điểm ở **Lần 3**. Người phụ trách: ……

## 1. Bài học cần nắm

- Cập nhật **một** trường của đối tượng bền là nguyên tử (mất điện → về giá trị cũ).
- Cập nhật **nhiều** trường cùng lúc phải bọc `JCSystem.beginTransaction()` … `commitTransaction()`; `abortTransaction()` hoàn tác.
- Chỉ đối tượng bền tham gia giao dịch; mảng tạm và biến cục bộ **không** được hoàn tác (ví dụ 3.3: `buffer2` tạm và `a_local` giữ giá trị mới sau `abort`).
- `Util.arrayCopy` ghi vào mảng bền là nguyên tử; `arrayCopyNonAtomic`/`arrayFillNonAtomic` thì không (dùng cho RAM, nhanh hơn).

## 2. Áp dụng vào đề tài

Một lần thanh toán đổi **3 thứ**: số dư, bộ đếm giao dịch, một bản ghi nhật ký. Rút thẻ giữa chừng mà chỉ đổi được 1–2 thứ thì sổ sách lệch (trừ tiền mà không có nhật ký, hoặc ngược lại) → bọc cả 3 trong một giao dịch.

| Thao tác trong đề tài | Cơ chế |
|---|---|
| Nạp / thanh toán / điểm danh | `beginTransaction` → `balance`, `txCounter`, `appendLog` → `commitTransaction` |
| Đổi PIN (bước 6) | Giao dịch quanh `dekByPin` + `pin.update` |
| Ghi một khối dữ liệu | Không cần giao dịch: `Util.arrayCopy` (nguyên tử) rồi mới cập nhật `objLen` |
| Phát hành thẻ | Không bọc cả lệnh (quá dài cho bộ đệm giao dịch); ghi `state = ACTIVE` **cuối cùng** → hỏng giữa chừng thì làm lại |

Quy ước tiền: `short`, đơn vị **nghìn đồng**, trần 30 000 (30 triệu) – vì Java Card 2.2.2 không chắc có `int`. Mã lỗi lấy theo applet mẫu *Wallet* của Java Card Kit: `6A 83` số tiền sai, `6A 84` vượt trần, `6A 85` không đủ tiền.

## 3. Việc cần làm

- [ ] Thêm trường `balance`, `txCounter`, mảng `log` 120 byte, `logHead`, `logCount`.
- [ ] `GET_BALANCE 50`, `TOPUP 51`, `PAY 52`, `GET_LOG 53` (bản bước 3: trả `số dư(2) bộ đếm(2)`, chưa có chữ ký).
- [ ] Mỗi giao dịch nhập PIN ngay trong lệnh (`[Lpin][PIN]…`) – yêu cầu đề: "mỗi lần thanh toán, nạp tiền yêu cầu xác thực bằng mã PIN".
- [ ] Kiểm tra trước, ghi sau: mọi `throwIt` (sai PIN, số tiền sai, không đủ tiền) nằm **trước** `beginTransaction`.
- [ ] Thí nghiệm: thêm tạm một dòng `ISOException.throwIt(…)` sau khi ghi `balance` nhưng trước `commitTransaction` → JCRE tự hủy giao dịch → số dư không đổi. Ghi kết quả vào nhật ký rồi xóa dòng thử.
- [ ] Phần mềm: màn thu ngân (nhập số tiền, chọn dịch vụ), màn lịch sử.

## 4. Mã (trích bản cuối)

```java
// 0x51 nạp / 0x52 thanh toán. Data: [Lpin][PIN][số tiền(2)][thời gian(4)][nonce(8)]
private void transfer(APDU apdu, byte type) {
    ...                                     // tách PIN, kiểm PIN, kiểm số tiền -> có thể throw
    short newBal;
    if (type == TX_TOPUP) {
        if (balance > (short) (MAX_BALANCE - amount)) {   // không viết balance + amount > MAX: tràn short
            ISOException.throwIt(SW_EXCEED_MAX_BALANCE);
        }
        newBal = (short) (balance + amount);
    } else {
        if (amount > balance) {
            ISOException.throwIt(SW_NOT_ENOUGH_BALANCE);
        }
        newBal = (short) (balance - amount);
    }

    JCSystem.beginTransaction();            // ---- 3 thay đổi cùng thành công hoặc cùng hủy
    balance = newBal;
    txCounter = (short) (txCounter + 1);
    appendLog(type, amount, buf, (short) (p + 2));
    JCSystem.commitTransaction();

    signReceipt(apdu, type, amount, buf, (short) (p + 2));   // bước 6
}

/** Bộ đệm vòng 10 bản ghi × 12 byte, gọi bên trong giao dịch. */
private void appendLog(byte type, short value, byte[] src, short timeOff) {
    short off = (short) (logHead * LOG_REC);
    log[off] = type;
    log[(short) (off + 1)] = 0;
    Util.setShort(log, (short) (off + 2), value);
    Util.setShort(log, (short) (off + 4), balance);
    Util.setShort(log, (short) (off + 6), txCounter);
    Util.arrayCopy(src, timeOff, log, (short) (off + 8), (short) 4);
    logHead++;
    if (logHead == LOG_SIZE) {      // thay cho (logHead + 1) % LOG_SIZE
        logHead = 0;
    }
    if (logCount < LOG_SIZE) {
        logCount++;
    }
}
```

Kiểm tràn: viết `balance + amount > MAX_BALANCE` sai vì `balance + amount` có thể vượt 32 767 và quay sang số âm khi ép về `short`; viết dạng `balance > MAX_BALANCE - amount` thì không bao giờ tràn.

## 5. Kiểm thử (đã chạy trên giả lập)

```text
>> B0 21 00 00 06 31 32 33 34 35 36                                                  << 90 00
>> B0 51 00 00 15 06 31 32 33 34 35 36 01 F4 00 00 00 00 01 02 03 04 05 06 07 08     << 01 F4 00 01 … 90 00   số dư 500, bộ đếm 1
>> B0 52 00 00 15 06 31 32 33 34 35 36 00 78 00 00 00 00 01 02 03 04 05 06 07 09     << 01 7C 00 02 … 90 00   còn 380
>> B0 52 00 00 15 06 31 32 33 34 35 36 03 E8 00 00 00 00 01 02 03 04 05 06 07 0A     << 6A 85                 trả 1000: không đủ
>> B0 50 00 00 00                                                                    << 01 7C 00 02 02 90 00  số dư không đổi
>> B0 53 00 00 00                                                                    << 02 00 00 78 01 7C 00 02 00 00 00 00
                                                                                        01 00 01 F4 01 F4 00 01 00 00 00 00 90 00
```

## 6. Câu hỏi có thể bị hỏi

1. *Rút thẻ ngay sau khi trừ tiền nhưng trước khi ghi nhật ký thì sao?* – Cả hai nằm trong một giao dịch: lần cấp điện sau JCRE hoàn tác, số dư như cũ.
2. *Biến cục bộ / mảng tạm có được hoàn tác không?* – Không (ví dụ 3.3 TH3).
3. *Thẻ đã trừ tiền nhưng phần mềm mất kết nối trước khi nhận biên nhận?* – Giao dịch trên thẻ đã commit; lần sau phần mềm đọc `GET_LOG`, so bộ đếm với CSDL để bổ sung (đối soát).
4. *Vì sao không dùng `int` cho tiền?* – Bài 3: không phải thẻ nào cũng có `int`.
5. *Vì sao `arrayCopy` chứ không `arrayCopyNonAtomic` khi ghi vào nhật ký?* – Ghi vào mảng bền cần nguyên tử; `NonAtomic` dành cho RAM.

## Nhật ký

| Ngày | Ai | Làm được | Vướng |
|---|---|---|---|
| | | | |
