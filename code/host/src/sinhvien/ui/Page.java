package sinhvien.ui;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import sinhvien.host.AppConfig;
import sinhvien.host.CardRepository;

/**
 * Một màn hình trong cửa sổ chính. Luôn khớp bề rộng khung cuộn (không cuộn ngang),
 * chỉ cuộn dọc khi nội dung cao hơn cửa sổ.
 */
public abstract class Page extends Ui.PageBg implements javax.swing.Scrollable {

    public java.awt.Dimension getPreferredScrollableViewportSize() {
        return getPreferredSize();
    }

    public int getScrollableUnitIncrement(java.awt.Rectangle r, int orientation, int direction) {
        return 18;
    }

    public int getScrollableBlockIncrement(java.awt.Rectangle r, int orientation, int direction) {
        return r.height - 40;
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return getParent() instanceof javax.swing.JViewport
                && getParent().getHeight() > getPreferredSize().height;
    }

    /** Mọi thứ một màn hình cần: cấu hình, CSDL, phiên thẻ, người đăng nhập. */
    public static class Ctx {
        public final AppConfig cfg;
        public final CardRepository repo;
        public final CardSession session;
        /** null = chế độ sinh viên (không đăng nhập, dùng thẻ + PIN). */
        public final CardRepository.UserAccount user;

        public Ctx(AppConfig cfg, CardRepository repo, CardSession session, CardRepository.UserAccount user) {
            this.cfg = cfg;
            this.repo = repo;
            this.session = session;
            this.user = user;
        }

        public String role() {
            return user == null ? "SINH_VIEN" : user.role;
        }

        public boolean isAdmin() {
            return "ADMIN".equals(role());
        }
    }

    protected final Ctx ctx;

    protected Page(Ctx ctx) {
        super(new BorderLayout(0, 16));
        this.ctx = ctx;
        setBorder(BorderFactory.createEmptyBorder(20, 24, 24, 24));
    }

    public abstract String title();

    public abstract String subtitle();

    public abstract Icons.Kind icon();

    /** Gọi mỗi khi mở màn hình. */
    public void onShow() {
    }

    /** Gọi khi thẻ được cắm / rút / đổi trạng thái. */
    public void onCardChanged() {
    }

    /** Chạy `next` sau khi chắc chắn đã nhập PIN đúng (hỏi PIN nếu cần). */
    protected void withPin(final Runnable next) {
        try {
            ctx.session.requireGenuine();
        } catch (Task.UserError e) {
            Dialogs.error(this, e.getMessage());
            return;
        }
        if (ctx.session.isPinVerified()) {
            next.run();
            return;
        }
        int left = ctx.session.status() == null ? 3 : ctx.session.status().pinTries;
        final String pin = Dialogs.askSecret(this, "Nhập mã PIN",
                "Nhập PIN của thẻ " + ctx.session.cardId() + " (còn " + left + " lần thử):");
        if (pin == null) {
            return;
        }
        Task.run(this, "Đang kiểm tra PIN…", () -> {
            ctx.session.verifyPin(pin);
            return null;
        }, v -> next.run());
    }

    /** Hỏi PIN quản trị của thẻ, trả về null nếu hủy. */
    protected String askAdminPin() {
        return Dialogs.askSecret(this, "PIN quản trị thẻ",
                "Thao tác này cần PIN quản trị của thẻ (tham số cài đặt applet):");
    }
}
