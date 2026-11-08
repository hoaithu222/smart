package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SwingUtilities;

/** Hộp thoại dùng chung. */
public final class Dialogs {

    private Dialogs() {
    }

    public static void error(Component owner, String msg) {
        JOptionPane.showMessageDialog(owner, msg, "Không thực hiện được", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(Component owner, String title, String msg) {
        JOptionPane.showMessageDialog(owner, msg, title, JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component owner, String title, String msg) {
        return JOptionPane.showConfirmDialog(owner, msg, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
    }

    public static String input(Component owner, String title, String msg) {
        return JOptionPane.showInputDialog(owner, msg, title, JOptionPane.QUESTION_MESSAGE);
    }

    /** Hỏi PIN / PIN quản trị. Trả về null nếu bấm Hủy. */
    public static String askSecret(Component owner, String title, String msg) {
        final JPasswordField f = Ui.password("••••••", 14);
        JPanel p = new JPanel(new BorderLayout(0, 8));
        JLabel l = new JLabel(msg);
        l.setFont(Theme.font(Font.PLAIN, 13.5f));
        p.add(l, BorderLayout.NORTH);
        p.add(f, BorderLayout.CENTER);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                f.requestFocusInWindow();
            }
        });
        int r = JOptionPane.showConfirmDialog(owner, p, title, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (r != JOptionPane.OK_OPTION) {
            return null;
        }
        String s = new String(f.getPassword());
        return s.isEmpty() ? null : s;
    }
}
