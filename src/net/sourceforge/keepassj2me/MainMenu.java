package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

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
	public static final int RESULT_INVALID = -1;
	public static final int RESULT_LAST = 0;
	public static final int RESULT_OPEN = 1;
	public static final int RESULT_INFORMATION = 2;
	public static final int RESULT_SETUP = 3;
	public static final int RESULT_EXIT = 4;
	public static final int RESULT_NEW = 5;
	
	private boolean isReady = false;
	private ListTag list;
	private int result = -1;

	/**
	 * Construct and display
	 * @param midlet
	 */
	public MainMenu(int select) {
		Icons icons = Icons.getInstance();
		
		list = new ListTag("Main menu", Choice.IMPLICIT);

		if (Config.getInstance().getLastOpened() != null)
			list.append("Open last", icons.getImageById(Icons.ICON_OPEN_LAST), RESULT_LAST);
		list.append("Open ...", icons.getImageById(Icons.ICON_OPEN), RESULT_OPEN);
		list.append("New", icons.getImageById(Icons.ICON_NEW), RESULT_NEW);
		list.append("Information", icons.getImageById(Icons.ICON_INFO), RESULT_INFORMATION);
		list.append("Setup", icons.getImageById(Icons.ICON_SETUP), RESULT_SETUP);
		list.append("Exit", icons.getImageById(Icons.ICON_EXIT), RESULT_EXIT);
		list.setSelectedTag(select, true);
		
		list.addCommand(new Command("Exit", Command.EXIT, 1));
		Command cmd_ok = new Command("OK", Command.OK, 1);
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
