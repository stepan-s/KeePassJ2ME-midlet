package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;


import net.sourceforge.keepassj2me.keydb.IKeydbGroupContentRecever;
import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
import net.sourceforge.keepassj2me.keydb.KeydbEntry;
import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.keydb.KeydbGroup;
import net.sourceforge.keepassj2me.keydb.KeydbLockedException;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.InputBox;

/**
 * KDB browser
 * @author Stepan Strelets
 */
public class KeydbBrowser implements CommandListener {
	private Icons icons;
    private Command cmdSelect;
    private Command cmdClose;
    private Command cmdBack;
	
	private KeydbDatabase keydb;
	
    private boolean isClose = false;
    
    final static int MODE_BROWSE = 0;
    final static int MODE_SEARCH = 1;
    
    private int activatedIndex = -1;//activated item in the list
    
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
	
	/**
	 * Construct browser
	 * 
	 * @param midlet Parent midlet
	 * @param keydb KDB Database
	 */
	public KeydbBrowser(KeydbDatabase keydb) {
		this.keydb = keydb;
		
		this.cmdSelect = new Command("OK", Command.OK, 1);
		this.cmdClose = new Command("Close", Command.EXIT, 2);
		this.cmdBack = new Command("Back", Command.BACK, 2);
		
		this.pageSize = Config.getInstance().getPageSize();
		this.icons = Icons.getInstance();
	}
	
	/**
	 * Display browser and wait for done
	 * @throws KeydbLockedException 
	 */
	public void display(int mode) throws KeydbLockedException {
		DisplayStack.pushSplash();
		
		switch(mode) {
		case MODE_BROWSE:
			currentPage = 0;
			fillList(0);
			break;
		case MODE_SEARCH:
			InputBox val = new InputBox("Enter the search value", searchValue, 64, TextField.NON_PREDICTIVE);
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
				synchronized (this) {
					this.wait();
				}
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
		} catch (KeydbLockedException e) {
			throw e;
		} catch (Exception e) {}
		
		DisplayStack.pop();
	}
	
	private void commandOnBrowse() throws KeydbLockedException {
		if (activatedIndex >= padding) {
			if ((activatedIndex - padding) < currentPageSize) {
				//item activated
				int activatedItem = activatedIndex - padding + currentPage * pageSize;
				
				if (activatedItem < groupsCount) {
					//group selected
					KeydbGroup group = keydb.getGroupByIndex(currentGroupId, activatedItem);
					currentPage = 0;
					fillList((group != null) ? group.id : 0);
					
				} else {
					//entry selected
					KeydbEntry entry = keydb.getEntryByIndex(currentGroupId, activatedItem - groupsCount);
					if (entry != null) {
						new KeydbRecordView(keydb, entry);
					}
				}
			} else {
				//special item on bottom activated
				int activatedItem = activatedIndex - padding - currentPageSize;
				
				currentPage = activatedItem;
				fillList(currentGroupId);
			}
		} else {
			//special item on top activated
			if (currentGroupId == 0) {
				this.stop();
				
			} else {
				//up selected
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
	}
	private void commandOnSearch() throws KeydbLockedException {
		if (activatedIndex >= padding) {
			if ((activatedIndex - padding) < currentPageSize) {
				//item activated
				int activatedItem = activatedIndex - padding + currentPage * pageSize;
				
				//entry selected
				KeydbEntry entry = keydb.getFoundEntry(activatedItem);
				if (entry != null) {
					new KeydbRecordView(keydb, entry);
				}
			} else {
				//special item on bottom activated
				int activatedItem = activatedIndex - padding - currentPageSize;
				
				currentPage = activatedItem;
				fillListSearch();
			}
		} else {
			//special item on top activated
			if (activatedIndex == 0) {
				this.stop();
			};
		}
	}
	
	/**
	 * Fill and display list with groups and entries
	 * @param groupId Zero for root
	 * @throws KeydbLockedException 
	 */
	private void fillList(int groupId) throws KeydbLockedException {
		boolean isRoot = (groupId == 0);

		final List list;
		KeydbGroup group = null;
		if (!isRoot) {
			try {
				group = keydb.getGroup(groupId);
			} catch(KeydbException e) {};
		}
		list = new List(group != null ? group.name : KeePassMIDlet.TITLE, List.IMPLICIT);
		list.append("..", icons.getImageById(Icons.ICON_BACK));
		padding = 1;
		
		selectedIndexOnPage = -1;
		groupsCount = 0;
		currentPageSize = 0;
		this.totalSize = keydb.enumGroupContent(groupId, new IKeydbGroupContentRecever() {
			public void addKeydbEntry(KeydbEntry entry) {
				list.append(entry.title, icons.getImageById(entry.imageIndex));
				++currentPageSize;
			}
			public void addKeydbGroup(KeydbGroup group) {
				int i = list.append("[+] " + group.name, icons.getImageById(group.imageIndex));
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
		list.addCommand(this.cmdBack);
		list.addCommand(this.cmdSelect);
		list.setSelectCommand(this.cmdSelect);
		list.setCommandListener(this);
		DisplayStack.replaceLast(list);
	}

	/**
	 * Fill and display list with groups and entries
	 * @param value search value - search title starts with this value 
	 * @throws KeydbLockedException 
	 */
	private void fillListSearch() throws KeydbLockedException {
		final List list = new List(KeePassMIDlet.TITLE, List.IMPLICIT);
		list.append("BACK", icons.getImageById(Icons.ICON_BACK));
		padding = 1;
		
		groupsCount = 0;
		currentPageSize = 0;
		keydb.enumFoundEntries(new IKeydbGroupContentRecever() {
			public void addKeydbEntry(KeydbEntry entry) {
				list.append(entry.title, icons.getImageById(entry.imageIndex));
				++currentPageSize;
			}
			public void addKeydbGroup(KeydbGroup group) {}
			public void totalGroups(int count) {}
		}, this.currentPage * this.pageSize, this.pageSize);
		
		addPager(list);
		list.addCommand(this.cmdBack);
		list.addCommand(this.cmdSelect);
		list.setSelectCommand(this.cmdSelect);
		list.setCommandListener(this);
		DisplayStack.replaceLast(list);
	}

	private void addPager(List list) {
		if (this.totalSize > this.pageSize) {
			int page = 0;
			int count = this.totalSize;
			while (count > 0) {
				if (this.currentPage == page) {
					list.append("> PAGE "+(page+1)+" <", null);
				} else {
					list.append("PAGE "+(page+1), null);
				}
				++page;
				count -= this.pageSize;
			};
			list.setTitle(list.getTitle() + ", Page "+(currentPage+1)+"/"+page);
		};
	}
	
	/**
	 * Command Listener implementation
	 */
	public void commandAction(Command c, Displayable d) {
		if (c == this.cmdSelect) {
			activate(((List)d).getSelectedIndex());

		} else if (c == this.cmdBack) {
			activate(0);
			
		} else if (c == this.cmdClose) {
			this.stop();
		}
	}
	
	/**
	 * Stop browsing and return
	 */
	public void stop() {
		isClose = true;
		synchronized (this) {
			this.notify();
		}
	}
	
	public void activate(int index) {
		activatedIndex = index;
		synchronized (this) {
			this.notify();
		}
	}
}
