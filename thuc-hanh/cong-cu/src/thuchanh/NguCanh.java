package thuchanh;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javacard.framework.AID;
import javacard.framework.Applet;
import javacard.framework.Shareable;

import com.licel.jcardsim.base.SimulatorSystem;

/**
 * Chuyen boi canh khi goi doi tuong chia se, vi jCardSim 2.2.2 khong lam:
 * ben trong phuong thuc chia se, JCSystem.getAID() la applet chu va
 * JCSystem.getPreviousContextAID() la applet goi (dung nhu the that).
 *
 * Chi mo phong DANH TINH nguoi goi; khong mo phong viec tuong lua chan truy cap mang.
 */
public final class NguCanh {

    private static final class Khung {
        final AID hienTai;
        final AID truoc;

        Khung(AID hienTai, AID truoc) {
            this.hienTai = hienTai;
            this.truoc = truoc;
        }
    }

    private static final List<Khung> NGAN_XEP = new ArrayList<Khung>();

    private NguCanh() {
    }

    static void xoa() {
        NGAN_XEP.clear();
    }

    public static AID aidHienTai() {
        return NGAN_XEP.isEmpty() ? SimulatorSystem.getAID() : NGAN_XEP.get(NGAN_XEP.size() - 1).hienTai;
    }

    /** null neu applet duoc JCRE goi truc tiep. */
    public static AID aidTruoc() {
        return NGAN_XEP.isEmpty() ? null : NGAN_XEP.get(NGAN_XEP.size() - 1).truoc;
    }

    public static Shareable layDoiTuongChiaSe(AID chu, byte thamSo) {
        Applet appletChu = TheGiaLap.applet(chu);
        if (appletChu == null) {
            return null;
        }
        AID khach = aidHienTai();
        Applet appletKhach = TheGiaLap.applet(khach);
        // JCRE goi applet chu trong boi canh cua chu, kem AID applet khach
        NGAN_XEP.add(new Khung(chu, khach));
        Shareable doiTuong;
        try {
            doiTuong = appletChu.getShareableInterfaceObject(khach, thamSo);
        } finally {
            NGAN_XEP.remove(NGAN_XEP.size() - 1);
        }
        if (doiTuong == null || appletKhach == null || cungGoi(appletChu, appletKhach)) {
            return doiTuong; // cung package: cung boi canh, khong chuyen
        }
        return boc(doiTuong, chu);
    }

    private static boolean cungGoi(Object a, Object b) {
        return tenGoi(a).equals(tenGoi(b));
    }

    private static String tenGoi(Object o) {
        String ten = o.getClass().getName();
        int i = ten.lastIndexOf('.');
        return i < 0 ? "" : ten.substring(0, i);
    }

    /**
     * Doi tuong trung gian cai dat moi giao dien Shareable cua doi tuong goc (the that
     * cung cho ep kieu sang bat ky giao dien Shareable nao ma lop do cai dat).
     */
    private static Shareable boc(final Shareable goc, final AID chu) {
        Set<Class<?>> giaoDien = new LinkedHashSet<Class<?>>();
        for (Class<?> c = goc.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                if (Shareable.class.isAssignableFrom(i)) {
                    giaoDien.add(i);
                }
            }
        }
        InvocationHandler xuLy = new InvocationHandler() {
            public Object invoke(Object doiTuong, Method m, Object[] thamSo) throws Throwable {
                if (m.getDeclaringClass() == Object.class) {
                    if (m.getName().equals("equals")) {
                        return doiTuong == thamSo[0];
                    }
                    return m.getName().equals("hashCode") ? System.identityHashCode(doiTuong) : "SIO";
                }
                NGAN_XEP.add(new Khung(chu, aidHienTai()));
                try {
                    m.setAccessible(true);
                    return m.invoke(goc, thamSo);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                } finally {
                    NGAN_XEP.remove(NGAN_XEP.size() - 1);
                }
            }
        };
        return (Shareable) Proxy.newProxyInstance(goc.getClass().getClassLoader(),
                giaoDien.toArray(new Class<?>[0]), xuLy);
    }
}
