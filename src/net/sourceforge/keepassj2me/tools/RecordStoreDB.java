package net.sourceforge.keepassj2me.tools;

import java.io.UnsupportedEncodingException;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordFilter;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.L10nConstants.keys;

/**
 * RecordStore database, handle named records
 * @author Stepan Strelets
 */
public class RecordStoreDB {
	/** RecordStore name (previous version) */
	public static final String KDBRecordStoreOldName = "KeePassKDB";
	
	/** Name of RecordStore holding records names */
	public static final String KDBRecordStoreNames = "KeePassKDBNames";
	/** Name of RecordStore holding records data */
	public static final String KDBRecordStoreData = "KeePassKDBData";
	protected static RecordStoreDB instance = null;
	
	private RecordStore names;
	private RecordStore data;
	
	/**
	 * Constructor
	 */
	public RecordStoreDB() {
		tryConvertOld();
	}
	
	private void open() throws Exception {
		try {
			names = RecordStore.openRecordStore(KDBRecordStoreNames, true);
			data = RecordStore.openRecordStore(KDBRecordStoreData, true);
		} catch (RecordStoreException e) {
			throw new Exception(e.getMessage());
		}
	}
	
	private void close() throws Exception {
		RecordStoreException ex = null; 
		try {
			if (names != null) {
				names.closeRecordStore();
				names = null;
			};
		} catch (RecordStoreException e) {
			ex = e;
		}
		try {
			if (data != null) {
				data.closeRecordStore();
				data = null;
			};
		} catch (RecordStoreException e) {
			ex = e;
		}
		if (ex != null) throw new  Exception(ex.getMessage());
	}
	
	private void tryConvertOld() {
		try {
			RecordStore rs = RecordStore.openRecordStore(KDBRecordStoreOldName, false);
			try {
				if (rs.getNumRecords() > 0) {
					save("untitled", rs.getRecord(1));
				}
			} finally {
				rs.closeRecordStore();
			};
			RecordStore.deleteRecordStore(KDBRecordStoreOldName);
		} catch (Exception e) {
		}
	}
	
	/**
	 * Get instance of RecordStoreDB
	 * @return instance
	 */
	static public RecordStoreDB getInstance() {
		if (instance == null) instance = new RecordStoreDB();
		return instance;
	}
	
	/**
	 * Get records in database
	 * @return records count
	 * @throws Exception
	 */
	public int getCount() throws Exception {
		try {
			int count;
			try {
				open();
				count = names.getNumRecords();
			} finally {
				close();
			};
			return count;
		} catch(Exception e) {
			throw new Exception(e.getMessage());
		}
	}
	
	/**
	 * Enumerate records names in database 
	 * @param receiver
	 * @throws Exception 
	 */
	public void enumerate(IRecordStoreListReceiver receiver) throws Exception {
		try {
			try {
				open();
				RecordEnumeration list = names.enumerateRecords(null, null, false);
				while(list.hasNextElement()) {
					byte[] rec = list.nextRecord();
					receiver.listRecord(new String(rec, 4, rec.length - 4, "UTF-8"));
				};
			} finally {
				close();
			};
		} catch(Exception e) {
			throw new Exception(e.getMessage());
		}
	}
	
	private byte[] getRecordName(final String name) {
		byte[] result = null;
		try {
			RecordEnumeration list = names.enumerateRecords(new RecordFilter() {
				public boolean matches(byte[] nm) {
					try {
						return name.equals(new String(nm, 4, nm.length - 4, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						return false;
					}
				}}, null, false);
			if (list.hasNextElement()) {
				result = list.nextRecord();
			};
		} catch(Exception e) {
		}
		return result;
	}

	private int getRecordId(final String name) {
		int result = -1;
		try {
			RecordEnumeration list = names.enumerateRecords(new RecordFilter() {
				public boolean matches(byte[] nm) {
					try {
						return name.equals(new String(nm, 4, nm.length - 4, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						return false;
					}
				}}, null, false);
			if (list.hasNextElement()) {
				result = list.nextRecordId();
			};
		} catch(Exception e) {
		}
		return result;
	}
	
	/**
	 * Check record with name
	 * @param name
	 * @return true if ex
	 */
	public boolean exists(String name) {
		byte[] rec = null;
		try {
			try {
				open();
				rec = getRecordName(name);
			} finally {
				close();
			};
		} catch(Exception e) {
		}
		return (rec != null);
	}
	
	/**
	 * Get record content by name
	 * @param name
	 * @return content
	 * @throws Exception 
	 */
	public byte[] load(String name) throws Exception {
		try {
			open();
			byte[] rec = getRecordName(name);
			if (rec != null) {
				int data_id = rec[0] | (rec[1] << 8) | (rec[2] << 16) | (rec[3] << 24);
				return data.getRecord(data_id);
			} else {
				throw new Exception(Config.getLocaleString(keys.RECORD_NOT_FOUND));
			}
		} finally {
			close();
		}
	}
	
	/**
	 * Save content to record with name
	 * @param name
	 * @param content 
	 * @param data
	 * @throws Exception 
	 */
	public void save(String name, byte[] content) throws Exception {
		try {
			open();
			byte[] rec = getRecordName(name);
			if (rec != null) {
				int data_id = rec[0] | (rec[1] << 8) | (rec[2] << 16) | (rec[3] << 24);
				data.setRecord(data_id, content, 0, content.length);
			} else {
				int data_id = data.addRecord(content, 0, content.length);
				byte[] nm = name.getBytes("UTF-8");
				rec = new byte[nm.length + 4];
				rec[0] = (byte)(data_id & 0xFF);
				rec[1] = (byte)((data_id >> 8) & 0xFF);
				rec[2] = (byte)((data_id >> 16) & 0xFF);
				rec[4] = (byte)((data_id >> 24) & 0xFF);
				System.arraycopy(nm, 0, rec, 4, nm.length);
				names.addRecord(rec, 0, rec.length);
			};
		} finally {
			close();
		};
	}
	
	/**
	 * Delete record by name
	 * @param name
	 */
	public void delete(String name) {
		try {
			try {
				open();
				int name_id = getRecordId(name);
				if (name_id != -1) {
					byte[] rec = names.getRecord(name_id);
					int data_id = rec[0] | (rec[1] << 8) | (rec[2] << 16) | (rec[3] << 24);
					names.deleteRecord(name_id);
					data.deleteRecord(data_id);
				}
			} finally {
				close();
			};
		} catch(Exception e) {
		}
	}
	
	/**
	 * Re
	 * @param old_name
	 * @param new_name
	 * @throws Exception
	 */
	public void rename(String old_name, String new_name) throws Exception {
		if (old_name.equals(new_name)) return;
		try {
			open();
			int name_id = getRecordId(old_name);
			if (name_id != -1) {
				if (getRecordId(new_name) != -1) throw new Exception(Config.getLocaleString(keys.RECORD_EXIST));
				
				byte[] rec = names.getRecord(name_id);
				int data_id = rec[0] | (rec[1] << 8) | (rec[2] << 16) | (rec[3] << 24);
				byte[] nm = new_name.getBytes("UTF-8");
				rec = new byte[nm.length + 4];
				rec[0] = (byte)(data_id & 0xFF);
				rec[1] = (byte)((data_id >> 8) & 0xFF);
				rec[2] = (byte)((data_id >> 16) & 0xFF);
				rec[4] = (byte)((data_id >> 24) & 0xFF);
				System.arraycopy(nm, 0, rec, 4, nm.length);
				names.setRecord(name_id, rec, 0, rec.length);
			} else {
				throw new Exception(Config.getLocaleString(keys.RECORD_NOT_FOUND));
			}
		} finally {
			close();
		};
	}
}
