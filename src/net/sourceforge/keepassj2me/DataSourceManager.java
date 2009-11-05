package net.sourceforge.keepassj2me;

import java.io.IOException;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.datasource.DataSourceAdapter;
import net.sourceforge.keepassj2me.datasource.DataSourceRegistry;
import net.sourceforge.keepassj2me.datasource.DataSourceSelect;
import net.sourceforge.keepassj2me.datasource.SerializeStream;
import net.sourceforge.keepassj2me.datasource.UnserializeStream;
import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.keydb.KeydbUtil;
import net.sourceforge.keepassj2me.tools.InputBox;
import net.sourceforge.keepassj2me.tools.ProgressForm;

/**
 * @author Stepan Strelets
 */
public class DataSourceManager {
	protected KeydbDatabase db = null;
	protected DataSourceAdapter dbSource = null;
	protected DataSourceAdapter keySource = null;
	protected MIDlet midlet = null;

	DataSourceManager(MIDlet midlet) {
		this.midlet = midlet;
	}
	
	/**
	 * Helper for open and display database
	 * @throws KeePassException 
	 */
	public static void openDatabaseAndDisplay(MIDlet midlet, boolean last) throws KeePassException {
		try {
			DataSourceManager dm = new DataSourceManager(midlet);
			
			//try unserialize last data sources
			if (last) {
				byte[] lastOpened = Config.getInstance().getLastOpened();
				if (lastOpened != null) {
					UnserializeStream in = new UnserializeStream(lastOpened);
					byte count;
					try {
						count = in.readByte();
					} catch (IOException e) {
						throw new KeePassException(e.getMessage());
					}
					dm.setDbSource(DataSourceRegistry.unserializeDataSource(in));
					if (count > 1) dm.setKeySource(DataSourceRegistry.unserializeDataSource(in));
					in = null;
				};
				lastOpened = null;
			};
			
			try {
				dm.openDatabase(!last);
				
				//try serialize data sources and store as last opened
				try {
					SerializeStream out = new SerializeStream();
					DataSourceAdapter ks = dm.getKeySource();
					out.writeByte(ks == null ? 1 : 2);
					dm.getDbSource().serialize(out);
					if (ks != null) ks.serialize(out);
					Config.getInstance().setLastOpened(out.getBytes());
					ks = null;
					out = null;
				} catch (IOException e) {
				}
				
				KeydbBrowser br = new KeydbBrowser(midlet, dm.getDB());
				br.display();
			} finally {
				dm.closeDatabase();
			}
		} catch (KeydbException e) {
			throw new KeePassException(e.getMessage());
		}
	}
	
	public KeydbDatabase getDB() {
		return db;
	}
	public void setDbSource(DataSourceAdapter ds) {
		dbSource = ds;
	}
	public void setKeySource(DataSourceAdapter ks) {
		keySource = ks;
	}
	public DataSourceAdapter getDbSource() {
		return dbSource;
	}
	public DataSourceAdapter getKeySource() {
		return keySource;
	}
	
	private void openDatabase(boolean ask) throws KeydbException {
		
		if (ask) {
			dbSource = this.selectSource("KDB", false, false);
			if (ask) dbSource.select(midlet, "kdb file");
		};

		byte[] kdbBytes = dbSource.load();
		
		if (kdbBytes == null)
			throw new KeydbException("KDB open error");
			
		InputBox pwb = new InputBox(midlet, "Enter KDB password", null, 64, TextField.PASSWORD);
		if (pwb.getResult() != null) {
			try {
				byte[] keyfile = null;
				
				if (ask) {
					keySource = this.selectSource("KEY", true, false);
					if (keySource != null) keySource.select(midlet, "key file");
				}
				
				if (keySource != null) {
					keyfile = KeydbUtil.hash(keySource.getInputStream(), -1);
				}
				
				db = new KeydbDatabase();
				try {
					Displayable back = Display.getDisplay(midlet).getCurrent();
					try {
						ProgressForm form = new ProgressForm(KeePassMIDlet.TITLE, true);
						db.setProgressListener(form);
						Display.getDisplay(midlet).setCurrent(form);
						db.open(kdbBytes, pwb.getResult(), keyfile);
					} finally {
						Display.getDisplay(midlet).setCurrent(back);
					};
				} catch(Exception e) {
					db.close();
					throw e;
				}
			} catch (Exception e) {
				// #ifdef DEBUG
				e.printStackTrace();
				// #endif
				throw new KeydbException(e.getMessage());
			}
		};
	}
	public void saveDatabase() throws KeydbException {
		this.dbSource.save(db.getEncoded());
	}
	public void closeDatabase() {
		if (this.db != null) {
			this.db.close();
			this.db = null;
		}
	}
	
	/**
	 * Select data source
	 * @param allow_no if true may nothing to be selected - return null
	 * @return data source object
	 * @throws KeydbException
	 */
	protected DataSourceAdapter selectSource(String caption, boolean allow_no, boolean save) throws KeydbException {
		DataSourceSelect menu = new DataSourceSelect(this.midlet, save ? "Save "+caption+" to" : "Open "+caption+" from", 0, allow_no, save);
		menu.waitForDone();
		int res = menu.getResult();
		menu = null;
		
		switch (res) {
		case DataSourceSelect.RESULT_NONE:
			if (allow_no) return null;
			else throw new KeydbException("Nothing selected");
			
		default:
			return DataSourceRegistry.createDataSource(res);
		}
	}
}
