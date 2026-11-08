package sinhvien.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

/** Biểu đồ cột nhóm đơn giản (vẽ tay, không cần thư viện): mỗi ngày 2 cột nạp / chi. */
public class BarChart extends JComponent {

    private List<String> labels = new ArrayList<String>();
    private List<long[]> values = new ArrayList<long[]>();
    private String[] series = {"Nạp", "Chi"};
    private Color[] colors = {Theme.SUCCESS, Theme.ACCENT};

    public BarChart() {
        setPreferredSize(new Dimension(520, 260));
    }

    public void setData(List<String> labels, List<long[]> values) {
        this.labels = labels;
        this.values = values;
        repaint();
    }

    protected void paintComponent(Graphics g0) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int left = 64, right = 12, top = 30, bottom = 28;
        int w = getWidth() - left - right, h = getHeight() - top - bottom;
        long max = 1;
        for (long[] v : values) {
            for (long x : v) {
                max = Math.max(max, x);
            }
        }
        long step = niceStep(max / 4.0);
        long top2 = ((max + step - 1) / step) * step;
        g.setFont(Theme.font(Font.PLAIN, 11.5f));
        FontMetrics fm = g.getFontMetrics();
        // lưới ngang
        for (long y = 0; y <= top2; y += step) {
            int py = top + h - (int) (h * y / (double) top2);
            g.setColor(Theme.border());
            g.setStroke(new BasicStroke(1f));
            g.drawLine(left, py, left + w, py);
            g.setColor(Theme.muted());
            String s = y >= 1000 ? (y / 1000) + " tr" : y + " k";
            g.drawString(s, left - 8 - fm.stringWidth(s), py + fm.getAscent() / 2 - 1);
        }
        // cột
        int n = Math.max(1, labels.size());
        double slot = w / (double) n;
        int bw = (int) Math.max(6, Math.min(26, slot / 3.2));
        for (int i = 0; i < labels.size(); i++) {
            long[] v = values.get(i);
            double cx = left + slot * i + slot / 2;
            for (int s = 0; s < v.length; s++) {
                int bh = (int) (h * v[s] / (double) top2);
                int x = (int) (cx - bw * v.length / 2.0 + s * bw) + s * 2;
                g.setColor(colors[s % colors.length]);
                g.fillRoundRect(x, top + h - bh, bw, Math.max(bh, 1), 6, 6);
            }
            g.setColor(Theme.muted());
            String l = labels.get(i);
            g.drawString(l, (int) (cx - fm.stringWidth(l) / 2.0), top + h + fm.getAscent() + 8);
        }
        // chú giải
        int lx = left;
        for (int s = 0; s < series.length; s++) {
            g.setColor(colors[s]);
            g.fillRoundRect(lx, 8, 12, 12, 4, 4);
            g.setColor(Theme.text());
            g.drawString(series[s] + " (nghìn đồng)", lx + 18, 18);
            lx += 18 + fm.stringWidth(series[s] + " (nghìn đồng)") + 20;
        }
        g.dispose();
    }

    static long niceStep(double raw) {
        if (raw <= 1) {
            return 1;
        }
        double p = Math.pow(10, Math.floor(Math.log10(raw)));
        double f = raw / p;
        double n = f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10;
        return (long) (n * p);
    }
}
