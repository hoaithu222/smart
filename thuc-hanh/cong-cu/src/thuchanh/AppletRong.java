package thuchanh;

import javacard.framework.APDU;
import javacard.framework.Applet;

/** Applet trong, dong vai "applet khac" de bo chon applet dang chay. */
public class AppletRong extends Applet {

    public static void install(byte[] b, short off, byte len) {
        new AppletRong().register(b, (short) (off + 1), b[off]);
    }

    public void process(APDU apdu) {
        // khong lam gi
    }
}
