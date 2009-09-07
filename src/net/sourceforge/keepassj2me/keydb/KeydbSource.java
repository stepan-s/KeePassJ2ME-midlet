package net.sourceforge.keepassj2me.keydb;

import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.TextField;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.FileBrowser;
import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.InputBox;
import net.sourceforge.keepassj2me.JarBrowser;
import net.sourceforge.keepassj2me.KeePassMIDlet;
import net.sourceforge.keepassj2me.ProgressForm;
import net.sourceforge.keepassj2me.SelectKeyFileSource;

/**
 * KDB source provider
 * @author Stepan Strelets
 */
public abstract class KeydbSource {
	protected boolean readonly = true;
	protected KeydbDatabase db = null;
	
	public boolean isReadOnly() {
		return this.readonly;
	}
	public KeydbDatabase getDB() {
		return db;
	}
	public abstract byte[] load_kdb() throws KeydbException;
	public abstract void save_kdb(byte[] content) throws KeydbException;
	
	public void openDatabase(MIDlet midlet) throws KeydbException {
		byte[] kdbBytes = this.load_kdb();
		if (kdbBytes != null) {
			InputBox pwb = new InputBox(midlet, "Enter KDB password", null, 64, TextField.PASSWORD);
			if (pwb.getResult() != null) {
				try {
					byte[] keyfile = null;
					
					SelectKeyFileSource sel = new SelectKeyFileSource(midlet);
					switch(sel.getResult()) {
					case SelectKeyFileSource.FROM_FILE:
						FileBrowser fileBrowser = new FileBrowser(midlet, "Select key file", Icons.getInstance().getImageById(Icons.ICON_DIR), Icons.getInstance().getImageById(Icons.ICON_FILE), Icons.getInstance().getImageById(Icons.ICON_BACK));
						fileBrowser.setDir(Config.getInstance().getLastDir());
						fileBrowser.display();
						String filename = fileBrowser.getUrl();
						if (filename != null) keyfile = KeydbUtil.hashKeyFile(filename);
						break;
					case SelectKeyFileSource.FROM_JAR:
						JarBrowser jb = new JarBrowser(midlet, "Select key file", Icons.getInstance().getImageById(Icons.ICON_FILE));
						jb.setDir(KeePassMIDlet.jarKdbDir);
						jb.display();
						String jarUrl = jb.getUrl();
						if (jarUrl != null) keyfile = KeydbUtil.hash(getClass().getResourceAsStream(jarUrl), -1);
						break;
					};
					
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
		} else {
			throw new KeydbException("KDB open error");
		};
	}
	public void saveDatabase() throws KeydbException {
		this.save_kdb(db.getEncoded());
	}
	public void closeDatabase() {
		if (this.db != null) {
			this.db.close();
			this.db = null;
		}
	}
}
