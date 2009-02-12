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

/**
 * Keepassj2me midlet
 * 
 * @author Naomaru Itoi
 * @author Stepan
 */
public class KeePassMIDlet extends MIDlet {
	static KeePassMIDlet myself = null;
	// private Form mMainForm;
	private boolean firstTime = true;
	private Display mDisplay;
	Image mIcon[];
	Image iconBack = null;
	
	/** The path to the directory icon resource. */
	private static final int DIR_ICON_RES = 48;
	/** The path to the file icon resource. */
	private static final int FILE_ICON_RES = 22;

	/**
	 * Constructor
	 */
	public KeePassMIDlet() {
		myself = this;
	}

	/**
	 * Request password, create and display KDB browser  
	 */
	public void openDatabaseAndDisplay() {
		PasswordBox pwb = new PasswordBox(this, "Enter KDB password", null, 64, TextField.PASSWORD);
		if (pwb.getResult() != null) {
			KDBBrowser br = new KDBBrowser(this);
			br.display(pwb.getResult());
		};
		// #ifdef DEBUG 
			System.out.println("openDatabaseAndDisplay() return");
		// #endif
	}
	
	/**
	 * Check if local store exists
	 * @return <code>true</code> if store exists, <code>false</code> if not
	 */
	public boolean existsRecordStore() {
		try {
			RecordStore rs = RecordStore.openRecordStore(Definition.KDBRecordStoreName, false);
			if (rs.getNumRecords() > 0) {
				rs.closeRecordStore();
				return true;
			}
			rs.closeRecordStore();
		} catch (RecordStoreException e) {
		}
		return false;
	}
	
/*	private void deleteRecordStore() {
		try {
			RecordStore.deleteRecordStore(Definition.KDBRecordStoreName);
		} catch (RecordStoreNotFoundException e) {
			// if it doesn't exist, it's OK
		} catch (RecordStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}*/

	/**
	 * Midlet main loop, show main menu and do action
	 */
	private void mainLoop() {
		do {
			KDBSelection kdbSelection = new KDBSelection(this);
			kdbSelection.waitForDone();
			int res = kdbSelection.getResult();
			kdbSelection = null;
			byte buf[] = null;
			
			try {
				switch (res) {
				case KDBSelection.RESULT_LAST:
					this.openDatabaseAndDisplay();
					break;
					
				case KDBSelection.RESULT_HTTP:
					buf = this.loadKdbFromHttp();
					if (buf != null) {
						this.storeKDBInRecordStore(buf, buf.length);
						this.openDatabaseAndDisplay();
					};
					break;
					
				case KDBSelection.RESULT_JAR:
					buf = this.loadKdbFromJar();
					if (buf != null) {
						storeKDBInRecordStore(buf, buf.length);
						this.openDatabaseAndDisplay();
					};
					break;
					
				case KDBSelection.RESULT_FILE:
					buf = this.loadKdbFromFile(null);
					if (buf != null) {
						storeKDBInRecordStore(buf, buf.length);
						this.openDatabaseAndDisplay();
					};
					break;
					
				case KDBSelection.RESULT_BOOKMARKS:
					this.doAlert("Not implemented");
					break;
					
				case KDBSelection.RESULT_INFORMATION:
					MessageBox box = new MessageBox(Definition.TITLE,
							Definition.TITLE+"\r\n" +
							"Version: "+this.getAppProperty("MIDlet-Version")+"\r\n\r\n" +
							"Project page: <http://sourceforge.net/projects/keepassj2me/>\r\n\r\n" +
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
							Definition.TITLE + " comes with ABSOLUTELY NO WARRANTY. This is free software, and you are welcome to redistribute it under certain conditions; for details visit: http://www.gnu.org/licenses/gpl-2.0.html",
							AlertType.INFO, this, false, getImageById(46));
					box.waitForDone();
					break;
					
				case KDBSelection.RESULT_EXIT:
					this.destroyApp(false);
					this.notifyDestroyed();
					return;
					
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
	 * Load KDB from internet
	 */
	private byte[] loadKdbFromHttp() {
		// download from HTTP
		// #ifdef DEBUG
			System.out.println("Download KDB from web server");
		// #endif
		
		URLCodeBox box = new URLCodeBox(Definition.TITLE, this);
		box.setURL(Definition.DEFAULT_KDB_URL);
		box.display();
		if (box.getCommandType() == Command.CANCEL) return null;
		
		// got secret code
		// now download kdb from web server
		Form waitForm = new Form(Definition.TITLE);
		waitForm.append("Downloading ...\n");
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
		return t.getContent();
	}
	
	/**
	 * Load KDB from file
	 * @param dir KDB file path
	 * @throws IOException
	 * @throws KeePassException
	 * @throws RecordStoreException
	 */
	private byte[] loadKdbFromFile(String dir) throws IOException, KeePassException, RecordStoreException {
		// we should use the FileConnection API to load from the file system
		FileBrowser fileBrowser = new FileBrowser(this, this.getImageById(DIR_ICON_RES), this.getImageById(FILE_ICON_RES), this.iconBack);
		fileBrowser.setDir(dir);
		fileBrowser.display();
		String dbUrl = fileBrowser.getUrl();
		if (dbUrl != null) {
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
	 * @throws RecordStoreException
	 */
	private byte[] loadKdbFromJar() throws IOException, KeePassException, RecordStoreException {
		// Use local KDB
		// read key database file
		JarBrowser jb = new JarBrowser(this, this.getImageById(FILE_ICON_RES));
		jb.setDir("/kdb");
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
						"Resource "+jarUrl+" is not found or not readable");
			}
			byte buf[] = new byte[is.available()];
			is.read(buf);
			return buf;
		};
		return null;
	}
	
	/**
	 * Store KDB in local store
	 * @param content Byte array with KDB
	 * @param length Size KDB data in <code>content</code>
	 * @throws RecordStoreException
	 */
	protected void storeKDBInRecordStore(byte[] content, int length)
			throws RecordStoreException {
		// delete record store
		try {
			RecordStore.deleteRecordStore(Definition.KDBRecordStoreName);
		} catch (RecordStoreNotFoundException e) {
			// if it doesn't exist, it's OK
		}

		// create record store
		RecordStore rs = RecordStore.openRecordStore(
				Definition.KDBRecordStoreName, true);

		rs.addRecord(content, 0, length);
		rs.closeRecordStore();
	}

	public void startApp() {
		mDisplay = Display.getDisplay(this);

		if (firstTime) {
			firstTime = false;
			try {
				// load the images
				mIcon = new Image[Definition.NUM_ICONS];
				for (int i = 0; i < Definition.NUM_ICONS; i++) {
					mIcon[i] = Image.createImage("/images/" + (i < 10 ? "0" : "") + i + ".png");
				}
				iconBack = Image.createImage("/images/back.png");
				
			} catch (IOException e) {
				// ignore the image loading failure the application can recover.
				doAlert(e.toString());
			}

			try {
				this.mainLoop();

				// #ifdef DEBUG
					System.out.println("startApp() done");
				// #endif
			} catch (Exception e) {
				doAlert(e.toString());
				return;
			}
		}
	}

	public void pauseApp() {
	}

	public void destroyApp(boolean unconditional) {
	}

	public void log(String str) {
		// mMainForm.append(new StringItem(null, str + "\r\n"));
	}

	static public void logS(String str) {
		myself.log(str);
	}

	/**
	 * Show alert
	 * @param msg message text
	 */
	public void doAlert(String msg) {
		MessageBox mb = new MessageBox(Definition.TITLE, msg, AlertType.ERROR, this, false, this.getImageById(2));
		mb.waitForDone();
		
		//Alert alert = new Alert(Definition.TITLE, msg, this.getImageById(2), AlertType.ERROR);
		//alert.setTimeout(5000);
		//mDisplay.setCurrent(alert);

/*		Alert alert = new Alert(Definition.TITLE);
		alert.setString(msg);
		alert.setTimeout(Alert.FOREVER);
		alert.addCommand(CMD_EXIT);
		alert.setCommandListener(this);
		mDisplay.setCurrent(alert);*/
	}

	/*
	 * Alert based message show message with specified title, msg, image, and
	 * whether it has yes/no buttons or only OK button
	 */
	/*
   	public void doMessage(String title, String msg, Image image, boolean yesno) {
		Displayable dspBACK;
		Alert alert = new Alert( title, msg, image, AlertType.INFO);
		alert.setTimeout( Alert.FOREVER );
		if (yesno == true) {
		    alert.addCommand(new Command("Yes", Command.OK, 1));
		    alert.addCommand(new Command("No", Command.CANCEL, 2));
		} else {
		    alert.addCommand(new Command("OK", Command.OK, 1));
		    // addCommand(new Command("Cancel", Command.CANCEL, 2));
		}
		dspBACK = Display.getDisplay(this).getCurrent();
		Display.getDisplay(this).setCurrent( alert );
		
		return;
    }
	 */

	/**
	 * Midlet exit
	 */
	public void exit() {
		// #ifdef DEBUG
			System.out.println("Exit!");
		// #endif

		destroyApp(true);
		notifyDestroyed();
	}

	/**
	 * Get image by index
	 * 
	 * @param index Image index
	 * @return Image or null
	 */
	public Image getImageById(int index) {
		if ((index >= 0) && (index < mIcon.length)) return mIcon[index];
		else return null;
	}
	
	/**
	 * Get image by index, if not found try get image by <code>def</code>
	 * 
	 * @param index primary Image index
	 * @param def second Image index (if primary not found)
	 * @return Image or null
	 */
	public Image getImageById(int index, int def) {
		Image res = getImageById(index);
		if (res == null) res = getImageById(def);
		return res;
	}
}
