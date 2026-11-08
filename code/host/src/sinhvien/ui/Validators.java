package sinhvien.ui;

import sinhvien.host.CardCrypto;

/** Kiểm dữ liệu nhập ở một chỗ (thẻ vẫn kiểm lại - phòng thủ 2 lớp). Sai thì ném Task.UserError. */
public final class Validators {

    private Validators() {
    }

    public static void required(String v, String name) {
        if (v == null || v.trim().isEmpty()) {
            throw new Task.UserError(name + " không được để trống");
        }
    }

    public static void cardId(String v) {
        if (v == null || !v.matches("[A-Za-z0-9]{1,16}")) {
            throw new Task.UserError("Mã sinh viên chỉ gồm chữ không dấu và số, 1–16 ký tự");
        }
    }

    public static void digits(String v, int len, String name) {
        if (v == null || !v.matches("\\d{" + len + "}")) {
            throw new Task.UserError(name + " phải đúng " + len + " chữ số");
        }
    }

    public static void secret(String v, String name) {
        if (v == null || v.length() < 4 || v.length() > 16) {
            throw new Task.UserError(name + " dài 4–16 ký tự");
        }
    }

    public static void date(String v, String name) {
        if (v != null && !v.trim().isEmpty() && !v.trim().matches("\\d{2}/\\d{2}/\\d{4}")) {
            throw new Task.UserError(name + " theo dạng dd/MM/yyyy");
        }
    }

    public static void same(String a, String b, String name) {
        if (!a.equals(b)) {
            throw new Task.UserError(name + " nhập lại không khớp");
        }
    }

    /** Dữ liệu sau khi đệm phải vừa ô nhớ trên thẻ. */
    public static void fits(byte[] plain, int capacity, String name) {
        int padded = CardCrypto.pad(plain).length;
        if (padded > capacity) {
            throw new Task.UserError(name + " quá dài: " + padded + " byte sau đệm, thẻ chỉ có " + capacity + " byte");
        }
    }

    public static int amount(String v, int max) {
        try {
            int a = Integer.parseInt(v.trim().replace(".", ""));
            if (a <= 0 || a > max) {
                throw new Task.UserError("Số tiền từ 1 đến " + max + " (nghìn đồng)");
            }
            return a;
        } catch (NumberFormatException e) {
            throw new Task.UserError("Số tiền phải là số (đơn vị nghìn đồng)");
        }
    }
}
