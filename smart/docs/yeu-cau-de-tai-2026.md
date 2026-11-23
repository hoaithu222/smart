# Ví dụ: Xây dựng thẻ bệnh nhân sử dụng Smart Card

> Nguồn: `380457193-1793635060488-viduyeucaudetai2026.pdf` · 4 trang · Cao Vinh - CNTT
> Chuyển từ PDF sang Markdown ngày 03/11/2026.

***Yêu cầu:***

- Có cơ chế xác thực thẻ bằng mã PIN, mã PIN có thể thay đổi, có giới hạn số lần nhập sai cho phép
- Lưu thông tin cá nhân của chủ thẻ (maBN, hoten, ngaysinh, quequan, matheBH, ảnh)
- Các thông tin trước khi lưu phải được mã hóa (sử dụng AES với khóa được tạo ra từ mã PIN). Có thể thay đổi thông tin người dùng.
- Có chức năng nạp tiền, thanh toán tiền khám chữa bệnh. Mỗi lần thanh toán, nạp tiền, yêu cầu cần phải xác thực bằng mã PIN và sử dụng mật mã khóa công khai (RSA hoặc ECC)
- Xây dựng phần mềm giao tiếp với thẻ trên máy tính để demo các chức năng

## CÁC MỐC BÁO CÁO PROJECT MÔN SMART CARD 2026

### LẦN 1 (30/11/2026)

*Yêu cầu tối thiểu:*

- Lên được giao diện cho phần mềm trên máy tính
- Lên được ý tưởng chung cho bài toán, mô tả rõ ràng, chi tiết các chức năng
- Tạo mã PIN, thay đổi, kiểm tra mã PIN, có cơ chế giới hạn số lần nhập sai
- Triển khai lưu thông tin lên thẻ

### LẦN 2 (14/12/2026)

*Yêu cầu tối thiểu:*

- Triển khai lưu thông tin và mã hóa thông tin trên thẻ bằng mật mã khóa bí mật AES
- Xác thực thẻ bằng mật mã khóa công khai RSA

### LẦN 3 Báo cáo lấy điểm cuối kì (dự kiến 28/12/2026)

- Xây dựng được phần mềm giao tiếp trên máy tính, có CSDL
- Hoàn thành Project với đầy đủ chức năng đã được mô tả trong bài toán
- Báo cáo bản Word, bản PP để thuyết trình

## Yêu cầu báo cáo cuối kì

**Nộp bản mềm** (bao gồm: Project + Báo cáo bản Word + Video demo + Phân công công việc (Cụ thể ai làm gì?)+ Điểm dự kiến) → gửi file nén)

Trong đó:

- Trừ project, tất cả các file word, slide, phân công, file tổng thể phải đặt tên theo quy tắc Tên lớp tín chỉ_Tên nhóm_Tên đề tài.

**Ví dụ: L01_Nhóm 1_ Thẻ gửi xe**

- Báo cáo (tối thiểu 30 trang A4) trình bày về việc triển khai Project của nhóm, không đi sa đà vào trình bày cơ sở lý thuyết nền tảng

  Báo cáo chỉ bao gồm 2 chương (Chương 1- Phân tích thiết kế (Trước khi phân tích thiết kế phải nêu rõ được bài toán) và Chương 2 - Cài đặt và thực nghiệm) (Bỏ qua 1 chương cơ sở lý thuyết)

Thang điểm:

| STT | Nội dung | Thang điểm | Lưu ý |
|---|---|---|---|
| 1 | Báo cáo bản Word | 0.5 điểm | |
| 2 | Khả năng thuyết trình | 0.5 điểm | |
| 3 | Nội dung Project:<br>- Giao diện đẹp (0.5 điểm)<br>- Khởi tạo thẻ (0.5 điểm)<br>- Thay đổi thông tin cá nhân (0.5 điểm)<br>- Mã hóa được thông tin (1 điểm)<br>- Thay đổi mã PIN, mã hóa mã PIN (0.5 điểm)<br>- Tạo được cặp khóa RSA (0.5 điểm)<br>- Xác thực được theo chức năng (0.5 điểm)<br>- Có CSDL (0.5 điểm)<br>- Có demo chức năng (0.5 điểm) | 5 điểm | |
| 4 | Trả lời câu hỏi | 4 điểm | Ở mục 4 này, nếu không trả lời được câu hỏi hoặc quá kém → Fail luôn mà không cần quan tâm tới 3 mục trên |

**Hai ngày** trước khi báo cáo, các nhóm gửi ***toàn bộ bản mềm*** (bao gồm các file đính kèm như đã nêu trên). Nếu thiếu 1 trong các nội dung đã nêu hoặc không gửi trước, nhóm sẽ không được báo cáo! → Thi lại luôn

Các nhóm đã báo cáo, nộp lại toàn bộ bản mềm sau khi đã chỉnh sửa. Deadline: **Thông báo sau**. Nếu quá ngày mà các nhóm chưa gửi **điểm sẽ bị hủy → Thi lại.**
