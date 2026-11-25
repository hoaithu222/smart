package javacard.framework;

import com.licel.jcardsim.base.SimulatorSystem;
import thuchanh.NhatKyGiaoDich;

/**
 * Ban thay the lop JCSystem cua jCardSim, CHI dung khi chay the gia lap trong
 * thu muc nay (khong bao gio nap xuong the).
 *
 * Moi ham deu chuyen tiep sang SimulatorSystem y nhu ban goc; khac duy nhat:
 * begin/commit/abortTransaction ghi nhat ky de abort HOAN TAC du lieu ben,
 * vi jCardSim 2.2.2 chi dem do sau giao dich ma khong hoan tac.
 */
public final class JCSystem {
    public static final byte MEMORY_TYPE_PERSISTENT = 0;
    public static final byte MEMORY_TYPE_TRANSIENT_RESET = 1;
    public static final byte MEMORY_TYPE_TRANSIENT_DESELECT = 2;
    public static final byte NOT_A_TRANSIENT_OBJECT = 0;
    public static final byte CLEAR_ON_RESET = 1;
    public static final byte CLEAR_ON_DESELECT = 2;

    @SuppressWarnings("unused")
    private static APDU apdu = new APDU();

    public static byte isTransient(Object o) {
        return SimulatorSystem.isTransient(o);
    }

    public static boolean[] makeTransientBooleanArray(short n, byte event) {
        return SimulatorSystem.makeTransientBooleanArray(n, event);
    }

    public static byte[] makeTransientByteArray(short n, byte event) {
        return SimulatorSystem.makeTransientByteArray(n, event);
    }

    public static short[] makeTransientShortArray(short n, byte event) {
        return SimulatorSystem.makeTransientShortArray(n, event);
    }

    public static Object[] makeTransientObjectArray(short n, byte event) {
        return SimulatorSystem.makeTransientObjectArray(n, event);
    }

    public static short getVersion() {
        return (short) 0x0202;
    }

    public static AID getAID() {
        return SimulatorSystem.getAID();
    }

    public static AID lookupAID(byte[] b, short off, byte len) {
        return SimulatorSystem.lookupAID(b, off, len);
    }

    public static void beginTransaction() throws TransactionException {
        SimulatorSystem.beginTransaction();
        NhatKyGiaoDich.batDau();
    }

    public static void abortTransaction() throws TransactionException {
        SimulatorSystem.abortTransaction();
        NhatKyGiaoDich.hoanTac();
    }

    public static void commitTransaction() throws TransactionException {
        SimulatorSystem.commitTransaction();
        NhatKyGiaoDich.chotLai();
    }

    public static byte getTransactionDepth() {
        return SimulatorSystem.getTransactionDepth();
    }

    public static short getUnusedCommitCapacity() {
        return SimulatorSystem.getUnusedCommitCapacity();
    }

    public static short getMaxCommitCapacity() {
        return SimulatorSystem.getMaxCommitCapacity();
    }

    public static AID getPreviousContextAID() {
        return SimulatorSystem.getPreviousContextAID();
    }

    public static short getAvailableMemory(byte type) throws SystemException {
        switch (type) {
        case MEMORY_TYPE_PERSISTENT:
            return SimulatorSystem.getAvailablePersistentMemory();
        case MEMORY_TYPE_TRANSIENT_RESET:
            return SimulatorSystem.getAvailableTransientResetMemory();
        case MEMORY_TYPE_TRANSIENT_DESELECT:
            return SimulatorSystem.getAvailableTransientDeselectMemory();
        default:
            SystemException.throwIt(SystemException.ILLEGAL_VALUE);
            return 0;
        }
    }

    public static Shareable getAppletShareableInterfaceObject(AID server, byte parameter) {
        return SimulatorSystem.getSharedObject(server, parameter);
    }

    public static boolean isObjectDeletionSupported() {
        return SimulatorSystem.isObjectDeletionSupported();
    }

    public static void requestObjectDeletion() throws SystemException {
        SimulatorSystem.requestObjectDeletion();
    }

    public static byte getAssignedChannel() {
        return SimulatorSystem.getCurrentlySelectedChannel();
    }

    public static boolean isAppletActive(AID aid) {
        return SimulatorSystem.isAppletActive(aid);
    }
}
