package net.sourceforge.keepassj2me;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreNotFoundException;

public class Config {
	static protected Config instance = null;
	static protected final String rsName = "config"; 
	
	static protected final byte PARAM_LAST_DIR = 1;
	static protected final byte PARAM_DOWNLOAD_URL = 2;
	static protected final byte PARAM_WATH_DOG_TIMEOUT = 3;
	static protected final byte PARAM_SEARCH_PAGE_SIZE = 4;
	
	private boolean autoSaveEnabled = true;
	
	//values
	protected String lastDir = null;
	protected String downloadUrl = "http://keepassserver.info/download.php";
	protected byte watchDogTimeout = 10;
	protected byte searchPageSize = 50;
	
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
				addParamByte(rs, PARAM_WATH_DOG_TIMEOUT, watchDogTimeout);
				addParamByte(rs, PARAM_SEARCH_PAGE_SIZE, searchPageSize);
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
							case PARAM_WATH_DOG_TIMEOUT:
								if (buffer.length == 2) watchDogTimeout = buffer[1];
								break;
							case PARAM_SEARCH_PAGE_SIZE:
								if (buffer.length == 2) searchPageSize = buffer[1];
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
	
	public int getWathDogTimeOut() {
		return watchDogTimeout;
	}
	public void setWathDogTimeout(byte timeout) {
		if (timeout < 0) timeout = 0;
		if (timeout > 60) timeout = 60;
		watchDogTimeout = timeout;
		autoSave();
	}

	public int getSearchPageSize() {
		return searchPageSize;
	}
	public void setSearchPageSize(byte size) {
		if (size < 0) size = 0;
		if (size > 100) size = 100;
		searchPageSize = size;
		autoSave();
	}
}
