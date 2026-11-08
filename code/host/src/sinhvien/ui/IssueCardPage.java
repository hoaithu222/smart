package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import sinhvien.host.CardClient;
import sinhvien.host.CardCrypto;
import sinhvien.host.ProfileTlv;

/**
 * Phát hành (khởi tạo) thẻ: thẻ trắng -> INIT_CARD (sinh RSA trên thẻ) -> lưu CSDL -> ghi thông tin AES + ảnh.
 * Tiêu chí chấm: "Khởi tạo thẻ", "Tạo được cặp khóa RSA", "Mã hóa được thông tin".
 */
public class IssueCardPage extends Page {

    private static final SecureRandom RND = new SecureRandom();

    private final JTextField maSv = Ui.field("CT070254", 12);
    private final JTextField hoTen = Ui.field("Vũ Thị Hoài Thu", 18);
    private final JTextField ngaySinh = Ui.field("dd/MM/yyyy", 10);
    private final JComboBox<String> gioiTinh = new JComboBox<String>(new String[] {"Nữ", "Nam"});
    private final JTextField lop = Ui.field("CT7C", 10);
    private final JTextField khoaNganh = Ui.field("Công nghệ thông tin", 18);
    private final JTextField khoaHoc = Ui.field("CT7 (2022–2027)", 12);
    private final JTextField hanThe = Ui.field("30/06/2027", 10);
    private final JTextField queQuan = Ui.field("Tỉnh / thành phố", 18);
    private final JTextField sdt = Ui.field("09xxxxxxxx", 12);
    private final JTextField email = Ui.field("ten@actvn.edu.vn", 18);
    private final JPasswordField pin = Ui.password("6 chữ số", 10);
    private final JTextField puk = Ui.field("8 chữ số", 10);
    private final JPasswordField adminPin = Ui.password("PIN quản trị thẻ", 10);
    private final Ui.PhotoBox photo = new Ui.PhotoBox(150, 200);
    private final JLabel photoInfo = Ui.muted("Chưa có ảnh");
    private final JLabel[] steps = new JLabel[7];
    private final JButton issue = Ui.button("Phát hành thẻ", Ui.Kind.PRIMARY, Icons.of(Icons.Kind.CARD, 18, Color.WHITE));
    private byte[] photoJpeg;

    public IssueCardPage(Ctx ctx) {
        super(ctx);
        Ui.Form f = new Ui.Form(3);
        f.add("Mã sinh viên *", maSv).add("Họ và tên *", hoTen, 2)
                .add("Ngày sinh", ngaySinh).add("Giới tính", gioiTinh).add("Lớp", lop)
                .add("Khoa / ngành", khoaNganh).add("Khóa học", khoaHoc).add("Hạn thẻ", hanThe)
                .add("Quê quán", queQuan, 2).add("Số điện thoại", sdt)
                .add("Email", email, 2);
        JButton sample = Ui.button("Điền dữ liệu mẫu", Ui.Kind.GHOST, Icons.of(Icons.Kind.PLUS, 16));
        sample.addActionListener(e -> fillSample());
        JButton find = Ui.button("Lấy từ CSDL (phát hành lại)", Ui.Kind.GHOST, Icons.of(Icons.Kind.SEARCH, 16));
        find.addActionListener(e -> loadFromDb());
        JPanel formWrap = Ui.transparent(new BorderLayout(0, 6));
        formWrap.add(f, BorderLayout.CENTER);
        formWrap.add(Ui.row(4, sample, find), BorderLayout.SOUTH);
        Ui.RoundPanel infoCard = Ui.card("Thông tin sinh viên",
                "Ghi lên thẻ dưới dạng TLV, thẻ mã hóa AES-128 trước khi lưu", formWrap);

        // ảnh
        JButton choose = Ui.button("Chọn ảnh…", Ui.Kind.DEFAULT, Icons.of(Icons.Kind.PHOTO, 16));
        choose.addActionListener(e -> choosePhoto());
        JButton gen = Ui.button("Tạo ảnh mẫu", Ui.Kind.GHOST);
        gen.addActionListener(e -> setPhoto(ImageUtil.sampleAvatar(hoTen.getText(), 240, 320)));
        JPanel ph = Ui.transparent(new BorderLayout(0, 10));
        JPanel pc = Ui.transparent(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER));
        pc.add(photo);
        ph.add(pc, BorderLayout.NORTH);
        ph.add(photoInfo, BorderLayout.CENTER);
        ph.add(Ui.row(6, choose, gen), BorderLayout.SOUTH);
        Ui.RoundPanel photoCard = Ui.card("Ảnh chân dung", "JPEG " + ctx.cfg.photoWidth() + "×" + ctx.cfg.photoHeight()
                + ", ≤ " + ctx.cfg.photoMaxBytes() + " byte", ph);

        // bảo mật
        JButton genPuk = Ui.button("Sinh PUK", Ui.Kind.GHOST, Icons.of(Icons.Kind.REFRESH, 16));
        genPuk.addActionListener(e -> puk.setText(randomDigits(ctx.cfg.pukLength())));
        Ui.Form sf = new Ui.Form(1);
        sf.add("PIN ban đầu của sinh viên *", pin);
        JPanel pukRow = Ui.transparent(new BorderLayout(6, 0));
        pukRow.add(puk, BorderLayout.CENTER);
        pukRow.add(genPuk, BorderLayout.EAST);
        sf.add("Mã PUK (in cho sinh viên giữ) *", pukRow);
        sf.add("PIN quản trị thẻ *", adminPin);
        Ui.RoundPanel secCard = Ui.card("Bảo mật", "PIN/PUK lưu trên thẻ dạng băm SHA-256", sf);

        JPanel right = Ui.transparent(new BorderLayout(0, 16));
        right.add(photoCard, BorderLayout.NORTH);
        right.add(secCard, BorderLayout.CENTER);

        // tiến trình
        String[] names = {"Kiểm tra thẻ trắng", "Xác thực PIN quản trị", "Khởi tạo thẻ + sinh cặp khóa RSA trên thẻ",
            "Lưu hồ sơ + khóa công khai vào CSDL", "Mở phiên bằng PIN ban đầu", "Ghi thông tin (thẻ mã hóa AES)",
            "Xác thực lại thẻ bằng RSA"};
        JPanel stepPanel = Ui.transparent(new GridLayout(0, 1, 0, 4));
        for (int i = 0; i < names.length; i++) {
            steps[i] = new JLabel((i + 1) + ". " + names[i]);
            steps[i].setIcon(Icons.of(Icons.Kind.CHECK, 16, Theme.border()));
            stepPanel.add(steps[i]);
        }
        issue.setFont(Theme.font(Font.BOLD, 15));
        issue.setPreferredSize(new Dimension(220, 42));
        issue.addActionListener(e -> doIssue());
        JPanel bottom = Ui.transparent(new BorderLayout(16, 0));
        bottom.add(stepPanel, BorderLayout.CENTER);
        bottom.add(Ui.rowRight(0, issue), BorderLayout.EAST);
        Ui.RoundPanel progress = Ui.card("Tiến trình phát hành", null, bottom);

        JPanel leftCol = Ui.transparent(new BorderLayout(0, 16));
        leftCol.add(infoCard, BorderLayout.NORTH);
        leftCol.add(progress, BorderLayout.CENTER);
        JPanel main = Ui.transparent(new BorderLayout(16, 16));
        main.add(leftCol, BorderLayout.CENTER);
        main.add(Ui.fixedWidth(right, 330), BorderLayout.EAST);
        add(main, BorderLayout.CENTER);
    }

    public String title() {
        return "Phát hành thẻ";
    }

    public String subtitle() {
        return "Khởi tạo thẻ trắng: mã SV, PIN, PUK, cặp khóa RSA, thông tin mã hóa AES";
    }

    public Icons.Kind icon() {
        return Icons.Kind.CARD;
    }

    // ------------------------------------------------------------ dữ liệu

    private void fillSample() {
        String[][] s = {
            {"CT070254", "Vũ Thị Hoài Thu", "09/10/2002", "Nữ", "CT7C", "Công nghệ thông tin", "CT7 (2022–2027)", "30/06/2027", "Tân Triều, Thanh Trì, Hà Nội", "0382329310", "thu601925@gmail.com"},
            {"CT070333", "Phạm Như Ngọc", "15/03/2004", "Nữ", "CT7D", "Công nghệ thông tin", "CT7 (2022–2027)", "30/06/2027", "Hà Nội", "0900000001", "ngoc@actvn.edu.vn"},
            {"CT070351", "Nguyễn Thị Phương Thảo", "21/08/2004", "Nữ", "CT7D", "Công nghệ thông tin", "CT7 (2022–2027)", "30/06/2027", "Nam Định", "0900000002", "thao@actvn.edu.vn"}};
        String[] r = s[RND.nextInt(s.length)];
        maSv.setText(r[0]);
        hoTen.setText(r[1]);
        ngaySinh.setText(r[2]);
        gioiTinh.setSelectedItem(r[3]);
        lop.setText(r[4]);
        khoaNganh.setText(r[5]);
        khoaHoc.setText(r[6]);
        hanThe.setText(r[7]);
        queQuan.setText(r[8]);
        sdt.setText(r[9]);
        email.setText(r[10]);
        pin.setText("123456");
        puk.setText(randomDigits(ctx.cfg.pukLength()));
        setPhoto(ImageUtil.sampleAvatar(r[1], 240, 320));
    }

    private void loadFromDb() {
        final String id = maSv.getText().trim();
        if (id.isEmpty()) {
            Dialogs.error(this, "Nhập mã sinh viên trước");
            return;
        }
        Task.run(this, "Đang tìm trong CSDL…", () -> new Object[] {ctx.repo.findStudent(id), ctx.repo.findPhoto(id)}, r -> {
            @SuppressWarnings("unchecked")
            Map<Integer, String> m = (Map<Integer, String>) r[0];
            if (m == null) {
                Dialogs.error(this, "Không có sinh viên " + id + " trong CSDL");
                return;
            }
            hoTen.setText(m.get(ProfileTlv.HO_TEN));
            ngaySinh.setText(m.get(ProfileTlv.NGAY_SINH));
            gioiTinh.setSelectedItem(m.get(ProfileTlv.GIOI_TINH));
            lop.setText(m.get(ProfileTlv.LOP));
            khoaNganh.setText(m.get(ProfileTlv.KHOA_NGANH));
            khoaHoc.setText(m.get(ProfileTlv.KHOA_HOC));
            hanThe.setText(m.get(ProfileTlv.HAN_THE));
            queQuan.setText(m.get(ProfileTlv.QUE_QUAN));
            sdt.setText(m.get(ProfileTlv.SDT));
            email.setText(m.get(ProfileTlv.EMAIL));
            byte[] p = (byte[]) r[1];
            if (p != null) {
                photoJpeg = p;
                photo.setImage(ImageUtil.read(p));
                photoInfo.setText("Ảnh từ CSDL · " + p.length + " byte");
            }
        });
    }

    private void choosePhoto() {
        JFileChooser fc = new JFileChooser();
        fc.setFileFilter(new FileNameExtensionFilter("Ảnh (jpg, png)", "jpg", "jpeg", "png"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File file = fc.getSelectedFile();
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new Task.UserError("Không đọc được ảnh " + file.getName());
            }
            setPhoto(img);
        } catch (Exception e) {
            Dialogs.error(this, Task.message(e));
        }
    }

    private void setPhoto(BufferedImage img) {
        try {
            photoJpeg = ImageUtil.toCardJpeg(img, ctx.cfg);
            photo.setImage(ImageUtil.read(photoJpeg));
            int chunks = (CardCrypto.pad(photoJpeg).length + CardClient.CHUNK - 1) / CardClient.CHUNK;
            photoInfo.setText("JPEG " + photoJpeg.length + " byte · ghi " + chunks + " khối APDU");
        } catch (Exception e) {
            Dialogs.error(this, Task.message(e));
        }
    }

    static String randomDigits(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(RND.nextInt(10));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------ phát hành

    Map<Integer, String> infoFields() {
        Map<Integer, String> m = new LinkedHashMap<Integer, String>();
        m.put(ProfileTlv.MA_SV, maSv.getText().trim());
        m.put(ProfileTlv.HO_TEN, hoTen.getText().trim());
        put(m, ProfileTlv.NGAY_SINH, ngaySinh);
        m.put(ProfileTlv.GIOI_TINH, String.valueOf(gioiTinh.getSelectedItem()));
        put(m, ProfileTlv.LOP, lop);
        put(m, ProfileTlv.KHOA_NGANH, khoaNganh);
        put(m, ProfileTlv.KHOA_HOC, khoaHoc);
        put(m, ProfileTlv.HAN_THE, hanThe);
        return m;
    }

    Map<Integer, String> contactFields() {
        Map<Integer, String> m = new LinkedHashMap<Integer, String>();
        put(m, ProfileTlv.QUE_QUAN, queQuan);
        put(m, ProfileTlv.SDT, sdt);
        put(m, ProfileTlv.EMAIL, email);
        return m;
    }

    private static void put(Map<Integer, String> m, int tag, JTextField f) {
        String v = f.getText().trim();
        if (!v.isEmpty()) {
            m.put(tag, v);
        }
    }

    private void doIssue() {
        final String id = maSv.getText().trim();
        final String pinS = new String(pin.getPassword());
        final String pukS = puk.getText().trim();
        final String adminS = new String(adminPin.getPassword());
        final Map<Integer, String> info;
        final Map<Integer, String> contact;
        try {
            Validators.cardId(id);
            Validators.required(hoTen.getText(), "Họ và tên");
            Validators.date(ngaySinh.getText(), "Ngày sinh");
            Validators.date(hanThe.getText(), "Hạn thẻ");
            Validators.digits(pinS, ctx.cfg.pinLength(), "PIN ban đầu");
            Validators.digits(pukS, ctx.cfg.pukLength(), "PUK");
            Validators.secret(adminS, "PIN quản trị thẻ");
            info = infoFields();
            contact = contactFields();
            Validators.fits(ProfileTlv.encode(info), 480, "Thông tin học vụ");
            Validators.fits(ProfileTlv.encode(contact), 240, "Thông tin liên lạc");
            ctx.session.requireConnected();
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        for (JLabel s : steps) {
            s.setIcon(Icons.of(Icons.Kind.CHECK, 16, Theme.border()));
            s.setForeground(null);
        }
        issue.setEnabled(false);
        Task.run(this, "Đang phát hành thẻ…", () -> issueOnCard(id, pinS, pukS, adminS, info, contact), modulusHex ->
                Dialogs.info(this, "Phát hành thành công",
                    "Đã phát hành thẻ " + id + " cho " + info.get(ProfileTlv.HO_TEN) + ".\n\n"
                    + "PUK: " + pukS + "  (đưa sinh viên giữ – dùng khi quên PIN)\n"
                    + "Khóa công khai RSA (modulus): " + modulusHex.substring(0, Math.min(47, modulusHex.length())) + " …"),
                () -> issue.setEnabled(true));
    }

    /** Chuỗi lệnh phát hành - chạy ngoài luồng giao diện. Trả modulus (hex) để hiện. */
    String issueOnCard(String id, String pinS, String pukS, String adminS, Map<Integer, String> info,
            Map<Integer, String> contact) throws Exception {
        CardClient c = ctx.session.client;
        step(0);
        CardClient.CardStatus st = c.status();
        if (st.state != 0) {
            throw new Task.UserError("Thẻ này đã phát hành cho " + st.cardId
                    + ". Muốn phát hành lại: cài lại applet (Run lại JCIDE) để có thẻ trắng.");
        }
        step(1);
        c.verifyAdmin(adminS);
        step(2);
        c.initCard(id, pinS, pukS);
        byte[][] key = c.publicKeyParts();
        step(3);
        Map<Integer, String> all = new LinkedHashMap<Integer, String>(info);
        all.putAll(contact);
        ctx.repo.upsertStudent(all, photoJpeg);
        ctx.repo.saveIssuedCard(id, key[0], key[1]);
        step(4);
        c.verifyPin(pinS);
        step(5);
        c.writeObject(CardClient.OBJ_INFO, ProfileTlv.encode(info));
        c.writeObject(CardClient.OBJ_CONTACT, ProfileTlv.encode(contact));
        if (photoJpeg != null) {
            c.writeObject(CardClient.OBJ_PHOTO, photoJpeg);
        }
        step(6);
        ctx.session.refresh();
        if (!ctx.session.isGenuine()) {
            throw new Task.UserError("Phát hành xong nhưng xác thực RSA không đạt – kiểm tra CSDL");
        }
        step(7);
        return CardCrypto.hex(key[0]);
    }

    private void step(final int done) {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < steps.length; i++) {
                if (i < done) {
                    steps[i].setIcon(Icons.of(Icons.Kind.CHECK, 16, Theme.SUCCESS));
                    steps[i].setForeground(null);
                } else if (i == done) {
                    steps[i].setIcon(Icons.of(Icons.Kind.REFRESH, 16, Theme.ACCENT));
                    steps[i].setForeground(Theme.ACCENT);
                }
            }
        });
    }

    // dùng cho kiểm thử tự động
    void fillForTest(String id, String name) {
        fillSample();
        maSv.setText(id);
        hoTen.setText(name);
        adminPin.setText(ctx.cfg.simAdminPin());
        pin.setText("123456");
        puk.setText("12345678");
        setPhoto(ImageUtil.sampleAvatar(name, 240, 320));
        photo.setBorder(BorderFactory.createEmptyBorder());
    }

    void issueForTest() throws Exception {
        issueOnCard(maSv.getText().trim(), "123456", "12345678", ctx.cfg.simAdminPin(), infoFields(), contactFields());
    }
}
