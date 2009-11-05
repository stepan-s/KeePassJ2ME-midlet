package net.sourceforge.keepassj2me.datasource;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;

import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.keydb.KeydbException;

/**
 * @author Stepan Strelets
 */
public class DataSourceSelect implements CommandListener {
	public static final int RESULT_INVALID = -1;
	public static final int RESULT_NONE = 0;
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
	public DataSourceSelect(MIDlet midlet, String caption, int select, boolean allow_no, boolean save) {
		this.midlet = midlet;
		int index = 0;
		Icons icons = Icons.getInstance();
		
		list = new List(caption, Choice.IMPLICIT);

		// No
		if (allow_no) {
			list.append("No", icons.getImageById(Icons.ICON_EXIT));
			index_to_command[index++] = RESULT_NONE;
		};
		
		for(int i = 0; i < DataSourceRegistry.reg.length; ++i) {
			try {
				if (addSource(DataSourceRegistry.createDataSource(DataSourceRegistry.reg[i]), save, icons, index)) index++;
			} catch (KeydbException e) {
			}
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

	private boolean addSource(DataSourceAdapter source, boolean save, Icons icons, int index) {
		if (save ? source.canSave() : source.canLoad()) {
			list.append(source.getName(), icons.getImageById(source.getIcon()));
			index_to_command[index] = source.getUid();
			return true;
		} else {;
			return false;
		}
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
