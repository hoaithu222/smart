package sinhvien.ui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;

/** Các khối giao diện dùng chung: thẻ bo góc, nút theo kiểu, nhãn, ô nhập, bảng, chip trạng thái. */
public final class Ui {

    public enum Kind { PRIMARY, SUCCESS, DANGER, DEFAULT, GHOST }

    private Ui() {
    }

    // ------------------------------------------------------------ khối nền

    /** Panel bo góc, nền "surface", viền mảnh. */
    public static class RoundPanel extends JPanel {
        private final int arc;
        private Color fill;

        public RoundPanel(LayoutManager lm) {
            this(lm, 16);
        }

        public RoundPanel(LayoutManager lm, int arc) {
            super(lm);
            this.arc = arc;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        }

        /** null = màu surface mặc định. */
        public void setFill(Color c) {
            fill = c;
            repaint();
        }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D r = new RoundRectangle2D.Double(0.5, 0.5, getWidth() - 1, getHeight() - 1, arc, arc);
            g.setColor(fill != null ? fill : Theme.surface());
            g.fill(r);
            g.setColor(Theme.border());
            g.setStroke(new BasicStroke(1f));
            g.draw(r);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Panel nền trang (xám nhạt / tối). */
    public static class PageBg extends JPanel {
        public PageBg(LayoutManager lm) {
            super(lm);
        }

        protected void paintComponent(Graphics g) {
            g.setColor(Theme.pageBg());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /** Thẻ có tiêu đề + phụ đề + nội dung. */
    public static RoundPanel card(String title, String subtitle, JComponent body) {
        RoundPanel p = new RoundPanel(new BorderLayout(0, 12));
        if (title != null) {
            JPanel head = transparent(new BorderLayout());
            JLabel t = new JLabel(title);
            t.setFont(Theme.font(Font.BOLD, 15));
            head.add(t, BorderLayout.NORTH);
            if (subtitle != null) {
                head.add(muted(subtitle), BorderLayout.SOUTH);
            }
            p.add(head, BorderLayout.NORTH);
        }
        if (body != null) {
            p.add(body, BorderLayout.CENTER);
        }
        return p;
    }

    public static JPanel transparent(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    public static JPanel row(int gap, Component... cs) {
        JPanel p = transparent(new FlowLayout(FlowLayout.LEFT, gap, 0));
        for (Component c : cs) {
            p.add(c);
        }
        return p;
    }

    public static JPanel rowRight(int gap, Component... cs) {
        JPanel p = transparent(new FlowLayout(FlowLayout.RIGHT, gap, 0));
        for (Component c : cs) {
            p.add(c);
        }
        return p;
    }

    // ------------------------------------------------------------ chữ

    public static JLabel h1(String s) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.font(Font.BOLD, 22));
        return l;
    }

    public static JLabel bold(String s, float size) {
        JLabel l = new JLabel(s);
        l.setFont(Theme.font(Font.BOLD, size));
        return l;
    }

    public static JLabel muted(String s) {
        JLabel l = new JLabel(s) {
            public Color getForeground() {
                return Theme.muted();
            }
        };
        l.setFont(Theme.font(Font.PLAIN, 13));
        return l;
    }

    public static JLabel mono(String s) {
        JLabel l = new JLabel(s);
        l.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        return l;
    }

    // ------------------------------------------------------------ nút

    public static JButton button(String text, Kind kind, Icon icon) {
        JButton b = new JButton(text, icon);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setIconTextGap(8);
        b.setMargin(new Insets(7, 14, 7, 14));
        b.setFont(Theme.font(Font.BOLD, 13.5f));
        Color bg = kind == Kind.PRIMARY ? Theme.ACCENT : kind == Kind.SUCCESS ? Theme.SUCCESS
                : kind == Kind.DANGER ? Theme.DANGER : null;
        if (bg != null) {
            b.setBackground(bg);
            b.setForeground(Color.WHITE);
            b.putClientProperty("JButton.buttonType", "roundRect");
        } else if (kind == Kind.GHOST) {
            b.putClientProperty("JButton.buttonType", "borderless");
        }
        return b;
    }

    public static JButton button(String text, Kind kind) {
        return button(text, kind, null);
    }

    // ------------------------------------------------------------ ô nhập

    public static JTextField field(String placeholder, int cols) {
        JTextField f = new JTextField(cols);
        f.putClientProperty("JTextField.placeholderText", placeholder);
        return f;
    }

    public static JPasswordField password(String placeholder, int cols) {
        JPasswordField f = new JPasswordField(cols);
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.putClientProperty("JComponent.style", "showRevealButton: true");
        return f;
    }

    /** Lưới biểu mẫu nhãn ở trên, ô ở dưới, nhiều cột; luôn dồn lên trên. */
    public static class Form extends JPanel {
        private final int columns;
        private int index;
        private final GridBagConstraints gc = new GridBagConstraints();
        private final Component filler = glue();

        public Form(int columns) {
            super(new GridBagLayout());
            this.columns = columns;
            setOpaque(false);
            gc.fill = GridBagConstraints.HORIZONTAL;
            gc.anchor = GridBagConstraints.NORTH;
            gc.weightx = 1;
            gc.insets = new Insets(0, 0, 12, 14);
        }

        public Form add(String label, JComponent field) {
            return add(label, field, 1);
        }

        public Form add(String label, JComponent field, int span) {
            if (index % columns + span > columns) {
                index += columns - index % columns;
            }
            JPanel cell = transparent(new BorderLayout(0, 5));
            JLabel l = new JLabel(label);
            l.setFont(Theme.font(Font.BOLD, 12.5f));
            cell.add(l, BorderLayout.NORTH);
            cell.add(field, BorderLayout.CENTER);
            gc.gridx = index % columns;
            gc.gridy = index / columns;
            gc.gridwidth = span;
            gc.weighty = 0;
            super.add(cell, gc);
            index += span;
            // hàng đệm cuối cùng ăn hết chiều cao thừa -> các ô luôn nằm trên cùng
            remove(filler);
            GridBagConstraints f = new GridBagConstraints();
            f.gridx = 0;
            f.gridy = (index + columns - 1) / columns + 1;
            f.gridwidth = columns;
            f.weighty = 1;
            f.fill = GridBagConstraints.BOTH;
            super.add(filler, f);
            return this;
        }
    }

    /** Phần đệm co giãn có kích thước ưu tiên 0 (JPanel layout null sẽ lấy kích thước cũ làm ưu tiên -> phình dần). */
    static Component glue() {
        return new javax.swing.Box.Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    }

    /** Xếp dọc, mỗi phần giữ chiều cao tự nhiên, phần thừa dồn xuống dưới. */
    public static JPanel vstack(int gap, Component... cs) {
        JPanel p = transparent(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0;
        g.weightx = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.anchor = GridBagConstraints.NORTH;
        for (int i = 0; i < cs.length; i++) {
            g.gridy = i;
            g.insets = new Insets(i == 0 ? 0 : gap, 0, 0, 0);
            p.add(cs[i], g);
        }
        g.gridy = cs.length;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        p.add(glue(), g);
        return p;
    }

    /** Đoạn văn tự xuống dòng theo bề rộng khung. */
    public static javax.swing.JTextArea paragraph(String text) {
        javax.swing.JTextArea a = new javax.swing.JTextArea(text) {
            public Color getForeground() {
                return Theme.text();
            }
        };
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        a.setEditable(false);
        a.setFocusable(false);
        a.setOpaque(false);
        a.setBorder(BorderFactory.createEmptyBorder());
        a.setFont(Theme.font(Font.PLAIN, 13.5f));
        return a;
    }

    /** Bọc để cố định bề rộng, chiều cao theo nội dung (dùng cho cột phải). */
    public static JPanel fixedWidth(final Component c, final int width) {
        JPanel p = new JPanel(new BorderLayout()) {
            public Dimension getPreferredSize() {
                return new Dimension(width, c.getPreferredSize().height);
            }
        };
        p.setOpaque(false);
        p.add(c, BorderLayout.CENTER);
        return p;
    }

    /** Viền 1px đọc màu lúc vẽ (đúng cả sáng lẫn tối). */
    public static class LineBorder extends javax.swing.border.AbstractBorder {
        private final boolean topOnly;

        public LineBorder(boolean topOnly) {
            this.topOnly = topOnly;
        }

        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(Theme.border());
            if (topOnly) {
                g.fillRect(x, y, w, 1);
            } else {
                g.drawRect(x, y, w - 1, h - 1);
            }
        }

        public Insets getBorderInsets(Component c) {
            return topOnly ? new Insets(1, 0, 0, 0) : new Insets(1, 1, 1, 1);
        }
    }

    // ------------------------------------------------------------ bảng

    /** Ô bảng có lề trái/phải 10px (DefaultTableCellRenderer tự đặt lại viền nên phải ghi đè). */
    public static class PaddedRenderer extends DefaultTableCellRenderer {
        public PaddedRenderer(int align) {
            setHorizontalAlignment(align);
        }

        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, false, r, c);
            l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            return l;
        }
    }

    public static JTable table(TableModel model) {
        JTable t = new JTable(model);
        t.setFillsViewportHeight(true);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setAutoCreateRowSorter(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setFont(Theme.font(Font.BOLD, 13));
        t.setDefaultRenderer(Object.class, new PaddedRenderer(SwingConstants.LEFT));
        t.setDefaultRenderer(Integer.class, new PaddedRenderer(SwingConstants.RIGHT));
        t.setDefaultRenderer(Long.class, new PaddedRenderer(SwingConstants.RIGHT));
        return t;
    }

    /** Đặt bề rộng ưu tiên cho các cột (px). */
    public static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane s = new JScrollPane(c);
        s.setBorder(new LineBorder(false));
        if (c instanceof JTable) {
            // JTable chỉ tự gắn hàng tiêu đề khi đã nằm trong cửa sổ; gắn sẵn để ảnh chụp/in cũng có
            s.setColumnHeaderView(((JTable) c).getTableHeader());
        }
        return s;
    }

    // ------------------------------------------------------------ chip trạng thái

    /** Viên "• Thẻ thật · CT070254" đổi màu theo trạng thái. */
    public static class Chip extends JLabel {
        private Color tone = Theme.muted();

        public Chip(String text) {
            super(text);
            setFont(Theme.font(Font.BOLD, 13));
            setBorder(BorderFactory.createEmptyBorder(6, 26, 6, 14));
        }

        public void set(String text, Color tone) {
            this.tone = tone;
            setText(text);
            repaint();
        }

        public Color getForeground() {
            return tone == null ? super.getForeground() : tone;
        }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(Theme.soft(tone));
            g.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g.setColor(tone);
            int d = 8;
            g.fillOval(12, (getHeight() - d) / 2, d, d);
            g.dispose();
            super.paintComponent(g0);
        }
    }

    // ------------------------------------------------------------ khung ảnh

    /** Khung ảnh chân dung bo góc; chưa có ảnh thì vẽ biểu tượng người. */
    public static class PhotoBox extends javax.swing.JComponent {
        private java.awt.image.BufferedImage img;

        public PhotoBox(int w, int h) {
            setPreferredSize(new Dimension(w, h));
            setMinimumSize(new Dimension(w, h));
            setMaximumSize(new Dimension(w, h));
        }

        public void setImage(java.awt.image.BufferedImage img) {
            this.img = img;
            repaint();
        }

        public java.awt.image.BufferedImage getImage() {
            return img;
        }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            RoundRectangle2D r = new RoundRectangle2D.Double(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
            g.setColor(Theme.surfaceAlt());
            g.fill(r);
            if (img != null) {
                g.setClip(r);
                g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                g.setClip(null);
            } else {
                Icons.of(Icons.Kind.USER, Math.min(getWidth(), getHeight()) / 2, Theme.muted())
                        .paintIcon(this, g, getWidth() / 4, (getHeight() - getWidth() / 2) / 2);
            }
            g.setColor(Theme.border());
            g.draw(r);
            g.dispose();
        }
    }

    // ------------------------------------------------------------ ô số liệu

    public static class StatTile extends RoundPanel {
        private final JLabel value = new JLabel("—");

        public StatTile(String label, Icon icon, final Color tone) {
            super(new BorderLayout(14, 0));
            JLabel ic = new JLabel(icon) {
                protected void paintComponent(Graphics g0) {
                    Graphics2D g = (Graphics2D) g0.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setColor(Theme.soft(tone));
                    g.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g.dispose();
                    super.paintComponent(g0);
                }
            };
            ic.setPreferredSize(new Dimension(48, 48));
            ic.setForeground(tone);
            add(ic, BorderLayout.WEST);
            JPanel text = transparent(new BorderLayout(0, 2));
            value.setFont(Theme.font(Font.BOLD, 22));
            text.add(value, BorderLayout.CENTER);
            text.add(muted(label), BorderLayout.SOUTH);
            add(text, BorderLayout.CENTER);
        }

        public void setValue(String v) {
            value.setText(v);
        }
    }
}
