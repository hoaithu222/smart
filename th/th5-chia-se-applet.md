# Bước 5 · TH5 → Chia sẻ đối tượng giữa các applet: dùng hay không?

> Tài liệu: [th5-chia-se-doi-tuong-giua-cac-applet.md](../../th5-chia-se-doi-tuong-giua-cac-applet.md), giao diện `Shareable` ở [bai4 slide 21](../../bai4-java-card-api.md).
> Mốc: không có tiêu chí chấm riêng. Bước này chủ yếu để **trả lời được câu hỏi** và giữ đường mở rộng. Người phụ trách: ……

## 1. Bài học cần nắm

- Mỗi applet một ngữ cảnh; **tường lửa** chặn applet này đọc trường/mảng của applet khác.
- Muốn chia sẻ: applet chủ khai báo giao diện kế thừa `javacard.framework.Shareable`, lớp nào đó cài đặt giao diện (thường chính applet), ghi đè `getShareableInterfaceObject(AID client, byte param)`.
- Applet khách: `JCSystem.lookupAID(...)` → `JCSystem.getAppletShareableInterfaceObject(aid, param)` → ép về kiểu giao diện → chỉ gọi được các phương thức trong giao diện.
- Tham số/kiểu trả về qua tường lửa nên là kiểu cơ bản hoặc mảng toàn cục (APDU buffer); truyền mảng của applet khách sang chủ thì chủ không đọc được (kịch bản 2 của TH5 ra `6F 00`).

## 2. Quyết định cho đề tài: **một applet**

| Phương án | Ưu | Nhược |
|---|---|---|
| **1 applet `StudentCard`** (chọn) | PIN, khóa AES, ví, nhật ký cùng ngữ cảnh → không qua tường lửa; ít lỗi; dễ demo trên JCIDE | Applet to hơn |
| 2 applet: Định danh (chủ, giữ PIN + thông tin) + Ví/Điểm danh (khách) | Minh họa TH5; tách trách nhiệm | Mỗi lần chuyển applet là **bỏ chọn** applet chủ → khóa phiên `CLEAR_ON_DESELECT` và cờ PIN bị xóa → phải thiết kế lại phiên đăng nhập; truyền dữ liệu qua tường lửa phức tạp; JCIDE phải cài 2 gói |

Lý do chính để nói khi bị hỏi: yêu cầu đề tài là PIN bảo vệ cả thông tin lẫn giao dịch; để chung một applet thì "đã nhập PIN" là một trạng thái duy nhất trong cùng ngữ cảnh, không cần mở lỗ qua tường lửa.

## 3. Phương án mở rộng (nếu cô yêu cầu hoặc dư thời gian)

Applet **Thư viện** (gói khác, AID `4B 4D 41 53 56 02 01`) muốn biết mã SV và "chủ thẻ đã nhập PIN chưa" mà không đọc được dữ liệu khác:

```java
// Gói sinhvien – giao diện công bố ra ngoài
public interface StudentShare extends Shareable {
    /** Chép mã SV vào buf (nên là APDU buffer), trả về độ dài. */
    short getCardId(byte[] buf, short off);
    boolean isPinValidated();
}

// Trong StudentCard
public class StudentCard extends Applet implements StudentShare {
    public Shareable getShareableInterfaceObject(AID clientAid, byte parameter) {
        // chỉ phục vụ applet thư viện, tham số 0x01 là "mã bí mật" chung
        if (clientAid.partialEquals(LIBRARY_AID, (short) 0, (byte) LIBRARY_AID.length) && parameter == (byte) 0x01) {
            return this;
        }
        return null;
    }
    public short getCardId(byte[] buf, short off) { ... Util.arrayCopyNonAtomic(cardId, 0, buf, off, cardIdLen) ... }
    public boolean isPinValidated() { return pin.isValidated(); }
}
```

Lưu ý khi làm thật: khi applet Thư viện được chọn thì `StudentCard` đã bị bỏ chọn → `deselect()` đã chạy `pin.reset()` → `isPinValidated()` luôn `false`. Muốn dùng phải bỏ `pin.reset()` khỏi `deselect()` (để cờ sống đến khi reset thẻ) – đổi lại kém an toàn hơn. Đây chính là lý do chọn 1 applet.

## 4. Việc cần làm

- [ ] Làm lại ví dụ `masterApp`/`slaveApp` của TH5 để hiểu (không đưa vào đề tài).
- [ ] Ghi vào báo cáo Chương 1, mục "Thiết kế applet": lý do chọn 1 applet (bảng mục 2).

## 5. Câu hỏi có thể bị hỏi

1. *Tường lửa applet là gì? Vì sao cần?* – mục 1.
2. *Muốn applet khác dùng dữ liệu của mình thì làm thế nào?* – Shareable, 3 bước của TH5.
3. *Sao nhóm không tách nhiều applet?* – mục 2.
4. *SIO là gì?* – đối tượng của lớp cài đặt giao diện có thể chia sẻ; với ngữ cảnh khác chỉ thấy các phương thức của giao diện.

## Nhật ký

| Ngày | Ai | Làm được | Vướng |
|---|---|---|---|
| | | | |
