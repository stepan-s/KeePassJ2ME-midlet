package net.sourceforge.keepassj2me;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreNotFoundException;

import net.sourceforge.keepassj2me.keydb.KeydbDatabase;

/**
 * Application config
 * @author Stepan Strelets
 */
public class Config {
	static protected Config instance = null;
	static protected final String rsName = "config"; 
	
	static protected final byte PARAM_LAST_DIR = 1;
	static protected final byte PARAM_DOWNLOAD_URL = 2;
	static protected final byte PARAM_WATCH_DOG_TIMEOUT = 3;
	static protected final byte PARAM_PAGE_SIZE = 4;
	static protected final byte PARAM_ICONS_DISABLED = 5;
	static protected final byte PARAM_SEARCH_BY = 6;
	static protected final byte PARAM_LAST_OPENED = 7;
	static protected final byte PARAM_ENCRYPTION_ROUNDS = 8;
	
	private boolean autoSaveEnabled = true;
	
	//values
	private String lastDir = null;
	private String downloadUrl = "http://keepassserver.info/download.php";
	private byte watchDogTimeout = 10;
	private byte pageSize = 50;
	private boolean iconsDisabled = false;
	private byte searchBy = 15;
	private byte[] lastOpened = null;
	private int rounds = 10000;
	
	private Config() {
		load();
	}
	
	static public Config getInstance() {
		if (instance == null) instance = new Config();
		return instance;
	}
	
	private void addParamString(RecordStore rs, byte param_type, String value) {
		try {
			if (value != null) {
				byte[] data = value.getBytes("UTF-8");
				byte[] buffer = new byte[1 + data.length];
				buffer[0] = param_type;
				System.arraycopy(data, 0, buffer, 1, data.length);
				rs.addRecord(buffer, 0, buffer.length);
			};
		} catch (Exception e) {
		}
	}
	private void addParamByte(RecordStore rs, byte param_type, byte value) {
		try {
			byte[] buffer = new byte[] {param_type, value};
			rs.addRecord(buffer, 0, buffer.length);
		} catch (Exception e) {
		}
	}
	private void addParamInt(RecordStore rs, byte param_type, int value) {
		try {
			byte[] buffer = new byte[] {param_type
					,(byte)(value & 0xFF)
					,(byte)((value >> 8) & 0xFF)
					,(byte)((value >> 16) & 0xFF)
					,(byte)((value >> 24) & 0xFF)};
			rs.addRecord(buffer, 0, buffer.length);
		} catch (Exception e) {
		}
	}
	private void addParamArray(RecordStore rs, byte param_type, byte[] value) {
		try {
			if (value != null) {
				byte[] buffer = new byte[1 + value.length];
				buffer[0] = param_type;
				System.arraycopy(value, 0, buffer, 1, value.length);
				rs.addRecord(buffer, 0, buffer.length);
			};
		} catch (Exception e) {
		}
	}
	
	private void autoSave() {
		if (autoSaveEnabled) save();
	}
	public void save() {
		try {
			try {
				RecordStore.deleteRecordStore(rsName);
			} catch (RecordStoreNotFoundException e1) {
			}
			RecordStore rs = RecordStore.openRecordStore(rsName, true);
			
			try {
				addParamString(rs, PARAM_LAST_DIR, lastDir);
				addParamString(rs, PARAM_DOWNLOAD_URL, downloadUrl);
				addParamByte(rs, PARAM_WATCH_DOG_TIMEOUT, watchDogTimeout);
				addParamByte(rs, PARAM_PAGE_SIZE, pageSize);
				addParamByte(rs, PARAM_ICONS_DISABLED, iconsDisabled ? (byte)1 : (byte)0);
				addParamByte(rs, PARAM_SEARCH_BY, searchBy);
				if (lastOpened != null) addParamArray(rs, PARAM_LAST_OPENED, lastOpened);
				addParamInt(rs, PARAM_ENCRYPTION_ROUNDS, rounds);
			} finally {
				rs.closeRecordStore();
			}
			
		} catch (Exception e) {
		}
	}
	public void load() {
		try {
			RecordStore rs = RecordStore.openRecordStore(rsName, true);
			
			try {
				byte[] buffer;
				for (int i = 1; i <= rs.getNumRecords(); ++i) {
					try {
						buffer = rs.getRecord(i);
						if (buffer.length > 0) {
							switch(buffer[0]) {
							case PARAM_LAST_DIR:
								lastDir = new String(buffer, 1, buffer.length - 1, "UTF-8");
								break;
							case PARAM_DOWNLOAD_URL:
								downloadUrl = new String(buffer, 1, buffer.length - 1, "UTF-8");
								break;
							case PARAM_WATCH_DOG_TIMEOUT:
								if (buffer.length == 2) watchDogTimeout = buffer[1];
								break;
							case PARAM_PAGE_SIZE:
								if (buffer.length == 2) pageSize = buffer[1];
								break;
							case PARAM_ICONS_DISABLED:
								if (buffer.length == 2) iconsDisabled = (buffer[1] != 0);
								break;
							case PARAM_SEARCH_BY:
								if (buffer.length == 2) searchBy = buffer[1];
								break;
							case PARAM_LAST_OPENED:
								lastOpened = new byte[buffer.length - 1];
								System.arraycopy(buffer, 1, lastOpened, 0, buffer.length - 1);
								break;
							case PARAM_ENCRYPTION_ROUNDS:
								if (buffer.length == 5) {
									rounds = (buffer[1] | (buffer[2] << 8) | (buffer[3] << 16) | (buffer[4] << 24));
								};
								break;
							};
						};
					} catch (Exception e) {
					}
				}
			} finally {
				rs.closeRecordStore();
			}
			
		} catch (Exception e) {
		}
	}
	
	public void setAutoSave(boolean enable) {
		this.autoSaveEnabled = enable;
	}
	
	public String getLastDir() {
		return lastDir;
	}
	public void setLastDir(String dir) {
		lastDir = dir;
		autoSave();
	}
	
	public String getDownloadUrl() {
		return downloadUrl;
	}
	public void setDownloadUrl(String url) {
		downloadUrl =  url;
		autoSave();
	}
	
	public int getWatchDogTimeOut() {
		return watchDogTimeout;
	}
	public void setWatchDogTimeout(byte timeout) {
		if (timeout < 0) timeout = 0;
		if (timeout > 60) timeout = 60;
		watchDogTimeout = timeout;
		autoSave();
	}

	public int getPageSize() {
		return pageSize;
	}
	public void setPageSize(byte size) {
		if (size < 20) size = 20;
		if (size > 100) size = 100;
		pageSize = size;
		autoSave();
	}
	
	public boolean isIconsDisabled() {
		return iconsDisabled;
	}
	public void setIconsDisabled(boolean disabled) {
		iconsDisabled = disabled;
		autoSave();
		Icons.getInstance().setIconsDisabled(disabled);
	}

	public byte getSearchBy() {
		return searchBy;
	}
	public void setSearchBy(byte by) {
		by &= KeydbDatabase.SEARCHBY_MASK;
		searchBy = (by == 0) ? KeydbDatabase.SEARCHBYTITLE : by;
		autoSave();
	}

	public byte[] getLastOpened() {
		return lastOpened;
	}
	public void setLastOpened(byte[] value) {
		lastOpened = value;
		autoSave();
	}

	public int getEncryptionRounds() {
		return rounds;
	}
	public void setEncryptionRounds(int value) {
		rounds = value;
		autoSave();
	}
}
