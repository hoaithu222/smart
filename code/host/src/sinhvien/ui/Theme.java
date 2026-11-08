package sinhvien.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import javax.swing.UIManager;
import sinhvien.host.AppConfig;

/**
 * Bảng màu + phông chữ dùng chung. Màu nền/thẻ/viền đọc lúc vẽ (theo sáng/tối),
 * nên đổi giao diện sáng ↔ tối không cần mở lại cửa sổ.
 * Giao diện dựa trên FlatLaf (vẫn là Java Swing); thiếu flatlaf.jar thì tự lùi về giao diện Windows.
 */
public final class Theme {

    public static final Color ACCENT = new Color(0x2F5BEA);
    public static final Color ACCENT_2 = new Color(0x14B8A6);
    public static final Color SUCCESS = new Color(0x16A34A);
    public static final Color DANGER = new Color(0xDC2626);
    public static final Color WARNING = new Color(0xD97706);
    public static final Color SIDEBAR_TOP = new Color(0x0B1633);
    public static final Color SIDEBAR_BOTTOM = new Color(0x13285C);
    public static final Color SIDEBAR_TEXT = new Color(0xC7D2FE);

    private static boolean dark;
    private static boolean flat;
    private static String fontName = "Segoe UI";
    private static int fontSize = 14;

    private Theme() {
    }

    public static void install(AppConfig cfg) {
        fontName = cfg.uiFont();
        fontSize = cfg.uiFontSize();
        String t = cfg.uiTheme();
        try {
            if ("system".equals(t)) {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } else {
                Map<String, String> extra = new HashMap<String, String>();
                extra.put("@accentColor", "#2F5BEA");
                FlatLaf.setGlobalExtraDefaults(extra);
                dark = "dark".equals(t);
                setupFlat();
                flat = true;
            }
        } catch (Throwable e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // giữ giao diện mặc định
            }
        }
        Font base = new Font(fontName, Font.PLAIN, fontSize);
        UIManager.put("defaultFont", base);
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 10);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("Table.rowHeight", 32);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("Table.intercellSpacing", new java.awt.Dimension(0, 1));
        UIManager.put("TableHeader.height", 36);
        UIManager.put("TabbedPane.showTabSeparators", true);
        if (!flat) {
            for (Object k : UIManager.getLookAndFeelDefaults().keySet()) {
                if (k.toString().endsWith(".font")) {
                    UIManager.put(k, base);
                }
            }
        }
    }

    private static void setupFlat() {
        if (dark) {
            FlatDarkLaf.setup();
        } else {
            FlatLightLaf.setup();
        }
        // bảng cùng màu với thẻ chứa nó (FlatDarkLaf mặc định xám hơn)
        javax.swing.plaf.ColorUIResource surface = new javax.swing.plaf.ColorUIResource(surface());
        UIManager.put("Table.background", dark ? surface : null);
        UIManager.put("TableHeader.background", dark ? new javax.swing.plaf.ColorUIResource(surfaceAlt()) : null);
        UIManager.put("Viewport.background", dark ? surface : null);
    }

    /** Đổi sáng ↔ tối ngay khi đang chạy. */
    public static void toggleDark() {
        if (!flat) {
            return;
        }
        dark = !dark;
        setupFlat();
        FlatLaf.updateUI();
    }

    public static boolean isDark() {
        return dark;
    }

    public static Font font(int style, float size) {
        return new Font(fontName, style, Math.round(size * fontSize / 14f));
    }

    // ---- màu đọc lúc vẽ ----
    public static Color pageBg() {
        return dark ? new Color(0x161A22) : new Color(0xF3F5FA);
    }

    public static Color surface() {
        return dark ? new Color(0x212733) : Color.WHITE;
    }

    public static Color surfaceAlt() {
        return dark ? new Color(0x2A3140) : new Color(0xF7F9FC);
    }

    public static Color border() {
        return dark ? new Color(0x353E4E) : new Color(0xE3E8F0);
    }

    public static Color text() {
        return dark ? new Color(0xE6EAF2) : new Color(0x0F172A);
    }

    public static Color muted() {
        return dark ? new Color(0x9AA4B2) : new Color(0x64748B);
    }

    public static Color soft(Color c) {
        float a = dark ? 0.22f : 0.12f;
        Color base = surface();
        return new Color(
                Math.round(c.getRed() * a + base.getRed() * (1 - a)),
                Math.round(c.getGreen() * a + base.getGreen() * (1 - a)),
                Math.round(c.getBlue() * a + base.getBlue() * (1 - a)));
    }
}
