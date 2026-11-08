package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

/**
 * Nội dung cửa sổ chính: thanh bên (menu theo vai trò) + header (tiêu đề, chip thẻ, nút kết nối)
 * + vùng màn hình + khung "Nhật ký APDU" có thể ẩn/hiện.
 * Tách khỏi JFrame để kiểm thử chụp ảnh được khi không có cửa sổ.
 */
public class MainView extends JPanel {

    private final Page.Ctx ctx;
    private final List<Page> pages = new ArrayList<Page>();
    private final List<NavItem> navItems = new ArrayList<NavItem>();
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final JLabel title = Ui.h1("");
    private final JLabel subtitle = Ui.muted("");
    private final Ui.Chip chip = new Ui.Chip("Chưa kết nối thẻ");
    private final JButton connectBtn = Ui.button("Kết nối thẻ", Ui.Kind.PRIMARY, Icons.of(Icons.Kind.PLUG, 16, Color.WHITE));
    private final JLabel busyLabel = Ui.muted("");
    private final JProgressBar busyBar = new JProgressBar();
    private final JTextArea logArea = new JTextArea();
    private final JScrollPane logPane;
    private Page current;
    private Runnable onLogout;

    public MainView(Page.Ctx ctx) {
        super(new BorderLayout());
        this.ctx = ctx;
        for (Page p : pagesFor(ctx)) {
            pages.add(p);
        }

        // ---- vùng màn hình ----
        content.setOpaque(false);
        for (Page p : pages) {
            JScrollPane sp = new JScrollPane(p);
            sp.setBorder(BorderFactory.createEmptyBorder());
            sp.getVerticalScrollBar().setUnitIncrement(18);
            sp.getViewport().setOpaque(false);
            content.add(sp, p.title());
        }

        // ---- nhật ký APDU ----
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        logArea.setRows(8);
        logPane = new JScrollPane(logArea);
        logPane.setBorder(new Ui.LineBorder(true));
        logPane.setVisible(false);
        ctx.session.log().addListener(line -> SwingUtilities.invokeLater(() -> appendLog(line)));

        JPanel center = new Ui.PageBg(new BorderLayout());
        center.add(buildHeader(), BorderLayout.NORTH);
        center.add(content, BorderLayout.CENTER);
        center.add(logPane, BorderLayout.SOUTH);

        add(buildSidebar(), BorderLayout.WEST);
        add(center, BorderLayout.CENTER);

        ctx.session.addListener(e -> SwingUtilities.invokeLater(this::onCardChanged));
        Task.busyListener = text -> {
            busyLabel.setText(text == null ? "" : text);
            busyBar.setVisible(text != null);
            setCursor(text == null ? null : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        };
        onCardChanged();
        open(pages.get(0));
    }

    void setLogVisible(boolean v) {
        logPane.setVisible(v);
        revalidate();
    }

    public void setOnLogout(Runnable r) {
        onLogout = r;
    }

    public List<Page> pages() {
        return pages;
    }

    /** Danh sách màn hình theo vai trò (chuỗi chặn lớp "vai trò"). */
    static List<Page> pagesFor(Page.Ctx ctx) {
        List<Page> l = new ArrayList<Page>();
        String role = ctx.role();
        if ("ADMIN".equals(role)) {
            l.add(new DashboardPage(ctx));
            l.add(new IssueCardPage(ctx));
            l.add(new StudentsPage(ctx));
            l.add(new MyInfoPage(ctx));
            l.add(new PinPage(ctx));
            l.add(new WalletPage(ctx));
            l.add(new CashierPage(ctx));
            l.add(new AttendancePage(ctx));
            l.add(new SecurityPage(ctx));
            l.add(new StatsPage(ctx));
        } else if ("THU_NGAN".equals(role)) {
            l.add(new CashierPage(ctx));
            l.add(new WalletPage(ctx));
            l.add(new SecurityPage(ctx));
        } else if ("GIANG_VIEN".equals(role)) {
            l.add(new AttendancePage(ctx));
            l.add(new SecurityPage(ctx));
        } else {
            l.add(new MyInfoPage(ctx));
            l.add(new PinPage(ctx));
            l.add(new WalletPage(ctx));
            l.add(new SecurityPage(ctx));
        }
        return l;
    }

    public void open(Page p) {
        current = p;
        cards.show(content, p.title());
        title.setText(p.title());
        subtitle.setText(p.subtitle());
        for (NavItem n : navItems) {
            n.setSelected(n.page == p);
        }
        p.onShow();
    }

    private void onCardChanged() {
        CardSession s = ctx.session;
        chip.set(s.chipText(), s.chipColor());
        if (s.isConnected()) {
            connectBtn.setText("Rút thẻ");
            connectBtn.setBackground(null);
            connectBtn.setForeground(null);
            connectBtn.setIcon(Icons.of(Icons.Kind.LOGOUT, 16));
        } else {
            connectBtn.setText("Kết nối thẻ");
            connectBtn.setBackground(Theme.ACCENT);
            connectBtn.setForeground(Color.WHITE);
            connectBtn.setIcon(Icons.of(Icons.Kind.PLUG, 16, Color.WHITE));
        }
        for (Page p : pages) {
            p.onCardChanged();
        }
    }

    private void appendLog(String line) {
        logArea.append(line + "\n");
        if (logArea.getLineCount() > 600) {
            try {
                logArea.replaceRange("", 0, logArea.getLineEndOffset(100));
            } catch (BadLocationException ignored) {
                // bỏ qua
            }
        }
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    // ------------------------------------------------------------ header

    private JComponent buildHeader() {
        JPanel h = new JPanel(new BorderLayout(16, 0)) {
            protected void paintComponent(Graphics g) {
                g.setColor(Theme.surface());
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Theme.border());
                g.fillRect(0, getHeight() - 1, getWidth(), 1);
            }
        };
        h.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 18));
        JPanel left = Ui.transparent(new BorderLayout(0, 2));
        title.setFont(Theme.font(Font.BOLD, 20));
        left.add(title, BorderLayout.NORTH);
        left.add(subtitle, BorderLayout.SOUTH);
        h.add(left, BorderLayout.CENTER);

        busyBar.setIndeterminate(true);
        busyBar.setPreferredSize(new Dimension(70, 6));
        busyBar.setVisible(false);

        JButton logBtn = iconButton(Icons.Kind.TERMINAL, "Ẩn/hiện nhật ký APDU");
        logBtn.addActionListener(e -> {
            logPane.setVisible(!logPane.isVisible());
            revalidate();
        });
        JButton themeBtn = iconButton(Theme.isDark() ? Icons.Kind.SUN : Icons.Kind.MOON, "Giao diện sáng / tối");
        themeBtn.addActionListener(e -> {
            Theme.toggleDark();
            themeBtn.setIcon(Icons.of(Theme.isDark() ? Icons.Kind.SUN : Icons.Kind.MOON, 18));
            SwingUtilities.getWindowAncestor(this).repaint();
        });
        JButton refreshBtn = iconButton(Icons.Kind.REFRESH, "Đọc lại thẻ");
        refreshBtn.addActionListener(e -> {
            if (ctx.session.isConnected()) {
                Task.run(this, "Đang đọc lại thẻ…", () -> {
                    ctx.session.refresh();
                    return null;
                }, v -> {
                    if (current != null) {
                        current.onShow();
                    }
                });
            }
        });
        connectBtn.addActionListener(e -> {
            if (ctx.session.isConnected()) {
                ctx.session.disconnect();
            } else {
                Task.run(this, "Đang kết nối thẻ…", () -> {
                    ctx.session.connect();
                    return null;
                }, v -> {
                    if (current != null) {
                        current.onShow();
                    }
                });
            }
        });
        JLabel mode = Ui.muted(ctx.session.modeLabel());
        JPanel right = Ui.rowRight(8, busyLabel, busyBar, mode, chip, connectBtn, refreshBtn, logBtn, themeBtn);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private static JButton iconButton(Icons.Kind k, String tip) {
        JButton b = new JButton(Icons.of(k, 18));
        b.setToolTipText(tip);
        b.putClientProperty("JButton.buttonType", "toolBarButton");
        b.setFocusable(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ------------------------------------------------------------ thanh bên

    private JComponent buildSidebar() {
        JPanel side = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0;
                g.setPaint(new GradientPaint(0, 0, Theme.SIDEBAR_TOP, 0, getHeight(), Theme.SIDEBAR_BOTTOM));
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        side.setPreferredSize(new Dimension(236, 10));
        side.setBorder(BorderFactory.createEmptyBorder(18, 12, 16, 12));

        // logo
        JPanel brand = Ui.transparent(new BorderLayout(10, 0));
        JLabel logo = new JLabel(Icons.of(Icons.Kind.CHIP, 26, Color.WHITE)) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, Theme.ACCENT, getWidth(), getHeight(), Theme.ACCENT_2));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g.dispose();
                super.paintComponent(g0);
            }
        };
        logo.setPreferredSize(new Dimension(44, 44));
        brand.add(logo, BorderLayout.WEST);
        JPanel bt = Ui.transparent(new BorderLayout());
        JLabel n1 = new JLabel("THẺ SINH VIÊN");
        n1.setFont(Theme.font(Font.BOLD, 15));
        n1.setForeground(Color.WHITE);
        JLabel n2 = new JLabel("Smart Card · Nhóm 1 – L02");
        n2.setFont(Theme.font(Font.PLAIN, 12));
        n2.setForeground(Theme.SIDEBAR_TEXT);
        bt.add(n1, BorderLayout.NORTH);
        bt.add(n2, BorderLayout.SOUTH);
        brand.add(bt, BorderLayout.CENTER);
        brand.setBorder(BorderFactory.createEmptyBorder(0, 6, 22, 0));
        side.add(brand, BorderLayout.NORTH);

        JPanel nav = Ui.transparent(null);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        JLabel sec = new JLabel("CHỨC NĂNG");
        sec.setFont(Theme.font(Font.BOLD, 11));
        sec.setForeground(new Color(0x7C8DB8));
        sec.setBorder(BorderFactory.createEmptyBorder(0, 10, 8, 0));
        nav.add(sec);
        for (Page p : pages) {
            NavItem item = new NavItem(p);
            navItems.add(item);
            nav.add(item);
            nav.add(Box.createVerticalStrut(2));
        }
        side.add(nav, BorderLayout.CENTER);

        // người dùng + đăng xuất
        JPanel foot = Ui.transparent(new BorderLayout(10, 0));
        foot.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(255, 255, 255, 30)),
                BorderFactory.createEmptyBorder(12, 6, 0, 0)));
        String who = ctx.user == null ? "Sinh viên" : ctx.user.fullName;
        String role = ctx.user == null ? "Dùng thẻ + PIN" : roleName(ctx.user.role) + " · " + ctx.user.username;
        JLabel u1 = new JLabel(who);
        u1.setForeground(Color.WHITE);
        u1.setFont(Theme.font(Font.BOLD, 13));
        JLabel u2 = new JLabel(role);
        u2.setForeground(Theme.SIDEBAR_TEXT);
        u2.setFont(Theme.font(Font.PLAIN, 12));
        JPanel ut = Ui.transparent(new BorderLayout());
        ut.add(u1, BorderLayout.NORTH);
        ut.add(u2, BorderLayout.SOUTH);
        foot.add(new JLabel(Icons.of(Icons.Kind.USER, 22, Theme.SIDEBAR_TEXT)), BorderLayout.WEST);
        foot.add(ut, BorderLayout.CENTER);
        JButton out = new JButton(Icons.of(Icons.Kind.LOGOUT, 18, Theme.SIDEBAR_TEXT));
        out.setToolTipText("Đăng xuất");
        out.putClientProperty("JButton.buttonType", "toolBarButton");
        out.setFocusable(false);
        out.addActionListener(e -> {
            if (onLogout != null) {
                onLogout.run();
            }
        });
        foot.add(out, BorderLayout.EAST);
        side.add(foot, BorderLayout.SOUTH);
        return side;
    }

    static String roleName(String role) {
        if ("ADMIN".equals(role)) {
            return "Quản trị";
        }
        if ("THU_NGAN".equals(role)) {
            return "Thu ngân";
        }
        if ("GIANG_VIEN".equals(role)) {
            return "Giảng viên";
        }
        return "Sinh viên";
    }

    /** Mục menu: biểu tượng + chữ, mục đang chọn có nền sáng + vạch màu bên trái. */
    private class NavItem extends JComponent {
        final Page page;
        private boolean selected;
        private boolean hover;

        NavItem(Page page) {
            this.page = page;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(200, 42));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
            setAlignmentX(0f);
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    open(NavItem.this.page);
                }

                public void mouseEntered(MouseEvent e) {
                    hover = true;
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    hover = false;
                    repaint();
                }
            });
        }

        void setSelected(boolean s) {
            selected = s;
            repaint();
        }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            if (selected || hover) {
                g.setColor(new Color(255, 255, 255, selected ? 34 : 16));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            }
            if (selected) {
                g.setColor(Theme.ACCENT_2);
                g.fillRoundRect(0, 10, 4, getHeight() - 20, 4, 4);
            }
            Color fg = selected ? Color.WHITE : Theme.SIDEBAR_TEXT;
            Icons.of(page.icon(), 19, fg).paintIcon(this, g, 14, (getHeight() - 19) / 2);
            g.setColor(fg);
            g.setFont(Theme.font(selected ? Font.BOLD : Font.PLAIN, 14));
            int y = (getHeight() + g.getFontMetrics().getAscent() - g.getFontMetrics().getDescent()) / 2;
            g.drawString(page.title(), 44, y);
            g.dispose();
        }
    }
}
