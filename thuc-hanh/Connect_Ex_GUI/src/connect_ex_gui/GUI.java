package connect_ex_gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

/**
 * Giao dien (huong dan ket noi, buoc 3 va 5): nut Connect / Disconnect va
 * nut Show Text. Viet tay bang Swing (khong can tep .form cua NetBeans).
 */
public class GUI extends JFrame {

    private static final long serialVersionUID = 1L;

    private final SmartCardWord card = new SmartCardWord();
    private boolean isConnect = false;

    private final JTextField txt = new JTextField(24);
    private final JButton bt_show = new JButton("Show Text");
    private final JButton bt_cnt = new JButton("Connect");

    public GUI() {
        super("Connect_Ex_GUI");
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(new Color(220, 240, 250));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(10, 16, 10, 16);
        g.gridwidth = 2;
        g.gridx = 0;
        g.gridy = 0;

        JLabel tieuDe = new JLabel("VÍ DỤ CONNECT VỚI SMARTCARD", SwingConstants.CENTER);
        tieuDe.setForeground(Color.RED);
        tieuDe.setFont(tieuDe.getFont().deriveFont(Font.BOLD, 16f));
        p.add(tieuDe, g);

        txt.setEditable(false);
        g.gridy = 1;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(txt, g);

        g.gridy = 2;
        g.gridwidth = 1;
        g.weightx = 1;
        bt_show.setEnabled(false);
        p.add(bt_show, g);
        g.gridx = 1;
        p.add(bt_cnt, g);

        bt_cnt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                bt_cntActionPerformed(evt);
            }
        });
        bt_show.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                bt_showActionPerformed(evt);
            }
        });

        setContentPane(p);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setMinimumSize(new Dimension(getWidth(), getHeight()));
        setLocationRelativeTo(null);
    }

    private void bt_cntActionPerformed(ActionEvent evt) {
        if (!isConnect) {
            if (card.connectCard()) {
                isConnect = true;
                bt_cnt.setText("Disconnect");
                bt_cnt.setForeground(Color.RED);
                bt_show.setEnabled(true);
            } else {
                JOptionPane.showMessageDialog(this, "Chưa connect được đến applet\n" + card.getLoi());
                isConnect = false;
                bt_cnt.setText("Connect");
                bt_cnt.setForeground(Color.BLACK);
            }
        } else {
            if (card.disconnect()) {
                isConnect = false;
                bt_cnt.setText("Connect");
                bt_cnt.setForeground(Color.BLACK);
                bt_show.setEnabled(false);
                resetButton();
            }
        }
    }

    private void bt_showActionPerformed(ActionEvent evt) {
        String s = card.getText();
        if (s == null) {
            JOptionPane.showMessageDialog(this, "Không đọc được thẻ\n" + card.getLoi());
        } else {
            txt.setText(s);
        }
    }

    private void resetButton() {
        txt.setText("");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new GUI().setVisible(true);
            }
        });
    }
}
