package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.*;

/**
 * UI for KDB selection
 * 
 * @author Unknown
 * @author Stepan Strelets
 *
 */
public class MainMenu implements CommandListener {
	public static final int RESULT_INVALID = -1;
	public static final int RESULT_LAST = 0;
	public static final int RESULT_HTTP = 1;
	public static final int RESULT_JAR = 2;
	public static final int RESULT_FILE = 3;
	public static final int RESULT_INFORMATION = 4;
	public static final int RESULT_SETUP = 5;
	public static final int RESULT_EXIT = 6;
	private int[] index_to_command = new int[7];//must be >= command list size
	
	protected KeePassMIDlet midlet;
	private boolean isReady = false;
	private List list;
	private Displayable dspBACK;
	private int result = -1;

	/**
	 * Construct and display
	 * @param midlet
	 */
	public MainMenu(KeePassMIDlet midlet, int select) {
		this.midlet = midlet;
		int index = 0;
		Icons icons = Icons.getInstance();
		
		list = new List("Main menu", Choice.IMPLICIT);

		// Stored KDB
		if (midlet.existsRecordStore()) {
			list.append("Open last downloaded", icons.getImageById(Icons.ICON_OPEN_LAST_DOWNLOADED));
			index_to_command[index++] = RESULT_LAST;
		};

		// KDB in JAR
		if (JarBrowser.contentExists(KeePassMIDlet.jarKdbDir)) {
			list.append("Open from midlet", icons.getImageById(Icons.ICON_OPEN_FROM_JAR));
			index_to_command[index++] = RESULT_JAR;
		};
	
		// File KBD
		if (FileBrowser.isSupported()) {
			list.append("Open file", icons.getImageById(Icons.ICON_OPEN_FROM_FILE));
			index_to_command[index++] = RESULT_FILE;
		};

		// Internet KDB
		list.append("Download from internet", icons.getImageById(Icons.ICON_OPEN_FROM_INTERNET));
		index_to_command[index++] = RESULT_HTTP;
		
		// INFORMATION
		list.append("Information", icons.getImageById(Icons.ICON_INFO));
		index_to_command[index++] = RESULT_INFORMATION;
		
		// SETUP
		list.append("Setup", icons.getImageById(Icons.ICON_SETUP));
		index_to_command[index++] = RESULT_SETUP;
		
		// EXIT
		list.append("Exit", icons.getImageById(Icons.ICON_EXIT));
		index_to_command[index++] = RESULT_EXIT;
		
		for(int i = 0; i < index; ++i) {
			if (index_to_command[i] == select) {
				list.setSelectedIndex(i, true);
				break;
			}
		}
		
		list.addCommand(new Command("Exit", Command.EXIT, 1));
		Command cmd_ok = new Command("OK", Command.OK, 1);
		list.addCommand(cmd_ok);
		list.setSelectCommand(cmd_ok);
		list.setCommandListener(this);

		// Previous Display
		dspBACK = Display.getDisplay(midlet).getCurrent();

		// Display message
		// #ifdef DEBUG
			System.out.println("Display KDBSelection list");
		// #endif
		Display.getDisplay(midlet).setCurrent(list);
		// #ifdef DEBUG
			System.out.println("Displayed KDBSelection list");
		// #endif
	}

	public void commandAction(Command cmd, Displayable dsp) {
		// #ifdef DEBUG
			System.out.println("KDBSelection commandAction()");
		// #endif

		switch(cmd.getCommandType()) {
		case Command.OK:
			int index = list.getSelectedIndex();
			if ((index >= 0) && (index < index_to_command.length)) result = index_to_command[index];
			else result = RESULT_INVALID;
			break;
			
		case Command.EXIT:
			result = RESULT_EXIT;
			break;
			
		default:
			return;
		}

		Display.getDisplay(midlet).setCurrent(dspBACK);
		
		isReady = true;
		synchronized (this) {
			this.notify();
		}
	}

	/**
	 * Get user selection
	 * @return result on from RESULT_*
	 */
	public int getResult() {
		return result;
	}

	/**
	 * wait for user
	 */
	public void waitForDone() {
		try {
			while (!isReady) {
				synchronized (this) {
					this.wait();
				}
			}
		} catch (Exception e) {
			// #ifdef DEBUG
				System.out.println(e.toString());
			// #endif
		}
	}
}
