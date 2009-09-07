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
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.ImageItem;
import javax.microedition.lcdui.StringItem;
import javax.microedition.midlet.*;

/// record store
import javax.microedition.rms.*;

import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.keydb.KeydbSource;
import net.sourceforge.keepassj2me.keydb.KeydbSourceFile;
import net.sourceforge.keepassj2me.keydb.KeydbSourceHttp;
import net.sourceforge.keepassj2me.keydb.KeydbSourceJar;
import net.sourceforge.keepassj2me.keydb.KeydbSourceRecordStore;

/**
 * Keepassj2me midlet
 * 
 * @author Naomaru Itoi
 * @author Stepan Strelets
 */
public class KeePassMIDlet extends MIDlet {
	private boolean firstTime = true;
	private Display mDisplay;
	Form splash = null;
	public static final String TITLE = "KeePass for J2ME";
	public static final String jarKdbDir = "/kdb";
	public static final String KDBRecordStoreName = "KeePassKDB";
	
	/**
	 * Constructor
	 */
	public KeePassMIDlet() {
	}

	/**
	 * Request password, key file, create and display KDB browser  
	 * @throws KeePassException 
	 */
	protected void openDatabaseAndDisplay(KeydbSource src) throws KeePassException {
		try {
			try {
				src.openDatabase(this);
				KeydbBrowser br = new KeydbBrowser(this, src.getDB());
				br.display();
			} finally {
				src.closeDatabase();
			}
		} catch (KeydbException e) {
			throw new KeePassException(e.getMessage());
		}
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
						this.getKeydbSourceRecordStore());
					break;
					
				case MainMenu.RESULT_HTTP:
					this.openDatabaseAndDisplay(
						this.getKeydbSourceHttp());
					break;
					
				case MainMenu.RESULT_JAR:
					this.openDatabaseAndDisplay(
						this.getKeydbSourceJar());
					break;
					
				case MainMenu.RESULT_FILE:
					this.openDatabaseAndDisplay(
						this.getKeydbSourceFile());
					break;
					
				case MainMenu.RESULT_INFORMATION:
					String hwrs = "";
					try {
						RecordStore rs = javax.microedition.rms.RecordStore.openRecordStore(Config.rsName, false);
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
					MessageBox box = new MessageBox(KeePassMIDlet.TITLE,
							KeePassMIDlet.TITLE+
							// #ifdef DEBUG
							" (DEBUG)"+
							// #endif
							"\r\n" +
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
							KeePassMIDlet.TITLE + " comes with ABSOLUTELY NO WARRANTY. This is free software, and you are welcome to redistribute it under certain conditions; for details visit: http://www.gnu.org/licenses/gpl-2.0.html"
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
	
	protected KeydbSourceRecordStore getKeydbSourceRecordStore() {
		return new KeydbSourceRecordStore();
	}
	
	/**
	 * Load KDB from internet
	 */
	protected KeydbSourceHttp getKeydbSourceHttp() {
		// download from HTTP
		// #ifdef DEBUG
			System.out.println("Download KDB from web server");
		// #endif
		
		URLCodeBox box = new URLCodeBox(KeePassMIDlet.TITLE, this);
		box.setURL(Config.getInstance().getDownloadUrl());
		box.display();
		if (box.getCommandType() != Command.CANCEL) {
			Config.getInstance().setDownloadUrl(box.getURL());
			
			return new KeydbSourceHttp(
					box.getURL(),
					box.getUserCode(),
					box.getPassCode(),
					box.getEncCode(),
					this);
		};
		return null;
	}
	
	/**
	 * Load KDB from file
	 * @param dir KDB file path
	 * @throws IOException
	 * @throws KeePassException
	 */
	protected KeydbSourceFile getKeydbSourceFile() {
		// we should use the FileConnection API to load from the file system
		FileBrowser fileBrowser = new FileBrowser(this, "Select KDB file", Icons.getInstance().getImageById(Icons.ICON_DIR), Icons.getInstance().getImageById(Icons.ICON_FILE), Icons.getInstance().getImageById(Icons.ICON_BACK));
		fileBrowser.setDir(Config.getInstance().getLastDir());
		fileBrowser.display();
		String dbUrl = fileBrowser.getUrl();
		if (dbUrl != null) {
			Config.getInstance().setLastDir(fileBrowser.getDir());
			return new KeydbSourceFile(dbUrl);
		};
		return null;
	}
	
	/**
	 * Load KDB from this midlet JAR
	 * @throws IOException
	 * @throws KeePassException
	 */
	protected KeydbSourceJar getKeydbSourceJar() throws KeePassException {
		// Use local KDB
		// read key database file
		JarBrowser jb = new JarBrowser(this, "Select KDB file", Icons.getInstance().getImageById(Icons.ICON_FILE));
		jb.setDir(KeePassMIDlet.jarKdbDir);
		jb.display();
		String jarUrl = jb.getUrl();
		if (jarUrl != null) {
			return new KeydbSourceJar(jarUrl);
		};
		return null;
	}
	
	public void startApp() {
		mDisplay = Display.getDisplay(this);

		if (firstTime) {
			splash = new Form(KeePassMIDlet.TITLE);
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
		MessageBox mb = new MessageBox(KeePassMIDlet.TITLE, msg, AlertType.ERROR, this, false, Icons.getInstance().getImageById(Icons.ICON_ALERT));
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
