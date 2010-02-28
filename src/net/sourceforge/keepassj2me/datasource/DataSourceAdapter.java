package net.sourceforge.keepassj2me.datasource;

import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.keepassj2me.keydb.KeydbException;

/**
 * Data adapter
 * @author Stepan Strelets
 */
public abstract class DataSourceAdapter {
	protected byte uid = DataSourceRegistry.NONE;
	protected String name = "Abstract";
	protected int icon = 0;
	
	public DataSourceAdapter(byte uid, String name, int icon) {
		this.uid = uid;
		this.name = name;
		this.icon = icon;
	}
	
	public void select(String caption) throws KeydbException {
	}

	public abstract InputStream getInputStream() throws KeydbException;

	public abstract byte[] load() throws KeydbException;

	public abstract void save(byte[] content) throws KeydbException;
	
	public boolean canLoad() {
		return true;
	}
	
	public boolean canSave() {
		return false;
	}
	
	public int getUid() {
		return this.uid;
	}
	
	public String getName() {
		return this.name;
	}

	public int getIcon() {
		return this.icon;
	}

	public abstract void serialize(SerializeStream out) throws IOException;
	public abstract void unserialize(UnserializeStream in) throws IOException;
}