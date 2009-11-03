package net.sourceforge.keepassj2me.keydb;

import java.io.IOException;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.InputBox;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.KeydbBrowser;
import net.sourceforge.keepassj2me.ProgressForm;

/**
 * @author Stepan Strelets
 */
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
	public static void openDatabaseAndDisplay(MIDlet midlet, boolean last) throws KeePassException {
		try {
			DataManager dm = new DataManager(midlet);
			
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
					dm.setDbSource(factory(in));
					if (count > 1) dm.setKeySource(factory(in));
					in = null;
				};
				lastOpened = null;
			};
			
			try {
				dm.openDatabase(!last);
				
				//try serialize data sources and store as last opened
				try {
					SerializeStream out = new SerializeStream();
					DataSource ks = dm.getKeySource();
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
	public void setDbSource(DataSource ds) {
		dbSource = ds;
	}
	public void setKeySource(DataSource ks) {
		keySource = ks;
	}
	public DataSource getDbSource() {
		return dbSource;
	}
	public DataSource getKeySource() {
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
				
				if (ask) keySource = this.selectSource("KEY", true, false);
				
				if (keySource != null) {
					try {
						if (ask) keySource.select(midlet, "key file");
						keyfile = KeydbUtil.hash(keySource.getInputStream(), -1);
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
	protected DataSource selectSource(String caption, boolean allow_no, boolean save) throws KeydbException {
		DataSelect menu = new DataSelect(this.midlet, save ? "Save "+caption+" to" : "Open "+caption+" from", 0, allow_no, save);
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

	public static DataSource factory(UnserializeStream in) throws KeydbException {
		byte sourceId;
		try {
			sourceId = in.readByte();
			DataSource ds;
			switch(sourceId) {
			case DataSourceFile.uid:
				ds = new DataSourceFile();
				break;
			case DataSourceJar.uid:
				ds = new DataSourceJar();
				break;
			case DataSourceHttpCrypt.uid:
				ds = new DataSourceHttpCrypt();
				break;
			case DataSourceRecordStore.uid:
				ds = new DataSourceRecordStore();
				break;
			default:
				throw new KeydbException("Unknown data source or data wrong");
			}
			ds.unserialize(in);
			return ds;
		} catch (IOException e) {
			throw new KeydbException(e.getMessage());
		}
	}
}
