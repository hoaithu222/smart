package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import sinhvien.host.CardClient;
import sinhvien.host.CardCrypto;
import sinhvien.host.CardRepository;
import sinhvien.host.ProfileTlv;

/**
 * Quầy thu ngân: nạp tiền / thanh toán. Mỗi giao dịch: sinh viên nhập PIN ngay trong lệnh,
 * thẻ cập nhật số dư trong giao dịch nguyên tử rồi ký biên nhận RSA; phần mềm kiểm chữ ký + bộ đếm
 * bằng khóa công khai trong CSDL rồi mới ghi sổ.
 */
public class CashierPage extends Page {

    private final JComboBox<String> service = new JComboBox<String>(new String[] {
        "Căng tin", "Gửi xe", "Photo / in ấn", "Lệ phí thi lại", "Thư viện", "Khác"});
    private final JTextField amount = Ui.field("Số tiền (nghìn đồng)", 10);
    private final JPasswordField pin = Ui.password("Sinh viên nhập PIN", 10);
    private final JLabel student = Ui.bold("Chưa có thẻ", 16);
    private final JLabel studentSub = Ui.muted(" ");
    private final Ui.Chip verdict = new Ui.Chip("Chưa có giao dịch");
    private final JLabel receipt = new JLabel(" ");
    private final JTextArea sig = new JTextArea(4, 30);

    public CashierPage(Ctx ctx) {
        super(ctx);
        amount.setFont(Theme.font(Font.BOLD, 18));
        JPanel quick = Ui.transparent(new GridLayout(2, 4, 8, 8));
        for (final int v : new int[] {10, 20, 30, 50, 100, 200, 500, 1000}) {
            JButton b = Ui.button(Fmt.money(v).replace(" đ", ""), Ui.Kind.DEFAULT);
            b.setFont(Theme.font(Font.PLAIN, 13));
            b.addActionListener(e -> amount.setText(String.valueOf(v)));
            quick.add(b);
        }
        JButton topup = Ui.button("Nạp tiền", Ui.Kind.SUCCESS, Icons.of(Icons.Kind.PLUS, 16, Color.WHITE));
        topup.addActionListener(e -> doTx(CardClient.TX_TOPUP));
        JButton pay = Ui.button("Thanh toán", Ui.Kind.PRIMARY, Icons.of(Icons.Kind.CART, 16, Color.WHITE));
        pay.addActionListener(e -> doTx(CardClient.TX_PAY));
        topup.setPreferredSize(new Dimension(150, 42));
        pay.setPreferredSize(new Dimension(150, 42));

        Ui.Form f = new Ui.Form(1);
        f.add("Dịch vụ", service).add("Số tiền (nghìn đồng) – tối đa " + ctx.cfg.maxTopUpThousand() + " mỗi lần nạp", amount);
        Ui.Form pf = new Ui.Form(1);
        pf.add("PIN của sinh viên (mỗi giao dịch đều nhập)", pin);
        JPanel left = Ui.vstack(12, student, studentSub, f, quick, pf, Ui.row(10, topup, pay));
        Ui.RoundPanel txCard = Ui.card("Giao dịch", "PIN + chữ ký RSA của thẻ cho từng lần nạp / thanh toán", left);

        sig.setEditable(false);
        sig.setLineWrap(true);
        sig.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        receipt.setVerticalAlignment(JLabel.TOP);
        JPanel rb = Ui.vstack(10, Ui.row(0, verdict), receipt, Ui.muted("Chữ ký RSA-1024 (SHA1withRSA) của thẻ:"), Ui.scroll(sig));
        Ui.RoundPanel rc = Ui.card("Biên nhận", "Thẻ ký: loại | mã SV | số tiền | số dư | bộ đếm | thời gian | nonce", rb);

        JPanel body = Ui.transparent(new BorderLayout(16, 0));
        body.add(txCard, BorderLayout.CENTER);
        body.add(Ui.fixedWidth(rc, 440), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    public String title() {
        return "Thu ngân";
    }

    public String subtitle() {
        return "Nạp tiền, thanh toán dịch vụ trong trường";
    }

    public Icons.Kind icon() {
        return Icons.Kind.CART;
    }

    public void onShow() {
        onCardChanged();
    }

    public void onCardChanged() {
        CardSession s = ctx.session;
        if (!s.isConnected() || s.status() == null || s.status().state != 1) {
            student.setText("Chưa có thẻ hợp lệ");
            studentSub.setText("Kết nối thẻ sinh viên đã phát hành");
            return;
        }
        final String id = s.cardId();
        Task.run(this, null, () -> ctx.repo.findStudent(id), m -> {
            student.setText(m == null ? id : m.get(ProfileTlv.HO_TEN));
            studentSub.setText(id + (m == null ? "" : " · " + m.get(ProfileTlv.LOP)) + " · "
                    + (ctx.session.isGenuine() ? "thẻ thật (RSA)" : "CHƯA xác thực"));
        });
    }

    private void doTx(final int type) {
        final int amt;
        final String pinS = new String(pin.getPassword());
        try {
            ctx.session.requireGenuine();
            amt = Validators.amount(amount.getText(), type == CardClient.TX_TOPUP ? ctx.cfg.maxTopUpThousand() : 30000);
            Validators.required(pinS, "PIN");
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        final String svc = type == CardClient.TX_TOPUP ? "Nạp tại quầy" : String.valueOf(service.getSelectedItem());
        Task.run(this, type == CardClient.TX_TOPUP ? "Đang nạp tiền…" : "Đang thanh toán…", () -> {
            CardClient c = ctx.session.client;
            CardClient.Receipt r = type == CardClient.TX_TOPUP ? c.topUp(pinS, amt) : c.pay(pinS, amt);
            CardRepository.RecordResult res = ctx.repo.recordReceipt(ctx.session.cardId(), r, svc);
            return new Object[] {r, res};
        }, o -> {
            pin.setText("");
            CardClient.Receipt r = (CardClient.Receipt) o[0];
            CardRepository.RecordResult res = (CardRepository.RecordResult) o[1];
            showReceipt(r, res, svc);
        }, () -> {
            pin.setText("");
            ctx.session.refreshQuietly();
        });
    }

    void showReceipt(CardClient.Receipt r, CardRepository.RecordResult res, String svc) {
        switch (res) {
        case OK:
            verdict.set("Chữ ký RSA hợp lệ · đã ghi sổ", Theme.SUCCESS);
            break;
        case BAD_SIGNATURE:
            verdict.set("Chữ ký KHÔNG hợp lệ – nghi thẻ giả", Theme.DANGER);
            break;
        case REPLAY:
            verdict.set("Biên nhận đã ghi trước đó (phát lại)", Theme.WARNING);
            break;
        default:
            verdict.set("Thẻ chưa đăng ký trong CSDL", Theme.DANGER);
        }
        receipt.setText("<html><table cellpadding='3'>"
                + "<tr><td>Loại</td><td><b>" + Fmt.txType(r.type) + "</b> · " + svc + "</td></tr>"
                + "<tr><td>Mã SV</td><td>" + ctx.session.cardId() + "</td></tr>"
                + "<tr><td>Số tiền</td><td><b>" + Fmt.money(r.value) + "</b></td></tr>"
                + "<tr><td>Số dư sau</td><td><b>" + Fmt.money(r.balance) + "</b></td></tr>"
                + "<tr><td>Bộ đếm thẻ</td><td>" + r.counter + "</td></tr>"
                + "<tr><td>Thời gian</td><td>" + Fmt.time(r.time()) + "</td></tr>"
                + "<tr><td>Nonce</td><td>" + CardCrypto.hex(java.util.Arrays.copyOfRange(r.timeAndNonce, 4, 12)) + "</td></tr>"
                + "</table></html>");
        sig.setText(CardCrypto.hex(r.signature));
        sig.setCaretPosition(0);
    }

}
