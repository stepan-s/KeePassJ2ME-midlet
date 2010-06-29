package net.sourceforge.keepassj2me;

import java.lang.Exception;

/**
 * KeePass exception
 */
public class KeePassException extends Exception {
    /**
     * Constructor
     * @param str message
     */
    public KeePassException(String str) {
    super(str);
    }
}
