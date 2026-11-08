package sinhvien;

import javacard.framework.*;
import javacard.security.*;
import javacardx.crypto.Cipher;

/**
 * THẺ SINH VIÊN SỬ DỤNG SMART CARD - Nhóm 1 (L02)
 *
 * Applet mẫu viết cho Java Card 2.2.2 (JCIDE, -source 1.5):
 * - Không dùng kiểu int, không dùng @Override, không dùng mảng nhiều chiều (bài 3).
 * - Mọi đối tượng được cấp phát trong hàm tạo (bài thực hành 4).
 *
 * Package AID : 4B 4D 41 53 56 01        ("KMASV" + 01)
 * Applet  AID : 4B 4D 41 53 56 01 01
 * CLA         : B0 (giống ví dụ "Hướng dẫn kết nối" của cô)
 *
 * Bảng lệnh APDU đầy đủ: xem notes/03-thiet-ke.md mục 3.
 */
public class StudentCard extends Applet {

    // ================== CLA / INS ==================
    static final byte CLA_SV = (byte) 0xB0;

    static final byte INS_GET_STATUS   = (byte) 0x10;
    static final byte INS_VERIFY_ADMIN = (byte) 0x20;
    static final byte INS_VERIFY_PIN   = (byte) 0x21;
    static final byte INS_CHANGE_PIN   = (byte) 0x22;
    static final byte INS_UNBLOCK_PIN  = (byte) 0x23;
    static final byte INS_INIT_CARD    = (byte) 0x30;
    static final byte INS_GET_PUBKEY   = (byte) 0x31;
    static final byte INS_AUTH_CARD    = (byte) 0x32;
    static final byte INS_SET_LOCK     = (byte) 0x33;
    static final byte INS_WRITE_OBJ    = (byte) 0x40;
    static final byte INS_READ_OBJ     = (byte) 0x41;
    static final byte INS_GET_BALANCE  = (byte) 0x50;
    static final byte INS_TOPUP        = (byte) 0x51;
    static final byte INS_PAY          = (byte) 0x52;
    static final byte INS_GET_LOG      = (byte) 0x53;
    static final byte INS_CHECK_IN     = (byte) 0x60;

    // ================== Status word riêng ==================
    // 63 Cx : sai PIN, còn x lần thử
    static final short SW_WRONG_SECRET_BASE  = (short) 0x63C0;
    static final short SW_SECRET_BLOCKED     = (short) 0x6983;
    // Ba mã ví lấy theo applet mẫu "Wallet" trong Java Card Kit
    static final short SW_INVALID_AMOUNT     = (short) 0x6A83;
    static final short SW_EXCEED_MAX_BALANCE = (short) 0x6A84;
    static final short SW_NOT_ENOUGH_BALANCE = (short) 0x6A85;
    // Thẻ bị khóa: dùng 6400 giống hàm connectCard() trong tài liệu kết nối
    static final short SW_CARD_LOCKED        = (short) 0x6400;

    // ================== Trạng thái thẻ ==================
    static final byte STATE_BLANK  = (byte) 0x00; // vừa cài applet, chưa phát hành
    static final byte STATE_ACTIVE = (byte) 0x01; // đã phát hành
    static final byte STATE_LOCKED = (byte) 0x02; // bị khóa (mất thẻ / hết lượt PUK)

    // ================== Tham số ==================
    static final byte PIN_TRY_LIMIT   = (byte) 3;
    static final byte PUK_TRY_LIMIT   = (byte) 5;
    static final byte ADMIN_TRY_LIMIT = (byte) 5;
    static final byte SECRET_MIN      = (byte) 4;
    static final byte SECRET_MAX      = (byte) 16;
    static final byte HASH_MAX        = (byte) 32;
    static final byte SALT_LEN        = (byte) 16;
    static final byte KEY_LEN         = (byte) 16;   // AES-128
    static final byte CARD_ID_MAX     = (byte) 16;

    static final byte OBJ_INFO    = (byte) 1;  // thông tin hành chính - chỉ admin ghi
    static final byte OBJ_CONTACT = (byte) 2;  // quê quán, SĐT, email - sinh viên tự sửa
    static final byte OBJ_PHOTO   = (byte) 3;  // ảnh - chỉ admin ghi
    static final short CHUNK       = (short) 240; // 15 khối AES, vừa 1 APDU
    static final short INFO_MAX    = (short) 480;
    static final short CONTACT_MAX = (short) 240;
    static final short PHOTO_MAX   = (short) 7680; // 32 khối 240 byte

    static final short MAX_BALANCE = (short) 30000; // đơn vị: nghìn đồng
    static final byte LOG_SIZE = (byte) 10;
    static final byte LOG_REC  = (byte) 12;
    static final byte TX_TOPUP   = (byte) 0x01;
    static final byte TX_PAY     = (byte) 0x02;
    static final byte TX_CHECKIN = (byte) 0x03;
    static final byte AUTH_TAG   = (byte) 0x41; // 'A' - chữ ký xác thực thẻ

    // Hai "miền" băm khác nhau: giá trị kiểm tra PIN != khóa KEK
    static final byte DOM_VERIFY = (byte) 0x50; // 'P'
    static final byte DOM_KEK    = (byte) 0x4B; // 'K'

    // Bố cục vùng nhớ tạm tmp (RAM)
    static final short OFF_IN  = (short) 0;    // đầu vào hàm băm  (<= 33 byte)
    static final short OFF_H1  = (short) 48;   // kết quả băm 1    (<= 32 byte)
    static final short OFF_H2  = (short) 80;   // kết quả băm 2    (<= 32 byte)
    static final short OFF_K   = (short) 112;  // 16 byte khóa
    static final short OFF_IV  = (short) 128;  // 16 byte IV
    static final short OFF_S   = (short) 144;  // dữ liệu cần ký  (<= 96 byte)
    static final short TMP_LEN = (short) 240;

    // ================== Dữ liệu bền (EEPROM) ==================
    private byte state;
    private byte[] cardId;
    private byte cardIdLen;
    private byte[] salt;
    private OwnerPIN pin, puk, admin;
    private byte[] dekByPin;   // DEK mã bởi KEK sinh từ PIN
    private byte[] dekByPuk;   // DEK mã bởi KEK sinh từ PUK
    private KeyPair rsaPair;
    private byte[] infoData, contactData, photoData;
    private short[] objLen;    // độ dài (đã đệm) của từng đối tượng, chỉ số = mã đối tượng
    private byte[] objIv;      // IV gốc của từng đối tượng (4 x 16 byte)
    private short balance;
    private short txCounter;
    private byte[] log;
    private byte logHead, logCount;

    // ================== Dữ liệu tạm (RAM) ==================
    private byte[] tmp;        // CLEAR_ON_RESET (xem giải thích trong notes/th/th2)
    private AESKey dek;        // khóa dữ liệu của phiên, xóa khi bỏ chọn
    private AESKey kek;        // khóa bọc DEK, chỉ dùng trong 1 lệnh

    // ================== Đối tượng mật mã ==================
    private MessageDigest md;
    private byte hashLen;
    private Cipher aesCbc, aesEcb;
    private Signature rsaSig;
    private RandomData rng;

    public static void install(byte[] bArray, short bOffset, byte bLength) {
        new StudentCard(bArray, bOffset, bLength);
    }

    private StudentCard(byte[] bArray, short bOffset, byte bLength) {
        // ---- 1. Cấp phát toàn bộ bộ nhớ ngay khi cài đặt (TH4) ----
        cardId = new byte[CARD_ID_MAX];
        salt = new byte[SALT_LEN];
        dekByPin = new byte[KEY_LEN];
        dekByPuk = new byte[KEY_LEN];
        infoData = new byte[INFO_MAX];
        contactData = new byte[CONTACT_MAX];
        photoData = new byte[PHOTO_MAX];
        objLen = new short[4];
        objIv = new byte[64];
        log = new byte[(short) (LOG_SIZE * LOG_REC)];

        pin = new OwnerPIN(PIN_TRY_LIMIT, HASH_MAX);
        puk = new OwnerPIN(PUK_TRY_LIMIT, HASH_MAX);
        admin = new OwnerPIN(ADMIN_TRY_LIMIT, HASH_MAX);

        tmp = JCSystem.makeTransientByteArray(TMP_LEN, JCSystem.CLEAR_ON_RESET);
        dek = buildSessionKey();
        kek = buildSessionKey();

        try {
            md = MessageDigest.getInstance(MessageDigest.ALG_SHA_256, false);
        } catch (CryptoException e) {
            md = MessageDigest.getInstance(MessageDigest.ALG_SHA, false); // SHA-1 dự phòng
        }
        hashLen = md.getLength();

        aesCbc = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_CBC_NOPAD, false);
        aesEcb = Cipher.getInstance(Cipher.ALG_AES_BLOCK_128_ECB_NOPAD, false);
        rsaSig = Signature.getInstance(Signature.ALG_RSA_SHA_PKCS1, false);
        rng = RandomData.getInstance(RandomData.ALG_SECURE_RANDOM);
        try {
            rsaPair = new KeyPair(KeyPair.ALG_RSA_CRT, KeyBuilder.LENGTH_RSA_1024);
        } catch (CryptoException e) {
            rsaPair = new KeyPair(KeyPair.ALG_RSA, KeyBuilder.LENGTH_RSA_1024);
        }

        rng.generateData(salt, (short) 0, SALT_LEN);

        // ---- 2. Đọc tham số cài đặt (TH4): [Li][AID][Lc][ctrl][La][PIN quản trị] ----
        byte li = bArray[bOffset];
        short p = (short) (bOffset + li + 1);
        byte lc = bArray[p];
        p = (short) (p + lc + 1);
        byte la = bArray[p];
        p = (short) (p + 1);
        if (la >= SECRET_MIN && la <= SECRET_MAX) {
            setSecret(admin, bArray, p, la);
        } else {
            byte[] def = {'1', '2', '3', '4', '5', '6', '7', '8'};
            setSecret(admin, def, (short) 0, (byte) def.length);
        }

        state = STATE_BLANK;

        // ---- 3. Đăng ký với JCRE (đặt cuối: lỗi ở trên thì không có applet dở dang) ----
        if (li == 0) {
            register();
        } else {
            register(bArray, (short) (bOffset + 1), li);
        }
    }

    private AESKey buildSessionKey() {
        try {
            return (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES_TRANSIENT_DESELECT,
                    KeyBuilder.LENGTH_AES_128, false);
        } catch (CryptoException e) {
            // Trình giả lập không có khóa tạm: dùng khóa bền và tự xóa trong deselect()
            return (AESKey) KeyBuilder.buildKey(KeyBuilder.TYPE_AES, KeyBuilder.LENGTH_AES_128, false);
        }
    }

    public void deselect() {
        // Kết thúc phiên: bỏ cờ đã xác thực và xóa khóa phiên
        pin.reset();
        puk.reset();
        admin.reset();
        dek.clearKey();
        kek.clearKey();
    }

    public void process(APDU apdu) {
        byte[] buf = apdu.getBuffer();

        if (selectingApplet()) {
            if (state == STATE_LOCKED) {
                ISOException.throwIt(SW_CARD_LOCKED);
            }
            return;
        }

        if (buf[ISO7816.OFFSET_CLA] != CLA_SV) {
            ISOException.throwIt(ISO7816.SW_CLA_NOT_SUPPORTED);
        }

        byte ins = buf[ISO7816.OFFSET_INS];
        if (state == STATE_LOCKED && ins != INS_GET_STATUS
                && ins != INS_VERIFY_ADMIN && ins != INS_SET_LOCK) {
            ISOException.throwIt(SW_CARD_LOCKED);
        }

        switch (ins) {
        case INS_GET_STATUS:   getStatus(apdu);   return;
        case INS_VERIFY_ADMIN: verifyAdmin(apdu); return;
        case INS_VERIFY_PIN:   verifyPin(apdu);   return;
        case INS_CHANGE_PIN:   changePin(apdu);   return;
        case INS_UNBLOCK_PIN:  unblockPin(apdu);  return;
        case INS_INIT_CARD:    initCard(apdu);    return;
        case INS_GET_PUBKEY:   getPubKey(apdu);   return;
        case INS_AUTH_CARD:    authCard(apdu);    return;
        case INS_SET_LOCK:     setLock(apdu);     return;
        case INS_WRITE_OBJ:    writeObj(apdu);    return;
        case INS_READ_OBJ:     readObj(apdu);     return;
        case INS_GET_BALANCE:  getBalance(apdu);  return;
        case INS_TOPUP:        transfer(apdu, TX_TOPUP); return;
        case INS_PAY:          transfer(apdu, TX_PAY);   return;
        case INS_GET_LOG:      getLog(apdu);      return;
        case INS_CHECK_IN:     checkIn(apdu);     return;
        default:
            ISOException.throwIt(ISO7816.SW_INS_NOT_SUPPORTED);
        }
    }

    // =====================================================================
    // Lệnh 0x10 - trạng thái thẻ (không cần PIN)
    // Trả: state | PIN còn | PUK còn | cờ | len INFO(2) | len CONTACT(2) | len PHOTO(2) | idLen | id
    // =====================================================================
    private void getStatus(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short p = 0;
        buf[p++] = state;
        buf[p++] = pin.getTriesRemaining();
        buf[p++] = puk.getTriesRemaining();
        byte flags = 0;
        if (admin.isValidated()) {
            flags |= (byte) 0x01;
        }
        if (pin.isValidated() && dek.isInitialized()) {
            flags |= (byte) 0x02;
        }
        buf[p++] = flags;
        p = Util.setShort(buf, p, objLen[OBJ_INFO]);
        p = Util.setShort(buf, p, objLen[OBJ_CONTACT]);
        p = Util.setShort(buf, p, objLen[OBJ_PHOTO]);
        buf[p++] = cardIdLen;
        p = Util.arrayCopyNonAtomic(cardId, (short) 0, buf, p, cardIdLen);
        apdu.setOutgoingAndSend((short) 0, p);
    }

    // =====================================================================
    // Lệnh 0x20 - xác thực quản trị (PIN quản trị nạp qua tham số cài đặt)
    // =====================================================================
    private void verifyAdmin(APDU apdu) {
        byte[] buf = apdu.getBuffer();
        short len = apdu.setIncomingAndReceive();
        checkLen(len, SECRET_MIN, SECRET_MAX);
        if (!checkSecret(admin, buf, ISO7816.OFFSET_CDATA, (byte) len)) {
            throwWrongSecret(admin);
        }
    }

    // =====================================================================
    // Lệnh 0x21 - xác thực PIN sinh viên, mở khóa dữ liệu (DEK) cho phiên
    // =====================================================================
    private void verifyPin(APDU apdu) {
        requireState(STATE_ACTIVE);
        byte[] buf = apdu.getBuffer();
        short len = apdu.setIncomingAndReceive();
        checkLen(len, SECRET_MIN, SECRET_MAX);
        if (!checkSecret(pin, buf, ISO7816.OFFSET_CDATA, (byte) len)) {
            throwWrongSecret(pin);
        }
        loadKek(buf, ISO7816.OFFSET_CDATA, (byte) len);
        unwrapDek(dekByPin);
    }

    // =====================================================================
    // Lệnh 0x22 - đổi PIN. Data: [Lold][PIN cũ][Lnew][PIN mới]
    // Chỉ bọc lại DEK, không phải mã lại toàn bộ dữ liệu.
    // =====================================================================
    private void changePin(APDU apdu) {
        requireState(STATE_ACTIVE);
        byte[] buf = apdu.getBuffer();
        short end = (short) (ISO7816.OFFSET_CDATA + apdu.setIncomingAndReceive());
        short oldOff = lvValue(buf, ISO7816.OFFSET_CDATA, end);
        byte oldLen = buf[(short) (oldOff - 1)];
        short newOff = lvValue(buf, (short) (oldOff + oldLen), end);
        byte newLen = buf[(short) (newOff - 1)];

        if (!checkSecret(pin, buf, oldOff, oldLen)) {
            throwWrongSecret(pin);
        }
        loadKek(buf, oldOff, oldLen);
        unwrapDek(dekByPin);
        rewrapDekWithNewPin(buf, newOff, newLen);
        // Phiên vẫn tiếp tục với PIN mới
        checkSecret(pin, buf, newOff, newLen);
    }

    // =====================================================================
    // Lệnh 0x23 - mở khóa PIN bằng PUK. Data: [Lpuk][PUK][Lnew][PIN mới]
    // =====================================================================
    private void unblockPin(APDU apdu) {
        requireState(STATE_ACTIVE);
        byte[] buf = apdu.getBuffer();
        short end = (short) (ISO7816.OFFSET_CDATA + apdu.setIncomingAndReceive());
        short pukOff = lvValue(buf, ISO7816.OFFSET_CDATA, end);
        byte pukLen = buf[(short) (pukOff - 1)];
        short newOff = lvValue(buf, (short) (pukOff + pukLen), end);
        byte newLen = buf[(short) (newOff - 1)];

        if (!checkSecret(puk, buf, pukOff, pukLen)) {
            if (puk.getTriesRemaining() == 0) {
                state = STATE_LOCKED; // hết lượt PUK: khóa thẻ, chỉ admin mở
            }
            throwWrongSecret(puk);
        }
        loadKek(buf, pukOff, pukLen);
        unwrapDek(dekByPuk);
        rewrapDekWithNewPin(buf, newOff, newLen); // pin.update() đặt lại bộ đếm sai
        puk.reset();
        dek.clearKey(); // bắt buộc xác thực lại bằng PIN mới
    }

    private void rewrapDekWithNewPin(byte[] buf, short newOff, byte newLen) {
        // DEK đang nằm trong khóa phiên dek
        loadKek(buf, newOff, newLen);
        wrapDekInto(tmp, OFF_K);          // tmp[OFF_K..+16] = AES_ECB(KEK_new, DEK)
        hashSecret(DOM_VERIFY, buf, newOff, newLen, OFF_H1);
        JCSystem.beginTransaction();      // TH3: hai thay đổi cùng thành công hoặc cùng hủy
        Util.arrayCopy(tmp, OFF_K, dekByPin, (short) 0, KEY_LEN);
        pin.update(tmp, OFF_H1, hashLen);
        JCSystem.commitTransaction();
        Util.arrayFillNonAtomic(tmp, (short) 0, TMP_LEN, (byte) 0);
    }

    // =====================================================================
    // Lệnh 0x30 - khởi tạo / phát hành thẻ (cần admin, thẻ đang BLANK)
    // Data: [Lid][mã SV][Lpin][PIN][Lpuk][PUK]
    // =====================================================================
    private void initCard(APDU apdu) {
        requireState(STATE_BLANK);
        requireAdmin();
        byte[] buf = apdu.getBuffer();
        short end = (short) (ISO7816.OFFSET_CDATA + apdu.setIncomingAndReceive());

        if (buf[ISO7816.OFFSET_CDATA] < 1 || buf[ISO7816.OFFSET_CDATA] > CARD_ID_MAX) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        short idOff = (short) (ISO7816.OFFSET_CDATA + 1);
        byte idLen = buf[ISO7816.OFFSET_CDATA];
        short pinOff = lvValue(buf, (short) (idOff + idLen), end);
        byte pinLen = buf[(short) (pinOff - 1)];
        short pukOff = lvValue(buf, (short) (pinOff + pinLen), end);
        byte pukLen = buf[(short) (pukOff - 1)];

        Util.arrayCopy(buf, idOff, cardId, (short) 0, idLen);
        cardIdLen = idLen;

        // DEK ngẫu nhiên - khóa thật sự mã hóa dữ liệu
        rng.generateData(tmp, OFF_K, KEY_LEN);
        dek.setKey(tmp, OFF_K);

        // Bọc DEK bằng KEK(PIN) và KEK(PUK)
        loadKek(buf, pinOff, pinLen);
        wrapDekInto(dekByPin, (short) 0);
        loadKek(buf, pukOff, pukLen);
        wrapDekInto(dekByPuk, (short) 0);

        setSecret(pin, buf, pinOff, pinLen);
        setSecret(puk, buf, pukOff, pukLen);

        // Cặp khóa RSA sinh ngay trên thẻ, khóa bí mật không bao giờ rời thẻ
        rsaPair.genKeyPair();

        balance = 0;
        txCounter = 0;
        logHead = 0;
        logCount = 0;
        objLen[OBJ_INFO] = 0;
        objLen[OBJ_CONTACT] = 0;
        objLen[OBJ_PHOTO] = 0;

        dek.clearKey();
        Util.arrayFillNonAtomic(tmp, (short) 0, TMP_LEN, (byte) 0);
        state = STATE_ACTIVE; // ghi sau cùng: rút thẻ giữa chừng thì vẫn BLANK, làm lại được
    }

    // =====================================================================
    // Lệnh 0x31 - lấy khóa công khai RSA: [Lmod(2)][modulus][Lexp(2)][exponent]
    // =====================================================================
    private void getPubKey(APDU apdu) {
        requireState(STATE_ACTIVE);
        byte[] buf = apdu.getBuffer();
        RSAPublicKey pub = (RSAPublicKey) rsaPair.getPublic();
        short modLen = pub.getModulus(buf, (short) 2);
        Util.setShort(buf, (short) 0, modLen);
        short p = (short) (2 + modLen);
        short expLen = pub.getExponent(buf, (short) (p + 2));
        Util.setShort(buf, p, expLen);
        apdu.setOutgoingAndSend((short) 0, (short) (p + 2 + expLen));
    }

    // =====================================================================
    // Lệnh 0x32 - xác thực thẻ (challenge-response). Data: challenge 8..64 byte
    // Thẻ ký: 'A' | Lid | mã SV | challenge
    // =====================================================================
    private void authCard(APDU apdu) {
        requireState(STATE_ACTIVE);
        byte[] buf = apdu.getBuffer();
        short len = apdu.setIncomingAndReceive();
        checkLen(len, (short) 8, (short) 64);
        short p = OFF_S;
        tmp[p++] = AUTH_TAG;
        tmp[p++] = cardIdLen;
        p = Util.arrayCopyNonAtomic(cardId, (short) 0, tmp, p, cardIdLen);
        p = Util.arrayCopyNonAtomic(buf, ISO7816.OFFSET_CDATA, tmp, p, len);
        rsaSig.init(rsaPair.getPrivate(), Signature.MODE_SIGN);
        short sigLen = rsaSig.sign(tmp, OFF_S, (short) (p - OFF_S), buf, (short) 0);
        apdu.setOutgoingAndSend((short) 0, sigLen);
    }

    // =====================================================================
    // Lệnh 0x33 - khóa / mở khóa thẻ (admin). P1 = 01 khóa, 00 mở
    // =====================================================================
    private void setLock(APDU apdu) {
        requireAdmin();
        byte[] buf = apdu.getBuffer();
        if (cardIdLen == 0) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED); // chưa phát hành
        }
        if (buf[ISO7816.OFFSET_P1] == (byte) 0x01) {
            state = STATE_LOCKED;
        } else if (buf[ISO7816.OFFSET_P1] == (byte) 0x00) {
            puk.resetAndUnblock();
            state = STATE_ACTIVE;
        } else {
            ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        }
    }

    // =====================================================================
    // Lệnh 0x40 - ghi đối tượng đã mã hóa. P1 = mã đối tượng, P2 = số thứ tự khối
    // Data: bản rõ đã đệm (ISO 9797-1 M2) ở máy tính, độ dài bội của 16, <= 240
    // Khối 0 bắt đầu ghi lại từ đầu; các khối sau phải nối tiếp.
    // =====================================================================
    private void writeObj(APDU apdu) {
        requireState(STATE_ACTIVE);
        requireSession();
        byte[] buf = apdu.getBuffer();
        byte obj = buf[ISO7816.OFFSET_P1];
        byte[] store = storage(obj);
        if (obj != OBJ_CONTACT) {
            requireAdmin();
        }
        short chunk = (short) (buf[ISO7816.OFFSET_P2] & 0x00FF);
        short len = apdu.setIncomingAndReceive();
        if (len <= 0 || len > CHUNK || (short) (len & 0x000F) != 0) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short off = (short) (chunk * CHUNK);
        if (chunk == 0) {
            objLen[obj] = 0;
            rng.generateData(tmp, OFF_IV, (short) 16);
            Util.arrayCopy(tmp, OFF_IV, objIv, (short) (obj * 16), (short) 16);
        } else if (off != objLen[obj]) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2); // khối phải nối tiếp
        }
        if ((short) (off + len) > (short) store.length) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH); // vượt dung lượng ô nhớ
        }
        makeIv(obj, (byte) chunk);
        aesCbc.init(dek, Cipher.MODE_ENCRYPT, tmp, OFF_IV, (short) 16);
        aesCbc.doFinal(buf, ISO7816.OFFSET_CDATA, len, buf, ISO7816.OFFSET_CDATA);
        Util.arrayCopy(buf, ISO7816.OFFSET_CDATA, store, off, len); // arrayCopy vào mảng bền là nguyên tử
        objLen[obj] = (short) (off + len);                         // cập nhật độ dài SAU khi dữ liệu đã ghi
    }

    // =====================================================================
    // Lệnh 0x41 - đọc đối tượng, giải mã trên thẻ. P1 = mã đối tượng, P2 = số khối
    // =====================================================================
    private void readObj(APDU apdu) {
        requireState(STATE_ACTIVE);
        requireSession();
        byte[] buf = apdu.getBuffer();
        byte obj = buf[ISO7816.OFFSET_P1];
        byte[] store = storage(obj);
        short chunk = (short) (buf[ISO7816.OFFSET_P2] & 0x00FF);
        short off = (short) (chunk * CHUNK);
        if (off >= objLen[obj]) {
            ISOException.throwIt(ISO7816.SW_WRONG_P1P2); // hết dữ liệu
        }
        short len = (short) (objLen[obj] - off);
        if (len > CHUNK) {
            len = CHUNK;
        }
        makeIv(obj, (byte) chunk);
        aesCbc.init(dek, Cipher.MODE_DECRYPT, tmp, OFF_IV, (short) 16);
        aesCbc.doFinal(store, off, len, buf, (short) 0);
        apdu.setOutgoingAndSend((short) 0, len);
    }

    // =====================================================================
    // Lệnh 0x50 - số dư: [balance(2)][txCounter(2)][logCount(1)]
    // =====================================================================
    private void getBalance(APDU apdu) {
        requireState(STATE_ACTIVE);
        requireSession();
        byte[] buf = apdu.getBuffer();
        Util.setShort(buf, (short) 0, balance);
        Util.setShort(buf, (short) 2, txCounter);
        buf[4] = logCount;
        apdu.setOutgoingAndSend((short) 0, (short) 5);
    }

    // =====================================================================
    // Lệnh 0x51 nạp tiền / 0x52 thanh toán
    // Data: [Lpin][PIN][số tiền(2)][thời gian(4)][nonce(8)]
    // Trả : [số dư mới(2)][bộ đếm(2)][chữ ký RSA]
    // =====================================================================
    private void transfer(APDU apdu, byte type) {
        requireState(STATE_ACTIVE);
        byte[] buf = apdu.getBuffer();
        short end = (short) (ISO7816.OFFSET_CDATA + apdu.setIncomingAndReceive());
        short pinOff = lvValue(buf, ISO7816.OFFSET_CDATA, end);
        byte pinLen = buf[(short) (pinOff - 1)];
        short p = (short) (pinOff + pinLen);
        if ((short) (p + 14) != end) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        // Mỗi giao dịch đều phải nhập PIN (yêu cầu đề tài)
        if (!checkSecret(pin, buf, pinOff, pinLen)) {
            throwWrongSecret(pin);
        }
        short amount = Util.getShort(buf, p);
        if (amount <= 0 || amount > MAX_BALANCE) {
            ISOException.throwIt(SW_INVALID_AMOUNT);
        }
        short newBal;
        if (type == TX_TOPUP) {
            if (balance > (short) (MAX_BALANCE - amount)) {
                ISOException.throwIt(SW_EXCEED_MAX_BALANCE);
            }
            newBal = (short) (balance + amount);
        } else {
            if (amount > balance) {
                ISOException.throwIt(SW_NOT_ENOUGH_BALANCE);
            }
            newBal = (short) (balance - amount);
        }

        // TH3: số dư + bộ đếm + nhật ký cùng thành công hoặc cùng hủy
        JCSystem.beginTransaction();
        balance = newBal;
        txCounter = (short) (txCounter + 1);
        appendLog(type, amount, buf, (short) (p + 2));
        JCSystem.commitTransaction();

        signReceipt(apdu, type, amount, buf, (short) (p + 2));
    }

    // =====================================================================
    // Lệnh 0x60 - điểm danh / quẹt thẻ (không cần PIN, thẻ ký để chống điểm danh hộ)
    // Data: [mã phòng(2)][thời gian(4)][nonce(8)]
    // =====================================================================
    private void checkIn(APDU apdu) {
        requireState(STATE_ACTIVE);
        byte[] buf = apdu.getBuffer();
        short len = apdu.setIncomingAndReceive();
        if (len != 14) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
        short room = Util.getShort(buf, ISO7816.OFFSET_CDATA);
        JCSystem.beginTransaction();
        txCounter = (short) (txCounter + 1);
        appendLog(TX_CHECKIN, room, buf, (short) (ISO7816.OFFSET_CDATA + 2));
        JCSystem.commitTransaction();
        signReceipt(apdu, TX_CHECKIN, room, buf, (short) (ISO7816.OFFSET_CDATA + 2));
    }

    // =====================================================================
    // Lệnh 0x53 - nhật ký (mới nhất trước), mỗi bản ghi 12 byte:
    // [loại][00][giá trị(2)][số dư sau(2)][bộ đếm(2)][thời gian(4)]
    // =====================================================================
    private void getLog(APDU apdu) {
        requireState(STATE_ACTIVE);
        requireSession();
        byte[] buf = apdu.getBuffer();
        short out = 0;
        byte idx = logHead;
        for (byte i = 0; i < logCount; i++) {
            idx--;
            if (idx < 0) {
                idx = (byte) (LOG_SIZE - 1);
            }
            out = Util.arrayCopyNonAtomic(log, (short) (idx * LOG_REC), buf, out, LOG_REC);
        }
        apdu.setOutgoingAndSend((short) 0, out);
    }

    // ============================ Hàm phụ ============================

    /** Ghi 1 bản ghi nhật ký vào bộ đệm vòng. Gọi bên trong giao dịch. */
    private void appendLog(byte type, short value, byte[] src, short timeOff) {
        short off = (short) (logHead * LOG_REC);
        log[off] = type;
        log[(short) (off + 1)] = 0;
        Util.setShort(log, (short) (off + 2), value);
        Util.setShort(log, (short) (off + 4), balance);
        Util.setShort(log, (short) (off + 6), txCounter);
        Util.arrayCopy(src, timeOff, log, (short) (off + 8), (short) 4);
        logHead++;
        if (logHead == LOG_SIZE) {
            logHead = 0;
        }
        if (logCount < LOG_SIZE) {
            logCount++;
        }
    }

    /**
     * Ký biên nhận: [loại][Lid][mã SV][giá trị(2)][số dư(2)][bộ đếm(2)][thời gian(4)][nonce(8)]
     * Trả về: [số dư(2)][bộ đếm(2)][chữ ký]
     */
    private void signReceipt(APDU apdu, byte type, short value, byte[] src, short timeOff) {
        byte[] buf = apdu.getBuffer();
        short p = OFF_S;
        tmp[p++] = type;
        tmp[p++] = cardIdLen;
        p = Util.arrayCopyNonAtomic(cardId, (short) 0, tmp, p, cardIdLen);
        p = Util.setShort(tmp, p, value);
        p = Util.setShort(tmp, p, balance);
        p = Util.setShort(tmp, p, txCounter);
        p = Util.arrayCopyNonAtomic(src, timeOff, tmp, p, (short) 12); // thời gian + nonce
        rsaSig.init(rsaPair.getPrivate(), Signature.MODE_SIGN);
        short sigLen = rsaSig.sign(tmp, OFF_S, (short) (p - OFF_S), buf, (short) 4);
        Util.setShort(buf, (short) 0, balance);
        Util.setShort(buf, (short) 2, txCounter);
        apdu.setOutgoingAndSend((short) 0, (short) (4 + sigLen));
    }

    /** H(miền | salt | bí mật) -> tmp[outOff..] */
    private void hashSecret(byte domain, byte[] src, short off, byte len, short outOff) {
        tmp[OFF_IN] = domain;
        Util.arrayCopyNonAtomic(salt, (short) 0, tmp, (short) (OFF_IN + 1), SALT_LEN);
        Util.arrayCopyNonAtomic(src, off, tmp, (short) (OFF_IN + 1 + SALT_LEN), len);
        md.reset();
        md.doFinal(tmp, OFF_IN, (short) (1 + SALT_LEN + len), tmp, outOff);
        Util.arrayFillNonAtomic(tmp, OFF_IN, (short) (1 + SALT_LEN + len), (byte) 0);
    }

    /** Lưu PIN dưới dạng giá trị băm (không lưu PIN rõ). */
    private void setSecret(OwnerPIN target, byte[] src, short off, byte len) {
        hashSecret(DOM_VERIFY, src, off, len, OFF_H1);
        target.update(tmp, OFF_H1, hashLen);
        Util.arrayFillNonAtomic(tmp, OFF_H1, (short) 32, (byte) 0);
    }

    private boolean checkSecret(OwnerPIN target, byte[] src, short off, byte len) {
        if (target.getTriesRemaining() == 0) {
            ISOException.throwIt(SW_SECRET_BLOCKED);
        }
        hashSecret(DOM_VERIFY, src, off, len, OFF_H1);
        boolean ok = target.check(tmp, OFF_H1, hashLen);
        Util.arrayFillNonAtomic(tmp, OFF_H1, (short) 32, (byte) 0);
        return ok;
    }

    private void throwWrongSecret(OwnerPIN target) {
        byte left = target.getTriesRemaining();
        if (left == 0) {
            ISOException.throwIt(SW_SECRET_BLOCKED);
        }
        ISOException.throwIt((short) (SW_WRONG_SECRET_BASE | left));
    }

    /** KEK = 16 byte đầu của H('K' | salt | bí mật) */
    private void loadKek(byte[] src, short off, byte len) {
        hashSecret(DOM_KEK, src, off, len, OFF_H2);
        kek.setKey(tmp, OFF_H2);
        Util.arrayFillNonAtomic(tmp, OFF_H2, (short) 32, (byte) 0);
    }

    private void wrapDekInto(byte[] dest, short destOff) {
        dek.getKey(tmp, OFF_K);
        aesEcb.init(kek, Cipher.MODE_ENCRYPT);
        aesEcb.doFinal(tmp, OFF_K, KEY_LEN, tmp, OFF_K);
        Util.arrayCopy(tmp, OFF_K, dest, destOff, KEY_LEN);
        kek.clearKey();
    }

    private void unwrapDek(byte[] wrapped) {
        aesEcb.init(kek, Cipher.MODE_DECRYPT);
        aesEcb.doFinal(wrapped, (short) 0, KEY_LEN, tmp, OFF_K);
        dek.setKey(tmp, OFF_K);
        Util.arrayFillNonAtomic(tmp, OFF_K, KEY_LEN, (byte) 0);
        kek.clearKey();
    }

    /** IV của khối = IV gốc của đối tượng, byte cuối XOR số thứ tự khối */
    private void makeIv(byte obj, byte chunk) {
        Util.arrayCopyNonAtomic(objIv, (short) (obj * 16), tmp, OFF_IV, (short) 16);
        tmp[(short) (OFF_IV + 15)] ^= chunk;
    }

    private byte[] storage(byte obj) {
        if (obj == OBJ_INFO) {
            return infoData;
        }
        if (obj == OBJ_CONTACT) {
            return contactData;
        }
        if (obj == OBJ_PHOTO) {
            return photoData;
        }
        ISOException.throwIt(ISO7816.SW_INCORRECT_P1P2);
        return null;
    }

    /** Đọc trường [L][giá trị]: kiểm tra biên, trả về vị trí của giá trị. */
    private short lvValue(byte[] buf, short off, short end) {
        if (off >= end) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        byte l = buf[off];
        if (l < SECRET_MIN || l > SECRET_MAX || (short) (off + 1 + l) > end) {
            ISOException.throwIt(ISO7816.SW_WRONG_DATA);
        }
        return (short) (off + 1);
    }

    private void checkLen(short len, short min, short max) {
        if (len < min || len > max) {
            ISOException.throwIt(ISO7816.SW_WRONG_LENGTH);
        }
    }

    private void requireState(byte expected) {
        if (state != expected) {
            ISOException.throwIt(ISO7816.SW_CONDITIONS_NOT_SATISFIED);
        }
    }

    private void requireAdmin() {
        if (!admin.isValidated()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
    }

    private void requireSession() {
        if (!pin.isValidated() || !dek.isInitialized()) {
            ISOException.throwIt(ISO7816.SW_SECURITY_STATUS_NOT_SATISFIED);
        }
    }
}
