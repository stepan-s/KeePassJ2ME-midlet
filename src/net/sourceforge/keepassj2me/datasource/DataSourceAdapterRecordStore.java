package net.sourceforge.keepassj2me.datasource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.L10nKeys.keys;
import net.sourceforge.keepassj2me.tools.RecordStoreDB;
import net.sourceforge.keepassj2me.tools.RecordStoreDBBrowser;

/**
 * Record store adapter
 * @author Stepan Strelets
 */
public class DataSourceAdapterRecordStore extends DataSourceAdapter {
	private String name;

	/**
	 * You dont need create adapters directly, use registry
	 */
	public DataSourceAdapterRecordStore() {
		super(DataSourceRegistry.RS, Config.getLocaleString(keys.DS_MEMORY), 42);
	}
	
	public boolean selectLoad(String caption) throws KeePassException {
		String name = RecordStoreDBBrowser.open(Config.getLocaleString(keys.DS_SELECT, new String[] {caption}));
		if (name != null) {
			this.name = name;
			return true;
		} else {
			return false;
		}
	}
	public boolean selectSave(String caption, String defaultName) throws KeePassException {
		String name = RecordStoreDBBrowser.save(Config.getLocaleString(keys.DS_SELECT, new String[] {caption}), defaultName);
		if (name != null) {
			this.name = name;
			return true;
		} else {
			return false;
		}
	}
	
	public InputStream getInputStream() throws KeePassException {
		return new ByteArrayInputStream(this.load());
	}
	
	public byte[] load() throws KeePassException {
		try {
			return RecordStoreDB.getInstance().load(name);
		} catch (Exception e) {
			throw new KeePassException(e.getMessage());
		}
	}

	public void save(byte[] content) throws KeePassException {
		try {
			if (content != null) {
				RecordStoreDB.getInstance().save(name, content);
			};
		} catch (Exception e) {
			throw new KeePassException(e.getMessage());
		}
	}

	/**
	 * Check if local store exists
	 * @return <code>true</code> if store exists, <code>false</code> if not
	 */
	public boolean canLoad() {
		boolean result = false;
		try {
			return (RecordStoreDB.getInstance().getCount() > 0);
		} catch (Exception e) {
		}
		return result;
	}
	
	public boolean canSave() {
		return true;
	}
	
	public void serialize(SerializeStream out) throws IOException {
		out.write(uid);
		out.writeUTF(this.name);
	}
	
	public void unserialize(UnserializeStream in) throws IOException {
		this.name = in.readUTF();
	}
	
	public String getCaption() {
		return "rs:"+getName();
	}
	public String getName() {
		return name;
	}
}
