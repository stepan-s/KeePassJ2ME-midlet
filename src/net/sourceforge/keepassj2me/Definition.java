package net.sourceforge.keepassj2me;

// import org.bouncycastle1.util.encoders.Hex;

public class Definition {
    public static final String KDBRecordStoreName = "KeePassKDB";
    public static final int NUM_ICONS = 69;
    public static final String TITLE = "KeePass for J2ME";
    public static final int MAX_TEXT_LEN = 128;
    public static final int PASSWORD_KEY_SHA_ROUNDS = 6000;
    public static final int KDB_HEADER_LEN = 124;

    public static final byte[] ZeroIV = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
}

