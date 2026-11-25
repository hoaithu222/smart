# Kết nối phần mềm máy tính với thẻ: phân tích chi tiết

> Đề: [huong-dan-ket-noi.md](../../smart/docs/huong-dan-ket-noi.md). Mã: [applet cnt](../src/cnt/cnt.java), [Connect_Ex_GUI](../Connect_Ex_GUI/src/connect_ex_gui/). Kết quả từ [cnt.apdu](../kich-ban/cnt.apdu) và [tn-cnt.apdu](../kich-ban/thi-nghiem/tn-cnt.apdu). **[Thêm]** = ngoài tài liệu.

## 1. Chuỗi truyền

```text
Nút bấm (GUI)
   └─► SmartCardWord ─► javax.smartcardio ─► dịch vụ PC/SC của Windows
                                                └─► đầu đọc (thật hoặc "JAVACOS Virtual Contact Reader 0")
                                                       └─► thẻ: JCRE ─► applet cnt
Phản hồi đi ngược lại: dữ liệu + SW1 SW2
```

## 2. Applet ([cnt.java](../src/cnt/cnt.java))

| Phần | Việc |
|---|---|
| Hàm tạo | đọc tham số cài đặt, `register`, tạo `text = "HELLO"` |
| `process` | bỏ qua SELECT; CLA khác `B0` → `6E 00`; INS `11` → gửi `text`; khác → `6D 00` |

Khác tài liệu: tài liệu tự so `00 A4` để bỏ qua lệnh chọn; ở đây dùng `selectingApplet()`.

## 3. Lớp giao tiếp ([SmartCardWord](../Connect_Ex_GUI/src/connect_ex_gui/SmartCardWord.java#L34))

| Bước | Lớp / lệnh (`javax.smartcardio`) |
|---|---|
| 1 | `TerminalFactory.getDefault()` |
| 2 | `factory.terminals().list()` – danh sách đầu đọc |
| 3 | `terminal.connect("T=0")` – mở kết nối, nhận ATR |
| 4 | `card.getBasicChannel()` |
| 5 | `channel.transmit(new CommandAPDU(0x00, 0xA4, 0x04, 0x00, AID))` – chọn applet |
| 6 | `channel.transmit(new CommandAPDU(0xB0, 0x11, 0x00, 0x00, 256))` – đọc chuỗi |
| 7 | `response.getSW()`, `response.getData()` |
| 8 | `card.disconnect(false)` – ngắt, không reset thẻ |

`CommandAPDU(cla, ins, p1, p2, 256)` sinh lệnh case 2 `B0 11 00 00 00`.

**[Thêm]** Với T = 0, thư viện Java tự xử lý `61 xx` (gửi GET RESPONSE) và `6C xx`.

Thêm so với tài liệu: chọn đầu đọc đang có thẻ (JCIDE có hai đầu đọc ảo), thử `*` khi T=0 lỗi, giữ lại lý do lỗi.

## 4. Giao diện ([GUI](../Connect_Ex_GUI/src/connect_ex_gui/GUI.java#L84))

| Trạng thái | Connect | Show Text |
|---|---|---|
| Chưa nối | "Connect" (đen) | khóa |
| Đã nối | "Disconnect" (đỏ) | mở; hiện `HELLO` |

**[Thêm]** Lệnh thẻ chạy trực tiếp trên luồng giao diện; lệnh lâu (sinh khóa RSA) sẽ làm cửa sổ đứng. Đề tài nhóm dùng `SwingWorker`.

## 5. Chạy

```powershell
.\chay-ket-noi.cmd
```

```powershell
.\chay-ket-noi.cmd pcsc
```

Lệnh đầu dùng thẻ giả lập; lệnh sau dùng JCIDE (đã cài `cnt` và Run).

**NetBeans:** tạo *Java Application* `Connect_Ex_GUI` (bỏ tạo lớp main), chép gói `connect_ex_gui` vào `src`, chạy `GUI.java`.

## 6. Cấu hình

| Mục | Giá trị |
|---|---|
| Package / Applet AID | `99 88 77 66 55` / `99 88 77 66 55 00` |
| JCIDE | Enable PCSC Interface; Server IP 127.0.0.1; Run trước khi mở chương trình |
| Giao thức | T = 0 |

## 7. Kết quả đã chạy

| Lệnh | Kết quả |
|---|---|
| SELECT `99 88 77 66 55 00` | `90 00` |
| SELECT AID sai | `6A 82` |
| `B0 11 00 00 00` | "HELLO" `90 00` |
| `00 11 00 00` | `6E 00` |
| `B0 12 00 00` | `6D 00` |

## 8. Lỗi hay gặp

| Hiện tượng | Nguyên nhân |
|---|---|
| Danh sách đầu đọc rỗng | JCIDE chưa Run, chưa bật PCSC, dịch vụ Smart Card dừng |
| Không nối được | PyApduTool đang giữ thẻ (Exclusive) |
| `6A 82` khi chọn | applet chưa cài hoặc sai AID |
| `6400` | tài liệu coi là thẻ bị vô hiệu hóa |
| Lỗi giao thức | thẻ chỉ hỗ trợ T = 1 |

## 9. Câu hỏi vấn đáp

1. **Phần mềm nói chuyện với thẻ qua gì?** PC/SC, thư viện `javax.smartcardio`.
2. **Vì sao phải SELECT trước?** Để JCRE chuyển các lệnh sau tới đúng applet.
3. **ATR là gì?** **[Thêm]** Chuỗi trả lời khi thẻ được cấp điện, mô tả giao thức thẻ hỗ trợ.
4. **`disconnect(false)` khác `true`?** Có reset thẻ hay không.
5. **Đổi chuỗi trả về thế nào?** Sửa mảng `text` hoặc thêm lệnh ghi.
