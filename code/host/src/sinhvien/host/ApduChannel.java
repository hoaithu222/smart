package sinhvien.host;

import javax.smartcardio.CardException;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

/**
 * Lớp trừu tượng "đường truyền APDU".
 * - Chạy thật: PcscChannel (JCIDE + PC/SC, hoặc đầu đọc thật).
 * - Kiểm thử tự động: kênh jCardSim trong thư mục test.
 * Giao diện (Swing) và CSDL chỉ gọi CardClient, không biết đang nói chuyện với thẻ nào.
 */
public interface ApduChannel {
    ResponseAPDU transmit(CommandAPDU command) throws CardException;
}
