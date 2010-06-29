package net.sourceforge.keepassj2me.keydb;

/**
 * @author Stepan Strelets
 */
public class KeydbLockedException extends KeydbException {
	/**
	 * Constructor
	 */
	public KeydbLockedException() {
		super("Database locked");
	}
}
