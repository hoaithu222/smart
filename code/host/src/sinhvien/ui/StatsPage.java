package sinhvien.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;
import javax.swing.table.DefaultTableModel;
import sinhvien.host.CardRepository;

/** Thống kê toàn trường từ CSDL: tổng nạp / chi, biểu đồ 7 ngày, chi theo dịch vụ. */
public class StatsPage extends Page {

    private final Ui.StatTile topup = new Ui.StatTile("Tổng nạp", Icons.of(Icons.Kind.PLUS, 24), Theme.SUCCESS);
    private final Ui.StatTile pay = new Ui.StatTile("Tổng chi", Icons.of(Icons.Kind.CART, 24), Theme.ACCENT);
    private final Ui.StatTile float_ = new Ui.StatTile("Tiền đang nằm trên thẻ", Icons.of(Icons.Kind.WALLET, 24), Theme.ACCENT_2);
    private final Ui.StatTile checkins = new Ui.StatTile("Lượt điểm danh 7 ngày", Icons.of(Icons.Kind.CHECK, 24), Theme.WARNING);
    private final BarChart chart = new BarChart();
    private final DefaultTableModel byService = new DefaultTableModel(new Object[] {"Dịch vụ", "Tổng chi"}, 0) {
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    public StatsPage(Ctx ctx) {
        super(ctx);
        JPanel tiles = Ui.transparent(new GridLayout(1, 4, 16, 0));
        tiles.add(topup);
        tiles.add(pay);
        tiles.add(float_);
        tiles.add(checkins);
        add(tiles, BorderLayout.NORTH);
        chart.setPreferredSize(new Dimension(600, 320));
        JPanel body = Ui.transparent(new BorderLayout(16, 0));
        body.add(Ui.card("Nạp và chi 7 ngày gần nhất", "Đơn vị nghìn đồng, theo giờ Việt Nam", chart), BorderLayout.CENTER);
        Ui.RoundPanel svc = Ui.card("Chi theo dịch vụ", null, Ui.scroll(Ui.table(byService)));
        body.add(Ui.fixedWidth(svc, 340), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    public String title() {
        return "Thống kê";
    }

    public String subtitle() {
        return "Số liệu từ CSDL (chỉ gồm biên nhận đã kiểm chữ ký)";
    }

    public Icons.Kind icon() {
        return Icons.Kind.CHART;
    }

    public void onShow() {
        Task.run(this, "Đang tổng hợp…", () -> new Object[] {
            ctx.repo.sumByType("TOPUP"), ctx.repo.sumByType("PAY"),
            ctx.repo.dailyTotals(7, System.currentTimeMillis() / 1000L), ctx.repo.payByService()}, o -> {
            long t = (Long) o[0], p = (Long) o[1];
            topup.setValue(Fmt.money(t));
            pay.setValue(Fmt.money(p));
            float_.setValue(Fmt.money(t - p));
            @SuppressWarnings("unchecked")
            List<CardRepository.DayTotal> days = (List<CardRepository.DayTotal>) o[2];
            List<String> labels = new ArrayList<String>();
            List<long[]> values = new ArrayList<long[]>();
            int ci = 0;
            for (CardRepository.DayTotal d : days) {
                labels.add(Fmt.day(d.dayStart));
                values.add(new long[] {d.topup, d.pay});
                ci += d.checkins;
            }
            checkins.setValue(String.valueOf(ci));
            chart.setData(labels, values);
            byService.setRowCount(0);
            @SuppressWarnings("unchecked")
            Map<String, Long> s = (Map<String, Long>) o[3];
            for (Map.Entry<String, Long> e : s.entrySet()) {
                byService.addRow(new Object[] {e.getKey(), Fmt.money(e.getValue())});
            }
        });
    }
}
