package net.sourceforge.keepassj2me;

import java.util.Vector;

import net.sourceforge.keepassj2me.keydb.KeydbUtil;

/**
 * Recent data sources
 * @author Stepan Strelets
 *
 */
public class RecentSources {
	private Vector sources;
	private int maxsize = 10;
	
	/**
	 * Constructor
	 */
	public RecentSources() {
		sources = new Vector();
	}
	
	/**
	 * Clean list
	 */
	public void clean() {
		sources.removeAllElements();
	}
	
	/**
	 * Add source to list or move to top 
	 * @param source serialized source
	 */
	public void setSource(byte[] source) {
		for (int i = sources.size() - 1; i >= 0; --i) {
			byte[] s = (byte[]) sources.elementAt(i);
			if (KeydbUtil.compare(source, s)) {
				sources.removeElementAt(i);
			};
		};
		sources.insertElementAt(source, 0);
		if (sources.size() > maxsize) {
			sources.removeElementAt(sources.size() - 1);
		}
	}
	
	/**
	 * Get source from list by index
	 * @param index 
	 * @return serialized source
	 */
	public byte[] getSource(int index) {
		if (index < sources.size()) {
			return (byte[]) sources.elementAt(index);
		} else {
			return null;
		}
	}
	
	/**
	 * Get size of list
	 * @return items count
	 */
	public int getSize() {
		return sources.size();		
	}
	
	/**
	 * Remove source from list
	 * @param index
	 */
	public void removeSource(int index) {
		if (index < sources.size()) sources.removeElementAt(index);
	}
}
