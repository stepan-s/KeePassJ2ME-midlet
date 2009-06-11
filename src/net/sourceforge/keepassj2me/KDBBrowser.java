package net.sourceforge.keepassj2me;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;


// #ifdef DEBUG
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.util.encoders.Hex;
// #endif

/**
 * Key database browser
 * 
 * @author Unknown
 * @author Stepan Strelets
 */
public class KDBBrowser implements CommandListener {
	private PwManager mPwManager = null;
	private PwGroup mCurrentGroup;
	private KeePassMIDlet midlet;
	private Display mDisplay;
	private List mainList;
	private final long TIMER_DELAY = 10 * 60 * 1000;
	Form mBackGroundForm = null;
	// timer
	Timer mTimer = new Timer();
	KDBBrowserTask mTimerTask = null;
	
    private boolean isReady = false;
    
    private Command cmdSelect;
    private Command cmdClose;
    private Command cmdBack;
	
	/**
	 * Construct browser
	 * 
	 * @param midlet Parent midlet
	 */
	public KDBBrowser(KeePassMIDlet midlet) {
		//TODO: Refactor - KDBBrowser must receive KDB as byte array (remove local store access)
		this.midlet = midlet;
		this.mDisplay = Display.getDisplay(midlet);
		this.cmdSelect = new Command("OK", Command.OK, 1);
		this.cmdClose = new Command("Close", Command.EXIT, 2);
		this.cmdBack = new Command("Back", Command.BACK, 2);
	}
	
	/**
	 * 
	 * @param pass KDB password
	 * @param kdbBytes kdb bytes array
	 * @return <code>true</code> on success, <code>false</code> on failure
	 * @throws KeePassException 
	 */
	public void decode(String pass, byte[] kdbBytes) throws KeePassException {
		try {
			ProgressForm form = new ProgressForm(Definition.TITLE);
			mDisplay.setCurrent(form);
			form.setProgress(0, "Reading KDB");
			
			// #ifdef DEBUG
				System.out.println("Reading Key Database ...\r\n");
				System.out.println("Will read record store\r\n");
			// #endif

			// #ifdef DEBUG
				System.out.println("kdbBytes: " + kdbBytes.length);
				System.out.println("kdb length: " + kdbBytes.length + "\r\n");
				System.out.println("kdb: " + new String(Hex.encode(kdbBytes)));
			// #endif

			ByteArrayInputStream is = new ByteArrayInputStream(kdbBytes);

			// decrypt database

			// #ifdef DEBUG
				System.out.println("Decrypting KDB ...");
			// #endif
			
			mPwManager = new ImporterV3(form)
					.openDatabase(is, pass);
			// #ifdef DEBUG
				if (mPwManager != null) System.out.println("pwManager created");
				System.out.println("KDB Decrypted");
			// #endif

		} catch (KeePassException e) {
			throw e;
		} catch (IOException e) {
			throw new KeePassException(e.getMessage());
		} catch (InvalidCipherTextException e) {
			throw new KeePassException(e.getMessage());
		}

		// #ifdef DEBUG
			System.out.println("call makeList");
		// #endif
		// construct tree
		mPwManager.constructTree(null);
		// start from root position
		mCurrentGroup = mPwManager.rootGroup;
	}
	/**
	 * Display browser and wait for done
	 */
	public void display() {
		mainList = makeList(mCurrentGroup);
		// #ifdef DEBUG
			System.out.println("makeList done");
		// #endif
		mainList.addCommand(this.cmdClose);
		mainList.addCommand(this.cmdSelect);
		mainList.setSelectCommand(this.cmdSelect);
		mainList.setCommandListener(this);
		// #ifdef DEBUG
			System.out.println("setCurrent to mainList");
		// #endif
		mDisplay.setCurrent(mainList);

		// create watch dog timer
		mTimerTask = new KDBBrowserTask(this);
		mTimer.schedule(mTimerTask, TIMER_DELAY);
		
		try {
			while (!isReady) {
				synchronized (this) {
					this.wait();
				}
			}
		} catch (Exception e) {
			// #ifdef DEBUG
				System.out.println(e.toString());
			// #endif
		}
		
		mTimer.cancel();
	}
	
	/**
	 * Return a list of child groups and entries under the specified group If
	 * argument "group" is empty, return the root group list
	 */
	List makeList(PwGroup group) {
		boolean isRoot;

		// #ifdef DEBUG
			System.out.println("makeList (1)");
		// #endif

		if (group == mPwManager.rootGroup) {
			// #ifdef DEBUG
				System.out.println("isRoot is true");
			// #endif
			isRoot = true;
		} else {
			// #ifdef DEBUG
				System.out.println("isRoot is false");
			// #endif
			isRoot = false;
		}

		//List resultList = null;
		String[] stringArray = null;
		Image[] imageArray = null;

		// get child groups
		Vector childGroups = group.childGroups;
		Vector childEntries = group.childEntries;
		int childGroupSize = childGroups.size();
		int childEntriesSize = childEntries.size();

		// size of string and image array is
		/* # child groups + # child entries + 1 (for "go up 1" entry) */
		stringArray = new String[childGroupSize + childEntriesSize
				+ (isRoot ? 0 : 1)];
		imageArray = new Image[childGroupSize + childEntriesSize
				+ (isRoot ? 0 : 1)];
		
		int index = 0;
		if (isRoot == false) {
			stringArray[index] = "..";
			imageArray[index] = midlet.iconBack;
			++index;
		}

		for (int i = 0; i < childGroupSize; i++) {
			PwGroup childGroup = (PwGroup) childGroups.elementAt(i);
			stringArray[index] = "[+] " + childGroup.name;
			imageArray[index] = midlet.getImageById(childGroup.imageId, 0);
			++index;
		}
		// #ifdef DEBUG
			if (childEntries == null)
				System.out.println("childEntries is null");
		// #endif
		for (int i = 0; i < childEntriesSize; i++) {
			PwEntry childEntry = (PwEntry) childEntries.elementAt(i);
			stringArray[index] = childEntry.title;
			imageArray[index] = midlet.getImageById(childEntry.imageId, 0);
			++index;
		}

		// #ifdef DEBUG
			System.out.println("makeList (2)");
		// #endif
		return new List(Definition.TITLE, List.IMPLICIT, stringArray,
				imageArray);
	}

	/**
	 * Command Listener implementation
	 */
	public void commandAction(Command c, Displayable d) {
		// reset watch dog timer
		mTimer.cancel();
		mTimerTask = new KDBBrowserTask(this);
		mTimer = new Timer();
		mTimer.schedule(mTimerTask, TIMER_DELAY);

		if (c == this.cmdSelect) {
			// #ifdef DEBUG
				System.out.println("Select Command");
			// #endif
			int i = ((List) d).getSelectedIndex();
			if (((List) d).getString(0) == "..") --i;

			if (i < 0) {
				// go up one
				mCurrentGroup = mCurrentGroup.parent;
				mainList = makeList(mCurrentGroup);
				mainList.addCommand(mCurrentGroup == mPwManager.rootGroup ? this.cmdClose : this.cmdBack);
				mainList.addCommand(this.cmdSelect);
				mainList.setSelectCommand(this.cmdSelect);
				mainList.setCommandListener(this);
				mDisplay.setCurrent(mainList);
				
			} else if (i < mCurrentGroup.childGroups.size()) {
				// if group is selected, move to that group
				mCurrentGroup = (PwGroup) mCurrentGroup.childGroups
						.elementAt(i);
				mainList = makeList(mCurrentGroup);
				mainList.addCommand(mCurrentGroup == mPwManager.rootGroup ? this.cmdClose : this.cmdBack);
				mainList.addCommand(this.cmdSelect);
				mainList.setSelectCommand(this.cmdSelect);
				mainList.setCommandListener(this);
				mDisplay.setCurrent(mainList);
				
			} else if (i < mCurrentGroup.childGroups.size()
					+ mCurrentGroup.childEntries.size()) {
				// if entry is selected, show it
				PwEntry entry = (PwEntry) mCurrentGroup.childEntries
						.elementAt(i - mCurrentGroup.childGroups.size());
				new KDBRecordView(this.midlet, entry);

			} else {
			}

			// display.setCurrent(exclusiveList);
		} else if (c == this.cmdBack) {
			mCurrentGroup = mCurrentGroup.parent;
			mainList = makeList(mCurrentGroup);
			mainList.addCommand(mCurrentGroup == mPwManager.rootGroup ? this.cmdClose : this.cmdBack);
			mainList.addCommand(this.cmdSelect);
			mainList.setSelectCommand(this.cmdSelect);
			mainList.setCommandListener(this);
			mDisplay.setCurrent(mainList);
			
		} else if (c == this.cmdClose) {
			this.stop();
		}
	}
	
	/**
	 * Stop browsing and return
	 */
	public void stop() {
		isReady = true;
		synchronized (this) {
			this.notify();
		}
	}
}
