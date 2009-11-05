package net.sourceforge.keepassj2me.datasource;

import java.io.IOException;

import net.sourceforge.keepassj2me.keydb.KeydbException;

public class DataSourceRegistry {
	public static final byte NONE = 0;
	public static final byte FILE = 1;
	public static final byte JAR = 2;
	public static final byte HTTPC = 3;
	public static final byte RS = 4;
	public static final byte[] reg = {FILE, JAR, HTTPC, RS};

	public static DataSourceAdapter unserializeDataSource(UnserializeStream in) throws KeydbException {
		byte sourceId;
		try {
			sourceId = in.readByte();
			DataSourceAdapter ds = createDataSource(sourceId);
			ds.unserialize(in);
			return ds;
		} catch (IOException e) {
			throw new KeydbException(e.getMessage());
		}
	}
	
	public static DataSourceAdapter createDataSource(int uid) throws KeydbException {
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
		default:
			throw new KeydbException("Unknown data source type");
		}
		return ds;
	}
}
