package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.AlertType;

import net.sourceforge.keepassj2me.L10nConstants.keys;
import net.sourceforge.keepassj2me.keydb.KeydbUtil;
import net.sourceforge.keepassj2me.tools.MessageBox;

/**
 * MIDlet main thread 
 * @author Stepan Strelets
 */
public class KeePassMIDletThread extends Thread {
	private KeePassMIDlet midlet;
	
	/**
	 * Constructor
	 * @param midlet
	 */
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
					L10nResources lc = Config.getInstance().getLocale();
					MessageBox box = new MessageBox(KeePassMIDlet.TITLE,
							KeePassMIDlet.TITLE+"\r\n" +
							lc.getString(keys.INF_DESCRIPTION)+"\r\n"+
							lc.getString(keys.INF_VERSION)+": "+midlet.getAppProperty("MIDlet-Version")+
								// #ifdef BETA
								"-beta"+
								// #endif
								// #ifdef DEBUG
								"-debug"+
								// #endif
								"\r\n\r\n" +
							lc.getString(keys.INF_PROJECT_PAGE)+": <http://keepassj2me.sourceforge.net/>\r\n\r\n" +
							lc.getString(keys.INF_LICENSE)+": GNU GPL v2 <http://www.gnu.org/licenses/gpl-2.0.html>\r\n\r\n" +
							lc.getString(keys.INF_AUTHORS)+":\r\n" +
							"Bill Zwicky\r\n" +
							"Dominik Reichl\r\n" +
							"Kevin O'Rourke\r\n" +
							"Naomaru Itoi\r\n" +
							"Stepan Strelets\r\n\r\n" +
							lc.getString(keys.INF_THANKS)+":\r\n" +
							"David Vignoni (icons)\r\n" +
							"The Legion Of The Bouncy Castle <http://www.bouncycastle.org>\r\n\r\n" +
							lc.getString(keys.INF_WARRANTY, new String [] {KeePassMIDlet.TITLE, "http://www.gnu.org/licenses/gpl-2.0.html"})
							+"\r\n\r\n"
							+lc.getString(keys.INF_HARDWARE, KeydbUtil.getHWInfo()),
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
				}
			} catch (Exception e) {
				MessageBox.showAlert(e.getMessage());
			}
		} while (true);
	}
}
