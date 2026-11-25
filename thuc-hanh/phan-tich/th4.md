# TH4 – Phương thức `install`: phân tích chi tiết

> Đề: [th4-phuong-thuc-install.md](../../smart/docs/th4-phuong-thuc-install.md). Câu hỏi lý thuyết đã trả lời đầy đủ ở [TH4-cau-hoi-install.md](../TH4-cau-hoi-install.md); file này đi sâu vào mã, dữ liệu và cấu hình.
> Mã: [src/bai4_InstallMethod](../src/bai4_InstallMethod/). Kết quả từ [th4.apdu](../kich-ban/th4.apdu) và [tn4.apdu](../kich-ban/thi-nghiem/tn4.apdu). **[Thêm]** = ngoài tài liệu.

## 1. Chuỗi cài đặt

```text
JCIDE / trình cài đặt
   │  nạp file CAP, gửi Instance AID + tham số
   ▼
JCRE ─► install(bArray, bOffset, bLength)
            └─► new Applet(...)
                    ├─ đọc + KIỂM tham số
                    ├─ cấp phát mọi đối tượng
                    └─ register(...)        ← từ đây applet mới chọn được
```

Lỗi (ném ngoại lệ, hết bộ nhớ) **trước** khi `register` thành công: việc cài thất bại, JCRE dọn sạch.

## 2. Dữ liệu đi từ JCIDE vào applet

Nhập ở JCIDE: Instance AID `44 49 45 4D 5F 54 48 49`, Application Specific Parameters `53 56 30 31 09`.

```text
bArray (trường hợp Lc = 0):
vị trí : 0    1 ........ 8   9    10   11 ....... 14   15
byte   : 08   44 49 45 4D 5F 54 48 49   00   05   53 56 30 31   09
ý nghĩa: Li   AID "DIEM_THI"            Lc   La   "SV01"        số môn
```

| Biến | Công thức vị trí |
|---|---|
| AID | `bOffset + 1`, dài `Li` |
| `Lc` | `bOffset + Li + 1` |
| `La` | `bOffset + Li + Lc + 2` |
| Dữ liệu | `bOffset + Li + Lc + 3` |

**[Thêm]** Trên thẻ theo GlobalPlatform, phần "thông tin điều khiển" thường chứa byte đặc quyền (`Lc = 1`), nên **luôn đọc `Lc` rồi nhảy qua**. `bLength` là `byte` (tối đa 127). Không giữ tham chiếu `bArray` sau `install`.

## 3. Phân tích mã

### 3.1 Applet1 – cấp phát trong hàm tạo ([dòng 17–28](../src/bai4_InstallMethod/Applet1.java#L17))

`monhoc`, `diem` (mỗi mảng 10 byte) cấp một lần; `register()` **không tham số** nên dùng AID mặc định trong file CAP.

### 3.2 Applet2 – ví dụ 4.4 ([dòng 34–66](../src/bai4_InstallMethod/Applet2.java#L34))

1. Nhớ vị trí đầu (`dau`) để đăng ký ở cuối.
2. Nhảy qua AID, qua thông tin điều khiển, đọc `La`.
3. `La != 0`: phải ≥ 5, nếu không `6A 80`; chép 4 byte mã SV, đọc 1 byte số môn.
4. `La == 0`: mặc định `SV01`, **8** môn (đề: chữ ghi 9, mã gán `0x08`).
5. `register` (theo AID trong tham số nếu `Li ≠ 0`).

| Cài | `00 00 01 02` trả |
|---|---|
| Tham số `53 56 30 31 09` | `53 56 30 31 09 90 00` |
| Không tham số | `53 56 30 31 08 90 00` |
| Tham số `53 56` | **cài bị từ chối** |

Hai chỗ khác mã đề, cần giải thích được:
- Đề khai báo `static`: cài hai thể hiện thì cái sau **đè** dữ liệu cái trước. Bỏ `static`.
- Đề `register` **trước** khi đọc tham số: lỗi xảy ra sau đó thì JCRE vẫn coi là đã cài. Đưa `register` xuống cuối.

### 3.3 Applet3 – bài tập 4.5

**Cấu trúc dữ liệu**

```text
diemThi (soLuongMonThi × 2 byte), soDaNhap = 3:
[01 08][02 06][04 09][00 00] ... [00 00]
  ID ^điểm
```

**Hàm tạo** ([dòng 49–79](../src/bai4_InstallMethod/Applet3.java#L49)): tham số đúng 5 byte, số môn 1..60 → cấp `diemThi` đúng kích thước → `register`.

**Điều phối** ([dòng 87–109](../src/bai4_InstallMethod/Applet3.java#L87)): nhập/sửa/xóa xong đều gọi `inDiem` (đúng yêu cầu "sau mỗi lần nhập, sửa, xóa đều in").

| Hàm | Kiểm trước | Ghi (trong giao dịch) |
|---|---|---|
| [`nhapDiem`](../src/bai4_InstallMethod/Applet3.java#L152) | số byte chẵn; điểm 0..10; ID chưa có và không lặp trong lệnh; đủ chỗ | chép khối vào cuối, tăng `soDaNhap` |
| [`suaDiem`](../src/bai4_InstallMethod/Applet3.java#L174) | mọi ID đều tồn tại | đổi byte điểm |
| [`xoaDiem`](../src/bai4_InstallMethod/Applet3.java#L193) | mọi ID tồn tại, không lặp | dồn phần sau lên, ghi 0 cặp thừa |

Nguyên tắc **"kiểm hết rồi mới ghi"**: một mục sai thì không mục nào thay đổi.

`timMon` duyệt bước 2 để chỉ so byte ID (không so nhầm byte điểm).

Khi xóa, dùng `Util.arrayCopy` (an toàn với vùng chồng nhau) rồi gán từng phần tử bằng 0 để nằm trong giao dịch; `arrayFillNonAtomic` sẽ **không** được hoàn tác.

**Diễn biến một phiên**

| Lệnh | `diemThi` sau lệnh |
|---|---|
| nhập `01 08 02 06 04 09` | `01 08 02 06 04 09` |
| nhập `02 07` | `6A 89`, giữ nguyên |
| sửa `02 0A` | `01 08 02 0A 04 09` |
| xóa `01` | `02 0A 04 09` |
| nhập thêm 7 môn | đủ 9 môn |
| nhập thêm | `6A 84` |

## 4. Bảng lệnh bài tập

| Lệnh | Case | Chức năng |
|---|---|---|
| `00 00 00 00 00` | 2 | mã SV (4) + số môn tối đa + số đã nhập |
| `00 01 00 00 Lc …` | 3/4 | nhập |
| `00 02 00 00 00` | 2 | in |
| `00 03 00 00 Lc …` | 3/4 | sửa |
| `00 04 00 00 Lc …` | 3/4 | xóa |

**[Thêm]** Lệnh có dữ liệu gửi xuống **và** có dữ liệu trả về là case 4; nên thêm `00` (Le) ở cuối khi chạy trên T = 1.

## 5. Mã lỗi

| SW | Nghĩa |
|---|---|
| `6A 80` | điểm ngoài 0..10, thiếu byte, tham số cài sai |
| `6A 89` | trùng ID môn |
| `6A 84` | vượt số môn thi |
| `6A 83` | không có môn đó |
| `67 00` | không có dữ liệu hoặc quá 120 byte |

## 6. Cấu hình

| Applet | Applet AID | Instance AID | Tham số |
|---|---|---|---|
| Applet1 | `11 22 33 44 55 04 01` | (mặc định) | — |
| Applet2 | `…04 02` | `44 49 45 4D 5F 54 48 49` | `53 56 30 31 09` |
| Applet3 | `…04 03` | `44 49 45 4D 5F 54 48 49` | `53 56 30 31 09` |

Applet2 và Applet3 **dùng chung Instance AID** nên chỉ cài một cái mỗi lần.
JCIDE: chuột phải applet → `Run or Debug Configuration` → `Edit` → nhập Instance AID và tham số (Hình 4.1–4.3). Chọn: `/select 4449454D5F544849`.
Công cụ: `.\chay.cmd kich-ban\th4.apdu` (25 mục, dừng chờ cài lại khi chạy PC/SC).

## 7. Thí nghiệm đã chạy

| Thử | Kết quả |
|---|---|
| Applet2 tham số `53 56` | cài bị từ chối |
| Applet3 tham số 4 byte / số môn 0 / 61 | cài bị từ chối |
| Số môn = 1: nhập môn thứ hai | `6A 84` |
| Cài thêm applet cùng Instance AID | bị từ chối |
| ID môn `00` và `FF` | nhận bình thường |
| Reset thẻ | điểm còn nguyên |

> Thẻ giả lập báo lỗi cài đặt bằng mã chung; JCIDE / thẻ thật có thể báo khác.

## 8. Câu hỏi vấn đáp bổ sung

1. **Vì sao kiểm tham số trước khi cấp phát?** Sai thì dừng sớm, không để lại gì.
2. **Hai thể hiện cùng lớp có chung biến không?** Trường thường: riêng; trường `static`: chung.
3. **Đổi số môn thi sau khi cài được không?** Không, phải cài lại (mảng đã cấp cố định).
4. **Vì sao sau xóa phải ghi 0 phần thừa?** Không để dữ liệu cũ còn trong mảng.
5. **Vì sao dùng giao dịch khi xóa?** Dồn mảng và giảm bộ đếm phải đi cùng nhau.
