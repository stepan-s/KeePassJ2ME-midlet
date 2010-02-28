package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.ListTag;

/**
 * KDB menu 
 * @author Stepan Strelets
 */
public class KeydbMenu implements CommandListener {
	public static final int RESULT_INVALID = -1;
	public static final int RESULT_BROWSE = 0;
	public static final int RESULT_SEARCH = 1;
	public static final int RESULT_OPTIONS = 2;
	public static final int RESULT_SAVE = 3;
	public static final int RESULT_SAVEAS = 4;
	public static final int RESULT_CLOSE = 5;
	public static final int RESULT_UNLOCK = 6;
	
	private boolean isReady = false;
	private ListTag list;
	private int result = -1;
	
	public KeydbMenu(boolean save, int selected, boolean locked) {
		Icons icons = Icons.getInstance();
		list = new ListTag(KeePassMIDlet.TITLE, List.IMPLICIT);
		if (!locked) {
			list.append("Browse", icons.getImageById(56), RESULT_BROWSE);
			list.append("Search", icons.getImageById(40), RESULT_SEARCH);
			list.append("Options", icons.getImageById(34), RESULT_OPTIONS);
		} else {
			list.append("Unlock", icons.getImageById(51), RESULT_UNLOCK);
		};
		if (save) list.append("Save", icons.getImageById(26), RESULT_SAVE);
		list.append("Save as ...", icons.getImageById(26), RESULT_SAVEAS);
		list.append("Close", icons.getImageById(45), RESULT_CLOSE);
		list.setSelectedTag(selected, true);

		list.addCommand(new Command("Close", Command.EXIT, 1));
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
			result = RESULT_CLOSE;
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
		DisplayStack.push(list);
		try {
			while (!isReady) {
				synchronized (this) {
					this.wait();
				}
			}
		} catch (Exception e) {}
		DisplayStack.pop();
	}
}