package net.sourceforge.keepassj2me.keydb;

/**
 * @author Stepan Strelets
 */
public interface IKeydbGroupContentRecever {
	/**
	 * Receive group
	 * @param group
	 */
	public void addKeydbGroup(KeydbGroup group);
	/**
	 * Groups count
	 * @param count
	 */
	public void totalGroups(int count);
	/**
	 * Receive entry
	 * @param entry
	 */
	public void addKeydbEntry(KeydbEntry entry);
}
