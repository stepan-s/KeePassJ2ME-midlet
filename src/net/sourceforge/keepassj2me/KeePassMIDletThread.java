package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.AlertType;
import javax.microedition.rms.RecordStore;

import net.sourceforge.keepassj2me.tools.MessageBox;

/**
 * MIDlet main thread 
 * @author Stepan Strelets
 */
public class KeePassMIDletThread extends Thread {
	private KeePassMIDlet midlet;
	
	public KeePassMIDletThread(KeePassMIDlet midlet) {
		this.midlet = midlet;
	}

	/**
	 * main loop, show main menu and do action
	 */
	public void run() {
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
							KeePassMIDlet.TITLE+"\r\n" +
							"Version: "+midlet.getAppProperty("MIDlet-Version")+
								// #ifdef BETA
								"-beta"+
								// #endif
								// #ifdef DEBUG
								"-debug"+
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
					midlet.exit();
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
}
