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

package net.sourceforge.keepassj2me.tools;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.L10nKeys.keys;

/**
 * RecordStore database browser 
 * @author Stepan Strelets
 */
public class RecordStoreDBBrowser implements CommandListener {
	private List list;
	private String title;
	private String result = null;
	private Image fileIcon;
	
	private String newRecordName = null;
	
	private Command cmdNewRecord;
	private Command cmdSelect;
	private Command cmdCancel;
	private Command cmdDelete;
	private Command cmdRename;

	private Command activatedCommand;
	private String activatedName;

	/**
	 * Constructor
	 * @param title
	 */
	public RecordStoreDBBrowser(String title) {
		this.title = title;
		this.fileIcon = Icons.getInstance().getImageById(Icons.ICON_FILE);

		cmdNewRecord = new Command(Config.getLocaleString(keys.NEW_RECORD), Command.SCREEN, 1);
		cmdSelect = new Command(Config.getLocaleString(keys.SELECT), Command.SCREEN, 2);
		cmdCancel = new Command(Config.getLocaleString(keys.CANCEL), Command.SCREEN, 3);
		cmdDelete = new Command(Config.getLocaleString(keys.DELETE), Command.SCREEN, 4);
		cmdRename = new Command(Config.getLocaleString(keys.RENAME), Command.SCREEN, 4);
	}
	
	/**
	 * Select record name for opening
	 * @param title
	 * @return content name
	 */
	public static String open(String title) {
		RecordStoreDBBrowser browser = new RecordStoreDBBrowser(title);
		browser.display(false);
		return browser.getName();
	}
	
	/**
	 * Select record name for saving
	 * @param title
	 * @param defaultRecordName
	 * @return content name
	 */
	public static String save(String title, String defaultRecordName) {
		RecordStoreDBBrowser browser = new RecordStoreDBBrowser(title);
		if (defaultRecordName != null) browser.setDefaultRecordName(defaultRecordName);
		browser.display(true);
		return browser.getName();
	}
	
	/**
	 * Set default record name
	 * @param name
	 */
	public void setDefaultRecordName(String name) {
		this.newRecordName = name;
	}
	
	/**
	 * Display dialog
	 * @param save true for save dialog, false for load dialog
	 */
	public void display(boolean save) {
		DisplayStack.getInstance().pushSplash();
		fillList(save);
		boolean run = true;
		while (run) {
			try {
				activatedCommand = null;
				
				synchronized (this.list) {
					this.list.wait();
				}
				
				if (activatedCommand == null) {
					result = null;
					break;
				}
				
				if (activatedCommand == cmdSelect) {
					result = activatedName;
					run = false;
					
				} else if (activatedCommand == cmdCancel) {
					result = null;
					run = false;

				} else if ((activatedCommand == cmdDelete) && (activatedName != null)) {
					if (MessageBox.showConfirm(Config.getLocaleString(keys.DELETE_RECORD_Q, new String[] {activatedName}))) {
						RecordStoreDB.getInstance().delete(activatedName);
						fillList(save);
					}
					
				} else if ((activatedCommand == cmdRename) && (activatedName != null)) {
					InputBox ib = new InputBox(Config.getLocaleString(keys.CHANGE_RECORD_NAME), activatedName, 100, TextField.ANY);
					String name = ib.getResult();
					if ((name != null) && (name.length() > 0)) {
						RecordStoreDB.getInstance().rename(activatedName, name);
						fillList(save);
					}
					
				} else if (activatedCommand == cmdNewRecord) {
					InputBox ib = new InputBox(Config.getLocaleString(keys.ENTER_RECORD_NAME), newRecordName == null ? "" : newRecordName, 100, TextField.ANY);
					String name = ib.getResult();
					if ((name != null) && (name.length() > 0)) { 
						if (RecordStoreDB.getInstance().exists(name)) {
							if (MessageBox.showConfirm(Config.getLocaleString(keys.OVERWRITE_RECORD_Q))) {
								result = name;
								run = false;
							};
						} else {
							result = name;
							run = false;
						};
					};
				}
			} catch (Exception e) {
				MessageBox.showAlert(e.getMessage());
			}
		}
		DisplayStack.getInstance().pop();
	}
	private void fillList(boolean save) {
		list = new List(title, List.IMPLICIT);
		try {
			RecordStoreDB.getInstance().enumerate(new IRecordStoreListReceiver() {
				public void listRecord(String name) {
					list.append(name, fileIcon);
				}});
		} catch (Exception e) {
		}
		
		if (save) list.addCommand(cmdNewRecord);
		list.addCommand(cmdSelect);
		list.addCommand(cmdCancel);
		list.addCommand(cmdDelete);
		list.addCommand(cmdRename);
		list.setSelectCommand(cmdSelect);
		list.setCommandListener(this);
		DisplayStack.getInstance().replaceLast(list);
	}
	public void commandAction(Command cmd, Displayable dsp) {
		activatedCommand = cmd;
		List list = (List)dsp;
		if (list.getSelectedIndex() != -1) activatedName = list.getString(list.getSelectedIndex());
		else activatedName = null;
		
		synchronized (this.list) {
			this.list.notify();
		}
	}
	/**
	 * Get selected record name
	 * @return name
	 */
	public String getName() {
		return result;
	}
}
