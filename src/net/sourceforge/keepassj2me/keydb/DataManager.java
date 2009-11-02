package net.sourceforge.keepassj2me.keydb;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.InputBox;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.KeydbBrowser;
import net.sourceforge.keepassj2me.ProgressForm;

public class DataManager {
	protected KeydbDatabase db = null;
	protected DataSource dbSource = null;
	protected DataSource keySource = null;
	protected MIDlet midlet = null;

	DataManager(MIDlet midlet) {
		this.midlet = midlet;
	}
	
	/**
	 * Helper for open and display database
	 * @throws KeePassException 
	 */
	public static void openDatabaseAndDisplay(MIDlet midlet) throws KeePassException {
		try {
			DataManager dm = new DataManager(midlet);
			try {
				dm.openDatabase(null, null);
				KeydbBrowser br = new KeydbBrowser(midlet, dm.getDB());
				br.display();
			} finally {
				dm.closeDatabase();
			}
		} catch (KeydbException e) {
			throw new KeePassException(e.getMessage());
		}
	}
	public static void openLastDatabaseAndDisplay(MIDlet midlet) throws KeePassException {
		
	}
	
	public KeydbDatabase getDB() {
		return db;
	}
	public void setDbSource(DataSource ds) {
		dbSource = ds;
	}
	public void setKeySource(DataSource ks) {
		keySource = ks;
	}
	
	public void openDatabase(DataSource dbs, DataSource ks) throws KeydbException {
		boolean ask = (dbs == null);
		
		if (ask) {
			dbs = this.selectSource(false, false);
			if (ask) dbs.select(midlet, "kdb file");
		};

		byte[] kdbBytes = dbs.load();
		
		if (kdbBytes == null)
			throw new KeydbException("KDB open error");
			
		InputBox pwb = new InputBox(midlet, "Enter KDB password", null, 64, TextField.PASSWORD);
		if (pwb.getResult() != null) {
			try {
				byte[] keyfile = null;
				
				if (ask) ks = this.selectSource(true, false);
				
				if (ks != null) {
					try {
						if (ask) ks.select(midlet, "key file");
						keyfile = KeydbUtil.hash(ks.getInputStream(), -1);
					} catch (KeydbException e) {
						//pass without key
					}
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
	protected DataSource selectSource(boolean allow_no, boolean save) throws KeydbException {
		DataSelect menu = new DataSelect(this.midlet, 0, allow_no, save);
		menu.waitForDone();
		int res = menu.getResult();
		menu = null;
		
		switch (res) {
		case DataSelect.RESULT_NONE:
			if (allow_no) return null;
			else throw new KeydbException("Nothing selected");
			
		case DataSelect.RESULT_RS:
			return new DataSourceRecordStore();
			
		case DataSelect.RESULT_HTTP:
			return new DataSourceHttpCrypt();
			
		case DataSelect.RESULT_JAR:
			return new DataSourceJar();
			
		case DataSelect.RESULT_FILE:
			return new DataSourceFile();

		default:
			throw new KeydbException("Unknown command");
		}
	}
}
