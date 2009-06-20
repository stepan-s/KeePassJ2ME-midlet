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
	private Display mDisplay;
    private Command cmdSelect;
    private Command cmdClose;
    private Command cmdBack;
	
	private KeydbDatabase keydb;
	
	private int currentGroupId = 0;
	private int groupsCount = 0;
	
	private long TIMER_DELAY = 600000; //10 min
	WathDogTimer wathDog = null;
	
    private boolean isClose = false;
    private int selected = -1;
    private String searchValue = null;
    //for group selection on up
    private int selectedIndex = -1; 
	
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
	}
	
	/**
	 * Display browser and wait for done
	 */
	public void display() {
		Displayable back = mDisplay.getCurrent();
		fillList(0);

		wathDog.setTimer(TIMER_DELAY);
		
		try {
			while (!isClose) {
				synchronized (this) {
					this.wait();
				}
				if (selected >= 0) {
					selected -= 1;
					if (selected < 0) {
						//top item selected
						
						if (currentGroupId == 0)  {
							//search selected
							mDisplay.setCurrent(back);
							InputBox val = new InputBox(this.midlet, "Enter the title starts with", searchValue, 64, TextField.NON_PREDICTIVE);
							if (val.getResult() != null) {
								fillList(val.getResult());
							}
						} else if (currentGroupId == -1)  {
							//back from search selected
							fillList(0);
							
						} else {
							//up selected
							KeydbGroup group;
							try {
								group = keydb.getGroupParent(currentGroupId);
							} catch (KeydbException e) {
								group = null;
							}
							fillList((group != null) ? group.id : 0);
						}
					} else if (selected < groupsCount) {
						//group selected
						KeydbGroup group = keydb.getGroupByIndex(currentGroupId, selected);
						fillList((group != null) ? group.id : 0);
						
					} else {
						//entry selected
						KeydbEntry entry;
						if (currentGroupId == -1)  {
							//search
							entry = keydb.getEntryByTitle(searchValue, selected);
						} else {
							//browse
							entry = keydb.getEntryByIndex(currentGroupId, selected);
						};
						if (entry != null) {
							new KeydbRecordView(this.midlet, keydb, entry);
						}
					}
					selected = -1;
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
			list.append("..", midlet.iconBack);
		} else {
			list.append("Search", midlet.getImageById(40));
		}
		
		selectedIndex = -1;
		groupsCount = 0;
		keydb.enumGroupContent(groupId, new IKeydbGroupContentRecever() {
			public void addKeydbEntry(KeydbEntry entry) {
				list.append(entry.title, midlet.getImageById(entry.imageIndex));
			}
			public void addKeydbGroup(KeydbGroup group) {
				int i = list.append("[+] " + group.name, midlet.getImageById(group.imageIndex));
				if (group.id == currentGroupId) selectedIndex = i;
				++groupsCount;
			}
		});
		
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
	private void fillList(String value) {
		final List list = new List(Definition.TITLE, List.IMPLICIT);
		list.append("..", midlet.iconBack);
		
		groupsCount = 0;
		keydb.enumEntriesByTitle(value, new IKeydbGroupContentRecever() {
			public void addKeydbEntry(KeydbEntry entry) {
				list.append(entry.title, midlet.getImageById(entry.imageIndex));
			}
			public void addKeydbGroup(KeydbGroup group) {
			}
		}, Config.getInstance().getSearchPageSize());
		
		this.searchValue = value;
		currentGroupId = -1;
		list.addCommand(this.cmdBack);
		list.addCommand(this.cmdSelect);
		list.setSelectCommand(this.cmdSelect);
		list.setCommandListener(this);
		mDisplay.setCurrent(list);
	}
	
	/**
	 * Command Listener implementation
	 */
	public void commandAction(Command c, Displayable d) {
		wathDog.setTimer(TIMER_DELAY);

		if (c == this.cmdSelect) {
			select(((List)d).getSelectedIndex());

		} else if (c == this.cmdBack) {
			select(0);
			
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
	
	public void select(int index) {
		selected = index;
		synchronized (this) {
			this.notify();
		}
	}

	public void invokeByWathDog() {
		this.stop();
	}
}
