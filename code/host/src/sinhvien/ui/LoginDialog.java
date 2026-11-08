package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import sinhvien.host.CardRepository;

/**
 * Màn đăng nhập: cán bộ (tài khoản trong bảng nguoi_dung) hoặc "Sinh viên – dùng thẻ".
 * Kết quả: result = tài khoản, studentMode = true nếu chọn sinh viên, cả hai rỗng = thoát.
 */
public class LoginDialog extends JDialog {

    public CardRepository.UserAccount result;
    public boolean studentMode;

    private final CardRepository repo;
    private final JTextField user = Ui.field("Tên đăng nhập", 18);
    private final JPasswordField pass = Ui.password("Mật khẩu", 18);
    private final JLabel error = new JLabel(" ");

    public LoginDialog(JFrame owner, CardRepository repo) {
        super(owner, "Đăng nhập – Thẻ sinh viên", true);
        this.repo = repo;
        setContentPane(build());
        setResizable(false);
        pack();
        setLocationRelativeTo(null);
    }

    JComponent build() {
        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setPreferredSize(new Dimension(860, 500));
        root.add(brand());
        root.add(form());
        return root;
    }

    private JComponent brand() {
        JPanel p = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, Theme.SIDEBAR_TOP, getWidth(), getHeight(), Theme.SIDEBAR_BOTTOM));
                g.fillRect(0, 0, getWidth(), getHeight());
                // thẻ minh họa
                int w = 250, h = 156, x = (getWidth() - w) / 2, y = 150;
                g.setPaint(new GradientPaint(x, y, Theme.ACCENT, x + w, y + h, Theme.ACCENT_2));
                g.fillRoundRect(x, y, w, h, 22, 22);
                java.awt.Shape old = g.getClip();
                g.clip(new java.awt.geom.RoundRectangle2D.Double(x, y, w, h, 22, 22));
                g.setColor(new Color(255, 255, 255, 45));
                g.fillOval(x + w - 110, y - 70, 180, 180);
                g.setClip(old);
                g.setColor(new Color(0xF5D57A));
                g.fillRoundRect(x + 22, y + 52, 42, 32, 8, 8);
                g.setColor(new Color(0xC9A646));
                g.drawLine(x + 22, y + 63, x + 64, y + 63);
                g.drawLine(x + 22, y + 73, x + 64, y + 73);
                g.drawLine(x + 43, y + 52, x + 43, y + 84);
                g.setColor(Color.WHITE);
                g.setFont(Theme.font(Font.BOLD, 13));
                g.drawString("HỌC VIỆN KỸ THUẬT MẬT MÃ", x + 22, y + 32);
                g.setFont(Theme.font(Font.BOLD, 16));
                g.drawString("CT070254", x + 22, y + 118);
                g.setFont(Theme.font(Font.PLAIN, 12));
                g.drawString("THẺ SINH VIÊN · JAVA CARD", x + 22, y + 138);
                g.dispose();
            }
        };
        p.setBorder(BorderFactory.createEmptyBorder(36, 36, 36, 36));
        JLabel t = new JLabel("<html>Thẻ sinh viên<br>thông minh</html>");
        t.setFont(Theme.font(Font.BOLD, 26));
        t.setForeground(Color.WHITE);
        p.add(t, BorderLayout.NORTH);
        JLabel s = new JLabel("<html>PIN băm SHA-256 · Dữ liệu mã hóa AES-128 · Xác thực thẻ RSA<br>"
                + "Nhóm 1 – L02 · Phát triển phần mềm trong thẻ thông minh</html>");
        s.setForeground(Theme.SIDEBAR_TEXT);
        s.setFont(Theme.font(Font.PLAIN, 13));
        p.add(s, BorderLayout.SOUTH);
        return p;
    }

    private JComponent form() {
        JPanel p = new Ui.PageBg(new BorderLayout());
        p.setBorder(BorderFactory.createEmptyBorder(40, 44, 36, 44));
        error.setForeground(Theme.DANGER);
        error.setFont(Theme.font(Font.PLAIN, 12.5f));
        JButton login = Ui.button("Đăng nhập", Ui.Kind.PRIMARY);
        login.setPreferredSize(new Dimension(10, 42));
        login.addActionListener(e -> doLogin());
        pass.addActionListener(e -> doLogin());
        JButton student = Ui.button("Sinh viên – dùng thẻ và mã PIN", Ui.Kind.DEFAULT,
                Icons.of(Icons.Kind.CARD, 18));
        student.setPreferredSize(new Dimension(10, 42));
        student.addActionListener(e -> {
            studentMode = true;
            dispose();
        });
        user.setPreferredSize(new Dimension(10, 40));
        pass.setPreferredSize(new Dimension(10, 40));
        JPanel col = Ui.vstack(12, Ui.h1("Đăng nhập"), Ui.muted("Cán bộ phòng Đào tạo, thu ngân, giảng viên"),
                user, pass, error, login, new JSeparator(), Ui.muted("Sinh viên tra cứu thông tin, đổi PIN:"), student);
        p.add(col, BorderLayout.CENTER);
        p.add(Ui.muted("<html>Tài khoản mẫu: admin / admin@123<br>thungan / thungan@123 · gv / gv@123</html>"), BorderLayout.SOUTH);
        getRootPane().setDefaultButton(login);
        return p;
    }

    private void doLogin() {
        try {
            CardRepository.UserAccount u = repo.login(user.getText().trim(), new String(pass.getPassword()));
            if (u == null) {
                error.setText("Sai tên đăng nhập hoặc mật khẩu");
                pass.setText("");
                return;
            }
            result = u;
            dispose();
        } catch (Exception ex) {
            error.setText("Lỗi CSDL: " + ex.getMessage());
        }
    }
}
