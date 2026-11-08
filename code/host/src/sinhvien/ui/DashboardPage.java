package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import sinhvien.host.CardClient;
import sinhvien.host.CardRepository;

/** Tổng quan cho phòng Đào tạo: số liệu nhanh, giao dịch gần đây, thẻ đang cắm. */
public class DashboardPage extends Page {

    private final Ui.StatTile students = new Ui.StatTile("Sinh viên", Icons.of(Icons.Kind.USERS, 24), Theme.ACCENT);
    private final Ui.StatTile cards = new Ui.StatTile("Thẻ đang hoạt động", Icons.of(Icons.Kind.CARD, 24), Theme.ACCENT_2);
    private final Ui.StatTile topup = new Ui.StatTile("Tổng tiền đã nạp", Icons.of(Icons.Kind.WALLET, 24), Theme.SUCCESS);
    private final Ui.StatTile checkins = new Ui.StatTile("Lượt điểm danh hôm nay", Icons.of(Icons.Kind.CHECK, 24), Theme.WARNING);
    private final DefaultTableModel recent = new DefaultTableModel(
            new Object[] {"Thời gian", "Mã SV", "Họ tên", "Loại", "Dịch vụ / phòng", "Giá trị"}, 0) {
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JLabel cardInfo = new JLabel();

    public DashboardPage(Ctx ctx) {
        super(ctx);
        JPanel tiles = Ui.transparent(new GridLayout(1, 4, 16, 0));
        tiles.add(students);
        tiles.add(cards);
        tiles.add(topup);
        tiles.add(checkins);
        add(tiles, BorderLayout.NORTH);

        JTable t = Ui.table(recent);
        Ui.widths(t, 120, 85, 160, 85, 130, 100);
        JPanel body = Ui.transparent(new BorderLayout(16, 0));
        Ui.RoundPanel left = Ui.card("Hoạt động gần đây", "20 biên nhận mới nhất đã kiểm chữ ký RSA", Ui.scroll(t));
        body.add(left, BorderLayout.CENTER);

        cardInfo.setVerticalAlignment(JLabel.TOP);
        JPanel right = Ui.transparent(new BorderLayout(0, 16));
        right.add(Ui.card("Thẻ đang cắm", null, cardInfo), BorderLayout.NORTH);
        JLabel steps = new JLabel("<html><ol style='margin-left:16px'>"
                + "<li>Kết nối thẻ trắng → <b>Phát hành thẻ</b></li>"
                + "<li>Rút / cắm lại → chip <b>Thẻ thật</b> (RSA)</li>"
                + "<li><b>Thông tin thẻ</b>: nhập PIN, sửa quê quán</li>"
                + "<li><b>Mã PIN</b>: đổi PIN, khóa, mở bằng PUK</li>"
                + "<li><b>Thu ngân</b>: nạp, thanh toán</li>"
                + "<li><b>Điểm danh</b>: quẹt thẻ</li>"
                + "<li><b>Thống kê</b> + mở bảng giao_dich</li></ol></html>");
        steps.setVerticalAlignment(JLabel.TOP);
        right.add(Ui.card("Quy trình demo", null, steps), BorderLayout.CENTER);
        body.add(Ui.fixedWidth(right, 330), BorderLayout.EAST);
        body.setPreferredSize(new Dimension(10, 470));
        add(body, BorderLayout.CENTER);
    }

    public String title() {
        return "Tổng quan";
    }

    public String subtitle() {
        return "Số liệu toàn trường và thẻ đang cắm";
    }

    public Icons.Kind icon() {
        return Icons.Kind.HOME;
    }

    public void onShow() {
        Task.run(this, "Đang tải số liệu…", () -> {
            Object[] r = new Object[5];
            r[0] = ctx.repo.countStudents();
            r[1] = ctx.repo.countActiveCards();
            r[2] = ctx.repo.sumByType("TOPUP");
            r[3] = ctx.repo.countCheckinsSince(Fmt.startOfToday());
            r[4] = ctx.repo.history(null, 20);
            return r;
        }, r -> {
            students.setValue(String.valueOf(r[0]));
            cards.setValue(String.valueOf(r[1]));
            topup.setValue(Fmt.money((Long) r[2]));
            checkins.setValue(String.valueOf(r[3]));
            recent.setRowCount(0);
            @SuppressWarnings("unchecked")
            List<CardRepository.TxRow> rows = (List<CardRepository.TxRow>) r[4];
            for (CardRepository.TxRow t : rows) {
                recent.addRow(new Object[] {Fmt.shortTime(t.thoiGian), t.maSv, t.hoTen, Fmt.txType(t.loai),
                    t.dichVu, "CHECKIN".equals(t.loai) ? String.format("Phòng %04X", t.giaTri) : Fmt.money(t.giaTri)});
            }
        });
        onCardChanged();
    }

    public void onCardChanged() {
        CardSession s = ctx.session;
        CardClient.CardStatus st = s.status();
        if (!s.isConnected() || st == null) {
            cardInfo.setText("<html>Chưa có thẻ.<br>Bấm <b>Kết nối thẻ</b> ở góc trên.</html>");
            return;
        }
        cardInfo.setText("<html><table cellpadding='3'>"
                + "<tr><td>Mã SV</td><td><b>" + (st.cardId.isEmpty() ? "—" : st.cardId) + "</b></td></tr>"
                + "<tr><td>Trạng thái</td><td><b>" + (st.state == 0 ? "Thẻ trắng" : st.state == 1 ? "Đang hoạt động" : "Bị khóa") + "</b></td></tr>"
                + "<tr><td>Xác thực RSA</td><td><b>" + (s.isGenuine() ? "Thẻ thật" : "Chưa / không đạt") + "</b></td></tr>"
                + "<tr><td>PIN còn</td><td>" + st.pinTries + " / 3 lần</td></tr>"
                + "<tr><td>PUK còn</td><td>" + st.pukTries + " / 5 lần</td></tr>"
                + "<tr><td>Dữ liệu</td><td>" + (st.infoLen + st.contactLen + st.photoLen) + " byte đã mã hóa</td></tr>"
                + "</table></html>");
    }
}
