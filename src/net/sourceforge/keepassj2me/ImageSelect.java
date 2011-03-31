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

/**
 * Select image from list 
 * @author Stepan Strelets
 */
public class ImageSelect implements CommandListener {
	private List list;
	private int result = -1;
	
	/**
	 * Create form
	 */
	public ImageSelect() {
		Icons icons = Icons.getInstance();
		L10n lc = Config.getInstance().getLocale();
		list = new List(lc.getString(keys.SELECT_ICON), List.IMPLICIT);
		for(int i = 0; i < Icons.NUM_ICONS; ++i) {
			list.append(Integer.toString(i), icons.getImageById(i));
		};

		list.addCommand(new Command(lc.getString(keys.CANCEL), Command.CANCEL, 1));
		Command cmd_ok = new Command(lc.getString(keys.OK), Command.OK, 1);
		list.addCommand(cmd_ok);
		list.setSelectCommand(cmd_ok);
		list.setCommandListener(this);
	}
	public void commandAction(Command cmd, Displayable dsp) {
		switch(cmd.getCommandType()) {
		case Command.OK:
			result = 1;
			break;
			
		case Command.CANCEL:
			result = 0;
			break;
		}

		synchronized (this.list) {
			this.list.notify();
		}
	}
	/**
	 * Show form and wait
	 * @param index selected icon
	 * @return true on success
	 */
	public boolean select(int index) {
		list.setSelectedIndex(index, true);
		DisplayStack.getInstance().push(list);
		try {
			synchronized (this.list) {
				this.list.wait();
			}
		} catch (Exception e) {}
		DisplayStack.getInstance().pop();
		return (result == 1);
	}
	/**
	 * Get selected icon index
	 * @return icon index
	 */
	public int getSelectedImageIndex() {
		return list.getSelectedIndex();
	}
}
