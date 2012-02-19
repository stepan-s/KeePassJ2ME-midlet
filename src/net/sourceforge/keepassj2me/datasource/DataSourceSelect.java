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

package net.sourceforge.keepassj2me.datasource;

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.L10nKeys.keys;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.ListTag;

/**
 * @author Stepan Strelets
 */
public class DataSourceSelect implements CommandListener {
	/** Result type - invalid */
	public static final int RESULT_INVALID = -1;
	/** Result type - none selected */
	public static final int RESULT_NONE = -2;
	/** Result type - cancel */
	public static final int RESULT_CANCEL = -3;
	
	private ListTag list;
	private int result = RESULT_CANCEL;

	/**
	 * Construct and display
	 * @param caption form title
	 * @param select source type
	 * @param allow_no allow `none` selection
	 * @param save true - save, false - load
	 */
	public DataSourceSelect(String caption, int select, boolean allow_no, boolean save) {
		Icons icons = Icons.getInstance();
		
		list = new ListTag(caption, Choice.IMPLICIT);

		// No
		if (allow_no)
			list.append(Config.getLocaleString(keys.DS_NOT_USE), icons.getImageById(Icons.ICON_EXIT), RESULT_NONE);
		
		DataSourceAdapter source;
		for(int i = 0; i < DataSourceRegistry.reg.length; ++i) {
			try {
				source = DataSourceRegistry.createDataSource(DataSourceRegistry.reg[i]);
				if (save ? source.canSave() : source.canLoad())
					list.append(source.getFamilyName(), icons.getImageById(source.getIcon()), source.getUid());
			} catch (KeePassException e) {
			}
		};
		list.setSelectedTag(select, true);
		
		list.addCommand(new Command(Config.getLocaleString(keys.CANCEL), Command.CANCEL, 1));
		Command cmd_ok = new Command(Config.getLocaleString(keys.OK), Command.OK, 1);
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
			};
			break;
			
		case Command.CANCEL:
			result = RESULT_CANCEL;
			break;
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
	 * wait for user
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
