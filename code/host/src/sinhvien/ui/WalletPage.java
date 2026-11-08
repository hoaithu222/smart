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
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import sinhvien.host.CardClient;
import sinhvien.host.CardRepository;

/**
 * Ví sinh viên: số dư và bộ đếm đọc từ THẺ (nguồn đúng), nhật ký 10 bản ghi trên thẻ,
 * lịch sử đầy đủ trong CSDL, cảnh báo đối soát khi thẻ có giao dịch chưa ghi sổ.
 */
public class WalletPage extends Page {

    private final JLabel balance = new JLabel("—");
    private final JLabel counter = new JLabel(" ");
    private final Ui.Chip recon = new Ui.Chip("Chưa đối soát");
    private final DefaultTableModel cardLog = model("Bộ đếm", "Loại", "Giá trị", "Số dư sau", "Thời gian");
    private final DefaultTableModel dbLog = model("Bộ đếm", "Thời gian", "Loại", "Dịch vụ / phòng", "Giá trị");

    public WalletPage(Ctx ctx) {
        super(ctx);
        JPanel hero = new JPanel(new BorderLayout(20, 0)) {
            protected void paintComponent(Graphics g0) {
                Graphics2D g = (Graphics2D) g0.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setPaint(new GradientPaint(0, 0, Theme.ACCENT, getWidth(), getHeight(), Theme.ACCENT_2));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g.setColor(new Color(255, 255, 255, 40));
                g.fillOval(getWidth() - 220, -90, 300, 300);
                g.dispose();
            }
        };
        hero.setOpaque(false);
        hero.setBorder(BorderFactory.createEmptyBorder(22, 26, 22, 26));
        JPanel ht = Ui.transparent(new BorderLayout(0, 4));
        JLabel cap = new JLabel("Số dư trên thẻ");
        cap.setForeground(new Color(255, 255, 255, 210));
        cap.setFont(Theme.font(Font.PLAIN, 14));
        balance.setForeground(Color.WHITE);
        balance.setFont(Theme.font(Font.BOLD, 34));
        counter.setForeground(new Color(255, 255, 255, 210));
        counter.setFont(Theme.font(Font.PLAIN, 13));
        ht.add(cap, BorderLayout.NORTH);
        ht.add(balance, BorderLayout.CENTER);
        ht.add(counter, BorderLayout.SOUTH);
        hero.add(ht, BorderLayout.CENTER);
        JButton refresh = Ui.button("Đọc số dư (nhập PIN)", Ui.Kind.DEFAULT, Icons.of(Icons.Kind.REFRESH, 16));
        refresh.addActionListener(e -> withPin(this::load));
        JPanel hr = Ui.transparent(new GridLayout(0, 1, 0, 10));
        hr.add(refresh);
        hr.add(recon);
        hero.add(hr, BorderLayout.EAST);
        hero.setPreferredSize(new Dimension(10, 150));
        add(hero, BorderLayout.NORTH);

        JPanel tables = Ui.transparent(new GridLayout(1, 2, 16, 0));
        javax.swing.JTable ct = Ui.table(cardLog);
        Ui.widths(ct, 60, 90, 100, 100, 130);
        javax.swing.JTable dt = Ui.table(dbLog);
        Ui.widths(dt, 60, 130, 90, 120, 100);
        tables.add(Ui.card("Nhật ký trên thẻ", "10 bản ghi gần nhất, bộ đệm vòng trong EEPROM", Ui.scroll(ct)));
        tables.add(Ui.card("Lịch sử trong CSDL", "Biên nhận đã kiểm chữ ký RSA", Ui.scroll(dt)));
        tables.setPreferredSize(new Dimension(10, 380));
        add(tables, BorderLayout.CENTER);
    }

    private static DefaultTableModel model(Object... cols) {
        return new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
    }

    public String title() {
        return "Ví & lịch sử";
    }

    public String subtitle() {
        return "Số dư, nhật ký giao dịch trên thẻ và trong CSDL";
    }

    public Icons.Kind icon() {
        return Icons.Kind.WALLET;
    }

    public void onShow() {
        if (ctx.session.isConnected() && ctx.session.isPinVerified()) {
            load();
        }
    }

    public void onCardChanged() {
        if (!ctx.session.isConnected()) {
            balance.setText("—");
            counter.setText(" ");
            recon.set("Chưa đối soát", Theme.muted());
            cardLog.setRowCount(0);
            dbLog.setRowCount(0);
        }
    }

    void load() {
        Task.run(this, "Đang đọc ví…", () -> {
            CardClient c = ctx.session.client;
            String id = ctx.session.cardId();
            return new Object[] {c.balance(), c.log(), ctx.repo.history(id, 100), ctx.repo.lastCounter(id)};
        }, o -> {
            int[] b = (int[]) o[0];
            @SuppressWarnings("unchecked")
            List<CardClient.LogEntry> log = (List<CardClient.LogEntry>) o[1];
            @SuppressWarnings("unchecked")
            List<CardRepository.TxRow> hist = (List<CardRepository.TxRow>) o[2];
            int dbCounter = (Integer) o[3];
            balance.setText(Fmt.money(b[0]));
            counter.setText("Bộ đếm giao dịch trên thẻ: " + b[1] + "  ·  CSDL đã ghi tới: " + dbCounter);
            if (b[1] > dbCounter) {
                recon.set("Thẻ có " + (b[1] - dbCounter) + " giao dịch chưa ghi sổ", Theme.WARNING);
            } else {
                recon.set("Khớp CSDL", Theme.SUCCESS);
            }
            cardLog.setRowCount(0);
            for (CardClient.LogEntry e : log) {
                cardLog.addRow(new Object[] {e.counter, Fmt.txType(e.type),
                    e.type == CardClient.TX_CHECKIN ? String.format("Phòng %04X", e.value) : Fmt.money(e.value),
                    Fmt.money(e.balance), Fmt.shortTime(e.time)});
            }
            dbLog.setRowCount(0);
            for (CardRepository.TxRow t : hist) {
                dbLog.addRow(new Object[] {t.boDem, Fmt.shortTime(t.thoiGian), Fmt.txType(t.loai), t.dichVu,
                    "CHECKIN".equals(t.loai) ? String.format("Phòng %04X", t.giaTri) : Fmt.money(t.giaTri)});
            }
        });
    }
}
