package net.sourceforge.keepassj2me.tools;

import net.sourceforge.keepassj2me.KeePassException;

/**
 * Progress listener interface 
 * @author Stepan Strelets
 */
public interface IProgressListener {
	/**
	 * Set progress position and text message
	 * @throws KeePassException throw on cancel
	 * @param procent Current progress 0-100
	 * @param message Current message, if <code>null</code> does not change
	 */
	public void setProgress(int procent, String message) throws KeePassException;
}