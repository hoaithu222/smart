package thuchanh;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javacard.framework.AID;
import javacard.framework.Applet;
import javacard.framework.JCSystem;
import javacard.framework.SystemException;

import com.licel.jcardsim.base.Simulator;
import com.licel.jcardsim.base.SimulatorSystem;

/**
 * The gia lap tren jCardSim 2.2.2, bo sung 4 hanh vi cua the that ma jCardSim thieu:
 *   1. lenh SELECT (00 A4 04 00) chon applet;
 *   2. bo chon applet thi xoa mang CLEAR_ON_DESELECT; reset thi xoa ca hai loai;
 *   3. abortTransaction hoan tac du lieu ben (xem NhatKyGiaoDich);
 *   4. lenh ket thuc ma giao dich con mo thi JCRE huy giao dich.
 * Van KHONG mo phong: tuong lua giua cac applet, APDU mo rong dai, gioi han bo nho.
 */
public final class TheGiaLap implements KenhThe {

    private static final byte[] AID_RONG = Hex.doc("F000000000FF");

    private static TheGiaLap hienTai;

    private Simulator sim;
    private final Map<String, byte[]> daCai = new LinkedHashMap<String, byte[]>();
    private String dangChon;
    private String ghiChu;

    public TheGiaLap() throws Exception {
        theMoi();
    }

    public boolean theMoi() throws Exception {
        hienTai = this;
        NhatKyGiaoDich.chotLai();
        sim = new Simulator();
        sim.resetRuntime();
        daCai.clear();
        dangChon = null;
        xoaTam("clearOnDeselect");
        xoaTam("clearOnReset");
        caiLop(AppletRong.class, AID_RONG, new byte[0]);
        daCai.remove(Hex.in(AID_RONG));
        return true;
    }

    public boolean cai(CauHinh.Lop lop, byte[] instanceAid, byte[] param) throws Exception {
        Class<?> c = Class.forName(lop.ten);
        caiLop(c.asSubclass(Applet.class), instanceAid, param);
        return true;
    }

    private void caiLop(Class<? extends Applet> c, byte[] aid, byte[] data) {
        String khoa = Hex.in(aid);
        if (daCai.containsKey(khoa)) {
            throw new IllegalStateException("AID " + khoa + " đã có applet trên thẻ (dùng /moi để lấy thẻ trắng)");
        }
        // [Li][AID][Lc=0][La][du lieu]
        byte[] p = Hex.noi(new byte[] {(byte) aid.length}, aid, new byte[] {0, (byte) data.length}, data);
        try {
            sim.installApplet(new AID(aid, (short) 0, (byte) aid.length), c, p, (short) 0, (byte) p.length);
        } catch (SystemException e) {
            throw new IllegalStateException("Cài đặt thất bại (SystemException " + e.getReason() + ")", e);
        }
        // jCardSim chi bao loi chung; kiem tra applet da dang ky that chua
        if (timApplet(aid) == null) {
            throw new IllegalStateException("Cài đặt thất bại: applet không đăng ký");
        }
        daCai.put(khoa, aid);
    }

    private Applet timApplet(byte[] aid) {
        try {
            Object runtime = runtime();
            Method m = runtime.getClass().getDeclaredMethod("getApplet", AID.class);
            m.setAccessible(true);
            return (Applet) m.invoke(runtime, new AID(aid, (short) 0, (byte) aid.length));
        } catch (Exception e) {
            return null;
        }
    }

    public byte[] gui(byte[] apdu) throws Exception {
        hienTai = this;
        ghiChu = null;
        if (laLenhSelect(apdu)) {
            return chon(Arrays.copyOfRange(apdu, 5, 5 + (apdu[4] & 0xFF)));
        }
        if (dangChon == null) {
            ghiChu = "chưa chọn applet nào (sau /reset hoặc /deselect phải /select lại)";
            return new byte[] {0x69, (byte) 0x99};
        }
        // Bo dem APDU cua jCardSim chi 260 byte: lenh 255 byte du lieu kem Le (261 byte)
        // se loi, trong khi the that nhan duoc. Bo Le (the van tra du lieu).
        if (apdu.length == 261 && (apdu[4] & 0xFF) == 255) {
            apdu = Arrays.copyOf(apdu, 260);
        }
        byte[] r = sim.transmitCommand(apdu);
        if (JCSystem.getTransactionDepth() != 0) {
            JCSystem.abortTransaction();
            ghiChu = "lệnh kết thúc khi giao dịch còn mở: JCRE hủy giao dịch";
        }
        return r;
    }

    private static boolean laLenhSelect(byte[] a) {
        return a.length >= 5 && a[0] == 0 && a[1] == (byte) 0xA4 && a[2] == 4
                && (a[4] & 0xFF) >= 5 && (a[4] & 0xFF) <= 16 && a.length >= 5 + (a[4] & 0xFF);
    }

    private byte[] chon(byte[] aid) {
        if (!daCai.containsKey(Hex.in(aid))) {
            ghiChu = "không có applet với AID này";
            return new byte[] {0x6A, (byte) 0x82};
        }
        // thu tu cua JCRE: deselect applet cu -> xoa RAM kieu DESELECT -> select applet moi
        sim.selectAppletWithResult(new AID(AID_RONG, (short) 0, (byte) AID_RONG.length));
        xoaTam("clearOnDeselect");
        byte[] r = sim.selectAppletWithResult(new AID(aid, (short) 0, (byte) aid.length));
        dangChon = (r.length >= 2 && r[r.length - 2] == (byte) 0x90) ? Hex.in(aid) : null;
        return r;
    }

    public boolean boChon() {
        hienTai = this;
        sim.selectAppletWithResult(new AID(AID_RONG, (short) 0, (byte) AID_RONG.length));
        xoaTam("clearOnDeselect");
        dangChon = null;
        return true;
    }

    public void reset() {
        hienTai = this;
        if (JCSystem.getTransactionDepth() != 0) {
            JCSystem.abortTransaction();
        }
        sim.reset();
        xoaTam("clearOnDeselect");
        xoaTam("clearOnReset");
        dangChon = null;
    }

    public boolean laGiaLap() {
        return true;
    }

    public String ghiChu() {
        return ghiChu;
    }

    /** Applet dang cai tren the gia lap dang dung (cho nhat ky giao dich). */
    static List<Applet> dsApplet() {
        List<Applet> ds = new ArrayList<Applet>();
        if (hienTai != null) {
            for (byte[] aid : hienTai.daCai.values()) {
                Applet a = hienTai.timApplet(aid);
                if (a != null) {
                    ds.add(a);
                }
            }
        }
        return ds;
    }

    private static Object runtime() throws Exception {
        Method m = SimulatorSystem.class.getDeclaredMethod("getRuntime");
        m.setAccessible(true);
        return m.invoke(null);
    }

    /** Ghi 0 / null vao moi mang tam thoi thuoc loai da cho. */
    private static void xoaTam(String loai) {
        try {
            Field f = SimulatorSystem.class.getDeclaredField("transientMemory");
            f.setAccessible(true);
            Object boNho = f.get(null);
            Field ds = boNho.getClass().getDeclaredField(loai);
            ds.setAccessible(true);
            List<?> mang = (List<?>) ds.get(boNho);
            if (mang == null) {
                return;
            }
            for (Object o : mang) {
                if (o instanceof byte[]) {
                    Arrays.fill((byte[]) o, (byte) 0);
                } else if (o instanceof short[]) {
                    Arrays.fill((short[]) o, (short) 0);
                } else if (o instanceof boolean[]) {
                    Arrays.fill((boolean[]) o, false);
                } else if (o instanceof Object[]) {
                    Arrays.fill((Object[]) o, null);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Không xóa được bộ nhớ tạm của thẻ giả lập", e);
        }
    }
}
