package sinhvien.ui;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Định dạng tiền, giờ theo kiểu Việt Nam. */
public final class Fmt {

    private static final TimeZone VN = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");

    private Fmt() {
    }

    /** 1234 (nghìn đồng) -> "1.234.000 đ" */
    public static String money(long thousand) {
        DecimalFormatSymbols s = new DecimalFormatSymbols(Locale.forLanguageTag("vi-VN"));
        s.setGroupingSeparator('.');
        return new DecimalFormat("#,##0", s).format(thousand * 1000L) + " đ";
    }

    public static String time(long sec) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        f.setTimeZone(VN);
        return f.format(new Date(sec * 1000L));
    }

    public static String shortTime(long sec) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM HH:mm:ss");
        f.setTimeZone(VN);
        return f.format(new Date(sec * 1000L));
    }

    public static String hm(long sec) {
        SimpleDateFormat f = new SimpleDateFormat("HH:mm:ss");
        f.setTimeZone(VN);
        return f.format(new Date(sec * 1000L));
    }

    public static String day(long sec) {
        SimpleDateFormat f = new SimpleDateFormat("dd/MM");
        f.setTimeZone(VN);
        return f.format(new Date(sec * 1000L));
    }

    /** 0 giờ hôm nay (giờ Việt Nam), tính bằng giây. */
    public static long startOfToday() {
        long now = System.currentTimeMillis() / 1000L;
        long tz = 7 * 3600L;
        return ((now + tz) / 86400L) * 86400L - tz;
    }

    public static String txType(String loai) {
        if ("TOPUP".equals(loai)) {
            return "Nạp tiền";
        }
        if ("PAY".equals(loai)) {
            return "Thanh toán";
        }
        return "Điểm danh";
    }

    public static String txType(int type) {
        return type == 1 ? "Nạp tiền" : type == 2 ? "Thanh toán" : "Điểm danh";
    }
}
