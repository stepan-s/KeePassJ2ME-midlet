package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.*;

/**
 * UI for KDB selection
 * 
 * @author Unknown
 * @author Stepan Strelets
 *
 */
public class KDBSelection implements CommandListener {
	public static final int RESULT_INVALID = -1;
	public static final int RESULT_LAST = 0;
	public static final int RESULT_HTTP = 1;
	public static final int RESULT_JAR = 2;
	public static final int RESULT_FILE = 3;
	public static final int RESULT_BOOKMARKS = 4;
	public static final int RESULT_INFORMATION = 5;
	public static final int RESULT_SETUP = 6;
	public static final int RESULT_EXIT = 7;
	private int[] index_to_command = new int[8];//must be >= command list size
	
	protected KeePassMIDlet midlet;
	private boolean isReady = false;
	private List list;
	//private Displayable dspBACK;
	private int result = -1;

	/**
	 * Construct and display
	 * @param midlet
	 */
	public KDBSelection(KeePassMIDlet midlet) {
		this.midlet = midlet;
		int index = 0;
		
		list = new List("Load KDB from:", Choice.IMPLICIT);

		// Stored KDB
		if (midlet.existsRecordStore()) {
			list.append("Last", midlet.getImageById(42));
			index_to_command[index++] = RESULT_LAST;
		};

		// Internet KDB
		list.append("Internet", midlet.getImageById(1));
		index_to_command[index++] = RESULT_HTTP;
		
		// KDB in JAR
		list.append("Midlet", midlet.getImageById(36));
		index_to_command[index++] = RESULT_JAR;
	
		// File KBD
		if (FileBrowser.isSupported()) {
			list.append("File", midlet.getImageById(48));
			index_to_command[index++] = RESULT_FILE;
		};
		
		// INFORMATION
		list.append("Information", midlet.getImageById(46));
		index_to_command[index++] = RESULT_INFORMATION;
		
		// SETUP
		list.append("Setup", midlet.getImageById(34));
		index_to_command[index++] = RESULT_SETUP;
		
		// EXIT
		list.append("Exit", midlet.getImageById(45));
		index_to_command[index++] = RESULT_EXIT;
		
		
		list.addCommand(new Command("Exit", Command.EXIT, 1));
		Command cmd_ok = new Command("OK", Command.OK, 1);
		list.addCommand(cmd_ok);
		list.setSelectCommand(cmd_ok);
		list.setCommandListener(this);

		// Previous Display
		//dspBACK = Display.getDisplay(midlet).getCurrent();

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

		// Display.getDisplay(midlet).setCurrent(dspBACK);
		
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
