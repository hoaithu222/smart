package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import sinhvien.host.CardClient;

/**
 * Đổi PIN và mở khóa PIN bằng PUK. Thẻ lưu PIN dạng băm SHA-256 có salt; đổi PIN chỉ bọc lại
 * khóa dữ liệu (DEK) nên thông tin đã mã hóa không bị mất.
 * Tiêu chí chấm: "Thay đổi mã PIN, mã hóa mã PIN", giới hạn số lần nhập sai.
 */
public class PinPage extends Page {

    private final JPasswordField oldPin = Ui.password("PIN hiện tại", 10);
    private final JPasswordField newPin = Ui.password("PIN mới", 10);
    private final JPasswordField newPin2 = Ui.password("Nhập lại PIN mới", 10);
    private final JPasswordField puk = Ui.password("Mã PUK", 10);
    private final JPasswordField pukNew = Ui.password("PIN mới", 10);
    private final JPasswordField pukNew2 = Ui.password("Nhập lại PIN mới", 10);
    private final Dots pinDots = new Dots(3);
    private final Dots pukDots = new Dots(5);
    private final JLabel pinText = Ui.bold("—", 15);
    private final JLabel pukText = Ui.bold("—", 15);

    public PinPage(Ctx ctx) {
        super(ctx);
        JPanel status = Ui.transparent(new GridLayout(1, 2, 16, 0));
        status.add(statusCard("Lượt nhập PIN còn lại", pinText, pinDots, "Sai 3 lần → PIN bị khóa (thẻ trả 69 83)"));
        status.add(statusCard("Lượt nhập PUK còn lại", pukText, pukDots, "Sai 5 lần → thẻ bị khóa hẳn, phải phát hành lại"));
        add(status, BorderLayout.NORTH);

        Ui.Form a = new Ui.Form(1);
        a.add("PIN hiện tại", oldPin).add("PIN mới (" + ctx.cfg.pinLength() + " chữ số)", newPin).add("Nhập lại PIN mới", newPin2);
        JButton change = Ui.button("Đổi PIN", Ui.Kind.PRIMARY, Icons.of(Icons.Kind.KEY, 16, Color.WHITE));
        change.addActionListener(e -> changePin());
        JPanel ab = Ui.vstack(4, a, Ui.row(0, change));

        Ui.Form b = new Ui.Form(1);
        b.add("Mã PUK (" + ctx.cfg.pukLength() + " chữ số, nhận lúc phát hành)", puk).add("PIN mới", pukNew).add("Nhập lại PIN mới", pukNew2);
        JButton unblock = Ui.button("Mở khóa và đặt PIN mới", Ui.Kind.DANGER, Icons.of(Icons.Kind.UNLOCK, 16, Color.WHITE));
        unblock.addActionListener(e -> unblock());
        JPanel bb = Ui.vstack(4, b, Ui.row(0, unblock));

        JPanel forms = Ui.transparent(new GridLayout(1, 2, 16, 0));
        forms.add(Ui.card("Đổi mã PIN", "Thẻ băm PIN mới, bọc lại khóa dữ liệu – không mất thông tin", ab));
        forms.add(Ui.card("Quên PIN – mở khóa bằng PUK", "PUK mở bản sao khóa dữ liệu, đặt PIN mới, dữ liệu còn nguyên", bb));
        add(forms, BorderLayout.CENTER);

        add(Ui.card("Cách thẻ giữ PIN", null, Ui.paragraph("Giá trị lưu trên thẻ = SHA-256('P' ‖ salt ‖ PIN) trong OwnerPIN – không ai đọc được PIN, "
                + "thẻ chỉ so sánh giá trị băm. Khóa mã dữ liệu (DEK) được bọc bằng KEK = SHA-256('K' ‖ salt ‖ PIN); "
                + "đổi PIN chỉ bọc lại DEK và cập nhật băm PIN trong một giao dịch nguyên tử, nên thông tin đã mã hóa không phải mã lại.")),
                BorderLayout.SOUTH);
    }

    private JComponent statusCard(String title, JLabel value, Dots dots, String note) {
        JPanel p = Ui.transparent(new BorderLayout(0, 8));
        JPanel top = Ui.transparent(new BorderLayout());
        top.add(Ui.muted(title), BorderLayout.WEST);
        top.add(value, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        p.add(dots, BorderLayout.CENTER);
        p.add(Ui.muted(note), BorderLayout.SOUTH);
        return Ui.card(null, null, p);
    }

    public String title() {
        return "Mã PIN";
    }

    public String subtitle() {
        return "Đổi PIN, giới hạn số lần nhập sai, mở khóa bằng PUK";
    }

    public Icons.Kind icon() {
        return Icons.Kind.KEY;
    }

    public void onShow() {
        onCardChanged();
    }

    public void onCardChanged() {
        CardClient.CardStatus st = ctx.session.status();
        if (!ctx.session.isConnected() || st == null || st.state == 0) {
            pinText.setText("—");
            pukText.setText("—");
            pinDots.set(-1);
            pukDots.set(-1);
            return;
        }
        pinText.setText(st.pinTries + " / 3");
        pukText.setText(st.pukTries + " / 5");
        pinDots.set(st.pinTries);
        pukDots.set(st.pukTries);
    }

    private void changePin() {
        final String o = new String(oldPin.getPassword());
        final String n = new String(newPin.getPassword());
        try {
            ctx.session.requireGenuine();
            Validators.digits(n, ctx.cfg.pinLength(), "PIN mới");
            Validators.same(n, new String(newPin2.getPassword()), "PIN mới");
            Validators.required(o, "PIN hiện tại");
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        Task.run(this, "Đang đổi PIN…", () -> {
            ctx.session.client.changePin(o, n);
            ctx.session.refreshQuietly();
            return null;
        }, v -> {
            clear();
            Dialogs.info(this, "Đã đổi PIN", "Đổi PIN thành công. Thông tin trên thẻ vẫn đọc được bằng PIN mới.");
        }, () -> ctx.session.refreshQuietly());
    }

    private void unblock() {
        final String p = new String(puk.getPassword());
        final String n = new String(pukNew.getPassword());
        try {
            ctx.session.requireConnected();
            Validators.digits(n, ctx.cfg.pinLength(), "PIN mới");
            Validators.same(n, new String(pukNew2.getPassword()), "PIN mới");
            Validators.required(p, "Mã PUK");
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        Task.run(this, "Đang mở khóa PIN…", () -> {
            ctx.session.client.unblockPin(p, n);
            ctx.session.markPinLost();
            ctx.session.refreshQuietly();
            return null;
        }, v -> {
            clear();
            Dialogs.info(this, "Đã mở khóa", "PIN mới đã được đặt, bộ đếm sai về 3. Dữ liệu trên thẻ còn nguyên.");
        }, () -> ctx.session.refreshQuietly());
    }

    private void clear() {
        for (JPasswordField f : new JPasswordField[] {oldPin, newPin, newPin2, puk, pukNew, pukNew2}) {
            f.setText("");
        }
    }

    /** Dãy chấm tròn: xanh = còn lượt, xám = đã dùng. */
    static class Dots extends JComponent {
        private final int max;
        private int value = -1;

        Dots(int max) {
            this.max = max;
            setPreferredSize(new java.awt.Dimension(10, 22));
        }

        void set(int v) {
            value = v;
            repaint();
        }

        protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = (getWidth() - (max - 1) * 8) / max;
            for (int i = 0; i < max; i++) {
                Color c = value < 0 ? Theme.border() : i < value ? (value == 1 ? Theme.WARNING : Theme.SUCCESS) : Theme.soft(Theme.DANGER);
                g.setColor(c);
                g.fillRoundRect(i * (w + 8), 6, w, 10, 10, 10);
            }
            g.dispose();
        }
    }
}
