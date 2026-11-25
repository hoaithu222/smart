# Bài thực hành 5 – Chia sẻ đối tượng giữa các applet: câu hỏi và trả lời

> Nguồn chính: [th5-chia-se-doi-tuong-giua-cac-applet.md](../smart/docs/th5-chia-se-doi-tuong-giua-cac-applet.md).
> Chỗ ghi **[Thêm]** là kiến thức ngoài tài liệu của cô (đặc tả Java Card, danh sách nguồn ở cuối).
> Mã ví dụ chạy được: [src/masterPack](src/masterPack/), [src/slavePack](src/slavePack/).
> Do một agent phân tích riêng (25/11/2026). Phần tường lửa dựa trên đặc tả, **chưa chạy thử**: thẻ giả lập không có tường lửa.

---

## Câu 1. Bối cảnh là gì?

**Trả lời ngắn:** bối cảnh (context) là **vùng đối tượng được tường lửa bảo vệ**. Java Card cấp một bối cảnh cho mỗi **package** có applet; mọi applet trong package dùng chung, gọi là **bối cảnh nhóm**. JCRE có bối cảnh riêng, nhiều quyền hơn.

**[Thêm] Giải thích**

- Mỗi đối tượng có **chủ sở hữu** (một thể hiện applet, xác định bằng AID, hoặc JCRE) và thuộc bối cảnh đang hoạt động lúc nó được tạo.
- Mỗi lúc chỉ có **một bối cảnh đang hoạt động**. Khi truy cập đối tượng (đọc/ghi trường, gọi phương thức, truy cập mảng, ép kiểu, `instanceof`), máy ảo so bối cảnh của đối tượng với bối cảnh này; không được phép thì ném `SecurityException`.
- Bối cảnh chỉ chuyển khi gọi và trả về từ một số phương thức: đối tượng chia sẻ, đối tượng điểm nhập, JCRE gọi applet.
- Trường `static` không thuộc bối cảnh nào. Package thư viện không có bối cảnh riêng: đối tượng nó tạo thuộc applet đang gọi.

**Hỏi thêm:** *Bối cảnh đang hoạt động có phải là applet đang được chọn?* Không. Khi applet khách gọi đối tượng chia sẻ, applet **được chọn** vẫn là khách nhưng **bối cảnh hoạt động** là của applet chủ.

---

## Câu 2. "Các applet trong cùng 1 package chung một bối cảnh nhóm, giữa chúng không có tường lửa"

**Đúng.** Viết cho đúng chữ: *applet*, ***package*** (không phải packet), *giữa chúng*.

**Trả lời ngắn:** cùng package thì cùng bối cảnh, applet này dùng được đối tượng của applet kia mà không cần `Shareable`.

**[Thêm]**

- Vẫn phải **có tham chiếu** tới đối tượng (ví dụ qua trường `static`), và `private` của Java vẫn có hiệu lực.
- Từ Java Card 3.x, bối cảnh nhóm tính theo **file CAP** (có thể gồm nhiều package).

**Hỏi thêm:** *Mỗi applet một bối cảnh?* Sai: tính theo package.

---

## Câu 3. "Tường lửa ngăn cách giữa các đối tượng nhóm khác nhau"

**Đúng ý.** Nên nói: tường lửa ngăn cách **các đối tượng thuộc những bối cảnh khác nhau**.

**Trả lời ngắn:** tường lửa là cơ chế máy ảo kiểm tra **lúc chạy**; truy cập trái phép sang bối cảnh khác bị `SecurityException`.

- Ví dụ mục 5.5 của đề: applet khách truyền mảng riêng vào `getArray`, applet chủ ghi vào mảng đó thì kết quả là `6F 00`.
- **[Thêm]** Có tham chiếu chưa chắc đã dùng được. Trình biên dịch **không** báo lỗi, chỉ phát hiện khi chạy.
- **[Thêm]** Tường lửa không đối xứng: JCRE vào được mọi bối cảnh, applet chỉ dùng được JCRE qua các cửa được phép.

---

## Câu 4. "Khác bối cảnh nhóm thì chia sẻ phải thông qua giao diện có thể chia sẻ"

(Gồm cả ý "khi không cùng bối cảnh nhóm phải thông qua giao diện chia sẻ" – hai câu trùng nhau.)

**Đúng cho việc gọi phương thức của applet khác, nhưng chưa chính xác nếu hiểu là mọi thứ đi qua tường lửa đều cần `Shareable`.**

**Trả lời ngắn:** muốn applet khác bối cảnh gọi vào đối tượng của mình thì phải qua **SIO** (đối tượng giao diện chia sẻ), và chỉ chia sẻ được **phương thức**. Ngoài ra còn vài thứ đi qua được mà không cần `Shareable`.

**[Thêm] Đi qua được mà không cần `Shareable`:**

| Thứ | Được gì | Giới hạn |
|---|---|---|
| Mảng toàn cục (bộ đệm APDU, `bArray` của `install`) | Mọi bối cảnh đọc, ghi | Không được lưu tham chiếu lại; applet không tự tạo được mảng loại này |
| Đối tượng điểm nhập JCRE (APDU, AID, ngoại lệ hệ thống) | Gọi phương thức | Trường vẫn bị chặn; loại tạm thời (APDU, ngoại lệ) không được lưu |
| Thành phần `static` | Tường lửa không kiểm tra | Đối tượng mà trường static trỏ tới **vẫn bị chặn** |
| JCRE | Truy cập mọi đối tượng | Chỉ JCRE có quyền này |

> **[Thêm] Điểm đề chưa nói:** package **applet** chỉ xuất ra ngoài các **giao diện Shareable**. Package khác **không liên kết được** tới trường `static public` của nó (đã thử: bộ chuyển đổi báo `field ... not found in export file`). Muốn dùng chung `static` giữa các package thì đặt trong **package thư viện**.

**Lưu ý về mục 5.5 của đề:** lớp `sinhVien` nằm trong package **khách**; thứ đi qua tường lửa chỉ là **giá trị `byte`**. Bản "không static" ra `6F 00` vì đối tượng chưa được `new` (`NullPointerException`), không phải do tường lửa.

### Tham số và kiểu trả về

| Truyền / trả về | Dùng được không |
|---|---|
| `boolean`, `byte`, `short` (`int` tùy thẻ) | Được |
| Bộ đệm APDU | Được, bên nhận không được lưu |
| Đối tượng điểm nhập JCRE | Được gọi phương thức |
| Tham chiếu SIO (kiểu giao diện Shareable) | Được; gọi ngược lại sẽ chuyển bối cảnh |
| Mảng/đối tượng **riêng** của khách | Truyền được tham chiếu, chủ **đụng vào** thì `SecurityException` |
| Mảng/đối tượng riêng của chủ trả về | Khách đụng vào thì `SecurityException` |

---

## Câu 5. "Bình thường có 5 cái nhưng đặt 3 cái vào cái có thể chia sẻ được"

**Đúng**, nếu "cái" là **phương thức**.

**Trả lời ngắn:** lớp có 5 phương thức, giao diện `Shareable` khai báo 3, thì bối cảnh khác chỉ gọi được 3; applet chủ vẫn dùng đủ 5.

```java
public interface IShare extends Shareable {
    short m1();
    short m2();
    short m3();
}
// lop cai dat con co m4(), m5(): ben ngoai khong goi duoc
```

Trường của đối tượng không bao giờ lộ ra; 2 phương thức còn lại dù `public` vẫn bị chặn (mục 5.2 của đề).

**Hỏi thêm**

- *Khách ép SIO về lớp của chủ để gọi `m4` được không?* **[Thêm]** Không: ép về lớp của đối tượng khác bối cảnh bị `SecurityException`.
- *Muốn chia sẻ một biến?* Viết phương thức lấy giá trị (getter) trong giao diện.

---

## Câu 6. "5 phương thức: 2 cho a, 3 cho b, 1 cho c; mỗi phương thức đặt vào interface khác nhau"

**Chưa chính xác** ở ba điểm.

**Trả lời ngắn:** 2 + 3 + 1 = 6 nên ít nhất một phương thức phải dùng chung. Chỉ chia interface thì **chưa chặn được**, vì khách ép kiểu sang interface khác của cùng đối tượng. Phải trả cho mỗi khách **một đối tượng riêng** và **kiểm tra AID** người gọi.

1. **Số học:** tổng là 6 mà chỉ có 5 phương thức, nên có chồng lấn (ví dụ phương thức của c cũng thuộc b). Ba nhóm rời nhau thì cần 6 phương thức.
2. **Không cần mỗi phương thức một interface:** thường mỗi **loại khách** một interface; một phương thức được khai báo ở nhiều interface.
3. **[Thêm] Một đối tượng cài nhiều interface không phân quyền được.** Tường lửa cho ép kiểu và gọi khi lớp của đối tượng cài giao diện đó và giao diện kế thừa `Shareable`; nó **không nhớ** khách được cấp giao diện nào. (Đề mục 5.3 nói một SIO giữ được nhiều vai trò: tiện, nhưng không bảo mật.)

```java
// Khach c nhan MOT doi tuong cai ca IForA, IForB, IForC
Shareable s = JCSystem.getAppletShareableInterfaceObject(srv, (byte) 0);
IForB b = (IForB) s;      // tuong lua cho ep kieu
b.debit((short) 1);       // va cho goi: c dung duoc quyen cua b
```

### Thiết kế đúng

> Đã biên dịch và chuyển đổi bằng bộ của Oracle (đích 3.0.4), 0 lỗi. **Chưa chạy trên thẻ.**

```java
public interface IForA extends Shareable {
    short getId(byte[] buf, short off);
    boolean isActive();
}
public interface IForB extends Shareable {
    short getBalance();
    boolean debit(short amount);
    short getLog(byte[] buf, short off);
}
public interface IForC extends Shareable {
    short getBalance();                    // phuong thuc dung chung voi B
}

// Kiem tra nguoi goi
class Guard {
    static void require(byte[] expected) {
        AID c = JCSystem.getPreviousContextAID();
        if (c == null || !c.equals(expected, (short) 0, (byte) expected.length)) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
    }
}

// Moi khach mot lop rieng; SvcA, SvcB viet giong SvcC
class SvcC implements IForC {
    private final ServerApp app;
    SvcC(ServerApp a) { app = a; }
    public short getBalance() {
        Guard.require(ServerApp.AID_C);
        return app.balance();
    }
}

// Trong ServerApp (svcA, svcB, svcC tao trong ham tao)
public Shareable getShareableInterfaceObject(AID client, byte p) {
    if (client == null || p != (byte) 0) {
        return null;
    }
    if (client.equals(AID_A, (short) 0, (byte) AID_A.length)) {
        return svcA;
    }
    if (client.equals(AID_B, (short) 0, (byte) AID_B.length)) {
        return svcB;
    }
    if (client.equals(AID_C, (short) 0, (byte) AID_C.length)) {
        return svcC;
    }
    return null;
}
```

- **Đối tượng riêng:** `SvcC` không cài `IForB` nên ép kiểu sẽ lỗi. Không cho giao diện này `extends` giao diện kia.
- **Kiểm tra trong phương thức:** a có thể chuyển tham chiếu `svcA` cho applet khác; tường lửa vẫn cho bên đó gọi, chỉ kiểm AID mới chặn.

**Hỏi thêm [Thêm]**

- *`getPreviousContextAID` trả gì?* AID của applet hoạt động ở lần chuyển bối cảnh gần nhất. Trong `getShareableInterfaceObject` thì dùng tham số `clientAID`.
- *Dùng `equals` hay `partialEquals`?* `equals` so cả AID; `partialEquals` chỉ so **phần đầu**, AID dài hơn cùng tiền tố vẫn lọt.
- *Kiểm AID đã đủ an toàn chưa?* Chưa: nếu thẻ không kiểm soát việc nạp applet, applet giả mang đúng AID vẫn cài được. Cần quản lý nạp applet và xác thực thử thách – trả lời.
- *Dùng `parameter` làm mật khẩu như đề gợi ý?* Yếu: chỉ 256 giá trị và do khách tự đưa vào.

---

## Câu 7. Có phải cứ chia sẻ là nhận được không?

**Không.**

**Trả lời ngắn:** phải cài đúng thứ tự, đúng AID, applet chủ đồng ý trả đối tượng, khách ép đúng interface, và khi gọi vẫn phải tuân tường lửa.

| Điều kiện | Nếu sai |
|---|---|
| 1. Giao diện `public`, kế thừa `Shareable`; package khách có file export của package chủ **[Thêm]** | Không chuyển đổi (build) được |
| 2. Package chủ đã có trên thẻ trước package khách **[Thêm]** | Cài khách thất bại |
| 3. Applet chủ đã được cài và đăng ký; khách biết đúng AID | `lookupAID` trả `null` |
| 4. Chủ không phải applet không đa chọn đang hoạt động trên kênh logic khác **[Thêm]** | `SecurityException` |
| 5. Chủ ghi đè `getShareableInterfaceObject` (mặc định trả `null`) và chấp nhận AID, tham số | Nhận `null` |
| 6. Đối tượng trả về cài đúng giao diện khách ép (giao diện của package chủ, bản chép cùng tên là kiểu khác) | `ClassCastException` |
| 7. Khách kiểm tra `null` trước khi gọi | `NullPointerException` → `6F 00` |
| 8. Tham số truyền sang là thứ chủ truy cập được | `SecurityException` |
| 9. Phương thức của chủ không đụng mảng `CLEAR_ON_DESELECT` của mình khi khách thuộc bối cảnh khác **[Thêm]** | `SecurityException` |
| 10. Qua được kiểm tra quyền trong phương thức | Lỗi do chủ ném |

**Hỏi thêm [Thêm]**

- *Applet chủ có cần đang được chọn?* Không.
- *Nhận `null` thì biết vì sao không?* Không phân biệt được.
- *Khách lưu SIO vào trường được không?* Được. Khi còn bị tham chiếu thì không xóa được applet chủ.
- *Vì sao điều kiện 9 hay gặp?* Khóa phiên thường là `CLEAR_ON_DESELECT`, nên không dùng được qua SIO từ package khác.

---

## Tóm tắt ôn trước vấn đáp

| Câu | Ý chính |
|---|---|
| 1 | Bối cảnh: vùng đối tượng được tường lửa bảo vệ; mỗi package một bối cảnh nhóm |
| 2 | Cùng package: không có tường lửa |
| 3 | Khác bối cảnh: kiểm tra lúc chạy, sai thì `SecurityException` |
| 4 | Gọi phương thức qua SIO; ngoại lệ: mảng toàn cục, điểm nhập JCRE, static, JCRE |
| 5 | Chỉ phương thức khai báo trong giao diện Shareable lộ ra |
| 6 | 2+3+1 = 6 > 5; mỗi khách một đối tượng + kiểm AID |
| 7 | Không tự nhận được: thứ tự cài, AID, chủ đồng ý, đúng kiểu, tường lửa |

## Tự kiểm tra

Mở PowerShell trong thư mục `thuc-hanh`:

```powershell
.\chay.cmd kich-ban\th5.apdu
```

| Applet khách | Kết quả |
|---|---|
| `slaveApp` | `01 02 03 04 05` |
| `slaveAppDiem`, `slaveApp1` | `08` |
| `slaveAppNonStatic` | `6F 00` (đối tượng chưa `new`) |
| `slaveAppMang` | `6F 00` **chỉ trên JCIDE / thẻ thật** |
| Chưa cài applet chủ | `6A 82` |

## Nguồn cho phần [Thêm]

- [JCRE 2.2.2, chương 6 – tường lửa và chia sẻ đối tượng](https://anon.inf.tu-dresden.de/svn/JavaCardStudents/ANONCard/trunk/java_card_kit-2_2_2/jc_specification/specs/jcre/html/JCRESpec06firewall.html) (bản sao trên máy chủ đại học)
- [JCRE 3.1 (Oracle)](https://docs.oracle.com/javacard/3.1/related-docs/JCCRE/JCCRE.pdf)
- [JCVM 2.2.2, chương 5 – file export](https://anon.inf.tu-dresden.de/svn/JavaCardStudents/ANONCard/trunk/java_card_kit-2_2_2/jc_specification/specs/jcvm/html/JCVM05export.html)
- API: [JCSystem](https://pinpasjc.win.tue.nl/docs/apis/jc222/javacard/framework/JCSystem.html), [AID](https://pinpasjc.win.tue.nl/docs/apis/jc222/javacard/framework/AID.html)
- [Montgomery & Krishna, "Secure Object Sharing in Java Card", USENIX 1999](https://www.usenix.org/events/smartcard99/full_papers/montgomery/montgomery_html/)
