package net.sourceforge.keepassj2me;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreNotFoundException;

public class Config {
	static protected Config instance = null;
	static protected final String rsName = "config"; 
	
	static protected final byte PARAM_LAST_DIR = 1;
	static protected final byte PARAM_DOWNLOAD_URL = 2;
	static protected final byte PARAM_WATH_DOG_TIMEOUT = 3;
	
	//values
	protected String last_dir = null;
	protected String download_url = "http://keepassserver.info/download.php";
	protected byte watch_dog_timeout = 10;
	
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
			byte[] buffer = new byte[] {param_type, watch_dog_timeout};
			rs.addRecord(buffer, 0, buffer.length);
		} catch (Exception e) {
		}
	}
	
	public void save() {
		try {
			try {
				RecordStore.deleteRecordStore(rsName);
			} catch (RecordStoreNotFoundException e1) {
			}
			RecordStore rs = RecordStore.openRecordStore(rsName, true);
			
			try {
				addParamString(rs, PARAM_LAST_DIR, last_dir);
				addParamString(rs, PARAM_DOWNLOAD_URL, download_url);
				addParamByte(rs, PARAM_WATH_DOG_TIMEOUT, watch_dog_timeout);
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
								last_dir = new String(buffer, 1, buffer.length - 1, "UTF-8");
								break;
							case PARAM_DOWNLOAD_URL:
								download_url = new String(buffer, 1, buffer.length - 1, "UTF-8");
								break;
							case PARAM_WATH_DOG_TIMEOUT:
								if (buffer.length == 2) watch_dog_timeout = buffer[1];
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
	
	public String getLastDir() {
		return last_dir;
	}
	public void setLastDir(String dir) {
		last_dir = dir;
		save();
	}
	
	public String getDownloadUrl() {
		return download_url;
	}
	public void setDownloadUrl(String url) {
		download_url =  url;
		save();
	}
	
	public int getWathDogTimeOut() {
		return watch_dog_timeout;
	}
	public void setWathDogTimeout(byte timeout) {
		if (timeout < 0) timeout = 0;
		if (timeout > 60) timeout = 60;
		watch_dog_timeout = timeout;
		save();
	}
}
