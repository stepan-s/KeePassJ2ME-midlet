/*
	Copyright 2008-2011 Stepan Strelets
	http://keepassj2me.sourceforge.net/

	This file is part of KeePass for J2ME.
	
	KeePass for J2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, version 2.
	
	KeePass for J2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with KeePass for J2ME.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import net.sourceforge.keepassj2me.L10nKeys.keys;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.ListTag;

/**
 * KDB menu 
 * @author Stepan Strelets
 */
public class KeydbMenu implements CommandListener {
	/** Menu item type - invalid */
	public static final int RESULT_INVALID = -1;
	/** Menu item type - `Browse` */
	public static final int RESULT_BROWSE = 0;
	/** Menu item type - `Search` */
	public static final int RESULT_SEARCH = 1;
	/** Menu item type - `Information` */
	public static final int RESULT_INFORMATION = 2;
	/** Menu item type - `Save` */
	public static final int RESULT_SAVE = 3;
	/** Menu item type - `Save as ...` */
	public static final int RESULT_SAVEAS = 4;
	/** Menu item type - `Close` */
	public static final int RESULT_CLOSE = 5;
	/** Menu item type - `Unlock` */
	public static final int RESULT_UNLOCK = 6;
	/** Menu item type - `Change password, key file, encryption rounds` */
	public static final int RESULT_CHANGE_MASTER_KEY = 7;
	
	private ListTag list;
	private int result = RESULT_INVALID;
	
	Command cmdOk;
	Command cmdClose;
	
	/**
	 * Create KDB menu
	 * @param title menu title
	 * @param save show menu item `Save`
	 * @param selected menu item type
	 * @param locked database locked
	 */
	public KeydbMenu(String title, boolean save, int selected, boolean locked) {
		Icons icons = Icons.getInstance();
		L10n lc = Config.getInstance().getLocale();
		list = new ListTag(title, List.IMPLICIT);
		if (!locked) {
			list.append(lc.getString(keys.BROWSE), icons.getImageById(56), RESULT_BROWSE);
			list.append(lc.getString(keys.SEARCH), icons.getImageById(40), RESULT_SEARCH);
			list.append(lc.getString(keys.INFORMATION), icons.getImageById(46), RESULT_INFORMATION);
			list.append(lc.getString(keys.CHANGE_KEY), icons.getImageById(13), RESULT_CHANGE_MASTER_KEY);
		} else {
			list.append(lc.getString(keys.UNLOCK), icons.getImageById(51), RESULT_UNLOCK);
		};
		if (save) list.append(lc.getString(keys.SAVE), icons.getImageById(26), RESULT_SAVE);
		list.append(lc.getString(keys.SAVE_AS), icons.getImageById(26), RESULT_SAVEAS);
		list.append(lc.getString(keys.CLOSE), icons.getImageById(45), RESULT_CLOSE);
		list.setSelectedTag(selected, true);

		cmdClose = new Command(lc.getString(keys.CLOSE), Command.SCREEN, 2);
		list.addCommand(cmdClose);
		cmdOk = new Command(lc.getString(keys.OK), Command.ITEM, 1);
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
