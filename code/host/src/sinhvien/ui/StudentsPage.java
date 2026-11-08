package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import sinhvien.host.CardRepository;
import sinhvien.host.ProfileTlv;

/** Danh sách sinh viên trong CSDL, báo mất / mở khóa (CSDL và trên thẻ đang cắm), lịch sử. */
public class StudentsPage extends Page {

    private final JTextField search = Ui.field("Tìm theo mã SV, họ tên, lớp…", 24);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[] {"Mã SV", "Họ tên", "Lớp", "Khoa / ngành", "Trạng thái thẻ", "Số dư"}, 0) {
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable table = Ui.table(model);
    private final List<CardRepository.StudentRow> rows = new ArrayList<CardRepository.StudentRow>();
    private final Ui.PhotoBox photo = new Ui.PhotoBox(120, 160);
    private final JLabel detail = new JLabel("Chọn một sinh viên");

    public StudentsPage(Ctx ctx) {
        super(ctx);
        JButton find = Ui.button("Tìm", Ui.Kind.DEFAULT, Icons.of(Icons.Kind.SEARCH, 16));
        find.addActionListener(e -> onShow());
        search.addActionListener(e -> onShow());
        JPanel top = Ui.transparent(new BorderLayout(8, 0));
        top.add(search, BorderLayout.CENTER);
        top.add(find, BorderLayout.EAST);

        Ui.widths(table, 90, 190, 60, 160, 110, 100);
        table.getColumnModel().getColumn(4).setCellRenderer(new StatusRenderer());
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDetail();
            }
        });
        JPanel listBody = Ui.transparent(new BorderLayout(0, 12));
        listBody.add(top, BorderLayout.NORTH);
        listBody.add(Ui.scroll(table), BorderLayout.CENTER);
        Ui.RoundPanel list = Ui.card("Sinh viên & thẻ", "Trạng thái thẻ lấy từ bảng the (lần phát hành mới nhất)", listBody);

        // chi tiết + thao tác
        JButton lostDb = Ui.button("Báo mất (khóa trong CSDL)", Ui.Kind.DANGER, Icons.of(Icons.Kind.LOCK, 16, Color.WHITE));
        lostDb.addActionListener(e -> setDbStatus("LOCKED"));
        JButton unlockDb = Ui.button("Mở khóa trong CSDL", Ui.Kind.DEFAULT, Icons.of(Icons.Kind.UNLOCK, 16));
        unlockDb.addActionListener(e -> setDbStatus("ACTIVE"));
        JButton lockCard = Ui.button("Khóa thẻ đang cắm", Ui.Kind.DEFAULT, Icons.of(Icons.Kind.LOCK, 16));
        lockCard.addActionListener(e -> lockOnCard(true));
        JButton unlockCard = Ui.button("Mở khóa thẻ đang cắm", Ui.Kind.DEFAULT, Icons.of(Icons.Kind.UNLOCK, 16));
        unlockCard.addActionListener(e -> lockOnCard(false));
        JButton hist = Ui.button("Lịch sử giao dịch", Ui.Kind.GHOST, Icons.of(Icons.Kind.WALLET, 16));
        hist.addActionListener(e -> showHistory());

        detail.setVerticalAlignment(JLabel.TOP);
        JPanel head = Ui.transparent(new BorderLayout(14, 0));
        head.add(photo, BorderLayout.WEST);
        head.add(detail, BorderLayout.CENTER);
        JPanel db = Ui.vstack(10, head, Ui.muted("CSDL (không cần thẻ)"), lostDb, unlockDb,
                Ui.muted("Thẻ đang cắm (cần PIN quản trị)"), lockCard, unlockCard, hist);
        Ui.RoundPanel right = Ui.card("Chi tiết", "Báo mất chỉ cần CSDL; khóa trên thẻ cần cắm thẻ + PIN quản trị", db);

        JPanel body = Ui.transparent(new BorderLayout(16, 0));
        body.add(list, BorderLayout.CENTER);
        body.add(Ui.fixedWidth(right, 360), BorderLayout.EAST);
        body.setPreferredSize(new Dimension(10, 600));
        add(body, BorderLayout.CENTER);
    }

    public String title() {
        return "Sinh viên & thẻ";
    }

    public String subtitle() {
        return "Hồ sơ trong CSDL, báo mất / khóa thẻ, lịch sử giao dịch";
    }

    public Icons.Kind icon() {
        return Icons.Kind.USERS;
    }

    public void onShow() {
        final String k = search.getText();
        Task.run(this, "Đang tải danh sách…", () -> ctx.repo.listStudents(k), list -> {
            rows.clear();
            rows.addAll(list);
            model.setRowCount(0);
            for (CardRepository.StudentRow r : list) {
                model.addRow(new Object[] {r.maSv, r.hoTen, r.lop, r.khoaNganh, r.trangThaiThe,
                    r.soDu < 0 ? "—" : Fmt.money(r.soDu)});
            }
            if (!list.isEmpty()) {
                table.setRowSelectionInterval(0, 0);
            }
        });
    }

    private CardRepository.StudentRow selected() {
        int v = table.getSelectedRow();
        return v < 0 ? null : rows.get(table.convertRowIndexToModel(v));
    }

    private void showDetail() {
        final CardRepository.StudentRow r = selected();
        if (r == null) {
            return;
        }
        Task.run(this, null, () -> new Object[] {ctx.repo.findStudent(r.maSv), ctx.repo.findPhoto(r.maSv)}, o -> {
            @SuppressWarnings("unchecked")
            Map<Integer, String> m = (Map<Integer, String>) o[0];
            photo.setImage(ImageUtil.read((byte[]) o[1]));
            if (m == null) {
                return;
            }
            detail.setText("<html><b style='font-size:13px'>" + m.get(ProfileTlv.HO_TEN) + "</b><br>" + r.maSv
                    + " · " + nz(m.get(ProfileTlv.LOP)) + "<br>Sinh " + nz(m.get(ProfileTlv.NGAY_SINH))
                    + "<br>" + nz(m.get(ProfileTlv.QUE_QUAN)) + "<br>Hạn thẻ " + nz(m.get(ProfileTlv.HAN_THE))
                    + "<br><br>Thẻ: <b>" + r.trangThaiThe + "</b>"
                    + (r.lyDoKhoa == null ? "" : "<br>Lý do: " + r.lyDoKhoa) + "</html>");
        });
    }

    private static String nz(String s) {
        return s == null ? "—" : s;
    }

    private void setDbStatus(final String status) {
        final CardRepository.StudentRow r = selected();
        if (r == null) {
            return;
        }
        String reason = null;
        if ("LOCKED".equals(status)) {
            reason = Dialogs.input(this, "Báo mất thẻ " + r.maSv, "Lý do khóa (vd: Sinh viên báo mất ngày …):");
            if (reason == null) {
                return;
            }
        }
        final String why = reason;
        Task.run(this, "Đang cập nhật CSDL…", () -> {
            ctx.repo.setCardStatus(r.maSv, status, why);
            ctx.session.refreshQuietly();
            return null;
        }, v -> onShow());
    }

    private void lockOnCard(final boolean lock) {
        try {
            ctx.session.requireConnected();
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        final String admin = askAdminPin();
        if (admin == null) {
            return;
        }
        Task.run(this, lock ? "Đang khóa thẻ…" : "Đang mở khóa thẻ…", () -> {
            ctx.session.client.verifyAdmin(admin);
            ctx.session.client.setLocked(lock);
            ctx.session.refresh();
            return null;
        }, v -> {
            Dialogs.info(this, "Xong", lock ? "Thẻ đã bị khóa. Lần kết nối sau thẻ trả 64 00." : "Thẻ đã mở khóa.");
            onShow();
        });
    }

    private void showHistory() {
        final CardRepository.StudentRow r = selected();
        if (r == null) {
            return;
        }
        Task.run(this, "Đang tải lịch sử…", () -> ctx.repo.history(r.maSv, 200), list -> {
            DefaultTableModel m = new DefaultTableModel(new Object[] {"Thời gian", "Loại", "Dịch vụ", "Giá trị", "Số dư sau", "Bộ đếm"}, 0);
            for (CardRepository.TxRow t : list) {
                m.addRow(new Object[] {Fmt.time(t.thoiGian), Fmt.txType(t.loai), t.dichVu,
                    "CHECKIN".equals(t.loai) ? String.format("Phòng %04X", t.giaTri) : Fmt.money(t.giaTri),
                    Fmt.money(t.soDuSau), t.boDem});
            }
            JTable t = Ui.table(m);
            javax.swing.JScrollPane sp = Ui.scroll(t);
            sp.setPreferredSize(new Dimension(760, 360));
            javax.swing.JOptionPane.showMessageDialog(this, sp, "Lịch sử – " + r.maSv + " " + r.hoTen,
                    javax.swing.JOptionPane.PLAIN_MESSAGE);
        });
    }

    /** Tô màu cột trạng thái thẻ. */
    private static class StatusRenderer extends DefaultTableCellRenderer {
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, focus, row, col);
            l.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 10, 0, 10));
            String s = String.valueOf(v);
            if (!sel) {
                l.setForeground("ACTIVE".equals(s) ? Theme.SUCCESS : "LOCKED".equals(s) ? Theme.DANGER : Theme.muted());
            }
            l.setFont(l.getFont().deriveFont(java.awt.Font.BOLD));
            return l;
        }
    }
}
