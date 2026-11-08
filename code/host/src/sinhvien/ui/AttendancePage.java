package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import sinhvien.host.CardClient;
import sinhvien.host.CardRepository;
import sinhvien.host.ProfileTlv;

/**
 * Điểm danh / ra vào bằng quẹt thẻ: không cần PIN để nhanh, nhưng thẻ phải KÝ (mã phòng, thời gian, nonce)
 * nên không ai điểm danh hộ được nếu không có thẻ thật. Chức năng riêng của nhóm 1.
 */
public class AttendancePage extends Page {

    private final JComboBox<CardRepository.Room> room = new JComboBox<CardRepository.Room>();
    private final Ui.PhotoBox photo = new Ui.PhotoBox(120, 160);
    private final JLabel who = Ui.bold("Mời quẹt thẻ", 20);
    private final JLabel whoSub = Ui.muted(" ");
    private final Ui.Chip verdict = new Ui.Chip("Đang chờ");
    private final JLabel count = Ui.bold("0", 28);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Giờ", "Mã SV", "Họ tên", "Bộ đếm thẻ"}, 0) {
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    public AttendancePage(Ctx ctx) {
        super(ctx);
        JButton swipe = Ui.button("Quẹt thẻ điểm danh", Ui.Kind.PRIMARY, Icons.of(Icons.Kind.CHECK, 20, Color.WHITE));
        swipe.setFont(Theme.font(Font.BOLD, 16));
        swipe.setPreferredSize(new Dimension(260, 50));
        swipe.addActionListener(e -> swipe());
        room.addActionListener(e -> loadList());
        Ui.Form f = new Ui.Form(1);
        f.add("Phòng / buổi học", room);

        JPanel last = Ui.transparent(new BorderLayout(16, 0));
        JPanel pw = Ui.transparent(new BorderLayout());
        pw.add(photo, BorderLayout.NORTH);
        last.add(pw, BorderLayout.WEST);
        last.add(Ui.vstack(8, who, whoSub, Ui.row(0, verdict)), BorderLayout.CENTER);

        JPanel left = Ui.vstack(18, f, last, Ui.row(0, swipe));
        Ui.RoundPanel lc = Ui.card("Máy điểm danh", "Thẻ ký: 03 | mã SV | mã phòng | thời gian | nonce – kiểm bằng khóa công khai trong CSDL", left);

        JPanel rh = Ui.transparent(new BorderLayout());
        rh.add(Ui.muted("Đã điểm danh hôm nay"), BorderLayout.WEST);
        rh.add(count, BorderLayout.EAST);
        JPanel rb = Ui.transparent(new BorderLayout(0, 10));
        rb.add(rh, BorderLayout.NORTH);
        rb.add(Ui.scroll(Ui.table(model)), BorderLayout.CENTER);
        Ui.RoundPanel rc = Ui.card("Danh sách", null, rb);

        JPanel body = Ui.transparent(new BorderLayout(16, 0));
        body.add(lc, BorderLayout.CENTER);
        body.add(Ui.fixedWidth(rc, 470), BorderLayout.EAST);
        body.setPreferredSize(new Dimension(10, 470));
        add(body, BorderLayout.CENTER);
    }

    public String title() {
        return "Điểm danh";
    }

    public String subtitle() {
        return "Quẹt thẻ – thẻ ký thời điểm điểm danh, chống điểm danh hộ";
    }

    public Icons.Kind icon() {
        return Icons.Kind.CHECK;
    }

    public void onShow() {
        if (room.getItemCount() == 0) {
            Task.run(this, "Đang tải danh sách phòng…", () -> ctx.repo.rooms(), list -> {
                int def = ctx.cfg.defaultRoom();
                for (CardRepository.Room r : list) {
                    room.addItem(r);
                    if (r.code == def) {
                        room.setSelectedItem(r);
                    }
                }
                loadList();
            });
        } else {
            loadList();
        }
    }

    private void loadList() {
        final CardRepository.Room r = (CardRepository.Room) room.getSelectedItem();
        if (r == null) {
            return;
        }
        Task.run(this, null, () -> ctx.repo.checkins(r.code, Fmt.startOfToday()), (List<CardRepository.TxRow> list) -> {
            model.setRowCount(0);
            for (CardRepository.TxRow t : list) {
                model.addRow(new Object[] {Fmt.hm(t.thoiGian), t.maSv, t.hoTen, t.boDem});
            }
            count.setText(String.valueOf(list.size()));
        });
    }

    void swipe() {
        final CardRepository.Room r = (CardRepository.Room) room.getSelectedItem();
        try {
            ctx.session.requireGenuine();
            if (r == null) {
                throw new Task.UserError("Chưa chọn phòng");
            }
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        Task.run(this, "Đang điểm danh…", () -> {
            String id = ctx.session.cardId();
            CardClient.Receipt rc = ctx.session.client.checkIn(r.code);
            CardRepository.RecordResult res = ctx.repo.recordReceipt(id, rc, r.name);
            return new Object[] {rc, res, ctx.repo.findStudent(id), ctx.repo.findPhoto(id)};
        }, o -> {
            CardClient.Receipt rc = (CardClient.Receipt) o[0];
            CardRepository.RecordResult res = (CardRepository.RecordResult) o[1];
            @SuppressWarnings("unchecked")
            java.util.Map<Integer, String> m = (java.util.Map<Integer, String>) o[2];
            who.setText(m == null ? ctx.session.cardId() : m.get(ProfileTlv.HO_TEN));
            whoSub.setText(ctx.session.cardId() + " · " + Fmt.time(rc.time()) + " · bộ đếm " + rc.counter);
            photo.setImage(ImageUtil.read((byte[]) o[3]));
            if (res == CardRepository.RecordResult.OK) {
                verdict.set("Đã điểm danh · chữ ký hợp lệ", Theme.SUCCESS);
            } else {
                verdict.set("Từ chối: " + res, Theme.DANGER);
            }
            loadList();
        });
    }
}
