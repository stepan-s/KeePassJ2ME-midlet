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
	
	/**
	 * Create data source adapter (you dont need create adapters directly, use registry)
	 * @param uid
	 * @param name
	 * @param icon
	 */
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
	 * @param defaultName 
	 * @return true on success select and false on cancel
	 * @throws KeePassException
	 */
	public boolean selectSave(String caption, String defaultName) throws KeePassException {
		return true;
	}
	
	/**
	 * Get data input stream
	 * @return input stream
	 * @throws KeePassException
	 */
	public abstract InputStream getInputStream() throws KeePassException;

	/**
	 * Load data from source
	 * @return data
	 * @throws KeePassException
	 */
	public abstract byte[] load() throws KeePassException;

	/**
	 * Save data to source
	 * @param content
	 * @throws KeePassException
	 */
	public abstract void save(byte[] content) throws KeePassException;
	
	/**
	 * Get loading ability
	 * @return true if can load from source
	 */
	public boolean canLoad() {
		return true;
	}
	
	/**
	 * Get saving ability
	 * @return true if can store to source
	 */
	public boolean canSave() {
		return false;
	}

	/**
	 * Get source identificator
	 * @return uid
	 */
	public int getUid() {
		return this.uid;
	}
	
	/**
	 * Get source type name
	 * @return name
	 */
	public String getFamilyName() {
		return this.familyName;
	}

	/**
	 * Get icon index
	 * @return icon index
	 */
	public int getIcon() {
		return this.icon;
	}

	/**
	 * Serialize data source
	 * @param out
	 * @throws IOException
	 */
	public abstract void serialize(SerializeStream out) throws IOException;
	/**
	 * Unserialize data source
	 * @param in
	 * @throws IOException
	 */
	public abstract void unserialize(UnserializeStream in) throws IOException;
	
	/**
	 * Get representative name of current source with short prefix
	 * @return name
	 */
	public abstract String getCaption();
	/**
	 * Get representative name of current source
	 * @return source name
	 */
	public abstract String getName();
}
