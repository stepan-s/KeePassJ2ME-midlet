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

import javax.microedition.lcdui.AlertType;
import javax.microedition.midlet.MIDlet;
import javax.microedition.rms.RecordStore;

import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.MessageBox;

/**
 * Keepassj2me midlet
 * 
 * @author Naomaru Itoi
 * @author Stepan Strelets
 */
public class KeePassMIDlet extends MIDlet {
	private boolean firstTime = true;
	public static final String TITLE = "KeePass for J2ME";
	
	/**
	 * Constructor
	 */
	public KeePassMIDlet() {
	}

	/**
	 * Midlet main loop, show main menu and do action
	 */
	protected void mainLoop() {
		int res = -1;
		do {
			MainMenu mainmenu = new MainMenu(res);
			mainmenu.displayAndWait();
			res = mainmenu.getResult();
			mainmenu = null;
			
			try {
				switch (res) {
				case MainMenu.RESULT_LAST:
					KeydbManager.openAndDisplayDatabase(true);
					break;
					
				case MainMenu.RESULT_OPEN:
					KeydbManager.openAndDisplayDatabase(false);
					break;

				case MainMenu.RESULT_NEW:
					KeydbManager.createAndDisplayDatabase();
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
							"Version: "+this.getAppProperty("MIDlet-Version")+
								// #ifdef BETA
								" beta"+
								// #endif
								"\r\n\r\n" +
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
							AlertType.INFO, false, Icons.getInstance().getImageById(Icons.ICON_INFO));
					box.displayAndWait();
					break;
					
				case MainMenu.RESULT_SETUP:
					ConfigUI c = new ConfigUI();
					c.show();
					break;
					
				case MainMenu.RESULT_EXIT:
					this.exit();
					return; //<-exit from loop
					
				default:
					MessageBox.showAlert("Unknown command");
					break;
				}
			} catch (Exception e) {
				MessageBox.showAlert(e.getMessage());
			}
		} while (true);
	}
	
	public void startApp() {
		if (firstTime) {
			new DisplayStack(this);
			DisplayStack.pushSplash();
			firstTime = false;
		} else {
		}
		
		this.mainLoop();
	}

	public void pauseApp() {
	}

	public void destroyApp(boolean unconditional) {
	}

	/**
	 * Midlet exit
	 */
	public void exit() {
		destroyApp(true);
		notifyDestroyed();
	}
}
