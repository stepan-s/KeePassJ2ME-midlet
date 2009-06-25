package net.sourceforge.keepassj2me;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;
import javax.microedition.lcdui.TextField;


import net.sourceforge.keepassj2me.keydb.IKeydbGroupContentRecever;
import net.sourceforge.keepassj2me.keydb.KeydbDatabase;
import net.sourceforge.keepassj2me.keydb.KeydbEntry;
import net.sourceforge.keepassj2me.keydb.KeydbException;
import net.sourceforge.keepassj2me.keydb.KeydbGroup;

/**
 * KDB browser
 * @author Stepan Strelets
 */
public class KeydbBrowser implements CommandListener, IWathDogTimerTarget {
	private KeePassMIDlet midlet;
	private Icons icons;
	private Display mDisplay;
    private Command cmdSelect;
    private Command cmdClose;
    private Command cmdBack;
	
	private KeydbDatabase keydb;
	
	private long TIMER_DELAY = 600000; //10 min
	WathDogTimer wathDog = null;
	
    private boolean isClose = false;
    
    final static int MODE_BROWSE = 0;
    final static int MODE_SEARCH = 1;
    
    private byte mode = MODE_BROWSE;
    private int activatedIndex = -1;//activated item in the list
    
    private int currentPage = 0;//page shown
    private int currentPageSize = 0;//items (groups and entries) shown
    private int pageSize = 50;//page size
    private int totalSize = 0;//entries found
    
    private int padding = 0;//special list items on top
    
    //MODE_BROWSE
	private int currentGroupId = 0;
	private int groupsCount = 0;
    private int selectedIndex = -1;//selected item in the list, for group selection on up 
	
    //MODE_SEARCH
    private String searchValue = null;
	
	/**
	 * Construct browser
	 * 
	 * @param midlet Parent midlet
	 * @param keydb KDB Database
	 */
	public KeydbBrowser(KeePassMIDlet midlet, KeydbDatabase keydb) {
		this.midlet = midlet;
		this.keydb = keydb;
		
		this.mDisplay = Display.getDisplay(midlet);
		this.cmdSelect = new Command("OK", Command.OK, 1);
		this.cmdClose = new Command("Close", Command.EXIT, 2);
		this.cmdBack = new Command("Back", Command.BACK, 2);
		this.TIMER_DELAY = 60000 * Config.getInstance().getWathDogTimeOut();
		this.wathDog = new WathDogTimer(this);
		
		this.pageSize = Config.getInstance().getPageSize();
		this.icons = Icons.getInstance();
	}
	
	/**
	 * Display browser and wait for done
	 */
	public void display() {
		Displayable back = mDisplay.getCurrent();
		mode = MODE_BROWSE;
		currentPage = 0;
		fillList(0);

		wathDog.setTimer(TIMER_DELAY);
		
		try {
			while (true) {
				synchronized (this) {
					this.wait();
				}
				if (isClose) break;
				
				if (activatedIndex >= padding) {
					if ((activatedIndex - padding) < currentPageSize) {
						//item activated
						int activatedItem = activatedIndex - padding + currentPage * pageSize;
						
						switch(mode) {
						case MODE_BROWSE:
							if (activatedItem < groupsCount) {
								//group selected
								KeydbGroup group = keydb.getGroupByIndex(currentGroupId, activatedItem);
								currentPage = 0;
								fillList((group != null) ? group.id : 0);
								
							} else {
								//entry selected
								KeydbEntry entry = keydb.getEntryByIndex(currentGroupId, activatedItem - groupsCount);
								if (entry != null) {
									new KeydbRecordView(this.midlet, keydb, entry);
								}
							}
							break;
						case MODE_SEARCH:
							//entry selected
							KeydbEntry entry = keydb.getFoundEntry(activatedItem);
							if (entry != null) {
								new KeydbRecordView(this.midlet, keydb, entry);
							}
							break;
						}
					} else {
						//special item on bottom activated
						int activatedItem = activatedIndex - padding - currentPageSize;
						
						switch(mode) {
						case MODE_BROWSE:
							currentPage = activatedItem;
							fillList(currentGroupId);
							break;
						case MODE_SEARCH:
							currentPage = activatedItem;
							fillListSearch();
							break;
						}
					}
				} else {
					//special item on top activated
					switch(mode) {
					case MODE_BROWSE:
						if (currentGroupId == 0) {
							//search selected
							mDisplay.setCurrent(back);
							InputBox val = new InputBox(this.midlet, "Enter the title starts with", searchValue, 64, TextField.NON_PREDICTIVE);
							if (val.getResult() != null) {
								mode = MODE_SEARCH; 
								currentPage = 0;
								searchValue = val.getResult();
								totalSize = keydb.searchEntriesByTitle(searchValue);
								fillListSearch();
							} else {
								currentPage = 0;
								fillList(0);
							}
						} else {
							//up selected
							KeydbGroup group;
							try {
								group = keydb.getGroupParent(currentGroupId);
							} catch (KeydbException e) {
								group = null;
							}
							currentPage = 0;
							fillList((group != null) ? group.id : 0);
						}
						break;
					case MODE_SEARCH:
						if (activatedIndex == 0) {
							//back from search selected
							mode = MODE_BROWSE;
							currentPage = 0;
							fillList(0);
						};
						break;
					};
				}
			}
		} catch (Exception e) {
			// #ifdef DEBUG
				System.out.println(e.toString());
			// #endif
		}
		
		wathDog.cancelTimer();
		mDisplay.setCurrent(back);
	}
	
	/**
	 * Fill and display list with groups and entries
	 * @param groupId Zero for root
	 */
	private void fillList(int groupId) {
		boolean isRoot = (groupId == 0);

		final List list = new List(Definition.TITLE, List.IMPLICIT);
		if (!isRoot) {
			list.append("..", icons.getImageById(Icons.ICON_BACK));
		} else {
			list.append("Search", icons.getImageById(Icons.ICON_SEARCH));
		}
		padding = 1;
		
		selectedIndex = -1;
		groupsCount = 0;
		currentPageSize = 0;
		this.totalSize = keydb.enumGroupContent(groupId, new IKeydbGroupContentRecever() {
			public void addKeydbEntry(KeydbEntry entry) {
				list.append(entry.title, icons.getImageById(entry.imageIndex));
				++currentPageSize;
			}
			public void addKeydbGroup(KeydbGroup group) {
				int i = list.append("[+] " + group.name, icons.getImageById(group.imageIndex));
				if (group.id == currentGroupId) selectedIndex = i;
				++groupsCount;
				++currentPageSize;
			}
		}, this.currentPage * this.pageSize, this.pageSize);
		
		addPager(list);
		if (selectedIndex >= 0) list.setSelectedIndex(selectedIndex, true);
		currentGroupId = groupId;
		list.addCommand(groupId == 0 ? this.cmdClose : this.cmdBack);
		list.addCommand(this.cmdSelect);
		list.setSelectCommand(this.cmdSelect);
		list.setCommandListener(this);
		mDisplay.setCurrent(list);
	}

	/**
	 * Fill and display list with groups and entries
	 * @param value search value - search title starts with this value 
	 */
	private void fillListSearch() {
		final List list = new List(Definition.TITLE, List.IMPLICIT);
		list.append("BACK", icons.getImageById(Icons.ICON_BACK));
		padding = 1;
		
		groupsCount = 0;
		currentPageSize = 0;
		keydb.enumFoundEntries(new IKeydbGroupContentRecever() {
			public void addKeydbEntry(KeydbEntry entry) {
				list.append(entry.title, icons.getImageById(entry.imageIndex));
				++currentPageSize;
			}
			public void addKeydbGroup(KeydbGroup group) {
			}
		}, this.currentPage * this.pageSize, this.pageSize);
		
		addPager(list);
		list.addCommand(this.cmdBack);
		list.addCommand(this.cmdSelect);
		list.setSelectCommand(this.cmdSelect);
		list.setCommandListener(this);
		mDisplay.setCurrent(list);
		
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
			list.setTitle("PAGE "+(currentPage+1)+"/"+page);
		};
	}
	
	/**
	 * Command Listener implementation
	 */
	public void commandAction(Command c, Displayable d) {
		wathDog.setTimer(TIMER_DELAY);

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

	public void invokeByWathDog() {
		this.stop();
	}
}
