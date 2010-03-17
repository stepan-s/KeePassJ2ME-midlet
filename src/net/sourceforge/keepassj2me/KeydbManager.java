package net.sourceforge.keepassj2me;

import java.io.IOException;

import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.datasource.DataSourceAdapter;
import net.sourceforge.keepassj2me.datasource.DataSourceRegistry;
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
			
			if (last) dm.loadRecentSources();
			
			try {
				dm.openDatabase(!last);
				if (dm.db == null) return;
				
				dm.saveSourcesAsRecent();
				dm.displayDatabase();
			} finally {
				dm.closeDatabase();
			}
		} catch (KeydbException e) {
			throw new KeePassException(e.getMessage());
		}
	}
	
	/**
	 * Try get and unserialize last data sources
	 * @throws KeePassException
	 * @throws KeydbException
	 */
	private void loadRecentSources() throws KeePassException, KeydbException {
		byte[] lastOpened = Config.getInstance().getLastOpened();
		if (lastOpened != null) {
			UnserializeStream in = new UnserializeStream(lastOpened);
			byte count;
			try {
				count = in.readByte();
			} catch (IOException e) {
				throw new KeePassException(e.getMessage());
			}
			this.setDbSource(DataSourceRegistry.unserializeDataSource(in));
			if (count > 1) this.setKeySource(DataSourceRegistry.unserializeDataSource(in));
		};
	}
	
	/**
	 * Try serialize data sources and store as last used 
	 */
	private void saveSourcesAsRecent() {
		try {
			SerializeStream out = new SerializeStream();
			DataSourceAdapter ks = this.getKeySource();
			out.writeByte(ks == null ? 1 : 2);
			this.getDbSource().serialize(out);
			if (ks != null) ks.serialize(out);
			Config.getInstance().setLastOpened(out.getBytes());
			ks = null;
			out = null;
		} catch (IOException e) {
		}
	}
	
	public static void createAndDisplayDatabase() throws KeePassException, KeydbException {
		KeydbManager dm = new KeydbManager();
		try {
			dm.createDatabase();
			if (dm.db != null) dm.displayDatabase();
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
	
	private void openDatabase(boolean ask) throws KeydbException, KeePassException {
		
		if (ask) {
			while(true) {
				dbSource = DataSourceRegistry.selectSource("KDB", false, false);
				if (dbSource.selectLoad("kdb file")) break;
			};
		};

		byte[] kdbBytes = dbSource.load();
		
		if (kdbBytes == null)
			throw new KeePassException("KDB open error");
			
		InputBox pwb = new InputBox("Enter KDB password", null, 64, TextField.PASSWORD);
		if (pwb.getResult() != null) {
			try {
				byte[] keyfile = null;
				
				if (ask) {
					while(true) {
						keySource = DataSourceRegistry.selectSource("KEY", true, false);
						if ((keySource == null) || keySource.selectLoad("key file")) {
							break;
						};
					};
				}
				
				if (keySource != null) {
					keyfile = KeydbUtil.hash(keySource.getInputStream(), -1);
				}
				
				db = new KeydbDatabase();
				try {
					ProgressForm form = new ProgressForm(true);
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
				throw new KeePassException(e.getMessage());
			}
		};
	}
	public void saveDatabase(boolean ask) throws KeydbException, KeePassException {
		if (ask) {
			DataSourceAdapter source;
			
			try {
				while(true) {
					source = DataSourceRegistry.selectSource("KDB", false, true);
					if (source.selectSave("kdb file", dbSource == null ? ".kdb" : dbSource.getName())) break;
				}
			} catch (KeePassException e) {
				//canceled
				return;
			}
			
			source.save(db.getEncoded());
			dbSource = source;
			this.saveSourcesAsRecent();
			
		} else {
			this.dbSource.save(db.getEncoded());
		}
		this.db.resetChangeIndicator();
	}
	public void closeDatabase() {
		if (this.db != null) {
			this.db.close();
			this.db = null;
		}
	}
	public void createDatabase() throws KeydbException, KeePassException {
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
			
			while (true) {
				keySource = DataSourceRegistry.selectSource("KEY", true, false);
				if (keySource != null) {
					if (keySource.selectLoad("key file")) {
						keyfile = KeydbUtil.hash(keySource.getInputStream(), -1);
						break;
					};
				} else {
					break;
				}
			};
			
			int rounds = Config.getInstance().getEncryptionRounds();
			InputBox ib = new InputBox("Encryption rounds", Integer.toString(rounds), 10, TextField.NUMERIC);
			String tmp = ib.getResult();
			if (tmp != null) {
				try {
					rounds = Integer.parseInt(tmp);
				} catch (NumberFormatException e) {
				};
			}
			
			db = new KeydbDatabase();
			try {
				ProgressForm form = new ProgressForm(true);
				db.setProgressListener(form);
				DisplayStack.push(form);
				try {
					db.create(pwb.getResult(), keyfile, rounds);
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
			throw new KeePassException(e.getMessage());
		}
	}
	public void changeMasterKeyDatabase() throws KeydbException, KeePassException {
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
			DataSourceAdapter source = null; 
			
			while (true) {
				if (db.isLocked()) return;
				
				source = DataSourceRegistry.selectSource("KEY", true, false);
				if (source != null) {
					if (source.selectLoad("key file")) {
						keyfile = KeydbUtil.hash(source.getInputStream(), -1);
						break;
					};
				} else {
					break;
				}
			};
			
			int rounds = db.getHeader().getEncryptionRounds();
			InputBox ib = new InputBox("Encryption rounds", Integer.toString(rounds), 10, TextField.NUMERIC);
			String tmp = ib.getResult();
			if (tmp != null) {
				try {
					rounds = Integer.parseInt(tmp);
				} catch (NumberFormatException e) {
				};
			}
			
			try {
				ProgressForm form = new ProgressForm(true);
				db.setProgressListener(form);
				DisplayStack.push(form);
				try {
					db.changeMasterKey(pwb.getResult(), keyfile, rounds);
					if (keySource != source) {
						keySource = source;
						saveSourcesAsRecent();
					};
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
			throw new KeePassException(e.getMessage());
		}
	}
	
	public void displayDatabase() {
		int menuitem = KeydbMenu.RESULT_BROWSE;
		do {
			KeydbBrowser br = null;
			
			String title = (this.db.isChanged()?"*":"")+(this.dbSource != null ? this.dbSource.getCaption() : "untitled");
			KeydbMenu menu = new KeydbMenu(title, (this.dbSource != null) && this.dbSource.canSave(), menuitem, this.db.isLocked());
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
					if (!this.db.isChanged() || MessageBox.showConfirm("Discard changes and close database?")) return;
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
				case KeydbMenu.RESULT_INFORMATION:
					String hw = 
						"Memory: free: "+java.lang.Runtime.getRuntime().freeMemory()/1024
							+"kB, total: "+java.lang.Runtime.getRuntime().totalMemory()/1024+"kB\r\n"
						;
					MessageBox box = new MessageBox(title,
							"Entries: "+db.getHeader().getEntriesCount()+"\r\n"
							+"Groups: "+db.getHeader().getGroupsCount()+"\r\n"
							+"Size: "+db.getSize()/1024+"kB\r\n"
							+"Encryption rounds: "+db.getHeader().getEncryptionRounds()+"\r\n"
							+"\r\n"+hw,
							AlertType.INFO, false, Icons.getInstance().getImageById(Icons.ICON_INFO));
					box.displayAndWait();
					break;
				case KeydbMenu.RESULT_CHANGE_MASTER_KEY:
					this.changeMasterKeyDatabase();
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
					
					ProgressForm form = new ProgressForm(true);
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
}
