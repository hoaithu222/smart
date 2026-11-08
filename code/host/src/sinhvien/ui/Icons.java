package sinhvien.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import javax.swing.Icon;

/** Biểu tượng vẽ bằng Graphics2D (nét 24×24, không cần file ảnh). */
public final class Icons implements Icon {

    public enum Kind {
        HOME, CARD, USERS, USER, CHART, KEY, WALLET, CART, CHECK, SHIELD, LOGOUT, PLUG, MOON, SUN, TERMINAL,
        LOCK, UNLOCK, REFRESH, PLUS, PHOTO, SEARCH, CHIP
    }

    private final Kind kind;
    private final int size;
    private final Color color;

    private Icons(Kind kind, int size, Color color) {
        this.kind = kind;
        this.size = size;
        this.color = color;
    }

    public static Icon of(Kind k, int size) {
        return new Icons(k, size, null);
    }

    public static Icon of(Kind k, int size, Color color) {
        return new Icons(k, size, color);
    }

    public int getIconWidth() {
        return size;
    }

    public int getIconHeight() {
        return size;
    }

    public void paintIcon(Component c, Graphics g0, int x, int y) {
        Graphics2D g = (Graphics2D) g0.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.translate(x, y);
        g.scale(size / 24.0, size / 24.0);
        g.setColor(color != null ? color : (c != null ? c.getForeground() : Color.DARK_GRAY));
        g.setStroke(new BasicStroke(1.9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
        case HOME:
            path(g, 3, 11, 12, 3.5, 21, 11);
            g.draw(new RoundRectangle2D.Double(5.5, 10, 13, 10.5, 2, 2));
            g.draw(new Line2D.Double(12, 15, 12, 20.5));
            break;
        case CARD:
            g.draw(new RoundRectangle2D.Double(2.5, 5, 19, 14, 3, 3));
            g.draw(new Line2D.Double(2.5, 9.5, 21.5, 9.5));
            g.draw(new Line2D.Double(6, 15, 10, 15));
            break;
        case CHIP:
            g.draw(new RoundRectangle2D.Double(4, 5, 16, 14, 4, 4));
            g.draw(new Line2D.Double(4, 10, 9, 10));
            g.draw(new Line2D.Double(4, 14, 9, 14));
            g.draw(new Line2D.Double(15, 10, 20, 10));
            g.draw(new Line2D.Double(15, 14, 20, 14));
            g.draw(new RoundRectangle2D.Double(9, 8, 6, 8, 2, 2));
            break;
        case USERS:
            g.draw(new Ellipse2D.Double(5, 4.5, 7, 7));
            g.draw(new Arc2D.Double(2, 13, 13, 12, 0, 180, Arc2D.OPEN));
            g.draw(new Arc2D.Double(13, 5, 6, 6, -90, 270, Arc2D.OPEN));
            g.draw(new Arc2D.Double(14, 13.5, 8, 9, 0, 120, Arc2D.OPEN));
            break;
        case USER:
            g.draw(new Ellipse2D.Double(8, 3.5, 8, 8));
            g.draw(new Arc2D.Double(4, 13, 16, 14, 0, 180, Arc2D.OPEN));
            break;
        case CHART:
            g.draw(new Line2D.Double(3.5, 20.5, 20.5, 20.5));
            g.draw(new RoundRectangle2D.Double(5.5, 12, 3, 8.5, 1, 1));
            g.draw(new RoundRectangle2D.Double(10.5, 7, 3, 13.5, 1, 1));
            g.draw(new RoundRectangle2D.Double(15.5, 10, 3, 10.5, 1, 1));
            break;
        case KEY:
            g.draw(new Ellipse2D.Double(3, 8, 8, 8));
            path(g, 11, 12, 21, 12, 21, 15);
            g.draw(new Line2D.Double(17.5, 12, 17.5, 14.5));
            break;
        case WALLET:
            g.draw(new RoundRectangle2D.Double(3, 6, 18, 14, 3, 3));
            path(g, 5, 6, 16, 3.5, 17.5, 6);
            g.draw(new RoundRectangle2D.Double(14, 11, 7, 4.5, 2, 2));
            break;
        case CART:
            path(g, 2.5, 4, 5.5, 4, 8, 15.5, 18.5, 15.5, 20.5, 8, 6.5, 8);
            g.fill(new Ellipse2D.Double(8, 18, 2.6, 2.6));
            g.fill(new Ellipse2D.Double(16, 18, 2.6, 2.6));
            break;
        case CHECK:
            g.draw(new RoundRectangle2D.Double(3.5, 3.5, 17, 17, 4, 4));
            path(g, 8, 12.5, 11, 15.5, 16.5, 9);
            break;
        case SHIELD:
            path(g, 12, 3, 19.5, 6, 19.5, 11.5);
            g.draw(new Arc2D.Double(4.5, 1.5, 15, 20, 270, 90, Arc2D.OPEN));
            path(g, 12, 3, 4.5, 6, 4.5, 11.5);
            g.draw(new Arc2D.Double(4.5, 1.5, 15, 20, 180, 90, Arc2D.OPEN));
            path(g, 9, 12, 11.2, 14.2, 15.5, 9.8);
            break;
        case LOGOUT:
            path(g, 14, 4, 5, 4, 5, 20, 14, 20);
            g.draw(new Line2D.Double(10, 12, 21, 12));
            path(g, 17.5, 8.5, 21, 12, 17.5, 15.5);
            break;
        case PLUG:
            g.draw(new Line2D.Double(9, 3, 9, 7.5));
            g.draw(new Line2D.Double(15, 3, 15, 7.5));
            path(g, 6, 7.5, 18, 7.5, 18, 11);
            g.draw(new Arc2D.Double(6, 5, 12, 12, 180, 180, Arc2D.OPEN));
            g.draw(new Line2D.Double(6, 7.5, 6, 11));
            g.draw(new Line2D.Double(12, 17, 12, 21));
            break;
        case MOON:
            g.draw(new Arc2D.Double(4, 4, 16, 16, 60, 270, Arc2D.OPEN));
            g.draw(new Arc2D.Double(10, 2.5, 11, 11, 150, 145, Arc2D.OPEN));
            break;
        case SUN:
            g.draw(new Ellipse2D.Double(8, 8, 8, 8));
            for (int i = 0; i < 8; i++) {
                double a = Math.PI / 4 * i;
                g.draw(new Line2D.Double(12 + Math.cos(a) * 7, 12 + Math.sin(a) * 7,
                        12 + Math.cos(a) * 9.5, 12 + Math.sin(a) * 9.5));
            }
            break;
        case TERMINAL:
            g.draw(new RoundRectangle2D.Double(2.5, 4, 19, 16, 3, 3));
            path(g, 6.5, 9, 9.5, 12, 6.5, 15);
            g.draw(new Line2D.Double(11.5, 15.5, 16.5, 15.5));
            break;
        case LOCK:
        case UNLOCK:
            g.draw(new RoundRectangle2D.Double(4.5, 10.5, 15, 10, 3, 3));
            g.draw(new Arc2D.Double(7.5, 3.5, 9, 11, 0, kind == Kind.LOCK ? 180 : 150, Arc2D.OPEN));
            g.draw(new Line2D.Double(7.5, 9, 7.5, 10.5));
            if (kind == Kind.LOCK) {
                g.draw(new Line2D.Double(16.5, 9, 16.5, 10.5));
            }
            break;
        case REFRESH:
            g.draw(new Arc2D.Double(4.5, 4.5, 15, 15, 40, 290, Arc2D.OPEN));
            path(g, 19, 3.5, 19, 8, 14.5, 8);
            break;
        case PLUS:
            g.draw(new Line2D.Double(12, 5, 12, 19));
            g.draw(new Line2D.Double(5, 12, 19, 12));
            break;
        case PHOTO:
            g.draw(new RoundRectangle2D.Double(3, 5, 18, 14, 3, 3));
            g.draw(new Ellipse2D.Double(7, 8, 3.5, 3.5));
            path(g, 4, 17.5, 10, 12.5, 14, 15.5, 17, 13, 20.5, 16.5);
            break;
        case SEARCH:
            g.draw(new Ellipse2D.Double(4, 4, 12, 12));
            g.draw(new Line2D.Double(14.5, 14.5, 20, 20));
            break;
        default:
            break;
        }
        g.dispose();
    }

    private static void path(Graphics2D g, double... xy) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(xy[0], xy[1]);
        for (int i = 2; i < xy.length; i += 2) {
            p.lineTo(xy[i], xy[i + 1]);
        }
        g.draw(p);
    }
}
