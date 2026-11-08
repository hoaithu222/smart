package sinhvien.ui;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import sinhvien.host.AppConfig;
import sinhvien.host.CardClient;
import sinhvien.host.CardRepository;
import sinhvien.host.DatabaseBootstrap;
import sinhvien.host.ProfileTlv;

/**
 * Kiểm thử giao diện: chạy đủ luồng nghiệp vụ trên thẻ giả lập jCardSim + SQLite qua đúng các màn hình,
 * rồi vẽ từng màn hình ra PNG (không mở cửa sổ). Dùng để soát giao diện và đưa ảnh vào báo cáo.
 * Chạy: xem code/README.md. Tham số: thư mục xuất ảnh.
 */
public class UiShots {

    public static void main(String[] args) throws Exception {
        File out = new File(args.length > 0 ? args[0] : "shots");
        out.mkdirs();
        File db = new File(out, "uishots.db");
        db.delete();
        File props = new File(out, "uishots.properties");
        Writer w = new OutputStreamWriter(new FileOutputStream(props), StandardCharsets.UTF_8);
        w.write("card.mode=jcardsim\ncard.sim.adminPin=87654321\nlog.apdu=false\nui.theme=light\n"
                + "db.url=jdbc:sqlite:" + db.getAbsolutePath().replace('\\', '/') + "\n");
        w.close();

        final AppConfig cfg = AppConfig.load(props.getPath());
        Theme.install(cfg);
        final CardRepository repo = DatabaseBootstrap.open(cfg);
        check("Đăng nhập admin bằng mật khẩu băm trong CSDL", repo.login("admin", "admin@123") != null);
        check("Sai mật khẩu bị từ chối", repo.login("admin", "sai") == null);
        final CardSession session = new CardSession(cfg, repo);
        Task.session = session;
        Task.sync = true;

        // vài hồ sơ chưa cấp thẻ cho bảng sinh viên
        seed(repo, "CT070333", "Phạm Như Ngọc", "CT7D", "Hà Nội");
        seed(repo, "CT070351", "Nguyễn Thị Phương Thảo", "CT7D", "Nam Định");

        final Page.Ctx ctx = new Page.Ctx(cfg, repo, session, repo.login("admin", "admin@123"));
        final MainView[] holder = new MainView[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new MainView(ctx));
        final MainView view = holder[0];

        // 1. cắm thẻ trắng
        session.connect();
        check("Thẻ giả lập mới cài là thẻ trắng", session.status().state == 0);

        // 2. phát hành qua màn Phát hành thẻ
        IssueCardPage issue = page(view, IssueCardPage.class);
        issue.fillForTest("CT070254", "Vũ Thị Hoài Thu");
        issue.issueForTest();
        check("Phát hành xong: chip 'Thẻ thật'", session.isGenuine() && session.chipText().contains("Thẻ thật"));
        shot(view, issue, out, "02-phat-hanh-the");

        // 3. giao dịch + điểm danh
        CardClient c = session.client;
        repo.recordReceipt("CT070254", c.topUp("123456", 500), "Nạp tại quầy");
        repo.recordReceipt("CT070254", c.pay("123456", 35), "Căng tin");
        repo.recordReceipt("CT070254", c.pay("123456", 15), "Gửi xe");
        CashierPage cashier = page(view, CashierPage.class);
        CardClient.Receipt last = c.pay("123456", 20);
        cashier.showReceipt(last, repo.recordReceipt("CT070254", last, "Photo / in ấn"), "Photo / in ấn");
        check("CSDL ghi đủ 4 biên nhận tiền", repo.history("CT070254", 50).size() == 4);
        AttendancePage att = page(view, AttendancePage.class);
        view.open(att);
        att.swipe();
        check("Điểm danh ghi vào CSDL", repo.countCheckinsSince(Fmt.startOfToday()) == 1);

        // 4. chụp các màn còn lại
        shot(view, page(view, DashboardPage.class), out, "01-tong-quan");
        shot(view, page(view, StudentsPage.class), out, "03-sinh-vien-va-the");
        session.verifyPin("123456");
        MyInfoPage info = page(view, MyInfoPage.class);
        view.open(info);
        info.loadForTest();
        check("Màn Thông tin thẻ giải mã đúng họ tên", "Vũ Thị Hoài Thu".equals(info.nameText()));
        shot(view, info, out, "04-thong-tin-the");
        shot(view, page(view, PinPage.class), out, "05-ma-pin");
        shot(view, page(view, WalletPage.class), out, "06-vi-va-lich-su");
        shot(view, cashier, out, "07-thu-ngan");
        if (System.getProperty("dump") != null) {
            dump(cashier, 0);
        }
        shot(view, att, out, "08-diem-danh");
        SecurityPage sec = page(view, SecurityPage.class);
        view.open(sec);
        sec.readKey();
        sec.authenticate();
        shotNoOpen(view, sec, out, "09-bao-mat-the");
        shot(view, page(view, StatsPage.class), out, "10-thong-ke");

        // 5. nhật ký APDU + giao diện tối
        view.setLogVisible(true);
        shot(view, page(view, DashboardPage.class), out, "11-nhat-ky-apdu");
        view.setLogVisible(false);
        SwingUtilities.invokeAndWait(() -> {
            Theme.toggleDark();
            SwingUtilities.updateComponentTreeUI(view);
        });
        shot(view, page(view, WalletPage.class), out, "12-giao-dien-toi");
        SwingUtilities.invokeAndWait(() -> {
            Theme.toggleDark();
            SwingUtilities.updateComponentTreeUI(view);
        });

        // 6. vai trò khác + màn đăng nhập
        Page.Ctx student = new Page.Ctx(cfg, repo, session, null);
        final MainView[] sv = new MainView[1];
        SwingUtilities.invokeAndWait(() -> sv[0] = new MainView(student));
        check("Chế độ sinh viên chỉ có 4 màn", sv[0].pages().size() == 4);
        shot(sv[0], sv[0].pages().get(0), out, "13-che-do-sinh-vien");
        LoginDialog d = new LoginDialog(null, repo);
        render((JComponent) d.getContentPane(), 860, 500, new File(out, "00-dang-nhap.png"));

        session.disconnect();
        repo.connection().close();
        System.out.println("TẤT CẢ " + passed + " KIỂM TRA GIAO DIỆN ĐỀU ĐÚNG · ảnh ở " + out.getAbsolutePath());
        System.exit(0);
    }

    static int passed;

    static void check(String name, boolean ok) {
        if (!ok) {
            System.out.println("SAI  " + name);
            throw new AssertionError(name);
        }
        passed++;
        System.out.println("OK   " + name);
    }

    static void seed(CardRepository repo, String id, String name, String lop, String que) throws Exception {
        Map<Integer, String> m = new LinkedHashMap<Integer, String>();
        m.put(ProfileTlv.MA_SV, id);
        m.put(ProfileTlv.HO_TEN, name);
        m.put(ProfileTlv.LOP, lop);
        m.put(ProfileTlv.KHOA_NGANH, "Công nghệ thông tin");
        m.put(ProfileTlv.QUE_QUAN, que);
        repo.upsertStudent(m, ImageUtil.jpeg(ImageUtil.sampleAvatar(name, 120, 160), 0.7f));
    }

    @SuppressWarnings("unchecked")
    static <T extends Page> T page(MainView v, Class<T> cls) {
        for (Page p : v.pages()) {
            if (cls.isInstance(p)) {
                return (T) p;
            }
        }
        throw new IllegalArgumentException(cls.getSimpleName());
    }

    static void shot(final MainView v, final Page p, File dir, String name) throws Exception {
        SwingUtilities.invokeAndWait(() -> v.open(p));
        shotNoOpen(v, p, dir, name);
    }

    static void shotNoOpen(final MainView v, final Page p, File dir, final String name) throws Exception {
        final File f = new File(dir, name + ".png");
        SwingUtilities.invokeAndWait(() -> { });
        SwingUtilities.invokeAndWait(() -> {
            try {
                render(v, 1360, 820, f);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        System.out.println("     ảnh " + f.getName() + "  (cao " + p.getPreferredSize().height + ")");
    }

    static void render(JComponent c, int w, int h, File f) throws Exception {
        c.setSize(w, h);
        layoutTree(c);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        c.paint(g);
        g.dispose();
        ImageIO.write(img, "png", f);
    }

    static void dump(Component c, int d) {
        if (c.getPreferredSize().height > 90 && d < 9) {
            System.out.println("DUMP " + new String(new char[d * 2]).replace(" ", " ") + c.getClass().getName() + " pref=" + c.getPreferredSize().height);
            if (c instanceof Container) {
                for (Component k : ((Container) c).getComponents()) {
                    dump(k, d + 1);
                }
            }
        }
    }

    static void layoutTree(Component c) {
        c.doLayout();
        if (c instanceof Container) {
            for (Component k : ((Container) c).getComponents()) {
                layoutTree(k);
            }
        }
    }
}
