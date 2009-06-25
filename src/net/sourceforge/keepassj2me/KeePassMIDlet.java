/*
KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.sourceforge.keepassj2me;

// Java
import javax.microedition.io.*;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;

import java.io.InputStream;
import java.io.IOException;

/// record store
import javax.microedition.rms.*;

import net.sourceforge.keepassj2me.keydb.KeydbDatabase;

/**
 * Keepassj2me midlet
 * 
 * @author Naomaru Itoi
 * @author Stepan
 */
public class KeePassMIDlet extends MIDlet {
	private boolean firstTime = true;
	private Display mDisplay;
	Form splash = null;
	public static final String jarKdbDir = "/kdb";
	public static final String KDBRecordStoreName = "KeePassKDB";
	
	/**
	 * Constructor
	 */
	public KeePassMIDlet() {
	}

	/**
	 * Request password, create and display KDB browser  
	 * @throws KeePassException 
	 */
	protected void openDatabaseAndDisplay(byte[] kdbBytes) throws KeePassException {
		if (kdbBytes != null) {
			InputBox pwb = new InputBox(this, "Enter KDB password", null, 64, TextField.PASSWORD);
			if (pwb.getResult() != null) {
				try {
					KeydbDatabase db = new KeydbDatabase();
					try {
						try {
							ProgressForm form = new ProgressForm(Definition.TITLE, true);
							db.setProgressListener(form);
							mDisplay.setCurrent(form);
							db.open(kdbBytes, pwb.getResult(), null);
						} finally {
							mDisplay.setCurrent(splash);
						};
						KeydbBrowser br = new KeydbBrowser(this, db);
						br.display();
					} finally {
						db.close();
					}
				} catch (Exception e) {
					// #ifdef DEBUG
					e.printStackTrace();
					// #endif
					throw new KeePassException(e.getMessage());
				}
				
				//KDBBrowser br = new KDBBrowser(this);
				//br.decode(pwb.getResult(), kdbBytes);
				//br.display();
			};
		};
	}
	
	/**
	 * Midlet main loop, show main menu and do action
	 */
	protected void mainLoop() {
		int res = -1;
		do {
			MainMenu mainmenu = new MainMenu(this, res);
			mainmenu.waitForDone();
			res = mainmenu.getResult();
			mainmenu = null;
			mDisplay.setCurrent(splash);
			
			try {
				switch (res) {
				case MainMenu.RESULT_LAST:
					this.openDatabaseAndDisplay(
						this.loadKdbFromRecordStore());
					break;
					
				case MainMenu.RESULT_HTTP:
					this.openDatabaseAndDisplay(
						this.saveKdbToRecordStore(
							this.loadKdbFromHttp()));
					break;
					
				case MainMenu.RESULT_JAR:
					this.openDatabaseAndDisplay(
						this.loadKdbFromJar());
					break;
					
				case MainMenu.RESULT_FILE:
					this.openDatabaseAndDisplay(
						this.loadKdbFromFile());
					break;
					
				case MainMenu.RESULT_INFORMATION:
					String hwrs = "";
					try {
						RecordStore rs = javax.microedition.rms.RecordStore.openRecordStore(KeePassMIDlet.KDBRecordStoreName, false);
						hwrs = "used: "+rs.getSize()/1024+"kB, available: "+rs.getSizeAvailable()/1024+"kB";
					} catch (Exception e) {
						hwrs = "Unknown";
					};
					String hw = 
								"Platform: "+java.lang.System.getProperty("microedition.platform")+"\r\n"
								+"Locale: "+java.lang.System.getProperty("microedition.locale")+"\r\n"
								+"Configuration: "+java.lang.System.getProperty("microedition.configuration")+"\r\n"
								+"Profiles: "+java.lang.System.getProperty("microedition.profiles")+"\r\n"
								+"Memory: free: "+java.lang.Runtime.getRuntime().freeMemory()/1024
									+"kB, total: "+java.lang.Runtime.getRuntime().totalMemory()/1024+"kB\r\n"
								+"RecordStore: "+hwrs;
					MessageBox box = new MessageBox(Definition.TITLE,
							Definition.TITLE+"\r\n" +
							"Version: "+this.getAppProperty("MIDlet-Version")+"\r\n\r\n" +
							"Project page: <http://keepassj2me.sourceforge.net/>\r\n\r\n" +
							"License: GNU GPL v2 <http://www.gnu.org/licenses/gpl-2.0.html>\r\n\r\n" +
							"Authors:\r\n(In alphabetic order)\r\n" +
							"Bill Zwicky\r\n" +
							"Dominik Reichl\r\n" +
							"Kevin O'Rourke\r\n" +
							"Naomaru Itoi\r\n" +
							"Stepan Strelets\r\n\r\n" +
							"Thanks to:\r\n" +
							"David Vignoni (icons)\r\n" +
							"The Legion Of The Bouncy Castle <http://www.bouncycastle.org>\r\n\r\n" +
							Definition.TITLE + " comes with ABSOLUTELY NO WARRANTY. This is free software, and you are welcome to redistribute it under certain conditions; for details visit: http://www.gnu.org/licenses/gpl-2.0.html"
							+"\r\n\r\n"+hw,
							AlertType.INFO, this, false, Icons.getInstance().getImageById(Icons.ICON_INFO));
					box.waitForDone();
					break;
					
				case MainMenu.RESULT_SETUP:
					ConfigUI c = new ConfigUI(this);
					c.show();
					break;
					
				case MainMenu.RESULT_EXIT:
					this.exit();
					return; //<-exit from loop
					
				default:
					this.doAlert("Unknown command");
					break;
				}
			} catch (Exception e) {
				this.doAlert(e.getMessage());
			}
		} while (true);
	}
	
	/**
	 * Check if local store exists
	 * @return <code>true</code> if store exists, <code>false</code> if not
	 */
	protected boolean existsRecordStore() {
		boolean result = false;
		try {
			RecordStore rs = RecordStore.openRecordStore(KeePassMIDlet.KDBRecordStoreName, false);
			try {
				result = (rs.getNumRecords() > 0);
			} finally {
				rs.closeRecordStore();
			};
		} catch (RecordStoreException e) {
		}
		return result;
	}
	
	/**
	 * Store KDB in local store
	 * @param content Byte array with KDB
	 * @return content Byte array with KDB (same)
	 * @throws RecordStoreException
	 */
	protected byte[] saveKdbToRecordStore(byte[] content) throws RecordStoreException {
		if (content != null) {
			// delete record store
			try {
				RecordStore.deleteRecordStore(KeePassMIDlet.KDBRecordStoreName);
			} catch (RecordStoreNotFoundException e) {
				// if it doesn't exist, it's OK
			}
	
			// create record store
			RecordStore rs = RecordStore.openRecordStore(KeePassMIDlet.KDBRecordStoreName, true);
			try {
				rs.addRecord(content, 0, content.length);
			} finally {
				rs.closeRecordStore();
			};
		};
		return content;
	}

	protected byte[] loadKdbFromRecordStore() throws KeePassException {
		try {
			RecordStore rs = RecordStore.openRecordStore(KeePassMIDlet.KDBRecordStoreName, false);
			try {
				return rs.getRecord(1);
			} finally {
				rs.closeRecordStore();
			};
		} catch (Exception e) {
			throw new KeePassException(e.getMessage());
		}
	}
	
	/**
	 * Load KDB from internet
	 */
	protected byte[] loadKdbFromHttp() {
		// download from HTTP
		// #ifdef DEBUG
			System.out.println("Download KDB from web server");
		// #endif
		
		URLCodeBox box = new URLCodeBox(Definition.TITLE, this);
		box.setURL(Config.getInstance().getDownloadUrl());
		box.display();
		if (box.getCommandType() == Command.CANCEL) return null;
		
		Config.getInstance().setDownloadUrl(box.getURL());
		
		// got secret code
		// now download kdb from web server
		Form waitForm = new Form(Definition.TITLE);
		waitForm.append("Downloading ...\n");
		Displayable back = mDisplay.getCurrent(); 
		mDisplay.setCurrent(waitForm);
		HTTPConnectionThread t = new HTTPConnectionThread(
				box.getURL(),
				box.getUserCode(),
				box.getPassCode(),
				box.getEncCode(),
				this,
				waitForm);
		t.start();

		try {
			t.join();
		} catch (InterruptedException e) {
			// #ifdef DEBUG
				System.out.println(e.toString());
			// #endif
		}
		mDisplay.setCurrent(back);
		return t.getContent();
	}
	
	/**
	 * Load KDB from file
	 * @param dir KDB file path
	 * @throws IOException
	 * @throws KeePassException
	 */
	protected byte[] loadKdbFromFile() throws IOException, KeePassException {
		// we should use the FileConnection API to load from the file system
		FileBrowser fileBrowser = new FileBrowser(this, Icons.getInstance().getImageById(Icons.ICON_DIR), Icons.getInstance().getImageById(Icons.ICON_FILE), Icons.getInstance().getImageById(Icons.ICON_BACK));
		fileBrowser.setDir(Config.getInstance().getLastDir());
		fileBrowser.display();
		String dbUrl = fileBrowser.getUrl();
		if (dbUrl != null) {
			Config.getInstance().setLastDir(fileBrowser.getDir());
			// open the file
			FileConnection conn = (FileConnection) Connector.open(dbUrl, Connector.READ);
			if (!conn.exists()) {
				// #ifdef DEBUG
					System.out.println("File doesn't exist");
				// #endif
				throw new KeePassException("File does not exist: "
						+ conn.getPath() + conn.getName());
			}
			InputStream is = conn.openInputStream();
			
			// TODO what if the file is too big for a single array?
			byte buf[] = new byte[(int) conn.fileSize()];
			// TODO this read is blocking and may not read the whole file
			// #ifdef DEBUG
				int read =
			// #endif
				is.read(buf);
			conn.close();
			// #ifdef DEBUG
				System.out.println("Storing " + read + " bytes into buf.");
			// #endif
			return buf;
		};
		return null;
	}
	
	/**
	 * Load KDB from this midlet JAR
	 * @throws IOException
	 * @throws KeePassException
	 */
	protected byte[] loadKdbFromJar() throws IOException, KeePassException {
		// Use local KDB
		// read key database file
		JarBrowser jb = new JarBrowser(this, Icons.getInstance().getImageById(Icons.ICON_FILE));
		jb.setDir(KeePassMIDlet.jarKdbDir);
		jb.display();
		String jarUrl = jb.getUrl();
		if (jarUrl != null) {
			InputStream is = getClass().getResourceAsStream(jarUrl);
			if (is == null) {
				// #ifdef DEBUG
					System.out
						.println("InputStream is null ... file probably not found");
				// #endif
				throw new KeePassException(
						"Resource '"+jarUrl+"' is not found or not readable");
			}
			byte buf[] = new byte[is.available()];
			is.read(buf);
			return buf;
		};
		return null;
	}
	
	public void startApp() {
		mDisplay = Display.getDisplay(this);

		if (firstTime) {
			splash = new Form(Definition.TITLE);
			splash.append(new ImageItem("",
								Icons.getInstance().getImageById(Icons.ICON_LOGO),
								ImageItem.LAYOUT_CENTER | ImageItem.LAYOUT_NEWLINE_AFTER,
								"", ImageItem.PLAIN));
			StringItem label = new StringItem("Please wait", "");
			label.setLayout(StringItem.LAYOUT_CENTER);
			splash.append(label);
			mDisplay.setCurrent(splash);
			
			firstTime = false;
		} else {
			mDisplay.setCurrent(splash);
		}
		this.mainLoop();
	}

	public void pauseApp() {
	}

	public void destroyApp(boolean unconditional) {
	}

	/**
	 * Show alert
	 * @param msg message text
	 */
	public void doAlert(String msg) {
		MessageBox mb = new MessageBox(Definition.TITLE, msg, AlertType.ERROR, this, false, Icons.getInstance().getImageById(Icons.ICON_ALERT));
		mb.waitForDone();
	}

	/**
	 * Midlet exit
	 */
	public void exit() {
		destroyApp(true);
		notifyDestroyed();
	}
}
