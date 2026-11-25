# Cách truyền dữ liệu giữa các phần

> Lý thuyết: [bài 1 slide 29–37](../../smart/docs/bai1-tong-quan-the-thong-minh.md), [bài 2 slide 6, 13](../../smart/docs/bai2-gioi-thieu-chung-ve-java-card.md), [bài 4](../../smart/docs/bai4-java-card-api.md). **[Thêm]** = ngoài tài liệu.

## 1. Toàn cảnh (bài 2 slide 6)

```text
┌──────────────┐   SQL    ┌────────────────┐  javax.smartcardio  ┌─────────────┐  TPDU  ┌──────────────────────┐
│ Máy chủ/CSDL │◄────────►│ Phần mềm máy   │◄───── (PC/SC) ─────►│ Đầu đọc thẻ │◄──────►│ Thẻ: JCRE → Applet   │
│ (back-end)   │          │ tính (host)    │       C-APDU/R-APDU │ (CAD)       │        │ (EEPROM, RAM, ROM)   │
└──────────────┘          └────────────────┘                     └─────────────┘        └──────────────────────┘
```

| Phần | Trong đề tài nhóm |
|---|---|
| Back-end | SQL Server (`CardRepository`) |
| Host | Swing (`CardClient`, các màn hình) |
| CAD | Đầu đọc ảo của JCIDE hoặc thẻ giả lập |
| Card | applet `StudentCard` |

Thẻ **không bao giờ tự gửi** trước: máy tính hỏi, thẻ trả lời.

## 2. APDU: đơn vị trao đổi

```text
C-APDU:  CLA INS P1 P2 [Lc] [Data] [Le]
R-APDU:  [Data] SW1 SW2
```

Giải mã một lệnh case 4:

```text
00      03      00  00  03    61 62 63    00
CLA     INS     P1  P2  Lc    dữ liệu     Le (tối đa 256)
```

| Case | Gửi dữ liệu | Nhận dữ liệu | Ví dụ |
|---|---|---|---|
| 1 | không | không | `00 01 00 00` |
| 2 | không | có | `00 00 00 00 0D` |
| 3 | có | không | `00 20 00 00 04 31 32 33 34` |
| 4 | có | có | `00 03 00 00 03 61 62 63 00` |

## 3. T = 0 và T = 1 (bài 1 slide 38; chi tiết **[Thêm]**)

| | T = 0 | T = 1 |
|---|---|---|
| Đơn vị | byte | khối |
| Lệnh case 4 | gửi không kèm Le; thẻ trả `61 xx`; máy tính gửi `00 C0 00 00 xx` để lấy dữ liệu | gửi trọn một lần |
| Le sai (case 2) | thẻ trả `6C xx`, gửi lại với Le đúng | |
| APDU mở rộng | khó | phù hợp |

Thư viện `javax.smartcardio` tự làm các bước `61 xx` / `6C xx`.

## 4. Bên trong thẻ: một lệnh đi qua đâu

```text
1. Đầu đọc chuyển lệnh vào thẻ
2. JCRE ghi header vào BỘ ĐỆM APDU (RAM, mảng toàn cục)
3. JCRE gọi applet.process(apdu)
4. applet: buf = apdu.getBuffer()                  đọc CLA INS P1 P2
           apdu.setIncomingAndReceive()            dữ liệu vào buf[5..]
           (receiveBytes nếu chưa đủ)
           xử lý: đọc/ghi EEPROM, RAM
           apdu.setOutgoing...  send...            dữ liệu ra
5. process trả về          → JCRE gửi 90 00
   ISOException.throwIt(sw) → JCRE gửi sw
   ngoại lệ khác            → 6F 00
```

Bộ đệm APDU vừa là đường vào vừa là đường ra: **đọc hết tham số cần thiết trước khi ghi đè** lên nó.

## 5. Chọn applet

```text
00 A4 04 00 Lc <AID>
   │
JCRE tìm applet theo AID ── không có ──► thường 6A 82
   │ có
applet cũ: deselect()  →  xóa mảng CLEAR_ON_DESELECT
applet mới: select()   →  process(SELECT), selectingApplet() = true  →  90 00
```

## 6. Dữ liệu lúc cài đặt

```text
JCIDE (Instance AID, tham số)
   └─► CAP + lệnh cài (qua APDU) ─► bộ cài trên thẻ ─► install(bArray, bOffset, bLength)
                                                          bArray = Li AID Lc ctrl La data
```

Chi tiết từng byte: [TH4 mục 2](th4.md).

## 7. Bên trong bộ nhớ

| Dữ liệu | Nằm ở | Sống tới khi |
|---|---|---|
| Mã applet, hằng `static final` | EEPROM | xóa applet |
| Trường của applet, mảng `new` | EEPROM | xóa applet |
| Mảng `CLEAR_ON_RESET` | RAM | reset |
| Mảng `CLEAR_ON_DESELECT` | RAM | bỏ chọn hoặc reset |
| Biến cục bộ, tham số | ngăn xếp (RAM) | hàm trả về |
| Bộ đệm APDU | RAM | dùng lại cho lệnh sau |

Ghi EEPROM chậm hơn RAM khoảng 1000 lần (bài 1 slide 23). Nhiều lần ghi cần nhất quán thì dùng giao dịch ([TH3](th3.md)).

## 8. Giữa hai applet

```text
Khách ── lookupAID ──► JCRE
Khách ── getAppletShareableInterfaceObject ──► JCRE ──► Chủ.getShareableInterfaceObject(AID khách, p)
Khách ◄──────────────── tham chiếu SIO ─────────────────────────┘
Khách ── sio.f(bộ đệm APDU, ...) ──► (đổi bối cảnh) Chủ.f ──► ghi kết quả vào bộ đệm APDU
```

Qua được tường lửa: kiểu cơ bản, mảng toàn cục. Mảng riêng của bên này, bên kia đụng vào sẽ `SecurityException`. Chi tiết và ví dụ có xác thực: [TH5](th5.md).

## 9. Luồng mật mã

### 9.1 Mã hóa dữ liệu (AES)

```text
Máy tính ─ bản rõ (đã đệm) ─► thẻ: AES(khóa trên thẻ) ─► bản mã ─► EEPROM / trả về
```

Khóa không rời thẻ; ranh giới bảo vệ là **bộ nhớ thẻ**.

### 9.2 Xác thực thẻ (RSA)

```text
Phát hành:  thẻ sinh cặp khóa ─ khóa công khai ─► CSDL
Mỗi lần:    máy tính ─ số ngẫu nhiên c ─► thẻ ─ ký(c) ─► máy tính ─ kiểm bằng khóa trong CSDL
```

### 9.3 Biên nhận giao dịch (đề tài)

```text
máy tính ─ lệnh nạp/trả (kèm PIN) ─► thẻ: giao dịch (số dư, bộ đếm, nhật ký) ─► ký biên nhận
máy tính ─ kiểm chữ ký + bộ đếm ─► CSDL
```

## 10. Chương trình kết nối

```text
Connect  → connectCard(): factory → list() → connect → getBasicChannel → transmit(SELECT)
Show Text→ getText(): transmit(B0 11) → getData() → chuỗi → ô văn bản
Disconnect → card.disconnect(false)
```

Chi tiết: [ket-noi.md](ket-noi.md).

## 11. Công cụ trong thư mục này

```text
kich-ban/*.apdu ─► ThucHanh ─┬─► TheGiaLap ─► jCardSim + phần bổ sung (SELECT, xóa RAM, giao dịch, AID người gọi)
                             └─► ThePcsc  ─► PC/SC ─► JCIDE / thẻ thật
```
