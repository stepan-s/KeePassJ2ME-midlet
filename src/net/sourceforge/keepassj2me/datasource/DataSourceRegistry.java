package net.sourceforge.keepassj2me.datasource;

import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.keydb.KeydbException;

public class DataSourceRegistry {
	//this numbers may be stored in config, please don`t change  
	public static final byte NONE = 0;
	public static final byte FILE = 1;
	public static final byte JAR = 2;
	public static final byte HTTPC = 3;
	public static final byte RS = 4;
	public static final byte HTTP = 5;
	public static final byte[] reg = {FILE, JAR, HTTPC, RS
	// #ifdef DEBUG
		, HTTP
	// #endif
	};

	/**
	 * Unserialize data source from bytes pack
	 * @param in
	 * @return
	 * @throws KeePassException
	 */
	public static DataSourceAdapter unserializeDataSource(UnserializeStream in) throws KeePassException {
		byte sourceId;
		try {
			sourceId = in.readByte();
			DataSourceAdapter ds = createDataSource(sourceId);
			ds.unserialize(in);
			return ds;
		} catch (Exception e) {
			throw new KeePassException(e.getMessage());
		}
	}
	
	/**
	 * Create data source object by uid
	 * @param uid
	 * @return
	 * @throws KeydbException
	 */
	public static DataSourceAdapter createDataSource(int uid) throws KeePassException {
		DataSourceAdapter ds;
		switch(uid) {
		case FILE:
			ds = new DataSourceAdapterFile();
			break;
		case JAR:
			ds = new DataSourceAdapterJar();
			break;
		case HTTPC:
			ds = new DataSourceAdapterHttpCrypt();
			break;
		case RS:
			ds = new DataSourceAdapterRecordStore();
			break;
	// #ifdef DEBUG
		case HTTP:
			ds = new DataSourceAdapterHttp();
			break;
	// #endif
		default:
			throw new KeePassException("Unknown data source type");
		}
		return ds;
	}
	
	/**
	 * Select data source
	 * @param caption reason representation
	 * @param allow_no allow select nothing, return <code>null</code>
	 * @param save select source for saving <code>true</code> or loading <code>false</code>
	 * @return data source object (<code>DataSourceAdapter</code> descendant)
	 * @throws KeydbException
	 */
	public static DataSourceAdapter selectSource(String caption, boolean allow_no, boolean save) throws KeePassException {
		DataSourceSelect menu = new DataSourceSelect(save ? "Save "+caption+" to" : "Open "+caption+" from", 0, allow_no, save);
		menu.displayAndWait();
		int res = menu.getResult();
		menu = null;
		
		switch (res) {
		case DataSourceSelect.RESULT_NONE:
			if (allow_no) return null;
			else throw new KeePassException("Nothing selected");
			
		case DataSourceSelect.RESULT_CANCEL:
			throw new KeePassException("Canceled");
			
		default:
			return DataSourceRegistry.createDataSource(res);
		}
	}
}
