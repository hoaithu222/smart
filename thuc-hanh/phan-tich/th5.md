# TH5 – Chia sẻ đối tượng giữa các applet: phân tích chi tiết

> Đề: [th5-chia-se-doi-tuong-giua-cac-applet.md](../../smart/docs/th5-chia-se-doi-tuong-giua-cac-applet.md). Hỏi đáp lý thuyết: [TH5-cau-hoi-chia-se.md](../TH5-cau-hoi-chia-se.md).
> Mã: [masterPack](../src/masterPack/), [slavePack](../src/slavePack/), phần mở rộng [chiaSeAnToan](../src/chiaSeAnToan/) + [khachAnToan](../src/khachAnToan/) + [khachLa](../src/khachLa/).
> Kết quả từ [th5.apdu](../kich-ban/th5.apdu) và [th5-an-toan.apdu](../kich-ban/th5-an-toan.apdu). **[Thêm]** = ngoài tài liệu.
> Thẻ giả lập **không có tường lửa**; công cụ ở đây chỉ bổ sung việc nhận biết **ai đang gọi** (`getAID`, `getPreviousContextAID`).

## 1. Luồng chia sẻ

```text
 applet KHÁCH (bối cảnh B)                 JCRE                      applet CHỦ (bối cảnh A)
 lookupAID(AID chủ) ───────────────────►  tìm applet  ─► AID object
 getAppletShareableInterfaceObject ────►  chuyển bối cảnh ───────►  getShareableInterfaceObject(AID khách, p)
                                          ◄─────────── trả SIO (hoặc null) ◄────
 sio.phuongThuc(bộ đệm APDU) ──────────►  chuyển sang A ─────────►  chạy trong bối cảnh A
                                          getPreviousContextAID() = AID khách
 ◄──────────── giá trị trả về / dữ liệu đã ghi vào bộ đệm APDU ◄──
```

Dữ liệu đi qua được: **kiểu cơ bản** và **mảng toàn cục** (bộ đệm APDU).

## 2. Ví dụ của đề

| Thành phần | Vai trò |
|---|---|
| [`masterInterface`](../src/masterPack/masterInterface.java#L9) | `extends Shareable`: `getArray`, `tinhDiem` |
| [`masterApp`](../src/masterPack/masterApp.java#L38) | trả `this` khi `parameter == 0` |
| [`slaveApp`](../src/slavePack/slaveApp.java#L33) | `lookupAID` → xin SIO → `getArray(buf)` |

| Applet khách | Kết quả | Vì sao |
|---|---|---|
| `slaveApp` | `01 02 03 04 05` | truyền bộ đệm APDU |
| `slaveAppDiem` | `08` | `(6×8 + 4×9) / 10 = 84 / 10 = 8` |
| `slaveApp1` | `08` | truy cập trường `static` qua tham chiếu null vẫn được |
| `slaveAppNonStatic` | `6F 00` | đối tượng chưa `new` |
| `slaveAppMang` | `6F 00` chỉ trên thẻ thật | chủ ghi vào mảng riêng của khách |
| chưa cài chủ | `6A 82` | `lookupAID` trả `null` |

**Phép tính `tinhDiem`:** `(byte)((byte)((byte)(6*t) + (byte)(4*v)) / 10)`. Với điểm 0..10, tổng tối đa 100, vẫn vừa `byte`. **[Thêm]** Nếu điểm lớn hơn, `(byte)` sẽ tràn.

## 3. Phần mở rộng: chia sẻ an toàn (theo ghi chú của nhóm)

Ghi chú trong [th/th5](../../th/th5-chia-se-applet.md) yêu cầu: chủ phải biết **ai** đang xin, kiểm AID **ở mỗi phương thức**, và thêm **thử thách – phản hồi** trước khi cấp quyền. Ba applet sau làm đúng như vậy.

### 3.1 Ba lớp bảo vệ

| Lớp | Chỗ trong mã | Chống |
|---|---|---|
| 1. Chỉ trả SIO cho đúng AID | [ChuApplet dòng 53](../src/chiaSeAnToan/ChuApplet.java#L53) | applet lạ xin đối tượng |
| 2. Mỗi phương thức kiểm `getPreviousContextAID` | [DichVu dòng 55–58](../src/chiaSeAnToan/DichVu.java#L55) | tham chiếu SIO bị chuyển tay cho applet khác |
| 3. Thử thách – phản hồi, quyền dùng một lần | [DichVu dòng 62–90](../src/chiaSeAnToan/DichVu.java#L62) | giả AID, phát lại, đọc nhiều lần |

Ngoài ra chủ trả **đối tượng `DichVu` riêng** chứ không trả `this`, nên khách không ép sang giao diện khác của applet chủ.

### 3.2 Giao thức

```text
KHÁCH                                   CHỦ
 layThuThach(buf) ───────────────────►  sinh 16 byte ngẫu nhiên, nhớ trong RAM, ghi ra buf
 R = AES(khóa chung, thử thách)
 traLoi(buf = R)  ───────────────────►  tự tính AES(khóa, thử thách), so sánh
                                        đúng: bật cờ quyền ; thử thách bị hủy
 docMaSV(buf)     ───────────────────►  kiểm cờ → trả "CT000001" → tắt cờ
```

### 3.3 Vì sao trạng thái ở chủ là `CLEAR_ON_RESET`

Khi khách gọi SIO, applet **được chọn** vẫn là khách, còn bối cảnh đang hoạt động là của chủ. Mảng `CLEAR_ON_DESELECT` chỉ truy cập được khi applet được chọn thuộc đúng bối cảnh đó, nên nếu chủ dùng loại này sẽ bị `SecurityException` trên thẻ thật. Khách thì dùng `CLEAR_ON_DESELECT` được vì nó đang được chọn. **[Thêm]**

### 3.4 Kết quả đã chạy

| Lệnh tới `KhachApplet` | Tình huống | Kết quả |
|---|---|---|
| `00 00 00 00 00` | đúng quy trình | `"CT000001" 90 00` |
| `00 01 00 00` | phản hồi sai 1 bit | `63 00` |
| `00 02 00 00` | đọc không qua thử thách | `69 82` |
| `00 03 00 00` | đọc hai lần với một quyền | `69 82` |
| `00 04 00 00 01` | phát lại phản hồi cũ | `00` (bị từ chối) |
| `00 05 00 00` | xin với tham số sai | `69 85` |
| `KeXamNhap` | applet lạ | `69 85` |
| Khách nạp khóa khác | sai khóa | `63 00` |
| Hai bên nạp cùng khóa riêng | | `"CT000001"` |

### 3.5 Mỗi bên phải làm gì

| Applet **chia sẻ** (chủ) | Applet **nhận** (khách) |
|---|---|
| Khai báo giao diện `extends Shareable`, chỉ gồm những phương thức cần chia sẻ | `import` giao diện của package chủ (cần file export khi build) |
| Viết lớp SIO riêng cài giao diện đó | Biết AID applet chủ |
| Ghi đè `getShareableInterfaceObject`, kiểm AID và tham số | `lookupAID`, kiểm `null` |
| Kiểm AID người gọi trong **mỗi** phương thức | `getAppletShareableInterfaceObject`, kiểm `null`, ép đúng giao diện |
| Xác thực thử thách – phản hồi, cấp quyền ngắn | Tính phản hồi bằng khóa của mình |
| Chỉ ghi dữ liệu ra mảng toàn cục | Truyền **bộ đệm APDU**, không truyền mảng riêng |
| Được cài **trước** | Được cài **sau** |

### 3.6 Giới hạn của ví dụ

- Khóa mẫu `00 01 … 0F` viết trong mã chỉ để học. Thẻ thật phải nạp khóa riêng qua tham số cài đặt hoặc kênh an toàn.
- Kiểm AID không chống được applet giả nếu ai cũng nạp được applet lên thẻ; lớp thử thách – phản hồi bù phần này vì applet giả không có khóa.
- Applet **cùng package** với applet khách thì chung bối cảnh: tường lửa không ngăn nếu nó lấy được tham chiếu tới đối tượng của khách. Đừng đặt mã không tin cậy cùng package.
- Chưa chạy trên JCIDE; tường lửa chưa được kiểm.

## 4. Cấu hình

| Package | AID | Applet | Applet AID |
|---|---|---|---|
| `masterPack` | `11 22 33 44 55 05 00` | masterApp | `…05 00 00` |
| `slavePack` | `11 22 33 44 55 05 01` | 5 applet khách | `…05 01 01` → `05` |
| `chiaSeAnToan` | `11 22 33 44 55 05 02` | ChuApplet | `…05 02 01` |
| `khachAnToan` | `11 22 33 44 55 05 03` | KhachApplet | `…05 03 01` |
| `khachLa` | `11 22 33 44 55 05 04` | KeXamNhap | `…05 04 01` |

Thứ tự build và cài: **package chủ trước** (khách dùng file export của chủ). Tham số tùy chọn cho `ChuApplet` và `KhachApplet`: 16 byte khóa chung, hai bên phải giống nhau.

```powershell
.\chay.cmd kich-ban\th5-an-toan.apdu
```

## 5. Câu hỏi vấn đáp bổ sung

1. **Có AID rồi sao còn thử thách?** AID chỉ là tên; thử thách chứng minh bên kia có khóa.
2. **Vì sao thử thách phải ngẫu nhiên và dùng một lần?** Để câu trả lời cũ không dùng lại được.
3. **Vì sao kiểm AID trong cả phương thức khi đã kiểm lúc cấp SIO?** Tham chiếu có thể bị chuyển cho applet khác.
4. **`getAID` trong phương thức chia sẻ trả gì?** AID applet chủ; `getPreviousContextAID` trả AID bên gọi.
5. **Vì sao không dùng `static` để chia sẻ?** Tường lửa không chặn trường static, ai cũng thấy, và package applet không xuất static cho package khác.
