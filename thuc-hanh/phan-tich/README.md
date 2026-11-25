# Phân tích chuyên sâu các buổi thực hành

Mỗi file gồm: yêu cầu đề, kiến thức nền, phân tích mã theo dòng, phân tích lệnh và kết quả, luồng dữ liệu, cấu hình, thí nghiệm, lỗi hay gặp và câu hỏi vấn đáp. Mọi kết quả lệnh lấy từ lần chạy trên thẻ giả lập ngày 25/11/2026; **chưa chạy trên JCIDE**.

| Đọc theo thứ tự | Nội dung |
|---|---|
| 1. [Luồng dữ liệu](luong-du-lieu.md) | Máy tính ↔ đầu đọc ↔ JCRE ↔ applet; APDU, T=0/T=1, bộ nhớ, giữa hai applet, mật mã |
| 2. [TH1](th1.md) | Applet đầu tiên, APDU, kiểu dữ liệu |
| 3. [TH2](th2.md) | Đối tượng bền và tạm |
| 4. [TH3](th3.md) | Thao tác nguyên tử và giao dịch |
| 5. [TH4](th4.md) | Phương thức `install`, tham số cài đặt |
| 6. [TH5](th5.md) | Tường lửa, chia sẻ đối tượng, chia sẻ có xác thực |
| 7. [TH6](th6.md) | Băm, AES, RSA, xác thực thẻ, mã hóa trên thẻ |
| 8. [APDU mở rộng](apdu-mo-rong.md) | Dữ liệu lớn |
| 9. [Kết nối](ket-noi.md) | Chương trình máy tính và PC/SC |
| 10. [Cấu hình](cau-hinh.md) | AID, tham số, JCIDE, kết quả kiểm |
| 11. [Toàn bộ câu hỏi](cau-hoi.md) | Gom mọi câu hỏi kèm trả lời ngắn |

## Chạy thử

Mở PowerShell trong thư mục `thuc-hanh`:

```powershell
.\kiem-tra.cmd
```

Kết quả: `16 kịch bản: 308 đạt, 0 sai, 4 chỉ kiểm được trên JCIDE / thẻ thật`.
Kịch bản thí nghiệm kèm theo từng bài nằm ở [kich-ban/thi-nghiem](../kich-ban/thi-nghiem/).
