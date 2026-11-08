package sinhvien.ui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import sinhvien.host.AppConfig;
import sinhvien.host.CardRepository;
import sinhvien.host.DatabaseBootstrap;

/**
 * Điểm chạy của phần mềm (Main Class trong NetBeans: sinhvien.ui.App).
 * 1. Đọc app.properties  2. Giao diện  3. Mở CSDL (tự tạo database + bảng)  4. Phiên thẻ  5. Đăng nhập.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        final AppConfig cfg = AppConfig.load(args.length > 0 ? args[0] : "app.properties");
        Theme.install(cfg);

        final CardRepository repo;
        try {
            repo = DatabaseBootstrap.open(cfg);
        } catch (SQLException e) {
            Dialogs.error(null, DatabaseBootstrap.explain(e, cfg));
            System.exit(1);
            return;
        } catch (Exception e) {
            Dialogs.error(null, "Không khởi tạo được CSDL: " + e.getMessage());
            System.exit(1);
            return;
        }

        final CardSession session;
        try {
            session = new CardSession(cfg, repo);
        } catch (Throwable e) {
            Dialogs.error(null, "Không tạo được kết nối thẻ (card.mode=" + cfg.cardMode() + "): " + e
                    + "\nChế độ jcardsim cần lib\\jcardsim-2.2.2.jar.");
            System.exit(1);
            return;
        }
        Task.session = session;
        EventQueue.invokeLater(() -> login(cfg, repo, session));
    }

    static void login(AppConfig cfg, CardRepository repo, CardSession session) {
        LoginDialog d = new LoginDialog(null, repo);
        d.setVisible(true);
        if (d.result == null && !d.studentMode) {
            if (session.isConnected()) {
                session.disconnect();
            }
            System.exit(0);
            return;
        }
        final JFrame f = new JFrame("Thẻ sinh viên Smart Card – Nhóm 1 L02");
        Page.Ctx ctx = new Page.Ctx(cfg, repo, session, d.result);
        MainView view = new MainView(ctx);
        view.setOnLogout(() -> {
            if (session.isConnected()) {
                session.disconnect();
            }
            f.dispose();
            login(cfg, repo, session);
        });
        f.setContentPane(view);
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (session.isConnected()) {
                    session.disconnect();
                }
                System.exit(0);
            }
        });
        f.setMinimumSize(new Dimension(1180, 720));
        f.setSize(1360, 820);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}
