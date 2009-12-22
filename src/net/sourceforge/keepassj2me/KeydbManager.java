package net.sourceforge.keepassj2me;

import java.io.IOException;

import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.datasource.DataSourceAdapter;
import net.sourceforge.keepassj2me.datasource.DataSourceRegistry;
import net.sourceforge.keepassj2me.datasource.DataSourceSelect;
import net.sourceforge.keepassj2me.datasource.SerializeStream;
import net.sourceforge.keepassj2me.datasource.UnserializeStream;
import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.keydb.KeydbUtil;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.InputBox;
import net.sourceforge.keepassj2me.tools.MessageBox;
import net.sourceforge.keepassj2me.tools.ProgressForm;

/**
 * @author Stepan Strelets
 */
public class KeydbManager {
	protected KeydbDatabase db = null;
	protected DataSourceAdapter dbSource = null;
	protected DataSourceAdapter keySource = null;

	KeydbManager() {
	}
	
	/**
	 * Helper for open and display database
	 * @throws KeePassException 
	 */
	public static void openAndDisplayDatabase(boolean last) throws KeePassException {
		try {
			KeydbManager dm = new KeydbManager();
			
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

				dm.displayDatabase();
			} finally {
				dm.closeDatabase();
			}
		} catch (KeydbException e) {
			throw new KeePassException(e.getMessage());
		}
	}
	
	public static void createAndDisplayDatabase() throws KeydbException {
		KeydbManager dm = new KeydbManager();
		try {
			dm.createDatabase();
			dm.displayDatabase();
		} finally {
			dm.closeDatabase();
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
			dbSource.select("kdb file");
		};

		byte[] kdbBytes = dbSource.load();
		
		if (kdbBytes == null)
			throw new KeydbException("KDB open error");
			
		InputBox pwb = new InputBox("Enter KDB password", null, 64, TextField.PASSWORD);
		if (pwb.getResult() != null) {
			try {
				byte[] keyfile = null;
				
				if (ask) {
					keySource = this.selectSource("KEY", true, false);
					if (keySource != null) keySource.select("key file");
				}
				
				if (keySource != null) {
					keyfile = KeydbUtil.hash(keySource.getInputStream(), -1);
				}
				
				db = new KeydbDatabase();
				try {
					ProgressForm form = new ProgressForm(KeePassMIDlet.TITLE, true);
					db.setProgressListener(form);
					DisplayStack.push(form);
					try {
						db.open(kdbBytes, pwb.getResult(), keyfile);
					} finally {
						DisplayStack.pop();
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
	public void saveDatabase(boolean ask) throws KeydbException {
		if (ask) {
			try {
				dbSource = this.selectSource("KDB", false, true);
				dbSource.select("kdb file");
			} catch (KeydbException e) {
				//canceled
				return;
			}
		};
		
		this.dbSource.save(db.getEncoded());
	}
	public void closeDatabase() {
		if (this.db != null) {
			this.db.close();
			this.db = null;
		}
	}
	public void createDatabase() throws KeydbException {
		InputBox pwb;
		do {
			pwb = new InputBox("Enter KDB password", null, 64, TextField.PASSWORD);
			if (pwb.getResult() == null) return;
			
			InputBox pwb2 = new InputBox("Repeat password", null, 64, TextField.PASSWORD);
			if (pwb2.getResult() == null) return;
			
			if (pwb.getResult().equals(pwb2.getResult())) break;
			else MessageBox.showAlert("Password mismatch");
		} while (true);
		
		try {
			byte[] keyfile = null;
			
			keySource = this.selectSource("KEY", true, false);
			if (keySource != null) {
				keySource.select("key file");
				keyfile = KeydbUtil.hash(keySource.getInputStream(), -1);
			}
			
			db = new KeydbDatabase();
			try {
				ProgressForm form = new ProgressForm(KeePassMIDlet.TITLE, true);
				db.setProgressListener(form);
				DisplayStack.push(form);
				try {
					db.create(pwb.getResult(), keyfile);
				} finally {
					DisplayStack.pop();
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
	}
	
	public void displayDatabase() {
		int menuitem = KeydbMenu.RESULT_BROWSE;
		do {
			KeydbBrowser br = null;
			
			KeydbMenu menu = new KeydbMenu((this.dbSource != null) && this.dbSource.canSave(), menuitem, this.db.isLocked());
			menu.displayAndWait();
			menuitem = menu.getResult();
			menu = null;
			try {
				try {
					this.db.reassureWatchDog();
				} catch (Exception e) {};
				
				switch(menuitem) {
				case KeydbMenu.RESULT_CLOSE:
					//FIXME: show confirm if database changed only
					if (MessageBox.showConfirm("Close database?")) return;
					break;
				case KeydbMenu.RESULT_BROWSE:
					br = new KeydbBrowser(this.db);
					br.display(KeydbBrowser.MODE_BROWSE);
					break;
				case KeydbMenu.RESULT_SEARCH:
					br = new KeydbBrowser(this.db);
					br.display(KeydbBrowser.MODE_SEARCH);
					break;
				case KeydbMenu.RESULT_SAVE:
					this.saveDatabase(false);
					break;
				case KeydbMenu.RESULT_SAVEAS:
					this.saveDatabase(true);
					break;
				case KeydbMenu.RESULT_OPTIONS:
					break;
				case KeydbMenu.RESULT_UNLOCK:
					this.unlockDB();
					break;
				default:
				}
			} catch (Exception e) {
				MessageBox.showAlert(e.getMessage());
			};
		} while (true);
	}
	
	private void unlockDB() {
		while (this.db.isLocked()) {
			InputBox pwb = new InputBox("Enter KDB password", null, 64, TextField.PASSWORD);
			if (pwb.getResult() != null) {
				try {
					byte[] keyfile = null;
					
					if (keySource != null) {
						keyfile = KeydbUtil.hash(keySource.getInputStream(), -1);
					}
					
					ProgressForm form = new ProgressForm(KeePassMIDlet.TITLE, true);
					db.setProgressListener(form);
					DisplayStack.push(form);
					try {
						db.unlock(pwb.getResult(), keyfile);
					} finally {
						DisplayStack.pop();
					};
					
				} catch (Exception e) {
					MessageBox.showAlert(e.getMessage());
				}
			} else {
				break;
			};
		};
	}
	
	/**
	 * Select data source
	 * @param allow_no if true may nothing to be selected - return null
	 * @return data source object
	 * @throws KeydbException
	 */
	protected DataSourceAdapter selectSource(String caption, boolean allow_no, boolean save) throws KeydbException {
		DataSourceSelect menu = new DataSourceSelect(save ? "Save "+caption+" to" : "Open "+caption+" from", 0, allow_no, save);
		menu.displayAndWait();
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
