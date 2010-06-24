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
	public static final int RESULT_INFORMATION = 2;
	public static final int RESULT_SAVE = 3;
	public static final int RESULT_SAVEAS = 4;
	public static final int RESULT_CLOSE = 5;
	public static final int RESULT_UNLOCK = 6;
	public static final int RESULT_CHANGE_MASTER_KEY = 7;
	
	private ListTag list;
	private int result = RESULT_INVALID;
	
	Command cmdOk;
	Command cmdClose;
	
	public KeydbMenu(String title, boolean save, int selected, boolean locked) {
		Icons icons = Icons.getInstance();
		list = new ListTag(title, List.IMPLICIT);
		if (!locked) {
			list.append("Browse", icons.getImageById(56), RESULT_BROWSE);
			list.append("Search", icons.getImageById(40), RESULT_SEARCH);
			list.append("Information", icons.getImageById(46), RESULT_INFORMATION);
			list.append("Change master key", icons.getImageById(13), RESULT_CHANGE_MASTER_KEY);
		} else {
			list.append("Unlock", icons.getImageById(51), RESULT_UNLOCK);
		};
		if (save) list.append("Save", icons.getImageById(26), RESULT_SAVE);
		list.append("Save as ...", icons.getImageById(26), RESULT_SAVEAS);
		list.append("Close", icons.getImageById(45), RESULT_CLOSE);
		list.setSelectedTag(selected, true);

		cmdClose = new Command("Close", Command.SCREEN, 2);
		list.addCommand(cmdClose);
		cmdOk = new Command("OK", Command.ITEM, 1);
		list.addCommand(cmdOk);
		list.setSelectCommand(cmdOk);
		list.setCommandListener(this);
	}

	public void commandAction(Command cmd, Displayable dsp) {
		if (cmd == cmdOk) {
			try {
				result = list.getSelectedTagInt();
			} catch (ArrayIndexOutOfBoundsException e) {
				result = RESULT_INVALID;
			}
		
		} else if (cmd == cmdClose) {
			result = RESULT_CLOSE;
		}

		synchronized (this.list) {
			this.list.notify();
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
			synchronized (this.list) {
				this.list.wait();
			}
		} catch (Exception e) {}
		DisplayStack.getInstance().pop();
	}
}
