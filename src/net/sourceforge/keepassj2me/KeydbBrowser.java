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
import javax.microedition.lcdui.TextField;


import net.sourceforge.keepassj2me.L10nKeys.keys;
import net.sourceforge.keepassj2me.keydb.IKeydbGroupContentRecever;
import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
import net.sourceforge.keepassj2me.keydb.KeydbEntry;
import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.keydb.KeydbGroup;
import net.sourceforge.keepassj2me.keydb.KeydbLockedException;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.InputBox;
import net.sourceforge.keepassj2me.tools.ListTag;
import net.sourceforge.keepassj2me.tools.MessageBox;

/**
 * KDB browser
 * @author Stepan Strelets
 */
public class KeydbBrowser implements CommandListener {
	/** List item type - go up in hierarchy */
	public final static int ITEM_UP = 1; 
	/** List item type - entry */
	public final static int ITEM_ENTRY = 2; 
	/** List item type - group */
	public final static int ITEM_GROUP = 3; 
	/** List item type - pagination */
	public final static int ITEM_PAGE = 4; 
	
	private ListTag list;
	
	private Icons icons;
    private Command cmdSelect;
    private Command cmdClose;
    private Command cmdBack;
    private Command cmdAddGroup;
    private Command cmdAddEntry;
    private Command cmdDelete;
    private Command cmdEdit;
    private Command cmdExit;
	
	private KeydbDatabase keydb;
	
    private boolean isClose = false;
    
    final static int MODE_BROWSE = 0;
    final static int MODE_SEARCH = 1;
    
    private Command activatedCommand;
    private int activatedIndex = -1;//activated item in the list
    private int activatedType;
    private int lastEntryIndex = -1;
    private int lastGroupId = -1;
    
    private int currentPage = 0;//page shown
    private int currentPageSize = 0;//items (groups and entries) shown
    private int pageSize = 50;//page size
    private int totalSize = 0;//entries found
    
    private int padding = 0;//special list items on top
    
    //MODE_BROWSE
	private int currentGroupId = 0;
	private int groupsCount = 0;
    private int selectedIndexOnPage = -1;//selected item in the list, for group selection on up 
	
    //MODE_SEARCH
    private String searchValue = null;
    
    L10n lc;
	
	/**
	 * Construct browser
	 * 
	 * @param keydb KDB Database
	 */
	public KeydbBrowser(KeydbDatabase keydb) {
		this.keydb = keydb;
		lc = Config.getInstance().getLocale();
		
		this.cmdSelect = new Command(lc.getString(keys.OK), Command.ITEM, 1);
		this.cmdClose = new Command(lc.getString(keys.TO_KDB_MENU), Command.SCREEN, 2);
		this.cmdBack = new Command(lc.getString(keys.BACK), Command.BACK, 2);
		this.cmdAddGroup = new Command(lc.getString(keys.ADD_GROUP), Command.SCREEN, 3);
		this.cmdAddEntry = new Command(lc.getString(keys.ADD_ENTRY), Command.SCREEN, 3);
		this.cmdEdit = new Command(lc.getString(keys.EDIT), Command.ITEM, 3);
		this.cmdDelete = new Command(lc.getString(keys.DELETE), Command.ITEM, 3);
		this.cmdExit = new Command(lc.getString(keys.EXIT), Command.EXIT, 4);
		
		this.pageSize = Config.getInstance().getPageSize();
		this.icons = Icons.getInstance();
	}
	
	/**
	 * Display browser and wait for done
	 * @param mode browse mode: group hierarchy or search list
	 * @throws KeydbLockedException 
	 * @throws ExitException 
	 */
	public void display(int mode) throws KeydbLockedException, ExitException {
		DisplayStack.getInstance().pushSplash();
		try {
			switch(mode) {
			case MODE_BROWSE:
				currentPage = 0;
				fillList(0);
				break;
			case MODE_SEARCH:
				InputBox val = new InputBox(lc.getString(keys.ENTER_SEARCH_VALUE), searchValue, 64, TextField.NON_PREDICTIVE);
				if (val.getResult() != null) {
					mode = MODE_SEARCH;
					currentPage = 0;
					searchValue = val.getResult();
					totalSize = keydb.searchEntriesByTextFields(searchValue, Config.getInstance().getSearchBy());
					fillListSearch();
				} else {
					return;
				}
				break;
			default:
				return;
			}
	
			try {
				while (true) {
					activatedCommand = null;
					synchronized (this.list) {
						this.list.wait();
					}
					if (activatedCommand == cmdExit) {
						throw new ExitException();
					}
					if (this.keydb.isLocked()) break;
					this.keydb.reassureWatchDog();
					if (isClose) break;
					
					switch(mode) {
					case MODE_BROWSE:
						commandOnBrowse();
						break;
					case MODE_SEARCH:
						commandOnSearch();
						break;
					}
					
					this.keydb.reassureWatchDog();
					if (isClose) break;
				}
			} catch (ExitException e) {
				throw e;
			} catch (KeydbLockedException e) {
				throw e;
			} catch (Exception e) {}
			
		} finally {
			DisplayStack.getInstance().pop();
		}
	}
	
	private void commandOnBrowse() throws KeydbLockedException, ExitException {
		lastEntryIndex = -1;
		lastGroupId = -1;
		if (activatedCommand == this.cmdSelect) {
			switch(activatedType) {
			case ITEM_UP:
				this.leaveGroup();
				break;
			case ITEM_GROUP:
				this.enterGroup(activatedIndex - padding + currentPage * pageSize);
				break;
			case ITEM_ENTRY:
				this.editEntry(activatedIndex - padding + currentPage * pageSize - groupsCount);
				break;
			case ITEM_PAGE:
				this.setPage(activatedIndex - padding - currentPageSize);
				break;
			};
		} else if (activatedCommand == this.cmdAddEntry) {
			this.addEntry();
			
		} else if (activatedCommand == this.cmdAddGroup) {
			this.addGroup();
			
		} else if (activatedCommand == this.cmdDelete) {
			switch(activatedType) {
			case ITEM_GROUP:
				if (MessageBox.showConfirm(lc.getString(keys.DELETE_GROUP_Q)))
					this.deleteGroup(activatedIndex - padding + currentPage * pageSize);
				break;
			case ITEM_ENTRY:
				if (MessageBox.showConfirm(lc.getString(keys.DELETE_ENTRY_Q)))
					this.deleteEntry(activatedIndex - padding + currentPage * pageSize - groupsCount);
				break;
			};
			
		} else if (activatedCommand == this.cmdEdit) {
			switch(activatedType) {
			case ITEM_GROUP:
				this.editGroup(activatedIndex - padding + currentPage * pageSize);
				break;
			case ITEM_ENTRY:
				this.editEntry(activatedIndex - padding + currentPage * pageSize - groupsCount);
				break;
			};
			
		} else if (activatedCommand == this.cmdBack) {
			this.leaveGroup();
			
		} else if (activatedCommand == this.cmdClose) {
			isClose = true;
		}
	}
	
	private void enterGroup(int index) throws KeydbLockedException {
		KeydbGroup group = keydb.getGroupByIndex(currentGroupId, index);
		currentPage = 0;
		fillList((group != null) ? group.id : 0);
	}
	private void leaveGroup() throws KeydbLockedException {
		if (currentGroupId == 0) {
			isClose = true;
			
		} else {
			KeydbGroup group;
			try {
				group = keydb.getGroupParent(currentGroupId);
			} catch (KeydbException e) {
				group = null;
			}
			currentPage = keydb.getGroupPage(group != null ? group.id : 0, currentGroupId, pageSize);
			fillList((group != null) ? group.id : 0);
		}
	}
	private void editGroup(int index) throws KeydbLockedException {
		KeydbGroup group = keydb.getGroupByIndex(currentGroupId, index);
		if (group != null) {
			lastGroupId = group.id;
			new KeydbGroupEdit(group);
		}
		this.fillList(this.currentGroupId);
	}
	private void editEntry(int index) throws KeydbLockedException, ExitException {
		KeydbEntry entry = keydb.getEntryByIndex(currentGroupId, index);
		if (entry != null) {
			lastEntryIndex = entry.index;
			new KeydbRecordView(entry);
		}
		this.fillList(this.currentGroupId);
	}
	private void setPage(int index) throws KeydbLockedException {
		currentPage = index;
		fillList(currentGroupId);
	}
	private void addGroup() throws KeydbLockedException {
		KeydbGroup group = new KeydbGroup(keydb);
		group.parentId = currentGroupId;
		if (currentGroupId != 0) {
			KeydbGroup parent = null;
			try {
				parent = keydb.getGroup(currentGroupId);
			} catch (KeydbException e) {
			}
			if (group != null) {
				group.level = parent.level + 1;
				new KeydbGroupEdit(group);
			}
		} else {
			group.level = 0;
			new KeydbGroupEdit(group);
		};
		if (group.index >= 0) this.currentGroupId = group.id;
		this.fillList(this.currentGroupId);
	}
	private void addEntry() throws KeydbLockedException, ExitException {
		if (currentGroupId != 0) {
			KeydbEntry entry = new KeydbEntry(keydb);
			entry.groupId = currentGroupId;
			new KeydbRecordView(entry);
			if (entry.index >= 0) lastEntryIndex = this.totalSize;
			this.fillList(this.currentGroupId);
		};
	}
	private void deleteGroup(int index) throws KeydbLockedException {
		KeydbGroup group = keydb.getGroupByIndex(currentGroupId, index);
		if (group != null) group.delete();
		this.fillList(this.currentGroupId);
	}
	private void deleteEntry(int index) throws KeydbLockedException {
		KeydbEntry entry = keydb.getEntryByIndex(currentGroupId, index);
		if (entry != null) entry.delete();
		this.fillList(this.currentGroupId);
	}
	
	private void commandOnSearch() throws KeydbLockedException, ExitException {
		lastEntryIndex = -1;
		if (activatedCommand == this.cmdSelect) {
			switch(activatedType) {
			case ITEM_UP:
				isClose = true;
				break;
			case ITEM_ENTRY:
				KeydbEntry entry = keydb.getFoundEntry(activatedIndex - padding + currentPage * pageSize);
				if (entry != null) {
					lastEntryIndex = entry.index;
					new KeydbRecordView(entry);
					totalSize = keydb.searchEntriesByTextFields(searchValue, Config.getInstance().getSearchBy());
				}
				fillListSearch();
				break;
			case ITEM_PAGE:
				currentPage = activatedIndex - padding - currentPageSize;
				fillListSearch();
				break;
			};
		} else if (activatedCommand == this.cmdBack) {
			isClose = true;
			
		} else if (activatedCommand == this.cmdClose) {
			isClose = true;
		}
	}
	
	/**
	 * Fill and display list with groups and entries
	 * @param groupId Zero for root
	 * @throws KeydbLockedException 
	 */
	private void fillList(int groupId) throws KeydbLockedException {
		boolean isRoot = (groupId == 0);

		KeydbGroup group = null;
		if (!isRoot) {
			try {
				group = keydb.getGroup(groupId);
			} catch(KeydbException e) {};
		}
		list = new ListTag(group != null ? group.name : "/", List.IMPLICIT);
		list.append("..", icons.getImageById(Icons.ICON_BACK), ITEM_UP);
		padding = 1;
		
		selectedIndexOnPage = -1;
		groupsCount = 0;
		currentPageSize = 0;
		this.totalSize = keydb.enumGroupContent(groupId, new IKeydbGroupContentRecever() {
			public void addKeydbEntry(KeydbEntry entry) {
				int i = list.append(entry.title, icons.getImageById(entry.imageIndex), ITEM_ENTRY);
				if ((selectedIndexOnPage == -1) && (entry.index == lastEntryIndex)) selectedIndexOnPage = i;
				++currentPageSize;
			}
			public void addKeydbGroup(KeydbGroup group) {
				int i = list.append("[+] " + group.name, icons.getImageById(group.imageIndex), ITEM_GROUP);
				if (group.id == lastGroupId) selectedIndexOnPage = i;
				if (group.id == currentGroupId) selectedIndexOnPage = i;
				++currentPageSize;
			}
			public void totalGroups(int count) {
				groupsCount = count;
			}
		}, this.currentPage * this.pageSize, this.pageSize);
		
		addPager(list);
		if (selectedIndexOnPage >= 0) list.setSelectedIndex(selectedIndexOnPage, true);
		currentGroupId = groupId;
		
		list.addCommand(this.cmdSelect);
		list.setSelectCommand(this.cmdSelect);
		list.addCommand(this.cmdBack);
		if (groupId != 0) list.addCommand(this.cmdClose);
		list.addCommand(this.cmdAddGroup);
		if (groupId != 0) list.addCommand(this.cmdAddEntry);
		list.setCommandListener(this);
		list.addCommand(this.cmdEdit);
		list.addCommand(this.cmdDelete);
		list.addCommand(this.cmdExit);
		
		DisplayStack.getInstance().replaceLast(list);
	}

	/**
	 * Fill and display list with groups and entries
	 * @param value search value - search title starts with this value 
	 * @throws KeydbLockedException 
	 */
	private void fillListSearch() throws KeydbLockedException {
		list = new ListTag(lc.getString(keys.QUERY)+": "+this.searchValue, List.IMPLICIT);
		list.append(lc.getString(keys.BACK), icons.getImageById(Icons.ICON_BACK), ITEM_UP);
		padding = 1;
		
		groupsCount = 0;
		currentPageSize = 0;
		selectedIndexOnPage = -1;
		keydb.enumFoundEntries(new IKeydbGroupContentRecever() {
			public void addKeydbEntry(KeydbEntry entry) {
				int i = list.append(entry.title, icons.getImageById(entry.imageIndex), ITEM_ENTRY);
				if (entry.index == lastEntryIndex) selectedIndexOnPage = i;
				++currentPageSize;
			}
			public void addKeydbGroup(KeydbGroup group) {}
			public void totalGroups(int count) {}
		}, this.currentPage * this.pageSize, this.pageSize);
		
		addPager(list);
		if (selectedIndexOnPage >= 0) list.setSelectedIndex(selectedIndexOnPage, true);
		
		list.addCommand(this.cmdBack);
		list.addCommand(this.cmdSelect);
		list.setSelectCommand(this.cmdSelect);
		list.setCommandListener(this);
		
		DisplayStack.getInstance().replaceLast(list);
	}

	private void addPager(ListTag list) {
		if (this.totalSize > this.pageSize) {
			int page = 0;
			int count = this.totalSize;
			while (count > 0) {
				if (this.currentPage == page) {
					list.append("> "+lc.getString(keys.PAGE)+" "+(page+1)+" <", null, ITEM_PAGE);
				} else {
					list.append(lc.getString(keys.PAGE)+" "+(page+1), null, ITEM_PAGE);
				}
				++page;
				count -= this.pageSize;
			};
			list.setTitle(list.getTitle() + ", "+lc.getString(keys.PAGE)+" "+(currentPage+1)+"/"+page);
		};
	}
	
	/**
	 * Command Listener implementation
	 * @param c command
	 * @param d displayable
	 */
	public void commandAction(Command c, Displayable d) {
		activatedCommand = c;
		activatedIndex = ((ListTag)d).getSelectedIndex();
		activatedType = ((ListTag)d).getSelectedTagInt();
		
		synchronized (this.list) {
			this.list.notify();
		}
	}
}
