# Bước 7 · APDU mở rộng → Lưu ảnh sinh viên trên thẻ

> Tài liệu: [apdu-mo-rong.md](../../apdu-mo-rong.md) (`ExtendedLength`, `receiveBytes`, bộ đệm APDU tối thiểu 133 byte, thẻ JCOP 261 byte).
> Mốc: **Lần 2–3**. Đề mẫu có "ảnh" trong thông tin chủ thẻ. Người phụ trách: ……

## 1. Bài học cần nắm

- APDU thường: tối đa 255 byte dữ liệu gửi xuống, 256 byte nhận về.
- APDU mở rộng (từ Java Card 2.2.2): applet `implements javacardx.apdu.ExtendedLength`, đọc phần dữ liệu còn lại bằng vòng `receiveBytes`; dễ làm với **T=1**, với T=0 cần cẩn thận.
- Bộ đệm APDU chỉ vài trăm byte → dữ liệu lớn luôn phải xử lý **từng phần** dù dùng cách nào.

## 2. Áp dụng vào đề tài: hai cách, chọn cách 1

| | Cách 1 – chia khối 240 byte bằng APDU thường (**chọn**) | Cách 2 – APDU mở rộng (như ví dụ trong tài liệu) |
|---|---|---|
| Giao thức | Chạy cả T=0 (như cấu hình của cô) | Nên chuyển T=1 (`card.protocol=T=1`) |
| Số lệnh cho ảnh 5 KB | 21 lệnh `WRITE_OBJ 40` (5 008 byte sau đệm ÷ 240) | 1 lệnh (vẫn nhận nhiều lần bằng `receiveBytes` bên trong thẻ) |
| Mã hóa AES | Mỗi khối 240 byte = 15 khối AES, mã ngay khi nhận | Phải mã từng đoạn trong vòng `receiveBytes` |
| Rút thẻ giữa chừng | Khối đã ghi vẫn đúng, `objLen` cho biết đã ghi tới đâu | Mất cả lệnh |
| Rủi ro trên giả lập | Thấp | JCIDE/đầu đọc ảo có thể không hỗ trợ APDU mở rộng |

Cách 1 dùng chung lệnh `40/41` với thông tin chữ (P1 = `03` là ảnh), không phải viết thêm lệnh. Trong báo cáo vẫn trình bày cách 2 như phương án đã cân nhắc (và có thể làm thử cho đủ bài).

## 3. Việc cần làm

- [ ] Phần mềm: chọn ảnh → thu nhỏ 120×160 → nén JPEG, giảm chất lượng tới khi ≤ `app.photo.maxBytes` (7600 byte, vì sau đệm phải ≤ 7680).
- [ ] Gửi bằng `CardClient.writeObject(OBJ_PHOTO, jpeg)` (tự đệm + chia khối); đọc bằng `readObject(OBJ_PHOTO, status().photoLen)` rồi hiện lên `JLabel`.
- [ ] Ghi ảnh chạy trong `SwingWorker` + thanh tiến trình (tới 32 lệnh với ảnh 7,6 KB).
- [ ] (tùy chọn) Làm lại ví dụ `bai7_APDU_Extended` của tài liệu với T=1 để trả lời được câu hỏi.

## 4. Mã phía máy tính: nén ảnh cho vừa thẻ

```java
static byte[] anhChoThe(File file, AppConfig cfg) throws IOException {
    BufferedImage src = ImageIO.read(file);
    BufferedImage dst = new BufferedImage(cfg.photoWidth(), cfg.photoHeight(), BufferedImage.TYPE_INT_RGB);
    Graphics2D g = dst.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(src, 0, 0, cfg.photoWidth(), cfg.photoHeight(), null);
    g.dispose();

    for (float q = 0.8f; q >= 0.2f; q -= 0.1f) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageWriter w = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam p = w.getDefaultWriteParam();
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(q);
        ImageOutputStream ios = ImageIO.createImageOutputStream(out);
        w.setOutput(ios);
        w.write(null, new IIOImage(dst, null, null), p);
        ios.close();
        w.dispose();
        if (out.size() <= cfg.photoMaxBytes()) {
            return out.toByteArray();
        }
    }
    throw new IOException("Ảnh vẫn quá lớn, chọn ảnh đơn giản hơn");
}

// Hiện ảnh đọc từ thẻ
byte[] jpeg = card.readObject(CardClient.OBJ_PHOTO, card.status().photoLen);
lblAnh.setIcon(new ImageIcon(ImageIO.read(new ByteArrayInputStream(jpeg))));
```

## 5. Kiểm thử

- Bộ test: kịch bản *Đọc lại ảnh 5000 byte qua 21 khối, đúng từng byte*.
- Tay: `B0 40 03 00 F0 <240 byte>` khối 0, `B0 40 03 01 …` khối 1; gửi khối 3 khi mới có khối 0 → `6B 00`; dữ liệu 100 byte (không bội 16) → `67 00`.

## 6. Câu hỏi có thể bị hỏi

1. *APDU mở rộng khác APDU thường thế nào? Cần gì để dùng?* – Lc/Le 3 byte, tới 65 535 byte; applet `implements ExtendedLength`; Java Card ≥ 2.2.2; nên T=1.
2. *`setIncomingAndReceive` và `receiveBytes` khác gì?* – lần nhận đầu / các lần nhận tiếp khi dữ liệu dài hơn bộ đệm.
3. *Sao nhóm không dùng APDU mở rộng cho ảnh?* – mục 2.
4. *Ảnh tốn bao nhiêu bộ nhớ thẻ?* – ô PHOTO 7 680 byte EEPROM; thẻ JCOP 40 KB còn dư.

## Nhật ký

| Ngày | Ai | Làm được | Vướng |
|---|---|---|---|
| | | | |
