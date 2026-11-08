# Bước 6 · TH6 → Băm PIN, AES khóa sinh từ PIN, RSA xác thực thẻ và ký giao dịch

> Tài liệu: [th6-mat-ma-trong-java-card.md](../../th6-mat-ma-trong-java-card.md) (lý thuyết: băm, khóa công khai, chữ ký số, challenge-response, AES), [bai4 slide 24](../../bai4-java-card-api.md) (gói `javacard.security`, `javacardx.crypto`).
> Mốc: **Lần 2 (14/12)** – "lưu thông tin và mã hóa bằng AES; xác thực thẻ bằng RSA". Tiêu chí chấm: mã hóa thông tin (1đ), mã hóa PIN (0.5đ), tạo cặp khóa RSA (0.5đ), xác thực theo chức năng (0.5đ). Người phụ trách: ……
> Thiết kế đầy đủ + lý do: [03 mục 5](../03-thiet-ke.md#5-thiết-kế-bảo-mật).

## 1. Bài học cần nắm → API Java Card 2.2.2 tương ứng

TH6 chỉ có lý thuyết, không có mã. Bảng dưới nối từng khái niệm sang lớp/hằng **có thật trong API 2.2.2** (đã tra trang tài liệu API 2.2.2):

| Khái niệm (slide TH6) | Lớp / hằng Java Card 2.2.2 | Dùng trong đề tài |
|---|---|---|
| Hàm băm SHA-256 / SHA-1 (slide 2–5) | `MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false)`; dự phòng `ALG_SHA` | Băm PIN, sinh khóa từ PIN |
| Số ngẫu nhiên | `RandomData.getInstance(RandomData.ALG_SECURE_RANDOM)` | salt, DEK, IV |
| AES (slide 11–13) | `KeyBuilder.buildKey(KeyBuilder.TYPE_AES[_TRANSIENT_DESELECT], KeyBuilder.LENGTH_AES_128, false)`; `Cipher.ALG_AES_BLOCK_128_CBC_NOPAD`, `ALG_AES_BLOCK_128_ECB_NOPAD` | Mã thông tin, bọc khóa |
| Đệm (padding) cho khối cuối (slide 13) | **2.2.2 không có** chế độ AES tự đệm (`ALG_AES_CBC_PKCS5` chỉ có ở bản 3.x) | Máy tính tự đệm ISO 9797-1 M2 |
| Cặp khóa RSA (slide 6–7) | `new KeyPair(KeyPair.ALG_RSA_CRT, KeyBuilder.LENGTH_RSA_1024)`, `genKeyPair()`; `RSAPublicKey.getModulus/getExponent` | Mỗi thẻ một cặp khóa |
| Chữ ký số (slide 9) | `Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false)` = SHA-1 + PKCS#1 v1.5 (2.2.2 **không có** `ALG_RSA_SHA_256_PKCS1`) | Ký challenge, ký biên nhận |
| Xác thực challenge-response (slide 10) | Máy tính sinh challenge, thẻ `sign`, máy tính `verify` bằng khóa công khai trong CSDL | Xác thực thẻ thật (F7) |
| Mã hóa bằng khóa công khai (slide 8) | `Cipher.ALG_RSA_PKCS1` (có trong 2.2.2) | Không dùng (đề tài cần ký, không cần mã RSA) – nêu là hướng mở rộng |

## 2. Áp dụng vào đề tài – 4 việc

### 2.1 "Mã hóa PIN": lưu băm có salt

```java
// H('P' | salt | PIN) rồi đưa giá trị băm vào OwnerPIN
private void setSecret(OwnerPIN target, byte[] src, short off, byte len) {
    hashSecret(DOM_VERIFY, src, off, len, OFF_H1);
    target.update(tmp, OFF_H1, hashLen);
    Util.arrayFillNonAtomic(tmp, OFF_H1, (short) 32, (byte) 0);
}

private boolean checkSecret(OwnerPIN target, byte[] src, short off, byte len) {
    if (target.getTriesRemaining() == 0) {
        ISOException.throwIt(SW_SECRET_BLOCKED);
    }
    hashSecret(DOM_VERIFY, src, off, len, OFF_H1);
    boolean ok = target.check(tmp, OFF_H1, hashLen);   // OwnerPIN tự giảm bộ đếm khi sai
    Util.arrayFillNonAtomic(tmp, OFF_H1, (short) 32, (byte) 0);
    return ok;
}
```

### 2.2 AES với khóa sinh từ PIN (DEK bọc bằng KEK)

```java
// KEK = 16 byte đầu của H('K' | salt | PIN)  - khác miền 'P' ở trên
private void loadKek(byte[] src, short off, byte len) {
    hashSecret(DOM_KEK, src, off, len, OFF_H2);
    kek.setKey(tmp, OFF_H2);
    Util.arrayFillNonAtomic(tmp, OFF_H2, (short) 32, (byte) 0);
}

// Lúc phát hành: DEK ngẫu nhiên, bọc 2 lần
rng.generateData(tmp, OFF_K, KEY_LEN);
dek.setKey(tmp, OFF_K);
loadKek(buf, pinOff, pinLen);  wrapDekInto(dekByPin, (short) 0);
loadKek(buf, pukOff, pukLen);  wrapDekInto(dekByPuk, (short) 0);

// Lúc nhập PIN đúng: mở DEK vào khóa tạm (RAM)
loadKek(buf, ISO7816.OFFSET_CDATA, (byte) len);
unwrapDek(dekByPin);
```

Mã/giải mã từng khối 240 byte (máy tính đã đệm sẵn):

```java
makeIv(obj, (byte) chunk);                                     // IV gốc XOR số khối
aesCbc.init(dek, Cipher.MODE_ENCRYPT, tmp, OFF_IV, (short) 16);
aesCbc.doFinal(buf, ISO7816.OFFSET_CDATA, len, buf, ISO7816.OFFSET_CDATA);   // mã tại chỗ
Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, store, off, len);    // ghi nguyên tử
objLen[obj] = (short) (off + len);
```

Phía máy tính ([CardCrypto.pad](../code/host/src/sinhvien/host/CardCrypto.java)): thêm `80`, rồi `00` cho đủ bội 16; đọc về thì bỏ các `00` cuối và byte `80`. Đệm sai → báo "sai khóa hoặc dữ liệu hỏng".

### 2.3 Cặp khóa RSA + xác thực thẻ

```java
// hàm tạo
try {
    rsaPair = new KeyPair(KeyPair.ALG_RSA_CRT, KeyBuilder.LENGTH_RSA_1024);
} catch (CryptoException e) {
    rsaPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);   // giả lập không có CRT
}
// lúc phát hành
rsaPair.genKeyPair();

// 0x32: ký 'A' | Lid | mã SV | challenge
rsaSig.init(rsaPair.getPrivate(), Signature.MODE_SIGN);
short sigLen = rsaSig.sign(tmp, OFF_S, (short) (p - OFF_S), buf, (short) 0);
```

Máy tính:

```java
PublicKey key = repo.activePublicKey(maSv);          // lấy từ CSDL, KHÔNG lấy từ thẻ đang cắm
boolean thatSu = card.authenticate(key, maSv);      // challenge 16 byte ngẫu nhiên, verify SHA1withRSA
if (!thatSu) { /* báo THẺ GIẢ, không cho làm gì tiếp */ }
```

### 2.4 Ký biên nhận nạp / thanh toán / điểm danh

Thẻ ký `[loại][Lid][mã SV][giá trị][số dư][bộ đếm][thời gian][nonce]` sau khi giao dịch đã commit; máy tính kiểm chữ ký + bộ đếm rồi mới ghi CSDL ([CardRepository.recordReceipt](../code/host/src/sinhvien/host/CardRepository.java)). Đây là phần "mỗi lần thanh toán, nạp tiền … sử dụng mật mã khóa công khai" của đề.

## 3. Việc cần làm

- [ ] Viết thử một applet nhỏ chỉ có `MessageDigest`, `AES`, `KeyPair` trên **JCIDE** trước để chắc giả lập hỗ trợ (nhất là `ALG_SHA_256`, `TYPE_AES_TRANSIENT_DESELECT`, `ALG_RSA_CRT`). Ghi kết quả vào nhật ký dưới. Mã mẫu đã có `try/catch` dự phòng cho cả ba.
- [ ] Nâng PIN bản bước 2 lên `OwnerPIN` + băm.
- [ ] Nâng `WRITE_OBJ`/`READ_OBJ` lên mã hóa; định dạng lệnh không đổi nên phần mềm chỉ thêm `pad`/`unpad`.
- [ ] `INIT_CARD` thêm sinh DEK, bọc, `genKeyPair`; thêm `GET_PUBKEY 31`, `AUTH_CARD 32`, `UNBLOCK_PIN 23`.
- [ ] `CHANGE_PIN` chuyển sang bọc lại DEK trong giao dịch.
- [ ] `TOPUP`/`PAY`/`CHECK_IN` trả thêm chữ ký.
- [ ] Phần mềm: lưu modulus/exponent vào bảng `the`; mỗi lần cắm thẻ chạy xác thực; màn hình "Thông tin bảo mật" hiện khóa công khai (hex) để chứng minh "tạo được cặp khóa RSA".
- [ ] Chạy bộ test ([code/README.md](../code/README.md)) → 49/49.
- [ ] Chuẩn bị chứng minh "đã mã hóa": chụp kết quả kịch bản *EEPROM không chứa họ tên / mã SV dạng rõ* và *OwnerPIN chỉ giữ giá trị băm 32 byte* của bộ test để đưa vào báo cáo.

## 4. Kiểm thử

```text
>> B0 31 00 00 00                     << 00 80 <128 byte modulus> 00 03 01 00 01 90 00
>> B0 32 00 00 10 00 11 22 … EE FF    << <128 byte chữ ký> 90 00
>> B0 40 02 00 10 11 06 48 61 20 4E 6F 69 80 00 00 00 00 00 00 00   << 90 00
>> B0 41 02 00 00                     << 11 06 48 61 20 4E 6F 69 80 00 … 90 00   (thẻ đã giải mã)
>> B0 22 00 00 0E 06 31 32 33 34 35 36 06 36 35 34 33 32 31          << 90 00   đổi PIN
   (bỏ chọn, chọn lại, nhập PIN mới 654321)
>> B0 41 02 00 00                     << vẫn ra 11 06 48 61 … : đổi PIN không mất dữ liệu
```

Kịch bản trong bộ test chứng minh: biên nhận bị sửa 1 trường → chữ ký sai; gửi lại biên nhận cũ → REPLAY; sai mã SV khi xác thực → chữ ký không khớp; mở bằng PUK → dữ liệu còn nguyên.

## 5. Câu hỏi có thể bị hỏi

1. *Vì sao băm PIN mà không mã hóa PIN?* – Thẻ chỉ cần **so sánh**, không cần lấy lại PIN → hàm một chiều là đủ và an toàn hơn (không có khóa để lộ).
2. *Salt để làm gì?* – Hai thẻ cùng PIN có giá trị băm khác nhau; không dùng bảng băm tính sẵn được.
3. *Khóa AES lấy từ đâu? Đổi PIN thì dữ liệu cũ sao?* – mục 2.2; [03 mục 5.2](../03-thiet-ke.md#52-khóa-dữ-liệu-aes-sinh-từ-pin-dek--kek).
4. *Vì sao CBC mà không ECB cho dữ liệu?* – ECB: hai khối rõ giống nhau ra hai khối mã giống nhau, lộ cấu trúc. ECB chỉ dùng để bọc DEK đúng 1 khối 16 byte ngẫu nhiên.
5. *IV là gì, để ở đâu?* – Vector khởi tạo cho CBC, không cần bí mật; lưu cạnh dữ liệu (`objIv`), sinh mới mỗi lần ghi lại.
6. *Khóa bí mật RSA nằm ở đâu? Lấy ra được không?* – Chỉ trong thẻ, không lệnh nào đọc ra; sinh ngay trên thẻ.
7. *Vì sao phần mềm không lấy khóa công khai từ thẻ khi xác thực?* – Thẻ giả cũng tự đưa được khóa của chính nó; khóa tin cậy là khóa đã lưu lúc phát hành.
8. *Chữ ký số đảm bảo gì?* – Toàn vẹn + nguồn gốc (chỉ thẻ thật có khóa bí mật) + chống chối bỏ.
9. *Challenge để làm gì? Sao phải ngẫu nhiên?* – Chống phát lại: chữ ký cũ không khớp challenge mới.
10. *Vì sao SHA-1 trong chữ ký?* – Java Card 2.2.2 chỉ có `ALG_RSA_SHA_PKCS1`; bản 3.0.x mới có SHA-256.
11. *Vì sao RSA 1024 mà không 2048?* – Nhanh trên giả lập; 2.2.2 có `LENGTH_RSA_2048`, chỉ đổi một hằng nếu thẻ hỗ trợ (chữ ký thành 256 byte, vẫn vừa APDU).

## Nhật ký

| Ngày | Ai | Kiểm trên JCIDE | Kết quả |
|---|---|---|---|
| | | `ALG_SHA_256` có không? | |
| | | `TYPE_AES_TRANSIENT_DESELECT` có không? | |
| | | `ALG_RSA_CRT` 1024 sinh mất bao lâu? | |
