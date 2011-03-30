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
	static protected final byte PARAM_LOCALE_NAME = 9;
	
	private boolean autoSaveEnabled = true;
	
	//values
	private String lastDir = null;
	private String downloadUrl = "http://keepassj2me.sourceforge.net/server/";
	private byte watchDogTimeout = 10;
	private byte pageSize = 50;
	private boolean iconsDisabled = false;
	private byte searchBy = 15;
	private RecentSources recent = null;
	private int rounds = 10000;
	private String locale_name = null;
	
	private L10n locale;
	
	private Config() {
		recent = new RecentSources();
		load();
		locale = L10n.getL10n(locale_name);
	}
	
	/**
	 * Get instance
	 * @return instance
	 */
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
	/**
	 * Save config to storage (dont need run exactly)
	 */
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
				for (int i = 0; i < recent.getSize(); ++i) {
					addParamArray(rs, PARAM_LAST_OPENED, recent.getSource(i));
				}
				addParamInt(rs, PARAM_ENCRYPTION_ROUNDS, rounds);
				if (locale_name != null) addParamString(rs, PARAM_LOCALE_NAME, locale_name);
			} finally {
				rs.closeRecordStore();
			}
			
		} catch (Exception e) {
		}
	}
	/**
	 * Load config from storage (dont need run exactly)
	 */
	public void load() {
		try {
			RecordStore rs = RecordStore.openRecordStore(rsName, true);
			recent.clean();
			
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
								String downloadUrl = new String(buffer, 1, buffer.length - 1, "UTF-8");
								//TODO: Remove this fix in future
								//Fix obsolete service URL
								if (!downloadUrl.equalsIgnoreCase("http://keepassserver.info/download.php"))
									this.downloadUrl = downloadUrl;
								//--------
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
								byte[] lastOpened = new byte[buffer.length - 1];
								System.arraycopy(buffer, 1, lastOpened, 0, buffer.length - 1);
								recent.setSource(lastOpened);
								break;
							case PARAM_ENCRYPTION_ROUNDS:
								if (buffer.length == 5) {
									rounds = (buffer[1] | (buffer[2] << 8) | (buffer[3] << 16) | (buffer[4] << 24));
								};
								break;
							case PARAM_LOCALE_NAME:
								locale_name = new String(buffer, 1, buffer.length - 1, "UTF-8");
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
	
	/**
	 * Enable or disable autosave when set any config parameters (disable need for batch)
	 * @param enable
	 */
	public void setAutoSave(boolean enable) {
		this.autoSaveEnabled = enable;
	}
	
	/**
	 * Get last opened KDB path
	 * @return path
	 */
	public String getLastDir() {
		return lastDir;
	}
	/**
	 * Set last opened KDB path
	 * @param dir path
	 */
	public void setLastDir(String dir) {
		lastDir = dir;
		autoSave();
	}
	
	/**
	 * Get URL for online KDB storage
	 * @return url
	 */
	public String getDownloadUrl() {
		return downloadUrl;
	}
	/**
	 * Set URL for online KDB storage
	 * @param url
	 */
	public void setDownloadUrl(String url) {
		downloadUrl = url;
		autoSave();
	}
	
	/**
	 * Get timeout for KDB browsing
	 * @return minutes
	 */
	public int getWatchDogTimeOut() {
		return watchDogTimeout;
	}
	/**
	 * Set timeout for KDB browsing
	 * @param timeout minutes 0-60
	 */
	public void setWatchDogTimeout(byte timeout) {
		if (timeout < 0) timeout = 0;
		if (timeout > 60) timeout = 60;
		watchDogTimeout = timeout;
		autoSave();
	}

	/**
	 * Get page size (for pagination)
	 * @return page size in items
	 */
	public int getPageSize() {
		return pageSize;
	}
	/**
	 * Set page size (for pagination)
	 * @param size page size in items
	 */
	public void setPageSize(byte size) {
		if (size < 20) size = 20;
		if (size > 100) size = 100;
		pageSize = size;
		autoSave();
	}
	
	/**
	 * Get status icons show/dont show
	 * @return true if icons disabled
	 */
	public boolean isIconsDisabled() {
		return iconsDisabled;
	}
	/**
	 * Set icon status show/dont show
	 * @param disabled
	 */
	public void setIconsDisabled(boolean disabled) {
		iconsDisabled = disabled;
		autoSave();
		Icons.getInstance().setIconsDisabled(disabled);
	}

	/**
	 * Get search flags
	 * @return flags
	 */
	public byte getSearchBy() {
		return searchBy;
	}
	/**
	 * Set search flags
	 * @param by flags
	 */
	public void setSearchBy(byte by) {
		by &= KeydbDatabase.SEARCHBY_MASK;
		searchBy = (by == 0) ? KeydbDatabase.SEARCHBYTITLE : by;
		autoSave();
	}

	/**
	 * Get last opened data source
	 * @param index source index in recent list
	 * @return serialized data source adapter
	 */
	public byte[] getLastOpened(int index) {
		return recent.getSource(index);
	}
	/**
	 * Set last opened data source
	 * @param value serialized data source adapter
	 */
	public void setLastOpened(byte[] value) {
		recent.setSource(value);
		autoSave();
	}
	/**
	 * Is last opened sources known
	 * @return true if known
	 */
	public boolean isLastOpened() {
		return recent.getSize() > 0;
	}
	/**
	 * Get list recent sources
	 * @return recent sources list
	 */
	public RecentSources getRecentSources() {
		return recent;
	}

	/**
	 * Get default encryption rounds
	 * @return rounds
	 */
	public int getEncryptionRounds() {
		return rounds;
	}
	/**
	 * Set default encryption rounds
	 * @param value rounds
	 */
	public void setEncryptionRounds(int value) {
		rounds = value;
		autoSave();
	}
	
	/**
	 * Get current locale resources
	 * @return locale
	 */
	public L10n getLocale() {
		return locale;
	}
	
	/**
	 * Gets the value for the specified key and current locale
	 * @param key resource key
	 * @return String values associated to the key
	 */
	static public String getLocaleString(String key) {
		return Config.getInstance().getLocale().getString(key);
	}
	
	/**
	 * Gets the value for the specified key and current locale, replace {N} tags in value with params (N - param index)
	 * @param key resource key
	 * @param params parameters to be formated
	 * @return String values associated to the key
	 */
	static public String getLocaleString(String key, String[] params) {
		return Config.getInstance().getLocale().getString(key, params);
	}

	/**
	 * Get locale name
	 * @return locale name
	 */
	public String getLocaleName() {
		return locale_name;
	}
	/**
	 * Set locale name
	 * @param name locale name
	 */
	public void setLocaleName(String name) {
		locale.setLocale(name);
		locale_name = name;
		autoSave();
	}
}
