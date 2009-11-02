package net.sourceforge.keepassj2me.keydb;

import java.io.InputStream;

import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;
import javax.microedition.rms.RecordStoreNotFoundException;

import net.sourceforge.keepassj2me.KeePassMIDlet;

/**
 * @author Stepan Strelets
 */
public class DataSourceRecordStore extends DataSource {
	public DataSourceRecordStore() {
	}
	
	public void select(MIDlet midlet, String caption) throws KeydbException {
		throw new KeydbException("Not implemented");
	}
	
	public InputStream getInputStream() throws KeydbException {
		throw new KeydbException("Not implemented");
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
	public static boolean canLoad() {
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
	
	public static boolean canSave() {
		return true;
	}
}
