import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

import thuchanh.CauHinh;

/**
 * Bien dich va chay moi thu cua thu muc thuc-hanh (goi tu cac file .cmd).
 *
 *   java XayDung kiemtra          build + chay moi kich ban + doi chieu mat ma + chuyen doi CAP
 *   java XayDung cap              chi chuyen doi CAP (bo chuyen doi cua Oracle)
 *   java XayDung chay [...]       build roi mo ThucHanh (shell APDU)
 *   java XayDung ketnoi [pcsc]    build roi mo chuong trinh Connect_Ex_GUI
 *   java XayDung matma [--pcsc]   doi chieu mat ma TH6 (gia lap hoac JCIDE)
 *   java XayDung donggoi          nen thanh file zip de mang sang may co JCIDE
 */
public class XayDung {

    private static final Path GOC = Paths.get("").toAbsolutePath();
    private static final Path BUILD = GOC.resolve("build");
    private static final Path JCARDSIM = GOC.resolve("../code/host/lib/jcardsim-2.2.2.jar").normalize();
    private static final Path JAR_GIA_LAP = BUILD.resolve("lib/jcardsim-khong-ky.jar");
    private static final Path LOP_APPLET = BUILD.resolve("applet");
    private static final Path LOP_CONG_CU = BUILD.resolve("cong-cu");

    public static void main(String[] args) throws Exception {
        String viec = args.length > 0 ? args[0].toLowerCase() : "kiemtra";
        List<String> them = new ArrayList<String>();
        for (int i = 1; i < args.length; i++) {
            them.add(args[i]);
        }
        if (!Files.isRegularFile(GOC.resolve("goi.cfg"))) {
            dung("Phải chạy trong thư mục thuc-hanh (không thấy goi.cfg).");
        }
        int ma;
        if (viec.equals("cap")) {
            ma = chuyenDoiCap() ? 0 : 1;
        } else if (viec.equals("chay")) {
            buildGiaLap();
            ma = chayJava("thuchanh.ThucHanh", them);
        } else if (viec.equals("ketnoi")) {
            buildGiaLap();
            boolean pcsc = !them.isEmpty() && them.get(0).equalsIgnoreCase("pcsc");
            ma = chayJava(pcsc ? "connect_ex_gui.GUI" : "thuchanh.ChayKetNoi", Collections.<String>emptyList());
        } else if (viec.equals("matma")) {
            buildGiaLap();
            ma = chayJava("thuchanh.KiemTraMatMa", them);
        } else if (viec.equals("donggoi")) {
            dongGoi();
            ma = 0;
        } else if (viec.equals("kiemtra")) {
            ma = kiemTra();
        } else {
            dung("Không hiểu lệnh: " + viec);
            return;
        }
        System.exit(ma);
    }

    private static int kiemTra() throws Exception {
        System.out.println("[1/5] Biên dịch applet và công cụ cho thẻ giả lập");
        buildGiaLap();

        System.out.println("[2/5] Chạy kịch bản APDU từng bài");
        List<String> kb = new ArrayList<String>();
        for (Path p : lietKe(GOC.resolve("kich-ban"), "*.apdu")) {
            kb.add("kich-ban" + File.separator + p.getFileName());
        }
        boolean okKichBan = !kb.isEmpty() && chayJava("thuchanh.ThucHanh", kb) == 0;

        System.out.println();
        System.out.println("[3/5] Đối chiếu mật mã TH6 với thư viện Java trên máy tính");
        boolean okMatMa = chayJava("thuchanh.KiemTraMatMa", Collections.<String>emptyList()) == 0;

        System.out.println();
        System.out.println("[4/5] Chương trình kết nối (Connect_Ex_GUI) trên thẻ giả lập");
        boolean okKetNoi = chayJava("thuchanh.KiemTraKetNoi", Collections.<String>emptyList()) == 0;

        System.out.println();
        System.out.println("[5/5] Chuyển đổi sang file CAP bằng bộ chuyển đổi của Oracle");
        boolean coBoCongCu = timBoCongCu() != null;
        boolean okCap = !coBoCongCu || chuyenDoiCap();
        if (!coBoCongCu) {
            System.out.println("Bỏ qua: không có thư mục java_card_devkit_tools-* (trên máy JCIDE thì Build trong JCIDE).");
        }

        System.out.println();
        System.out.println("================ KẾT QUẢ CHUNG");
        System.out.println("Kịch bản APDU    : " + nhan(okKichBan));
        System.out.println("Mật mã TH6       : " + nhan(okMatMa));
        System.out.println("Chương trình PC  : " + nhan(okKetNoi));
        System.out.println("Chuyển đổi CAP   : " + (coBoCongCu ? nhan(okCap) : "BỎ QUA"));
        return okKichBan && okMatMa && okKetNoi && okCap ? 0 : 1;
    }

    // ------------------------------------------------------------ goi mang sang may co JCIDE

    /** Nen thuc-hanh (tru build), thu vien gia lap va tai lieu mon thanh mot file zip. */
    private static void dongGoi() throws IOException {
        final Path duAn = GOC.getParent();
        Path zip = BUILD.resolve("thuc-hanh-may-jcide.zip");
        Files.createDirectories(BUILD);
        List<Path> tep = new ArrayList<Path>();
        themTep(GOC, tep);
        themTep(duAn.resolve("smart/docs"), tep);
        if (!Files.isRegularFile(JCARDSIM)) {
            dung("Không thấy " + JCARDSIM);
        }
        tep.add(JCARDSIM);
        try (ZipOutputStream ra = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (Path f : tep) {
                ra.putNextEntry(new ZipEntry(duAn.relativize(f).toString().replace('\\', '/')));
                Files.copy(f, ra);
                ra.closeEntry();
            }
        }
        System.out.println("Đã tạo " + zip + "  (" + tep.size() + " tệp, " + (Files.size(zip) / 1024) + " KB)");
        System.out.println("Chép sang máy có JCIDE, giải nén vào đường dẫn không dấu (ví dụ C:\\bai),");
        System.out.println("rồi làm theo thuc-hanh\\MAY-JCIDE.md.");
    }

    private static void themTep(Path goc, final List<Path> ds) throws IOException {
        if (!Files.isDirectory(goc)) {
            return;
        }
        Files.walkFileTree(goc, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path d, BasicFileAttributes a) {
                return d.equals(BUILD) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                if (!f.getFileName().toString().equals(".DS_Store")) {
                    ds.add(f);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String nhan(boolean ok) {
        return ok ? "ĐẠT" : "LỖI";
    }

    // ------------------------------------------------------------ the gia lap

    private static void buildGiaLap() throws IOException {
        if (!Files.isRegularFile(JCARDSIM)) {
            dung("Không thấy " + JCARDSIM + " - chạy ..\\code\\host\\tai-thu-vien.cmd trước.");
        }
        xoa(BUILD.resolve("applet"));
        xoa(BUILD.resolve("cong-cu"));
        boChuKy(JCARDSIM, JAR_GIA_LAP);
        // applet phai thuan ASCII: JCIDE bien dich bang windows-1252, chu co dau se bi doc sai
        bienDich(timNguon(GOC.resolve("src")), LOP_APPLET, JAR_GIA_LAP.toString(), "US-ASCII");
        List<String> nguon = timNguon(GOC.resolve("cong-cu/src"));
        nguon.addAll(timNguon(GOC.resolve("cong-cu/shim")));
        nguon.addAll(timNguon(GOC.resolve("Connect_Ex_GUI/src")));
        bienDich(nguon, LOP_CONG_CU, JAR_GIA_LAP + File.pathSeparator + LOP_APPLET, "UTF-8");
    }

    /** jCardSim duoc ky so; ban sao bo chu ky de thay duoc lop JCSystem khi chay gia lap. */
    private static void boChuKy(Path goc, Path dich) throws IOException {
        if (Files.isRegularFile(dich) && Files.getLastModifiedTime(dich).compareTo(Files.getLastModifiedTime(goc)) >= 0) {
            return;
        }
        Files.createDirectories(dich.getParent());
        try (JarFile vao = new JarFile(goc.toFile());
                JarOutputStream ra = new JarOutputStream(Files.newOutputStream(dich))) {
            Enumeration<JarEntry> ds = vao.entries();
            while (ds.hasMoreElements()) {
                String ten = ds.nextElement().getName();
                if (ten.matches("META-INF/[^/]+\\.(SF|RSA|DSA)") || ten.equals("META-INF/MANIFEST.MF")) {
                    continue;
                }
                ra.putNextEntry(new JarEntry(ten));
                try (InputStream in = vao.getInputStream(vao.getJarEntry(ten))) {
                    chep(in, ra);
                }
                ra.closeEntry();
            }
        }
    }

    private static int chayJava(String lop, List<String> thamSo) throws Exception {
        List<String> l = new ArrayList<String>();
        l.add(java());
        l.add("-Dfile.encoding=UTF-8");
        l.add("-cp");
        l.add(LOP_CONG_CU + File.pathSeparator + LOP_APPLET + File.pathSeparator + JAR_GIA_LAP);
        l.add(lop);
        l.addAll(thamSo);
        System.out.flush();
        return new ProcessBuilder(l).directory(GOC.toFile()).inheritIO().start().waitFor();
    }

    // ------------------------------------------------------------ file CAP

    private static boolean chuyenDoiCap() throws Exception {
        Path kit = timBoCongCu();
        if (kit == null) {
            System.out.println("Không thấy bộ Java Card Development Kit Tools (java_card_devkit_tools-*).");
            System.out.println("Đặt biến môi trường JC_TOOLS trỏ tới thư mục đó rồi chạy lại.");
            return false;
        }
        System.out.println("Bộ công cụ: " + kit.getFileName() + "  (đích Java Card 3.0.4)");
        Path lop = BUILD.resolve("cap-lop");
        Path ra = BUILD.resolve("cap");
        xoa(lop);
        xoa(ra);
        Files.createDirectories(lop);
        Files.createDirectories(ra);
        Path api = kit.resolve("lib/api_classic-3.0.4.jar");
        List<String> o = new ArrayList<String>(Arrays.asList("-encoding", "US-ASCII", "-g", "-nowarn",
                "-source", "1.6", "-target", "1.6", "-bootclasspath", api.toString(), "-extdirs", "",
                "-classpath", "", "-d", lop.toString()));
        o.addAll(timNguon(GOC.resolve("src")));
        if (trinhBienDich().run(null, null, null, o.toArray(new String[0])) != 0) {
            System.out.println("LỖI: mã applet không biên dịch được với API Java Card.");
            return false;
        }
        CauHinh c = CauHinh.doc(GOC.resolve("goi.cfg"));
        StringBuilder cp = new StringBuilder();
        for (String j : new String[] {"tools.jar", "jctasks_tools.jar", "json.jar", "commons-cli-1.9.0.jar"}) {
            cp.append(kit.resolve("lib").resolve(j)).append(File.pathSeparator);
        }
        boolean tatCa = true;
        for (CauHinh.Goi g : c.goi) {
            List<String> l = new ArrayList<String>(Arrays.asList(java(), "-Djc.home=" + kit,
                    "-cp", cp.toString(), "com.sun.javacard.converter.Main",
                    "-classdir", lop.toString(), "-d", ra.toString(),
                    "-out", "CAP", "EXP", "-target", "3.0.4", "-nobanner"));
            if (!g.can.isEmpty()) {
                l.add("-exportpath");
                l.add(ra.toString());
            }
            for (CauHinh.Lop a : g.lop) {
                l.add("-applet");
                l.add(aid(a.aid));
                l.add(a.ten);
            }
            l.add(g.ten);
            l.add(aid(g.aid));
            l.add("1.0");
            String log = chayLayChu(l);
            Path cap = ra.resolve(g.ten).resolve("javacard").resolve(g.ten + ".cap");
            boolean ok = log.contains("0 errors and 0 warnings") && Files.isRegularFile(cap);
            System.out.println(String.format("  %-20s %s", g.ten,
                    ok ? "ĐẠT  0 lỗi, 0 cảnh báo  -> build\\cap\\" + g.ten + "\\javacard\\" + g.ten + ".cap ("
                            + Files.size(cap) + " byte)" : "LỖI"));
            if (!ok) {
                System.out.println(log);
                tatCa = false;
            }
        }
        return tatCa;
    }

    private static Path timBoCongCu() throws IOException {
        String bien = System.getenv("JC_TOOLS");
        if (bien != null && !bien.isEmpty()) {
            Path p = Paths.get(bien);
            return Files.isRegularFile(p.resolve("lib/tools.jar")) ? p : null;
        }
        for (Path p : lietKe(GOC.getParent(), "java_card_devkit_tools*")) {
            if (Files.isRegularFile(p.resolve("lib/tools.jar"))) {
                return p;
            }
        }
        return null;
    }

    private static String aid(byte[] a) {
        StringBuilder s = new StringBuilder();
        for (byte b : a) {
            if (s.length() > 0) {
                s.append(':');
            }
            s.append(String.format("0x%02X", b & 0xFF));
        }
        return s.toString();
    }

    // ------------------------------------------------------------ tien ich

    private static JavaCompiler trinhBienDich() {
        JavaCompiler j = ToolProvider.getSystemJavaCompiler();
        if (j == null) {
            dung("Máy chỉ có JRE. Cần JDK 8 và JAVA_HOME / PATH trỏ tới JDK.");
        }
        return j;
    }

    private static void bienDich(List<String> nguon, Path dich, String classpath, String maHoa) throws IOException {
        Files.createDirectories(dich);
        List<String> o = new ArrayList<String>(Arrays.asList("-encoding", maHoa, "-nowarn",
                "-XDignore.symbol.file", "-cp", classpath, "-d", dich.toString()));
        o.addAll(nguon);
        if (trinhBienDich().run(null, null, null, o.toArray(new String[0])) != 0) {
            dung("Biên dịch thất bại.");
        }
    }

    private static List<String> timNguon(Path goc) throws IOException {
        final List<String> ds = new ArrayList<String>();
        if (Files.isDirectory(goc)) {
            Files.walkFileTree(goc, new SimpleFileVisitor<Path>() {
                public FileVisitResult visitFile(Path f, BasicFileAttributes a) {
                    if (f.toString().endsWith(".java")) {
                        ds.add(f.toString());
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        }
        Collections.sort(ds);
        return ds;
    }

    private static List<Path> lietKe(Path thuMuc, String mau) throws IOException {
        List<Path> ds = new ArrayList<Path>();
        if (thuMuc != null && Files.isDirectory(thuMuc)) {
            try (DirectoryStream<Path> s = Files.newDirectoryStream(thuMuc, mau)) {
                for (Path p : s) {
                    ds.add(p);
                }
            }
        }
        Collections.sort(ds);
        return ds;
    }

    private static void xoa(Path p) throws IOException {
        if (!Files.exists(p)) {
            return;
        }
        Files.walkFileTree(p, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path f, BasicFileAttributes a) throws IOException {
                Files.delete(f);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult postVisitDirectory(Path d, IOException e) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void chep(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[8192];
        int n;
        while ((n = in.read(b)) > 0) {
            out.write(b, 0, n);
        }
    }

    private static String java() {
        return new File(System.getProperty("java.home"), "bin/java").getPath();
    }

    private static String chayLayChu(List<String> lenh) throws Exception {
        Process p = new ProcessBuilder(lenh).directory(GOC.toFile()).redirectErrorStream(true).start();
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        chep(p.getInputStream(), o);
        p.waitFor();
        return new String(o.toByteArray(), Charset.defaultCharset());
    }

    private static void dung(String thongBao) {
        System.out.println(thongBao);
        System.exit(1);
    }
}
