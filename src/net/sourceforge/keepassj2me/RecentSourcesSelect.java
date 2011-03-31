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

import javax.microedition.lcdui.Choice;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import net.sourceforge.keepassj2me.L10nKeys.keys;
import net.sourceforge.keepassj2me.datasource.DataSourceAdapter;
import net.sourceforge.keepassj2me.datasource.DataSourceRegistry;
import net.sourceforge.keepassj2me.datasource.UnserializeStream;
import net.sourceforge.keepassj2me.tools.DisplayStack;

/**
 * Select data source from recent
 * @author Stepan Strelets
 */
public class RecentSourcesSelect implements CommandListener {
	/** Result type - back */
	public static final int RESULT_NONE = -1;
	
	private int result = RESULT_NONE;
	private List list;
	
	/**
	 * Constructor
	 */
	public RecentSourcesSelect() {
		list = new List(Config.getLocaleString(keys.SELECT_RECENT), Choice.IMPLICIT);

		RecentSources rs = Config.getInstance().getRecentSources();
		for(int i = 0; i < rs.getSize(); ++i) {
			byte[] s = rs.getSource(i);
			
			UnserializeStream in = new UnserializeStream(s);
			byte count;
			try {
				count = in.readByte();
				DataSourceAdapter ds = DataSourceRegistry.unserializeDataSource(in);
				//DataSourceAdapter ks = null;
				//if (count > 1) ks = DataSourceRegistry.unserializeDataSource(in);
				list.append(ds.getName() + (count > 1 ? " [+]" : ""), Icons.getInstance().getImageById(ds.getIcon()));
			} catch (Exception e) {
				list.append("???", null);
			}
			
		}
		
		list.addCommand(new Command(Config.getLocaleString(keys.BACK), Command.BACK, 1));
		Command cmd_ok = new Command(Config.getLocaleString(keys.OK), Command.OK, 1);
		list.addCommand(cmd_ok);
		list.setSelectCommand(cmd_ok);
		list.addCommand(new Command(Config.getLocaleString(keys.DELETE), Command.ITEM, 2));
		list.setCommandListener(this);
	}
	
	/**
	 * Return selected source or null
	 * @return serialized data source
	 */
	public byte[] getSelected() {
		if (result >= 0) {
			return Config.getInstance().getLastOpened(result);
		} else {
			return null;
		}
	}

	public void commandAction(Command cmd, Displayable dsp) {
		switch(cmd.getCommandType()) {
		case Command.OK:
			try {
				result = list.getSelectedIndex();
			} catch (ArrayIndexOutOfBoundsException e) {
				result = RESULT_NONE;
			};
			break;
			
		case Command.ITEM:
			RecentSources rs = Config.getInstance().getRecentSources();
			rs.removeSource(list.getSelectedIndex());
			list.delete(list.getSelectedIndex());
			if (list.size() > 0) return;
			result = RESULT_NONE;
			break;
			
		case Command.BACK:
			result = RESULT_NONE;
			break;
		}

		synchronized (this.list) {
			this.list.notify();
		}
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
