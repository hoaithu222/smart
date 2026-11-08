package sinhvien.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.ImageIcon;
import sinhvien.host.AppConfig;

/** Thu nhỏ + nén ảnh cho vừa ô PHOTO trên thẻ; tạo ảnh mẫu theo tên. */
public final class ImageUtil {

    private ImageUtil() {
    }

    /** Ảnh -> JPEG cỡ app.photo.width x height, giảm chất lượng tới khi <= app.photo.maxBytes. */
    public static byte[] toCardJpeg(BufferedImage src, AppConfig cfg) throws IOException {
        BufferedImage dst = cover(src, cfg.photoWidth(), cfg.photoHeight());
        for (float q = 0.85f; q >= 0.15f; q -= 0.1f) {
            byte[] b = jpeg(dst, q);
            if (b.length <= cfg.photoMaxBytes()) {
                return b;
            }
        }
        throw new IOException("Ảnh vẫn lớn hơn " + cfg.photoMaxBytes() + " byte sau khi nén, hãy chọn ảnh khác");
    }

    /** Cắt giữa + phóng cho đầy khung (giống object-fit: cover). */
    static BufferedImage cover(BufferedImage src, int w, int h) {
        double s = Math.max((double) w / src.getWidth(), (double) h / src.getHeight());
        int sw = (int) Math.round(src.getWidth() * s), sh = (int) Math.round(src.getHeight() * s);
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
        g.dispose();
        return dst;
    }

    static byte[] jpeg(BufferedImage img, float quality) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageWriter w = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam p = w.getDefaultWriteParam();
        p.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        p.setCompressionQuality(quality);
        ImageOutputStream ios = ImageIO.createImageOutputStream(out);
        w.setOutput(ios);
        w.write(null, new IIOImage(img, null, null), p);
        ios.close();
        w.dispose();
        return out.toByteArray();
    }

    /** Ảnh mẫu: nền chuyển màu + chữ cái đầu của họ tên (dùng khi demo chưa có ảnh thật). */
    public static BufferedImage sampleAvatar(String fullName, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int hash = fullName == null ? 0 : Math.abs(fullName.hashCode());
        Color[] tones = {new Color(0x2F5BEA), new Color(0x14B8A6), new Color(0x7C3AED), new Color(0xDB2777),
            new Color(0xEA580C)};
        Color a = tones[hash % tones.length];
        Color b = tones[(hash / 7 + 1) % tones.length];
        g.setPaint(new GradientPaint(0, 0, a, w, h, b));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(255, 255, 255, 70));
        g.fillOval(w / 2 - w / 4, h / 5, w / 2, w / 2);
        g.fillRoundRect(w / 6, h / 5 + w / 2 + 6, w * 2 / 3, h, w / 2, w / 2);
        String initials = initials(fullName);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Segoe UI", Font.BOLD, w / 4));
        FontMetrics fm = g.getFontMetrics();
        g.drawString(initials, (w - fm.stringWidth(initials)) / 2, h - h / 10);
        g.dispose();
        return img;
    }

    static String initials(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "SV";
        }
        String[] p = name.trim().split("\\s+");
        String s = p.length == 1 ? p[0].substring(0, 1) : p[p.length - 2].substring(0, 1) + p[p.length - 1].substring(0, 1);
        return s.toUpperCase();
    }

    public static BufferedImage read(byte[] jpeg) throws IOException {
        return jpeg == null || jpeg.length == 0 ? null : ImageIO.read(new ByteArrayInputStream(jpeg));
    }

    public static ImageIcon icon(BufferedImage img, int w, int h) {
        return new ImageIcon(img.getScaledInstance(w, h, Image.SCALE_SMOOTH));
    }
}
