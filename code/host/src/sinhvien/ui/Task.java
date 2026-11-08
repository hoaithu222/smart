package sinhvien.ui;

import java.awt.Component;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import javax.smartcardio.CardException;
import javax.swing.SwingWorker;
import sinhvien.host.CardClient;

/**
 * Chạy việc chậm (gửi APDU, sinh khóa RSA, CSDL) ngoài luồng giao diện - giống useMutation:
 * tự báo "đang xử lý", tự hiện lỗi tiếng Việt, xong thì gọi lại trên luồng giao diện.
 */
public final class Task {

    /** Nhận thông báo bận/rảnh (thanh trạng thái của MainFrame). */
    public interface BusyListener {
        void busy(String text);
    }

    /** Việc xong (chạy trên luồng giao diện). */
    public interface Done<T> {
        void accept(T value) throws Exception;
    }

    /** Lỗi do người dùng nhập sai / thao tác sai - chỉ hiện thông báo, không phải lỗi hệ thống. */
    public static class UserError extends RuntimeException {
        public UserError(String msg) {
            super(msg);
        }
    }

    public static BusyListener busyListener;
    /** Kiểm thử: chạy đồng bộ, không mở hộp thoại. */
    public static boolean sync;
    public static CardSession session;

    private Task() {
    }

    public static <T> void run(final Component owner, final String busyText, final Callable<T> work,
            final Done<T> done) {
        run(owner, busyText, work, done, null);
    }

    /** Như trên, thêm "always" chạy sau cùng dù thành công hay lỗi (vd. bật lại nút). */
    public static <T> void run(final Component owner, final String busyText, final Callable<T> work,
            final Done<T> done, final Runnable always) {
        if (sync) {
            try {
                T v = work.call();
                if (done != null) {
                    done.accept(v);
                }
            } catch (Exception e) {
                System.err.println("[Task] " + busyText + ": " + message(e));
            }
            if (always != null) {
                always.run();
            }
            return;
        }
        if (busyListener != null) {
            busyListener.busy(busyText);
        }
        new SwingWorker<T, Void>() {
            protected T doInBackground() throws Exception {
                return work.call();
            }

            protected void done() {
                if (busyListener != null) {
                    busyListener.busy(null);
                }
                try {
                    T v = get();
                    if (done != null) {
                        done.accept(v);
                    }
                } catch (ExecutionException e) {
                    Dialogs.error(owner, message(e.getCause()));
                } catch (Exception e) {
                    Dialogs.error(owner, message(e));
                } finally {
                    if (always != null) {
                        always.run();
                    }
                }
            }
        }.execute();
    }

    public static String message(Throwable e) {
        if (e instanceof CardClient.CardSwException) {
            CardClient.CardSwException sw = (CardClient.CardSwException) e;
            if (sw.sw == 0x6982 && session != null) {
                session.markPinLost();
            }
            return e.getMessage();
        }
        if (e instanceof UserError) {
            return e.getMessage();
        }
        if (e instanceof SQLException) {
            return "Lỗi CSDL: " + e.getMessage();
        }
        if (e instanceof CardException) {
            return "Lỗi kết nối thẻ: " + e.getMessage();
        }
        return e.getClass().getSimpleName() + ": " + e.getMessage();
    }
}
