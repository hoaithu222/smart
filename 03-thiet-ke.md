# 03 · Thiết kế: thẻ, APDU, bảo mật, CSDL, phần mềm

> Khớp từng byte với mã mẫu [code/applet/src/sinhvien/StudentCard.java](code/applet/src/sinhvien/StudentCard.java) và [code/host/src/sinhvien/host/](code/host/src/sinhvien/host/). Sửa thiết kế thì sửa cả hai và chạy lại bộ test ([code/README.md](code/README.md)).
> Phiên bản API: **Java Card 2.2.2** (bộ chuyển đổi Feitian trong JCIDE, theo [huong-dan-ket-noi.md](../huong-dan-ket-noi.md)).

## 1. Kiến trúc tổng thể

```text
┌──────────────── Thẻ (Java Card 2.2.2) ────────────────┐
│ Applet StudentCard  AID 4B 4D 41 53 56 01 01           │
│  EEPROM: mã SV, băm PIN/PUK/QT, DEK đã bọc, RSA,       │
│          INFO/CONTACT/PHOTO (AES), số dư, nhật ký vòng  │
│  RAM   : tmp 240 B, khóa phiên DEK, cờ PIN              │
└───────────────▲───────────────────────────────────────┘
                │ APDU (CLA B0) qua PC/SC, T=0
                │ JCIDE: "JAVACOS Virtual Contact Reader 0"
┌───────────────┴──────── Máy tính (Java 8, NetBeans) ───┐
│  Giao diện Swing (4 nhóm màn hình theo vai trò)         │
│        │ gọi hàm, không tự ghép byte                    │
│  CardClient ── ApduChannel ─┬─ PcscChannel (thật)       │
│        │                    └─ kênh jCardSim (test)     │
│  CardRepository ── JDBC ── SQL Server (test: SQLite)    │
│  AppConfig ← app.properties                              │
└─────────────────────────────────────────────────────────┘
```

Nguyên tắc phân vai: **thẻ quyết định** (PIN đúng/sai, đủ tiền không, ký hay không); **máy tính hiển thị và ghi sổ** (CSDL chỉ nhận biên nhận đã kiểm chữ ký).

## 2. Bộ nhớ trên thẻ

### 2.1 Đối tượng bền (EEPROM) – cấp phát hết trong hàm tạo (TH4)

| Trường | Kiểu / cỡ | Ý nghĩa |
|---|---|---|
| `state` | byte | `00` BLANK · `01` ACTIVE · `02` LOCKED |
| `cardId`, `cardIdLen` | byte[16] | Mã sinh viên, **lưu rõ** (in trên mặt thẻ, cần để tra khóa công khai) |
| `salt` | byte[16] | Ngẫu nhiên, sinh lúc cài applet |
| `pin`, `puk`, `admin` | `OwnerPIN` (3 / 5 / 5 lần, 32 byte) | Chứa **giá trị băm**, không chứa PIN |
| `dekByPin`, `dekByPuk` | byte[16] ×2 | DEK đã mã bằng KEK(PIN) và KEK(PUK) |
| `rsaPair` | `KeyPair` RSA-1024 (CRT nếu có) | Sinh trên thẻ lúc phát hành |
| `infoData` / `contactData` / `photoData` | 480 / 240 / 7680 byte | Bản mã AES-CBC |
| `objLen` | short[4] | Độ dài (đã đệm) từng đối tượng |
| `objIv` | byte[64] | IV gốc từng đối tượng |
| `balance`, `txCounter` | short | Số dư (nghìn đồng), bộ đếm giao dịch |
| `log`, `logHead`, `logCount` | byte[120] | Nhật ký vòng 10 × 12 byte |

Tổng dữ liệu ≈ 8,8 KB + khóa RSA + mã applet → vừa thẻ JCOP 40 KB. Không có ảnh thì chỉ ≈ 1 KB.

### 2.2 Đối tượng tạm (RAM)

| Trường | Loại | Vì sao |
|---|---|---|
| `tmp` byte[240] | `CLEAR_ON_RESET` | Vùng làm việc: đầu vào băm, khóa trung gian, IV, dữ liệu cần ký. Không dùng `CLEAR_ON_DESELECT` vì hàm tạo cần dùng `tmp` để băm PIN quản trị **trong lúc cài đặt** – khi đó applet chưa được chọn, truy cập mảng `CLEAR_ON_DESELECT` sẽ lỗi. Dữ liệu nhạy cảm được xóa tay ngay sau khi dùng |
| `dek`, `kek` | `AESKey` `TYPE_AES_TRANSIENT_DESELECT` | Khóa dữ liệu chỉ tồn tại trong phiên; bỏ chọn applet là mất. Nếu thẻ không hỗ trợ khóa tạm, applet dùng khóa bền và tự `clearKey()` trong `deselect()` |
| Cờ "đã xác thực" của `OwnerPIN` | do JCRE quản lý | Mất khi reset thẻ; applet gọi `pin.reset()` trong `deselect()` |

## 3. Bảng lệnh APDU

CLA = `B0` (giống ví dụ kết nối của cô). Dữ liệu dạng `[L][giá trị]` gọi là LV. PIN/PUK/PIN quản trị là chuỗi ASCII 4–16 ký tự.

| INS | Tên | P1 / P2 | Data gửi xuống | Trả về | Điều kiện | Chức năng |
|---|---|---|---|---|---|---|
| `10` | GET_STATUS | – | – | `state, PIN còn, PUK còn, cờ(01=QT,02=PIN), lenINFO(2), lenCONTACT(2), lenPHOTO(2), Lid, mã SV` | luôn được (kể cả thẻ khóa) | Cắm thẻ |
| `20` | VERIFY_ADMIN | – | PIN quản trị | – | | Đăng nhập QT trên thẻ |
| `21` | VERIFY_PIN | – | PIN | – | ACTIVE | F2 |
| `22` | CHANGE_PIN | – | LV(PIN cũ) LV(PIN mới) | – | ACTIVE | F3 |
| `23` | UNBLOCK_PIN | – | LV(PUK) LV(PIN mới) | – | ACTIVE | F4 |
| `30` | INIT_CARD | – | LV(mã SV) LV(PIN) LV(PUK) | – | BLANK + QT | F1 |
| `31` | GET_PUBKEY | – | – | `Lmod(2) modulus Lexp(2) exponent` | ACTIVE | F1 |
| `32` | AUTH_CARD | – | challenge 8–64 byte | chữ ký RSA của `'A' Lid mãSV challenge` | ACTIVE | F7 |
| `33` | SET_LOCK | P1 `01` khóa / `00` mở | – | – | QT, đã phát hành | F10 |
| `40` | WRITE_OBJ | P1 đối tượng `01` INFO `02` CONTACT `03` PHOTO; P2 số khối | bản rõ **đã đệm**, bội 16, ≤ 240 | – | PIN; INFO/PHOTO thêm QT | F1, F6 |
| `41` | READ_OBJ | P1 đối tượng; P2 số khối | – | bản rõ (còn đệm) ≤ 240 | PIN | F5 |
| `50` | GET_BALANCE | – | – | `số dư(2) bộ đếm(2) số bản ghi(1)` | PIN | F8 |
| `51` | TOPUP | – | LV(PIN) `số tiền(2) thời gian(4) nonce(8)` | `số dư(2) bộ đếm(2) chữ ký(128)` | ACTIVE | F8 |
| `52` | PAY | – | như `51` | như `51` | ACTIVE | F8 |
| `53` | GET_LOG | – | – | n × 12 byte, mới nhất trước | PIN | F8, F9 |
| `60` | CHECK_IN | – | `mã phòng(2) thời gian(4) nonce(8)` | `số dư(2) bộ đếm(2) chữ ký(128)` | ACTIVE, **không cần PIN** | F9 |

Bản ghi nhật ký 12 byte: `[loại 01 nạp / 02 trả / 03 điểm danh][00][giá trị(2)][số dư sau(2)][bộ đếm(2)][thời gian(4)]`.

Thông điệp thẻ ký cho `51/52/60`: `[loại][Lid][mã SV][giá trị(2)][số dư(2)][bộ đếm(2)][thời gian(4)][nonce(8)]`.

### 3.1 Status word

| SW | Hằng trong applet | Nghĩa | Thông báo trên giao diện (`CardClient.describe`) |
|---|---|---|---|
| `90 00` | – | Thành công | – |
| `63 Cx` | `SW_WRONG_SECRET_BASE` | Sai PIN/PUK/PIN QT, còn x lần | "Sai mã, còn x lần thử" |
| `69 83` | `SW_SECRET_BLOCKED` | Đã khóa do sai quá số lần | "Mã đã bị khóa…" |
| `69 82` | `ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED` | Chưa nhập PIN / chưa đăng nhập QT | "Chưa xác thực…" |
| `69 85` | `ISO7816.SW_CONDITIONS_NOT_SATISFIED` | Sai trạng thái (vd khởi tạo thẻ đã phát hành) | |
| `64 00` | `SW_CARD_LOCKED` | Thẻ bị khóa – trả ngay lúc SELECT, đúng như `connectCard()` trong tài liệu kết nối | "Thẻ đã bị khóa" |
| `6A 83` / `6A 84` / `6A 85` | lấy theo applet mẫu *Wallet* của Java Card Kit | Số tiền sai / vượt trần / không đủ số dư | |
| `6A 80`, `67 00`, `6B 00`, `6A 86` | ISO7816 | Sai dữ liệu / độ dài / P1P2 | |
| `6D 00`, `6E 00` | ISO7816 | Sai INS / CLA | |

## 4. Máy trạng thái

```text
            cài applet (JCIDE)                 SET_LOCK 01 (QT)
 (không có) ───────────────▶ BLANK ──INIT_CARD──▶ ACTIVE ◀────────────▶ LOCKED
                               (cần QT)                SET_LOCK 00 (QT)     ▲
                                                                            │ sai PUK đủ 5 lần
 Trong ACTIVE, PIN có trạng thái riêng:                                     │
   Chưa xác thực ──VERIFY_PIN đúng──▶ Đã xác thực (mở DEK) ──deselect/reset──▶ Chưa xác thực
   Sai 3 lần ──▶ PIN bị khóa ──UNBLOCK_PIN (PUK đúng)──▶ PIN mới, bộ đếm = 3 ─┘
```

`state = ACTIVE` được ghi **sau cùng** trong `INIT_CARD`: rút thẻ giữa chừng thì thẻ vẫn BLANK, phát hành lại được.

## 5. Thiết kế bảo mật

### 5.1 PIN được "mã hóa" thế nào

```text
giá trị lưu trong OwnerPIN = SHA-256( 'P' ‖ salt(16) ‖ PIN )      (thiếu SHA-256 thì lùi về SHA-1)
```

- Thẻ **không lưu PIN**: kể cả đọc trộm EEPROM cũng không thấy chuỗi `123456` (có kịch bản test soi bộ nhớ).
- `salt` ngẫu nhiên riêng từng thẻ → hai thẻ cùng PIN `123456` có giá trị băm khác nhau.
- Dùng `OwnerPIN` của `javacard.framework` để có sẵn bộ đếm sai được JCRE bảo vệ (không tham gia giao dịch nên không "hoàn tác" được bộ đếm), `getTriesRemaining()`, `isValidated()`.
- Tiền tố `'P'` khác với `'K'` ở mục 5.2: giá trị kiểm PIN và khóa mã dữ liệu **không bao giờ trùng nhau**.

### 5.2 Khóa dữ liệu AES sinh từ PIN (DEK + KEK)

```text
Lúc phát hành:  DEK = 16 byte ngẫu nhiên (RandomData)          ← khóa thật sự mã dữ liệu
                KEK_pin = 16 byte đầu SHA-256('K' ‖ salt ‖ PIN)  ← "khóa được tạo ra từ mã PIN"
                KEK_puk = 16 byte đầu SHA-256('K' ‖ salt ‖ PUK)
                dekByPin = AES-ECB(KEK_pin, DEK)   dekByPuk = AES-ECB(KEK_puk, DEK)

Nhập PIN đúng:  KEK_pin → giải dekByPin → DEK nằm trong khóa tạm (RAM) cho đến khi bỏ chọn
Đổi PIN:        DEK (đang mở) → bọc lại bằng KEK_pin_mới, ghi dekByPin + băm PIN mới trong 1 giao dịch
Quên PIN:       PUK đúng → KEK_puk → DEK → bọc bằng KEK của PIN mới
```

Vì sao không dùng thẳng KEK_pin để mã dữ liệu?

| | Mã thẳng bằng khóa từ PIN | DEK bọc bằng KEK (nhóm mình) |
|---|---|---|
| Đổi PIN | Phải giải mã + mã lại toàn bộ ~8 KB; rút thẻ giữa chừng là hỏng dữ liệu | Chỉ ghi lại 16 byte + băm PIN trong 1 giao dịch |
| Quên PIN | Mất hết dữ liệu | Mở bằng PUK, dữ liệu còn nguyên |
| Vẫn đúng yêu cầu "khóa tạo từ PIN"? | Có | Có: không có PIN (hoặc PUK) thì không có DEK |

### 5.3 Mã hóa dữ liệu

- Java Card 2.2.2 chỉ có `Cipher.ALG_AES_BLOCK_128_CBC_NOPAD` và `ALG_AES_BLOCK_128_ECB_NOPAD` (không có chế độ tự đệm PKCS5) → **máy tính đệm** theo ISO/IEC 9797-1 phương pháp 2 (`80 00 … 00`) trước khi gửi, thẻ chỉ nhận dữ liệu bội 16.
- Dữ liệu chia khối 240 byte (15 khối AES, vừa 1 APDU). Mỗi khối mã CBC riêng với IV = IV gốc của đối tượng (ngẫu nhiên mỗi lần ghi lại) XOR số thứ tự khối.
- Thẻ tự mã/giải mã; khóa không bao giờ ra khỏi thẻ. Máy tính chỉ thấy bản rõ **sau khi** chủ thẻ nhập PIN.

### 5.4 RSA

- `KeyPair(ALG_RSA_CRT, LENGTH_RSA_1024).genKeyPair()` ngay trên thẻ lúc phát hành. Khóa bí mật không có lệnh nào đọc ra.
- Khóa công khai (`GET_PUBKEY`) lưu vào bảng `the` của CSDL **lúc phát hành, khi thẻ đang ở tay phòng Đào tạo**. Về sau phần mềm luôn lấy khóa từ CSDL, không lấy từ thẻ đang cắm (thẻ giả cũng tự đưa được khóa của nó).
- Ký bằng `Signature.ALG_RSA_SHA_PKCS1` = SHA-1 + PKCS#1 v1.5 → phía Java kiểm bằng `Signature.getInstance("SHA1withRSA")`. Java Card 2.2.2 không có `ALG_RSA_SHA_256_PKCS1` (đã tra API 2.2.2); nêu là hạn chế của nền tảng.
- **Xác thực thẻ** (F7): máy tính sinh challenge 16 byte ngẫu nhiên → thẻ ký `'A' ‖ mã SV ‖ challenge` → máy tính kiểm. Challenge mới mỗi lần nên chép lại chữ ký cũ vô dụng.
- **Biên nhận** (F8, F9): thẻ ký sau khi đã cập nhật số dư. Tiền tố loại (`01/02/03`) khác `'A'` nên chữ ký xác thực không dùng giả làm biên nhận được.

### 5.5 Toàn vẹn khi rút thẻ, chống lặp

| Tình huống | Xử lý |
|---|---|
| Rút thẻ giữa lúc cập nhật số dư | `beginTransaction` → số dư, bộ đếm, nhật ký → `commitTransaction`: cả ba cùng còn hoặc cùng mất (TH3) |
| Rút thẻ giữa lúc đổi PIN | `dekByPin` + `pin.update` trong một giao dịch |
| Rút thẻ giữa lúc ghi một khối dữ liệu | `Util.arrayCopy` vào mảng bền là nguyên tử; độ dài `objLen` chỉ cập nhật **sau** khi dữ liệu đã ghi |
| Thẻ trừ tiền xong nhưng máy tính chưa nhận được biên nhận | Lần sau cắm thẻ: đọc `GET_LOG`, so bộ đếm với `bo_dem_cuoi` trong CSDL để đối soát |
| Gửi lại biên nhận cũ vào CSDL | `bo_dem` phải > `bo_dem_cuoi`, thêm `UNIQUE(ma_sv, bo_dem)` → REPLAY |
| Sửa số tiền trên biên nhận | Chữ ký sai → BAD_SIGNATURE |
| Dò PIN | 3 lần sai khóa PIN; PUK 5 lần sai khóa hẳn thẻ |

### 5.6 Giới hạn (nói thẳng khi bị hỏi)

- PIN 6 số chỉ có 10⁶ khả năng: nếu kẻ tấn công **đọc được toàn bộ EEPROM** thì dò offline được. Thẻ thật dựa vào phần cứng chống đọc trộm; giả lập thì không.
- Thời gian do máy tính gửi, thẻ không có đồng hồ → chỉ dùng để ghi sổ, không dùng làm điều kiện bảo mật.
- SHA-1 trong chữ ký là do giới hạn của Java Card 2.2.2.
- Mã SV lưu rõ (có chủ đích, mục 2.1).

## 6. Các luồng chính

**Cắm thẻ (mọi màn hình):** `SELECT` (→ `64 00` thì báo thẻ khóa) → `GET_STATUS` (lấy mã SV, số lần PIN còn) → tra CSDL lấy khóa công khai → `AUTH_CARD` → sai chữ ký thì báo **thẻ giả**, dừng.

**Phát hành thẻ (QT):** nhập thông tin + ảnh trên form → lưu `sinh_vien` → `VERIFY_ADMIN` → `INIT_CARD(mã SV, PIN ban đầu, PUK)` → `GET_PUBKEY` → lưu bảng `the` → `VERIFY_PIN(PIN ban đầu)` → `WRITE_OBJ` INFO, CONTACT, PHOTO → in PUK cho sinh viên.

**Sinh viên xem / sửa thông tin:** cắm thẻ → nhập PIN (`VERIFY_PIN`, sai thì hiện "còn x lần") → `READ_OBJ` từng đối tượng → sửa CONTACT → `WRITE_OBJ 02`.

**Thanh toán:** thu ngân nhập số tiền + dịch vụ → sinh viên nhập PIN → `PAY` → nhận biên nhận → `CardRepository.recordReceipt` (kiểm chữ ký, bộ đếm) → in hóa đơn.

**Điểm danh:** chọn phòng/môn → quẹt thẻ → `CHECK_IN` → kiểm chữ ký → ghi `giao_dich` loại CHECKIN.

**Quên PIN:** nhập PUK + PIN mới → `UNBLOCK_PIN` → `VERIFY_PIN(PIN mới)`.

## 7. CSDL

**SQL Server** (đã chốt). Schema: [schema-sqlserver.sql](code/host/src/sinhvien/host/db/schema-sqlserver.sql) – phần mềm tự chạy khi khởi động, tự `CREATE DATABASE` nếu chưa có. Bản SQLite cùng cấu trúc cho bộ test: [schema-sqlite.sql](code/host/src/sinhvien/host/db/schema-sqlite.sql). Mọi câu SQL trong `CardRepository` viết theo cú pháp chung (không `TOP`/`LIMIT` – giới hạn số dòng bằng `setMaxRows`; gộp theo ngày trong Java) nên chạy được cả hai.

| Bảng | Vai trò | Ghi chú |
|---|---|---|
| `sinh_vien` | Hồ sơ gốc của trường (để phát hành lại thẻ), kèm ảnh `VARBINARY(MAX)` | Cột chữ `NVARCHAR` cho tiếng Việt. CSDL nằm ở máy chủ có phân quyền; mã hóa trên thẻ là để bảo vệ khi **mất thẻ** |
| `the` | Mỗi lần phát hành 1 dòng: khóa công khai (hex), trạng thái, lý do khóa, `bo_dem_cuoi` | Phát hành lại → dòng cũ `REVOKED`; báo mất → `LOCKED` |
| `giao_dich` | Biên nhận nạp / trả / điểm danh đã kiểm chữ ký | `UNIQUE(ma_sv, bo_dem)` |
| `nguoi_dung` | Tài khoản đăng nhập phần mềm theo vai trò | Mật khẩu băm + salt |
| `phong` | Mã phòng điểm danh | |

## 8. Kiến trúc phần mềm máy tính

Đã viết xong ([code/host/src/](code/host/src/)), ảnh từng màn ở [images/giao-dien/](images/giao-dien/).

```text
sinhvien.host  ← lõi, không biết gì về giao diện (có bộ test 49 kịch bản)
  AppConfig ← app.properties
  ApduChannel ─┬─ PcscChannel    (JCIDE / đầu đọc thật, card.mode=pcsc)
               └─ SimCardChannel (jCardSim chạy StudentCard trong phần mềm, card.mode=jcardsim)
  LoggingChannel (nhật ký APDU, che PIN) → CardClient (mỗi lệnh APDU = 1 hàm)
  CardCrypto, ProfileTlv
  DatabaseBootstrap → CardRepository (JDBC: SQL Server / SQLite)
sinhvien.ui    ← Swing + FlatLaf
  App → LoginDialog → MainView (thanh bên theo vai trò + header chip thẻ + Nhật ký APDU)
  CardSession: trạng thái thẻ dùng chung, chuỗi chặn SELECT → GET_STATUS → khóa công khai CSDL → AUTH_CARD
  Task: chạy APDU/CSDL ngoài luồng giao diện, hiện lỗi tiếng Việt
  10 màn: Dashboard, IssueCard, Students, MyInfo, Pin, Wallet, Cashier, Attendance, Security, Stats
```

| Vai trò (bảng `nguoi_dung`) | Màn hình |
|---|---|
| ADMIN | cả 10 |
| THU_NGAN | Thu ngân, Ví & lịch sử, Bảo mật thẻ |
| GIANG_VIEN | Điểm danh, Bảo mật thẻ |
| Sinh viên (không đăng nhập, dùng thẻ + PIN) | Thông tin thẻ, Mã PIN, Ví & lịch sử, Bảo mật thẻ |

Quy tắc: màn hình **không** tạo `CommandAPDU`, không viết SQL – chỉ gọi `CardClient` / `CardRepository`. Việc chậm (sinh khóa RSA, ghi ảnh, CSDL) chạy qua `Task` (SwingWorker). Phân quyền thật vẫn do thẻ quyết; ẩn màn hình chỉ cho gọn.

## Nhật ký

| Ngày | Ai | Thay đổi |
|---|---|---|
| 07/11/2026 | Thu | Bản đầu; khớp mã mẫu đã test 49 kịch bản |
| 08/11/2026 | Thu | CSDL chuyển SQL Server; mục 8 cập nhật theo phần mềm đã viết |
| | | |
