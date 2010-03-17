package net.sourceforge.keepassj2me.datasource;

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.keepassj2me.KeePassException;

/**
 * Data adapter
 * @author Stepan Strelets
 */
public abstract class DataSourceAdapter {
	protected byte uid = DataSourceRegistry.NONE;
	protected String familyName = "Abstract";
	protected int icon = 0;
	
	public DataSourceAdapter(byte uid, String name, int icon) {
		this.uid = uid;
		this.familyName = name;
		this.icon = icon;
	}
	
	/**
	 * Select resource for loading
	 * @param caption
	 * @return true on success select and false on cancel  
	 * @throws KeePassException
	 */
	public boolean selectLoad(String caption) throws KeePassException {
		return true;
	}

	/**
	 * Select resource for saving 
	 * @param caption
	 * @return true on success select and false on cancel
	 * @throws KeePassException
	 */
	public boolean selectSave(String caption, String defaultName) throws KeePassException {
		return true;
	}
	
	public abstract InputStream getInputStream() throws KeePassException;

	public abstract byte[] load() throws KeePassException;

	public abstract void save(byte[] content) throws KeePassException;
	
	public boolean canLoad() {
		return true;
	}
	
	public boolean canSave() {
		return false;
	}
	
	public int getUid() {
		return this.uid;
	}
	
	public String getFamilyName() {
		return this.familyName;
	}

	public int getIcon() {
		return this.icon;
	}

	public abstract void serialize(SerializeStream out) throws IOException;
	public abstract void unserialize(UnserializeStream in) throws IOException;
	
	/**
	 * Get representative name of current source with short prefix
	 * @return name
	 */
	public abstract String getCaption();
	/**
	 * Get representative name of current source
	 * @return
	 */
	public abstract String getName();
}
