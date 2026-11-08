package sinhvien.host;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;

/** Các hàm mật mã / định dạng phía máy tính, khớp từng byte với applet. */
public final class CardCrypto {

    private CardCrypto() {
    }

    /** Đệm ISO/IEC 9797-1 phương pháp 2: thêm 0x80 rồi 0x00 cho đủ bội 16 (luôn thêm ít nhất 1 byte). */
    public static byte[] pad(byte[] plain) {
        int padded = (plain.length / 16 + 1) * 16;
        byte[] out = Arrays.copyOf(plain, padded);
        out[plain.length] = (byte) 0x80;
        return out;
    }

    public static byte[] unpad(byte[] padded) {
        int i = padded.length - 1;
        while (i >= 0 && padded[i] == 0) {
            i--;
        }
        if (i < 0 || padded[i] != (byte) 0x80) {
            throw new IllegalArgumentException("Sai định dạng đệm - sai khóa hoặc dữ liệu hỏng");
        }
        return Arrays.copyOf(padded, i);
    }

    /** Dựng khóa công khai RSA từ modulus + exponent thẻ trả về (lưu vào CSDL dạng hex). */
    public static PublicKey rsaPublicKey(byte[] modulus, byte[] exponent) throws GeneralSecurityException {
        RSAPublicKeySpec spec = new RSAPublicKeySpec(new BigInteger(1, modulus), new BigInteger(1, exponent));
        return KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    /** Thẻ dùng Signature.ALG_RSA_SHA_PKCS1 = SHA-1 + PKCS#1 v1.5 -> phía Java là "SHA1withRSA". */
    public static boolean verify(PublicKey key, byte[] message, byte[] signature) throws GeneralSecurityException {
        Signature s = Signature.getInstance("SHA1withRSA");
        s.initVerify(key);
        s.update(message);
        return s.verify(signature);
    }

    /** Thông điệp thẻ ký khi xác thực: 'A' | Lid | mã SV | challenge */
    public static byte[] authMessage(String cardId, byte[] challenge) {
        byte[] id = cardId.getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(0x41);
        o.write(id.length);
        o.write(id, 0, id.length);
        o.write(challenge, 0, challenge.length);
        return o.toByteArray();
    }

    /** Thông điệp thẻ ký cho nạp/thanh toán/điểm danh. */
    public static byte[] receiptMessage(String cardId, CardClient.Receipt r) {
        byte[] id = cardId.getBytes(StandardCharsets.US_ASCII);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        o.write(r.type);
        o.write(id.length);
        o.write(id, 0, id.length);
        writeShort(o, r.value);
        writeShort(o, r.balance);
        writeShort(o, r.counter);
        o.write(r.timeAndNonce, 0, r.timeAndNonce.length);
        return o.toByteArray();
    }

    static void writeShort(ByteArrayOutputStream o, int v) {
        o.write((v >> 8) & 0xFF);
        o.write(v & 0xFF);
    }

    public static String hex(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(String.format("%02X", b[i] & 0xFF));
        }
        return sb.toString();
    }
}
