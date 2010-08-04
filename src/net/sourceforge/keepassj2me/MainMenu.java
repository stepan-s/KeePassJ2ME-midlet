package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import net.sourceforge.keepassj2me.L10nConstants.keys;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.ListTag;

/**
 * Main menu
 * 
 * @author Unknown
 * @author Stepan Strelets
 *
 */
public class MainMenu implements CommandListener {
	/** Menu item type - invalid */
	public static final int RESULT_INVALID = -1;
	/** Menu item type - `Open last` */
	public static final int RESULT_LAST = 0;
	/** Menu item type - `Open` */
	public static final int RESULT_OPEN = 1;
	/** Menu item type - `Information` */
	public static final int RESULT_INFORMATION = 2;
	/** Menu item type - `Setup` */
	public static final int RESULT_SETUP = 3;
	/** Menu item type - `Exit` */
	public static final int RESULT_EXIT = 4;
	/** Menu item type - `New` */
	public static final int RESULT_NEW = 5;
	
	private boolean isReady = false;
	private ListTag list;
	private int result = -1;

	/**
	 * Construct and display
	 * @param select menu item type
	 * @param midlet
	 */
	public MainMenu(int select) {
		Icons icons = Icons.getInstance();
		L10nResources lc = Config.getInstance().getLocale();
		
		list = new ListTag(lc.getString(keys.MAIN_MENU), Choice.IMPLICIT);

		if (Config.getInstance().getLastOpened() != null)
			list.append(lc.getString(keys.OPEN_LAST), icons.getImageById(Icons.ICON_OPEN_LAST), RESULT_LAST);
		list.append(lc.getString(keys.OPEN), icons.getImageById(Icons.ICON_OPEN), RESULT_OPEN);
		list.append(lc.getString(keys.NEW), icons.getImageById(Icons.ICON_NEW), RESULT_NEW);
		list.append(lc.getString(keys.INFORMATION), icons.getImageById(Icons.ICON_INFO), RESULT_INFORMATION);
		list.append(lc.getString(keys.SETUP), icons.getImageById(Icons.ICON_SETUP), RESULT_SETUP);
		list.append(lc.getString(keys.EXIT), icons.getImageById(Icons.ICON_EXIT), RESULT_EXIT);
		list.setSelectedTag(select, true);
		
		list.addCommand(new Command(lc.getString(keys.EXIT), Command.EXIT, 1));
		Command cmd_ok = new Command(lc.getString(keys.OK), Command.OK, 1);
		list.addCommand(cmd_ok);
		list.setSelectCommand(cmd_ok);
		list.setCommandListener(this);
	}

	public void commandAction(Command cmd, Displayable dsp) {
		switch(cmd.getCommandType()) {
		case Command.OK:
			try {
				result = list.getSelectedTagInt();
			} catch (ArrayIndexOutOfBoundsException e) {
				result = RESULT_INVALID;
			}
			break;
			
		case Command.EXIT:
			result = RESULT_EXIT;
			break;
			
		default:
			return;
		}

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
	 * Display and wait for user command
	 */
	public void displayAndWait() {
		DisplayStack.getInstance().push(list);
		try {
			while (!isReady) {
				synchronized (this) {
					this.wait();
				}
			}
		} catch (Exception e) {}
		DisplayStack.getInstance().pop();
	}
}
