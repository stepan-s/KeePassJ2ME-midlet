package net.sourceforge.keepassj2me.keydb;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.L10nKeys.keys;

/**
 * @author Stepan Strelets
 */
public class KeydbLockedException extends KeydbException {
	/**
	 * Constructor
	 */
	public KeydbLockedException() {
		super(Config.getLocaleString(keys.KD_DB_LOCKED));
	}
}
