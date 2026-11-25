package thuchanh;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;

import javacard.framework.Applet;

import com.licel.jcardsim.base.SimulatorSystem;

/**
 * Nhat ky giao dich cho the gia lap.
 *
 * beginTransaction: chup lai moi du lieu BEN cua cac applet (truong cua doi tuong,
 * truong tinh, mang khong tam thoi). abortTransaction: tra lai nhu luc chup.
 * Mang tam thoi (RAM) va bien cuc bo khong bi dong den - dung bai thuc hanh 3.
 *
 * Gioi han so voi the that:
 *  - cap nhat bang ham ...NonAtomic trong giao dich cung bi hoan tac;
 *  - mang ben chi duoc giu boi bien cuc bo (khong nam trong truong nao) thi khong duoc hoan tac.
 */
public final class NhatKyGiaoDich {

    private static final List<Runnable> BAN_CHUP = new ArrayList<Runnable>();

    private NhatKyGiaoDich() {
    }

    public static void batDau() {
        BAN_CHUP.clear();
        IdentityHashMap<Object, Boolean> daDuyet = new IdentityHashMap<Object, Boolean>();
        for (Applet a : TheGiaLap.dsApplet()) {
            duyet(a, daDuyet);
        }
    }

    public static void hoanTac() {
        for (Runnable r : BAN_CHUP) {
            r.run();
        }
        BAN_CHUP.clear();
    }

    public static void chotLai() {
        BAN_CHUP.clear();
    }

    private static void duyet(final Object o, IdentityHashMap<Object, Boolean> daDuyet) {
        if (o == null || daDuyet.put(o, Boolean.TRUE) != null) {
            return;
        }
        Class<?> c = o.getClass();
        if (c.isArray()) {
            if (SimulatorSystem.isTransient(o) != 0) {
                return;
            }
            final int n = java.lang.reflect.Array.getLength(o);
            final Object ban = java.lang.reflect.Array.newInstance(c.getComponentType(), n);
            System.arraycopy(o, 0, ban, 0, n);
            BAN_CHUP.add(new Runnable() {
                public void run() {
                    System.arraycopy(ban, 0, o, 0, n);
                }
            });
            if (!c.getComponentType().isPrimitive()) {
                for (Object phanTu : (Object[]) o) {
                    duyet(phanTu, daDuyet);
                }
            }
            return;
        }
        for (Class<?> lop = c; lop != null && laLopCuaBai(lop); lop = lop.getSuperclass()) {
            for (final Field f : lop.getDeclaredFields()) {
                final boolean tinh = Modifier.isStatic(f.getModifiers());
                if (tinh && Modifier.isFinal(f.getModifiers())) {
                    continue;
                }
                try {
                    f.setAccessible(true);
                    final Object chu = tinh ? null : o;
                    final Object giaTri = f.get(chu);
                    BAN_CHUP.add(new Runnable() {
                        public void run() {
                            try {
                                f.set(chu, giaTri);
                            } catch (IllegalAccessException e) {
                                throw new IllegalStateException(e);
                            }
                        }
                    });
                    if (!f.getType().isPrimitive()) {
                        duyet(giaTri, daDuyet);
                    }
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    /** Lop do bai thuc hanh viet (khong phai thu vien Java, Java Card hay trinh gia lap). */
    private static boolean laLopCuaBai(Class<?> c) {
        String ten = c.getName();
        return !(ten.startsWith("java.") || ten.startsWith("javax.") || ten.startsWith("javacard")
                || ten.startsWith("com.licel.") || ten.startsWith("org.bouncycastle.") || ten.startsWith("sun."));
    }
}
