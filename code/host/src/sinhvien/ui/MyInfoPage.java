package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import sinhvien.host.CardClient;
import sinhvien.host.ProfileTlv;

/**
 * Thông tin trên thẻ: thẻ chỉ giải mã sau khi nhập PIN đúng.
 * Sinh viên sửa được thông tin liên lạc; quản trị sửa được cả thông tin học vụ + ảnh (cần PIN quản trị).
 * Tiêu chí chấm: "Thay đổi thông tin cá nhân", "Mã hóa được thông tin".
 */
public class MyInfoPage extends Page {

    private final Ui.PhotoBox photo = new Ui.PhotoBox(168, 224);
    private final JLabel name = Ui.bold("—", 18);
    private final Ui.Chip idChip = new Ui.Chip("Chưa đọc thẻ");
    private final JLabel hint = Ui.muted("<html>Dữ liệu trên thẻ đã mã hóa AES-128.<br>Nhập PIN để thẻ giải mã.</html>");
    private final JTextField[] info = new JTextField[8];
    private final int[] infoTags = {ProfileTlv.MA_SV, ProfileTlv.HO_TEN, ProfileTlv.NGAY_SINH, ProfileTlv.GIOI_TINH,
        ProfileTlv.LOP, ProfileTlv.KHOA_NGANH, ProfileTlv.KHOA_HOC, ProfileTlv.HAN_THE};
    private final String[] infoNames = {"Mã sinh viên", "Họ và tên", "Ngày sinh", "Giới tính", "Lớp", "Khoa / ngành",
        "Khóa học", "Hạn thẻ"};
    private final JTextField queQuan = Ui.field("", 20);
    private final JTextField sdt = Ui.field("", 12);
    private final JTextField email = Ui.field("", 20);
    private final JButton saveInfo = Ui.button("Lưu thông tin học vụ (PIN quản trị)", Ui.Kind.DEFAULT, Icons.of(Icons.Kind.KEY, 16));
    private final JButton changePhoto = Ui.button("Đổi ảnh…", Ui.Kind.GHOST, Icons.of(Icons.Kind.PHOTO, 16));
    private byte[] newPhoto;
    private boolean loaded;

    public MyInfoPage(Ctx ctx) {
        super(ctx);
        // cột trái: ảnh
        JPanel pc = Ui.transparent(new FlowLayout(FlowLayout.CENTER, 0, 0));
        pc.add(photo);
        name.setHorizontalAlignment(JLabel.CENTER);
        JPanel chipRow = Ui.transparent(new FlowLayout(FlowLayout.CENTER, 0, 0));
        chipRow.add(idChip);
        hint.setHorizontalAlignment(JLabel.CENTER);
        JButton read = Ui.button("Đọc thẻ (nhập PIN)", Ui.Kind.PRIMARY, Icons.of(Icons.Kind.KEY, 16, Color.WHITE));
        read.addActionListener(e -> withPin(this::load));
        changePhoto.addActionListener(e -> choosePhoto());
        JPanel left = Ui.vstack(12, pc, name, chipRow, hint, read, changePhoto);
        Ui.RoundPanel photoCard = Ui.card(null, null, left);

        // học vụ: 3 cột - Mã SV | Giới tính | Ngày sinh / Họ tên (2) | Lớp / Khoa ngành (2) | Khóa học / Hạn thẻ
        Ui.Form f = new Ui.Form(3);
        for (int i = 0; i < info.length; i++) {
            info[i] = Ui.field("", 12);
            info[i].setEditable(false);
        }
        int[] order = {0, 3, 2, 1, 4, 5, 6, 7};
        for (int i : order) {
            f.add(infoNames[i], info[i], i == 1 || i == 5 ? 2 : 1);
        }
        saveInfo.addActionListener(e -> saveInfo());
        JPanel ib = Ui.vstack(2, f, Ui.row(0, saveInfo));
        Ui.RoundPanel infoCard = Ui.card("Thông tin học vụ (INFO)",
                "Chỉ phòng Đào tạo sửa được – thẻ đòi PIN quản trị khi ghi đối tượng này", ib);

        // liên lạc
        Ui.Form cf = new Ui.Form(2);
        cf.add("Quê quán", queQuan, 2).add("Số điện thoại", sdt).add("Email", email);
        JButton saveContact = Ui.button("Lưu thay đổi", Ui.Kind.SUCCESS, Icons.of(Icons.Kind.CHECK, 16, Color.WHITE));
        saveContact.addActionListener(e -> saveContact());
        JPanel cb = Ui.vstack(2, cf, Ui.row(0, saveContact));
        Ui.RoundPanel contactCard = Ui.card("Thông tin liên lạc (CONTACT)",
                "Sinh viên tự sửa sau khi nhập PIN · ghi lại lên thẻ (AES) và CSDL", cb);

        JPanel center = Ui.transparent(new BorderLayout(0, 16));
        center.add(infoCard, BorderLayout.NORTH);
        center.add(contactCard, BorderLayout.CENTER);

        JPanel body = Ui.transparent(new BorderLayout(16, 0));
        body.add(Ui.fixedWidth(photoCard, 250), BorderLayout.WEST);
        body.add(center, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        applyRole();
    }

    private void applyRole() {
        boolean admin = ctx.isAdmin();
        saveInfo.setVisible(admin);
        changePhoto.setVisible(admin);
        for (int i = 1; i < info.length; i++) {
            info[i].setEditable(admin);
        }
    }

    public String title() {
        return "Thông tin thẻ";
    }

    public String subtitle() {
        return "Đọc và cập nhật thông tin đã mã hóa trên thẻ";
    }

    public Icons.Kind icon() {
        return Icons.Kind.USER;
    }

    public void onCardChanged() {
        if (!ctx.session.isConnected() || !ctx.session.isPinVerified()) {
            if (loaded) {
                clear();
            }
        }
    }

    private void clear() {
        loaded = false;
        photo.setImage(null);
        name.setText("—");
        idChip.set("Chưa đọc thẻ", Theme.muted());
        for (JTextField f : info) {
            f.setText("");
        }
        queQuan.setText("");
        sdt.setText("");
        email.setText("");
        newPhoto = null;
    }

    /** Đọc 3 đối tượng từ thẻ (thẻ tự giải mã). */
    void load() {
        Task.run(this, "Đang đọc thẻ…", () -> {
            CardClient c = ctx.session.client;
            CardClient.CardStatus st = c.status();
            return new Object[] {
                ProfileTlv.decode(c.readObject(CardClient.OBJ_INFO, st.infoLen)),
                ProfileTlv.decode(c.readObject(CardClient.OBJ_CONTACT, st.contactLen)),
                c.readObject(CardClient.OBJ_PHOTO, st.photoLen),
                st.infoLen + st.contactLen + st.photoLen};
        }, o -> {
            @SuppressWarnings("unchecked")
            Map<Integer, String> in = (Map<Integer, String>) o[0];
            @SuppressWarnings("unchecked")
            Map<Integer, String> ct = (Map<Integer, String>) o[1];
            for (int i = 0; i < info.length; i++) {
                info[i].setText(nz(in.get(infoTags[i])));
            }
            queQuan.setText(nz(ct.get(ProfileTlv.QUE_QUAN)));
            sdt.setText(nz(ct.get(ProfileTlv.SDT)));
            email.setText(nz(ct.get(ProfileTlv.EMAIL)));
            byte[] p = (byte[]) o[2];
            photo.setImage(p.length == 0 ? null : ImageUtil.read(p));
            name.setText(nz(in.get(ProfileTlv.HO_TEN)));
            idChip.set(nz(in.get(ProfileTlv.MA_SV)) + " · đã giải mã", Theme.SUCCESS);
            hint.setText("<html>Thẻ đã giải mã " + o[3] + " byte<br>bằng khóa AES mở từ PIN.</html>");
            loaded = true;
        });
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }

    private void saveContact() {
        if (!loaded) {
            Dialogs.error(this, "Bấm \"Đọc thẻ\" trước");
            return;
        }
        final Map<Integer, String> m = new LinkedHashMap<Integer, String>();
        putIf(m, ProfileTlv.QUE_QUAN, queQuan.getText());
        putIf(m, ProfileTlv.SDT, sdt.getText());
        putIf(m, ProfileTlv.EMAIL, email.getText());
        try {
            Validators.fits(ProfileTlv.encode(m), 240, "Thông tin liên lạc");
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        withPin(() -> Task.run(this, "Đang ghi lên thẻ…", () -> {
            ctx.session.client.writeObject(CardClient.OBJ_CONTACT, ProfileTlv.encode(m));
            ctx.repo.updateContact(ctx.session.cardId(), m.get(ProfileTlv.QUE_QUAN), m.get(ProfileTlv.SDT),
                    m.get(ProfileTlv.EMAIL));
            ctx.session.refreshQuietly();
            return null;
        }, v -> {
            Dialogs.info(this, "Đã lưu", "Đã ghi thông tin liên lạc lên thẻ (mã hóa AES) và cập nhật CSDL.");
            load();
        }));
    }

    private void saveInfo() {
        if (!loaded) {
            Dialogs.error(this, "Bấm \"Đọc thẻ\" trước");
            return;
        }
        final Map<Integer, String> m = new LinkedHashMap<Integer, String>();
        for (int i = 0; i < info.length; i++) {
            putIf(m, infoTags[i], info[i].getText());
        }
        try {
            Validators.required(m.get(ProfileTlv.HO_TEN), "Họ và tên");
            Validators.date(m.get(ProfileTlv.NGAY_SINH), "Ngày sinh");
            Validators.date(m.get(ProfileTlv.HAN_THE), "Hạn thẻ");
            Validators.fits(ProfileTlv.encode(m), 480, "Thông tin học vụ");
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        final String admin = askAdminPin();
        if (admin == null) {
            return;
        }
        final byte[] photoToWrite = newPhoto;
        withPin(() -> Task.run(this, "Đang ghi thông tin học vụ…", () -> {
            CardClient c = ctx.session.client;
            c.verifyAdmin(admin);
            c.writeObject(CardClient.OBJ_INFO, ProfileTlv.encode(m));
            if (photoToWrite != null) {
                c.writeObject(CardClient.OBJ_PHOTO, photoToWrite);
            }
            Map<Integer, String> all = new LinkedHashMap<Integer, String>(m);
            all.put(ProfileTlv.QUE_QUAN, queQuan.getText().trim());
            all.put(ProfileTlv.SDT, sdt.getText().trim());
            all.put(ProfileTlv.EMAIL, email.getText().trim());
            byte[] p = photoToWrite != null ? photoToWrite : ctx.repo.findPhoto(ctx.session.cardId());
            ctx.repo.upsertStudent(all, p);
            ctx.session.refreshQuietly();
            return null;
        }, v -> {
            newPhoto = null;
            Dialogs.info(this, "Đã lưu", "Đã ghi thông tin học vụ" + (photoToWrite != null ? " và ảnh" : "")
                    + " lên thẻ và CSDL.");
            load();
        }));
    }

    private void choosePhoto() {
        JFileChooser fc = new JFileChooser();
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            File file = fc.getSelectedFile();
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                throw new Task.UserError("Không đọc được ảnh");
            }
            newPhoto = ImageUtil.toCardJpeg(img, ctx.cfg);
            photo.setImage(ImageUtil.read(newPhoto));
            hint.setText("<html>Ảnh mới " + newPhoto.length + " byte –<br>bấm Lưu thông tin học vụ.</html>");
        } catch (Exception e) {
            Dialogs.error(this, Task.message(e));
        }
    }

    private static void putIf(Map<Integer, String> m, int tag, String v) {
        if (v != null && !v.trim().isEmpty()) {
            m.put(tag, v.trim());
        }
    }

    // kiểm thử tự động
    void loadForTest() {
        load();
    }

    String nameText() {
        return name.getText();
    }

}
