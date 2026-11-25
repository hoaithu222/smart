# TH6 – Mật mã trong Java Card: phân tích chi tiết

> Đề: [th6-mat-ma-trong-java-card.md](../../smart/docs/th6-mat-ma-trong-java-card.md) (13 slide lý thuyết, **không có đề mã**). Ghi chú đề tài: [th/th6-mat-ma.md](../../th/th6-mat-ma.md).
> Mã minh họa: [src/bai6_Crypto](../src/bai6_Crypto/). Kết quả từ [th6.apdu](../kich-ban/th6.apdu), [tn6.apdu](../kich-ban/thi-nghiem/tn6.apdu) và `.\kiem-mat-ma.cmd`. **[Thêm]** = ngoài tài liệu.

## 1. Ba nhóm công cụ

| Nhóm | Khóa | Dùng để | Ví dụ trên thẻ |
|---|---|---|---|
| Hàm băm (slide 2–5) | không | Kiểm toàn vẹn, lưu PIN | `MessageDigest` |
| Khóa bí mật (slide 11–13) | một khóa dùng chung | Mã hóa dữ liệu | `Cipher` + `AESKey` |
| Khóa công khai (slide 6–10) | cặp công khai / bí mật | Mã hóa, chữ ký, xác thực | `KeyPair`, `Signature`, `Cipher` |

Gói cần import: `javacard.security.*` (khóa, băm, chữ ký, số ngẫu nhiên) và `javacardx.crypto.Cipher`.

## 2. Hàm băm

**Tính chất (slide 5):** một chiều; cùng đầu vào luôn ra cùng kết quả; đầu vào khác nên ra khác (vẫn có thể va chạm).

| Thuật toán | Đầu ra | Hằng |
|---|---|---|
| MD5 | 16 byte | `ALG_MD5` |
| SHA-1 | 20 byte | `ALG_SHA` |
| SHA-256 | 32 byte | `ALG_SHA_256` (có từ 2.2.2) |

**[Thêm]** MD5 và SHA-1 đã bị phá về chống va chạm; chỉ dùng để học.

### Mã: [HashApplet](../src/bai6_Crypto/HashApplet.java#L76)

```java
md.reset();
short conLai = Lc;
short n = apdu.setIncomingAndReceive();
while (n < conLai) {                       // dữ liệu dài đến thành nhiều đoạn
    md.update(buf, OFFSET_CDATA, n);
    conLai -= n;
    n = apdu.receiveBytes(OFFSET_CDATA);
}
short len = md.doFinal(buf, OFFSET_CDATA, n, ketQua, (short) 0);
```

`update` cho đoạn giữa, `doFinal` cho đoạn cuối. Nhờ vậy xử lý được dữ liệu dài hơn bộ đệm.

| Lệnh | Kết quả |
|---|---|
| `00 02 00 00 03 46 6F 78 00` ("Fox") | `DF CD 34 54 BB EA 78 8A 75 1A 69 6C 24 D9 70 09 CA 99 2D 17` (đúng slide 3) |
| `00 02 00 00 00` (chuỗi rỗng) | `DA 39 A3 EE 5E 6B 4B 0D 32 55 BF EF 95 60 18 90 AF D8 07 09` |

## 3. AES

- Khối **128 bit**; khóa 128, 192, 256 bit (slide 13).
- Khối cuối thiếu thì **đệm** (padding).

### 3.1 Đệm ISO 9797-1 cách 2

```text
"abc" = 61 62 63
đệm   → 61 62 63 80 00 00 00 00 00 00 00 00 00 00 00 00      (16 byte)
```

Luôn thêm ít nhất `80`: dữ liệu đúng 16 byte sẽ thành 32 byte. Java Card 2.2.2 chỉ có AES `NOPAD`, nên applet (hoặc máy tính) tự đệm.

### 3.2 ECB và CBC **[Thêm]**

| | ECB | CBC |
|---|---|---|
| Cách mã | Mỗi khối độc lập | Khối rõ XOR khối mã trước rồi mới mã |
| IV | Không | Có (ở applet này là 16 byte `00`) |
| Hai khối rõ giống nhau | Ra hai khối mã **giống nhau** | Ra hai khối mã khác nhau |

Thí nghiệm (khóa `00..0F`, rõ = 32 ký tự `A`):

```text
ECB: DD4B1A0B47DAA7067D0B59D95D58A6AE | DD4B1A0B47DAA7067D0B59D95D58A6AE   ← lộ cấu trúc
CBC: DD4B1A0B47DAA7067D0B59D95D58A6AE | 0FDBC4E2EEA77988B21FAAE055CA5999
```

Khối đầu giống nhau vì IV bằng 0. Thực tế IV phải **ngẫu nhiên** và đổi mỗi lần ghi.

### 3.3 Mã: [AesApplet](../src/bai6_Crypto/AesApplet.java#L146)

| Lệnh | Việc |
|---|---|
| `00 10 00 00 Lc <khóa>` | 16/24/32 byte chọn AES-128/192/256 |
| `00 11 P1 P2 …` | mã hóa; P1 = ECB/CBC, P2 = có đệm |
| `00 12 P1 P2 …` | giải mã |

Quy trình: kiểm khóa → nhận dữ liệu → chép sang mảng tạm → đệm → `init` → `doFinal` → gửi. Giải mã bằng khóa sai thì phần đệm không hợp lệ: `6A 80`.

Ví dụ chuẩn FIPS-197: khóa `00..0F`, rõ `00 11 … FF` → `69 C4 E0 D8 6A 7B 04 30 D8 CD B7 80 70 B4 C5 5A`.

## 4. RSA

| Việc | Khóa | Ai làm |
|---|---|---|
| Mã hóa (slide 8) | công khai | bên gửi |
| Giải mã | bí mật | thẻ |
| Ký (slide 9) | **bí mật** | thẻ |
| Kiểm chữ ký | **công khai** | máy tính |

### Mã: [RsaApplet](../src/bai6_Crypto/RsaApplet.java#L61)

| Lệnh | Việc | Giới hạn |
|---|---|---|
| `00 20` | sinh cặp khóa 1024 bit ngay trên thẻ | |
| `00 21 00` / `01` | đọc modulus (128 byte) / số mũ (`01 00 01`) | |
| `00 22` | mã hóa PKCS#1 | rõ ≤ 117 byte (128 − 11 byte đệm) |
| `00 23` | giải mã | đúng 128 byte |
| `00 24` | ký SHA-1 + PKCS#1 | chữ ký 128 byte |
| `00 25` | thẻ tự kiểm chữ ký | `01` đúng, `00` sai |

Không có lệnh nào đọc khóa bí mật.

**Đã chạy thử [Thêm]:**
- Ký hai lần cùng dữ liệu cho **cùng** chữ ký (PKCS#1 v1.5 không có yếu tố ngẫu nhiên) → vì vậy thử thách phải mới mỗi lần.
- Mã hóa hai lần cùng dữ liệu cho **khác** bản mã (đệm ngẫu nhiên).

## 5. Xác thực thẻ bằng thử thách – phản hồi (slide 10)

```text
MÁY TÍNH                                    THẺ
(đã lưu khóa công khai lúc phát hành)
 sinh số ngẫu nhiên c ───── 00 24 c ─────►  s = ký(c) bằng khóa BÍ MẬT
 ◄──────────────────────── s ─────────────
 kiểm(s, c, khóa công khai ĐÃ LƯU)
 đúng → thẻ thật ; sai → thẻ giả
```

Phải dùng khóa công khai **đã lưu**, không lấy từ thẻ đang cắm: thẻ giả cũng tự đưa được khóa của nó.

## 6. "Dữ liệu phải được mã hóa trên thẻ" – cách đề tài nhóm làm

| Yêu cầu | Cách làm trong [StudentCard](../../code/applet/src/sinhvien/StudentCard.java) |
|---|---|
| Không lưu PIN rõ | `SHA-256('P' ‖ salt ‖ PIN)` đưa vào `OwnerPIN` |
| Khóa mã dữ liệu | khóa ngẫu nhiên (DEK), được bọc bằng khóa sinh từ PIN (KEK) và từ PUK |
| Mã dữ liệu | AES-CBC theo khối, IV riêng; thẻ nhận rõ, **mã trên thẻ** rồi mới ghi |
| Đổi PIN | chỉ bọc lại DEK, trong một giao dịch |
| Xác thực thẻ | lệnh `32`: ký thử thách ([authCard, dòng 514](../../code/applet/src/sinhvien/StudentCard.java#L514)); máy tính kiểm ([CardClient.authenticate, dòng 167](../../code/host/src/sinhvien/host/CardClient.java#L167)) |
| Ký giao dịch | biên nhận được ký; máy tính kiểm rồi mới ghi CSDL ([recordReceipt, dòng 331](../../code/host/src/sinhvien/host/CardRepository.java#L331)) |

Luồng ghi/đọc thông tin:

```text
Ghi : máy tính đệm 80 00… ─► APDU ─► thẻ: AES-CBC(DEK) ─► EEPROM (bản mã)
Đọc : EEPROM ─► thẻ: giải mã bằng DEK (đã mở bằng PIN) ─► APDU ─► máy tính bỏ đệm
```

Dữ liệu rõ đi qua đầu đọc: đề tài mã hóa **lúc lưu trên thẻ**, không mã hóa đường truyền **[Thêm]**.

## 7. Cấu hình

| Applet | AID |
|---|---|
| HashApplet | `11 22 33 44 55 06 01` |
| AesApplet | `…06 02` |
| RsaApplet | `…06 03` |

Kiểm trên JCIDE trước: thẻ ảo có hỗ trợ `ALG_SHA_256`, AES-192/256, RSA 1024 không. Thiếu thuật toán băm hoặc độ dài khóa AES thì lệnh tương ứng trả `6A 81`; thiếu hẳn chế độ AES hoặc RSA thì **cài applet thất bại**.

```powershell
.\chay.cmd kich-ban\th6.apdu
```

```powershell
.\kiem-mat-ma.cmd
```

Lệnh sau so kết quả thẻ với thư viện Java (27 mục). Trên JCIDE thêm `--pcsc` và cài mới 3 applet.

## 8. Câu hỏi vấn đáp

1. **Băm khác mã hóa?** Băm một chiều, không khóa; mã hóa giải ngược được bằng khóa.
2. **Vì sao băm PIN thay vì mã hóa?** Chỉ cần so sánh, không có khóa để lộ.
3. **Salt để làm gì?** Cùng PIN ra giá trị khác nhau trên mỗi thẻ.
4. **AES khối và khóa bao nhiêu?** Khối 128 bit; khóa 128/192/256.
5. **Vì sao cần đệm?** Dữ liệu không tròn khối.
6. **ECB có vấn đề gì?** Khối giống nhau ra bản mã giống nhau.
7. **Ký và mã hóa dùng khóa nào?** Ký: bí mật; mã hóa: công khai của người nhận.
8. **Chữ ký số bảo đảm gì?** Toàn vẹn, nguồn gốc, chống chối bỏ.
9. **Vì sao thử thách phải ngẫu nhiên?** Chống phát lại.
10. **Vì sao RSA không dùng để mã dữ liệu lớn?** Chỉ mã được khối nhỏ (≤ 117 byte) và chậm; dùng AES cho dữ liệu.
