package net.sourceforge.keepassj2me.datasource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.tools.RecordStoreDB;
import net.sourceforge.keepassj2me.tools.RecordStoreDBBrowser;

/**
 * @author Stepan Strelets
 */
public class DataSourceAdapterRecordStore extends DataSourceAdapter {
	public static final String KDBRecordStoreName = "KeePassKDB";
	private String name;

	public DataSourceAdapterRecordStore() {
		super(DataSourceRegistry.RS, "Memory", 42);
	}
	
	public boolean selectLoad(String caption) throws KeydbException {
		String name = RecordStoreDBBrowser.open("Select " + caption);
		if (name != null) {
			this.name = name;
			return true;
		} else {
			return false;
		}
	}
	public boolean selectSave(String caption) throws KeydbException {
		String name = RecordStoreDBBrowser.save("Select " + caption);
		if (name != null) {
			this.name = name;
			return true;
		} else {
			return false;
		}
	}
	
	public InputStream getInputStream() throws KeydbException {
		return new ByteArrayInputStream(this.load());
	}
	
	public byte[] load() throws KeydbException {
		try {
			return RecordStoreDB.getInstance().load(name);
		} catch (Exception e) {
			throw new KeydbException(e.getMessage());
		}
	}

	public void save(byte[] content) throws KeydbException {
		try {
			if (content != null) {
				RecordStoreDB.getInstance().save(name, content);
			};
		} catch (Exception e) {
			throw new KeydbException(e.getMessage());
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
}
