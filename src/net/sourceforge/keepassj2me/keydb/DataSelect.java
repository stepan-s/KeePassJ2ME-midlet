package net.sourceforge.keepassj2me.keydb;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.Icons;

public class DataSelect implements CommandListener {
	public static final int RESULT_INVALID = -1;
	public static final int RESULT_NONE = 0;
	public static final int RESULT_RS = 1;
	public static final int RESULT_HTTP = 2;
	public static final int RESULT_JAR = 3;
	public static final int RESULT_FILE = 4;
	private int[] index_to_command = new int[5];//must be >= command list size
	
	protected MIDlet midlet;
	private boolean isReady = false;
	private List list;
	private Displayable dspBACK;
	private int result = -1;

	/**
	 * Construct and display
	 * @param midlet
	 */
	public DataSelect(MIDlet midlet, int select, boolean allow_no, boolean save) {
		this.midlet = midlet;
		int index = 0;
		Icons icons = Icons.getInstance();
		
		list = new List(save ? "Save to" : "Open from", Choice.IMPLICIT);

		// No
		if (allow_no) {
			list.append("No", icons.getImageById(Icons.ICON_EXIT));
			index_to_command[index++] = RESULT_NONE;
		};
		
		// RS
		if (save ? DataSourceRecordStore.canSave() : DataSourceRecordStore.canLoad()) {
			list.append("RecordStore", icons.getImageById(Icons.ICON_OPEN_FROM_RS));
			index_to_command[index++] = RESULT_RS;
		};

		// JAR
		if (save ? DataSourceJar.canSave() : DataSourceJar.canLoad()) {
			list.append("Midlet", icons.getImageById(Icons.ICON_OPEN_FROM_JAR));
			index_to_command[index++] = RESULT_JAR;
		};
	
		// File
		if (save ? DataSourceFile.canSave() : DataSourceFile.canLoad()) {
			list.append("File", icons.getImageById(Icons.ICON_OPEN_FROM_FILE));
			index_to_command[index++] = RESULT_FILE;
		};

		// Internet
		if (save ? DataSourceHttpCrypt.canSave() : DataSourceHttpCrypt.canLoad()) {
			list.append("Internet", icons.getImageById(Icons.ICON_OPEN_FROM_INTERNET));
			index_to_command[index++] = RESULT_HTTP;
		};
		
		for(int i = 0; i < index; ++i) {
			if (index_to_command[i] == select) {
				list.setSelectedIndex(i, true);
				break;
			}
		}
		
		list.addCommand(new Command("Cancel", Command.CANCEL, 1));
		Command cmd_ok = new Command("OK", Command.OK, 1);
		list.addCommand(cmd_ok);
		list.setSelectCommand(cmd_ok);
		list.setCommandListener(this);

		// Previous Display
		dspBACK = Display.getDisplay(midlet).getCurrent();

		// Display message
		// #ifdef DEBUG
			System.out.println("Display data source list");
		// #endif
		Display.getDisplay(midlet).setCurrent(list);
		// #ifdef DEBUG
			System.out.println("Displayed data source list");
		// #endif
	}

	public void commandAction(Command cmd, Displayable dsp) {
		// #ifdef DEBUG
			System.out.println("data source list commandAction()");
		// #endif

		switch(cmd.getCommandType()) {
		case Command.OK:
			int index = list.getSelectedIndex();
			if ((index >= 0) && (index < index_to_command.length)) result = index_to_command[index];
			else result = RESULT_INVALID;
			break;
			
		case Command.CANCEL:
			result = RESULT_NONE;
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
