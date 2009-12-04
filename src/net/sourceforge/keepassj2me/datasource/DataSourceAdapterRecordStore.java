package net.sourceforge.keepassj2me.datasource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.keydb.KeydbException;

/**
 * @author Stepan Strelets
 */
public class DataSourceAdapterRecordStore extends DataSourceAdapter {
	public DataSourceAdapterRecordStore() {
		super(DataSourceRegistry.RS, "Memory", 42);
	}
	
	public InputStream getInputStream() throws KeydbException {
		return new ByteArrayInputStream(this.load());
	}
	
	public byte[] load() throws KeydbException {
		try {
			RecordStore rs = RecordStore.openRecordStore(KeePassMIDlet.KDBRecordStoreName, false);
			try {
				return rs.getRecord(1);
			} finally {
				rs.closeRecordStore();
			};
		} catch (Exception e) {
			throw new KeydbException(e.getMessage());
		}
	}

	public void save(byte[] content) throws KeydbException {
		try {
			if (content != null) {
				// delete record store
				try {
					RecordStore.deleteRecordStore(KeePassMIDlet.KDBRecordStoreName);
				} catch (RecordStoreNotFoundException e) {
					// if it doesn't exist, it's OK
				}
		
				// create record store
				RecordStore rs = RecordStore.openRecordStore(KeePassMIDlet.KDBRecordStoreName, true);
				try {
					rs.addRecord(content, 0, content.length);
				} finally {
					rs.closeRecordStore();
				};
			};
		} catch (RecordStoreException e) {
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
			RecordStore rs = RecordStore.openRecordStore(KeePassMIDlet.KDBRecordStoreName, false);
			try {
				result = (rs.getNumRecords() > 0);
			} finally {
				rs.closeRecordStore();
			};
		} catch (RecordStoreException e) {
		}
		return result;
	}
	
	public boolean canSave() {
		return true;
	}
	
	public void serialize(SerializeStream out) throws IOException {
		out.write(uid);
	}
	
	public void unserialize(UnserializeStream in) throws IOException {
	}
}
