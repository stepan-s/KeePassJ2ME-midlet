/*
KeePass for J2ME

Copyright 2007 Naomaru Itoi <nao@phoneid.org>

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; version 2

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
*/

package org.phoneid.keepassj2me;

// PhoneID utils
import org.phoneid.*;

// Java
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.*;
import java.io.UnsupportedEncodingException;
import java.io.ByteArrayInputStream;

/// record store
import javax.microedition.rms.*;


public class KeePassMIDlet
    extends MIDlet
    implements CommandListener
{
    static KeePassMIDlet myself = null;
    //private Form mMainForm;
    private List mainList;
    private boolean firstTime = true;
    private Display mDisplay;
    protected Command CMD_EXIT = new Command("Exit", Command.EXIT, 1);
    PwManager mPwManager = null;
    PwGroup mCurrentGroup;
    Image mIcon[];

    // timer
    KeePassTimerTask mTimerTask = null; 
    Timer mTimer = new Timer();
    private final long TIMER_DELAY = 60 * 1000 * 1000; // one hour
    // background
    Form mBackGroundForm = null;

    /**
     * Constructor
     */
    public KeePassMIDlet() {
	myself = this;
    }

    /**
     *
     * return 0 for usual exit
     * return 1 for reload KDB
     */
    public int openDatabaseAndDisplay()
    {
	// open database
	try {
	    Form form = new Form(Definition.TITLE);
	        
	    PasswordBox pwb = new PasswordBox (Definition.TITLE,
					       "Enter KDB password",
					       null, 64, this, true, TextField.PASSWORD,
					       "Reload", "\r\nUse Reload button to reload KDB instead of using locally stored one");

	    if (Definition.DEBUG) {
		form.append("Reading Key Database ...\r\n");
	    }
	    
	    if (pwb.getCommandType() == Command.ITEM) {
		// Reload
		System.out.println ("Reload KDB");
		if (Definition.DEBUG) {
		    form.append("Reload KDB\r\n");
		}

		// delete record store 
		try {
		    if (Definition.DEBUG) {
			form.append("Delete record store\r\n");
		    }
		    RecordStore.deleteRecordStore(Definition.KDBRecordStoreName);
		} catch (RecordStoreNotFoundException e) {
		    // if it doesn't exist, it's OK
		    if (Definition.DEBUG) {
			form.append("Exception in deleting record store\r\n");
		    }
		}
		return 1;
	    }
	    
	    // read KDB from record store
	    // open record store
	    if (Definition.DEBUG) {
		form.append("Will read record store\r\n");
	    }
	    RecordStore rs = RecordStore.openRecordStore( Definition.KDBRecordStoreName, false );
	    byte[] kdbBytes = rs.getRecord(1);
	    rs.closeRecordStore();

	    System.out.println ("kdbBytes: " + kdbBytes.length);
	    if (Definition.DEBUG) {
		form.append("kdb length: " + kdbBytes.length + "\r\n");
	    }
	    
	    ByteArrayInputStream is = new ByteArrayInputStream(kdbBytes);
	    
	    // decrypt database
	    form.append("Decrypting Key Database ...\r\n");
	    form.append("Please Wait\r\n");
	    mDisplay.setCurrent(form);

	    mPwManager = new ImporterV3(Definition.DEBUG ? form : null).openDatabase(is, pwb.getResult());
	    if (mPwManager != null)
		System.out.println ("pwManager created");

	} catch (Exception e) {
	    System.out.println ("openDatabaseAndDisplay() received exception: " + e.toString());
	    // doAlert(e.toString());
	    MessageBox box = new MessageBox (Definition.TITLE,
					     "openDatabaseAndDisplay() received exception: " + e.toString(),
					     AlertType.ERROR,
					     this,
					     false,
					     null);
	    box.waitForDone();
	    System.out.println ("alert done");
	    return -1;
	}
	
	System.out.println ("call makeList");
	// construct tree
	mPwManager.constructTree(null);
	// start from root position
	mCurrentGroup = mPwManager.rootGroup; 
	mainList = makeList(mCurrentGroup);
	System.out.println ("makeList done");
	mainList.addCommand(CMD_EXIT);
	mainList.setCommandListener(this);
	System.out.println ("setCurrent to mainList");
	mDisplay.setCurrent(mainList);

	mTimerTask = new KeePassTimerTask(this);
	mTimer.schedule(mTimerTask, TIMER_DELAY);

	System.out.println ("openDatabaseAndDisplay() return");

	return 0;
    }

    // Ask user whether I should download KDB from web server,
    // or use the one stored locally in .jar.
    // Then get KDB
    private void obtainKDB()
	throws IOException, PhoneIDException, RecordStoreException
    {
	try {
	    // if KDB is already in record store, don't do anything
	    RecordStore rs = RecordStore.openRecordStore(Definition.KDBRecordStoreName, false);
	    if (rs.getNumRecords() > 0) {
		rs.closeRecordStore();
		return;
	    }
	    rs.closeRecordStore();
	} catch (RecordStoreNotFoundException e) {
	    // record store doesn't exist yet - that's OK
	}
	
	// Ask user KDB download preference
	KDBSelection kdbSelection = new KDBSelection(this);
	kdbSelection.waitForDone();
	
	if (kdbSelection.getResult() == 0) {
	    // download from HTML
	    System.out.println ("Download KDB from web server");
	    String secretCode = null, url = null;
	    while (true) {
		URLCodeBox box = new URLCodeBox(Definition.TITLE, this, true);
		//url = pwb.getResult();
		
		/* if (secretCode.length() == Definition.SECRET_CODE_LEN) {
		    break;
		} else {
		    MessageBox msg = new MessageBox(Definition.TITLE,
						    new String("Secret code length must be " + Definition.SECRET_CODE_LEN),
						    AlertType.ERROR, this,
						    false, null);
		    msg.waitForDone();
		    }*/
		break;
	    }
	    
	    // got secret code
	    // now download kdb from web server
	    Form waitForm = new Form(Definition.TITLE);
	    waitForm.append("Downloading ...");
	    mDisplay.setCurrent(waitForm);
	    HTTPConnectionThread t =  new HTTPConnectionThread(secretCode, url, this); 
	    t.start();
	    
	    try {
		t.join();
	    } catch (InterruptedException e) {
		System.out.println (e.toString());
	    }
	    
	} else {
	    // Use local KDB
	    // read key database file
	    InputStream is = getClass( ).getResourceAsStream("/Database.kdb");
	    if (is == null) {
		System.out.println ("InputStream is null ... file probably not found");
		throw new PhoneIDException("InputStream is null.  Database.kdb is not found or not readable");
	    }
	    byte buf[] = new byte[is.available()];
	    is.read(buf);
	    storeKDBInRecordStore(buf);
	}

	    
    }

    protected void storeKDBInRecordStore(byte[] content)
	throws RecordStoreException
    {
	// delete record store 
	try {
	    RecordStore.deleteRecordStore(Definition.KDBRecordStoreName);
	} catch (RecordStoreNotFoundException e) {
	    // if it doesn't exist, it's OK
	}
	
	// create record store
	RecordStore rs = RecordStore.openRecordStore(Definition.KDBRecordStoreName, true);
	
	rs.addRecord(content, 0, content.length);
	rs.closeRecordStore();
    }
    
    public void startApp()
    {
	mDisplay = Display.getDisplay(this);
	
	if (firstTime) {
            try {
                // load the images
		mIcon = new Image[Definition.NUM_ICONS];
                for (int i=0; i<Definition.NUM_ICONS; i++) {
		    mIcon[i] = Image.createImage("/images/" + i + "_gt.png");
		}
	    } catch (IOException e) {
                // ignore the image loading failure the application can recover.
		doAlert(e.toString());
            }

	    try {
		// TODO: check if kdb is loaded.  If so, skip
		while (true) {
		    obtainKDB();
		    int rv = openDatabaseAndDisplay();
		    if (rv == 0) {
			// usual return code
			break;
		    } else {
			// reload KDB
		    }
		}
		firstTime = false;

		System.out.println ("startApp() done");
	    } catch (Exception e) {
		doAlert(e.toString());
		return;
	    }
        }
    }
    
    public void pauseApp() {}
    
    public void destroyApp(boolean unconditional) {}
    
    public void log(String str) {
	//mMainForm.append(new StringItem(null, str + "\r\n"));
    }

    
    static public void logS(String str) {
	myself.log(str);
    }
    
    public void doAlert(String msg) {
	Alert alert = new Alert( Definition.TITLE );
	alert.setString( msg );
	alert.setTimeout( Alert.FOREVER );
	alert.addCommand(CMD_EXIT);
	alert.setCommandListener(this);
	mDisplay.setCurrent( alert );
	return;
    }

    /**
     * Alert based message
     * show message with specified title, msg, image, and
     * whether it has yes/no buttons or only OK button
     */
    /*
    public void doMessage(String title, String msg, Image image, boolean yesno) {
	Displayable dspBACK;
	Alert alert = new Alert( title, msg, image, AlertType.INFO);
	alert.setTimeout( Alert.FOREVER );
	if (yesno == true) {
	    alert.addCommand(new Command("Yes", Command.OK, 1));
	    alert.addCommand(new Command("No", Command.CANCEL, 2));
	} else {
	    alert.addCommand(new Command("OK", Command.OK, 1));
	    // addCommand(new Command("Cancel", Command.CANCEL, 2));
	}
	dspBACK = Display.getDisplay(this).getCurrent();
	Display.getDisplay(this).setCurrent( alert );
	
	return;
    }
    */

    /**
     * Return a list of child groups and entries under the specified group
     * If argument "group" is empty, return the root group list
     */
    List makeList(PwGroup group)
    {
	boolean isRoot;

	System.out.println ("makeList (1)");
		
	if (group == mPwManager.rootGroup) {
	    System.out.println ("isRoot is true");
	    isRoot = true;
	} else {
	    System.out.println ("isRoot is false");
	    isRoot = false;
	}
	
	List resultList = null;
	String[] stringArray = null;
	Image[] imageArray = null;
	
	// get child groups
	Vector childGroups = group.childGroups;
	Vector childEntries = group.childEntries;
	int childGroupSize = childGroups.size();
	int childEntriesSize = childEntries.size();

	// size of string and image array is
	// # child groups + # child entries + 1 (for "go up 1" entry)
	stringArray = new String[childGroupSize + childEntriesSize + (isRoot ? 0 : 1)];
	imageArray = new Image[childGroupSize + childEntriesSize + (isRoot ? 0 : 1)];

	for (int i=0; i<childGroupSize; i++) {
	    PwGroup childGroup = (PwGroup)childGroups.elementAt(i);
	    stringArray[i] = childGroup.name + "/";
	    imageArray[i] = mIcon[childGroup.imageId]; // TODO: change this
	}
	if (childEntries == null)
	    System.out.println ("childEntries is null");
	for (int i=0; i<childEntriesSize; i++) {
	    PwEntry childEntry = (PwEntry)childEntries.elementAt(i);
	    stringArray[childGroupSize + i] = childEntry.title;
	    imageArray[childGroupSize + i] = mIcon[childEntry.imageId]; // TODO: change this
	}

	if (isRoot == false) {
	    stringArray[stringArray.length - 1] = ".. go up one";
	    imageArray[imageArray.length - 1] = null;
	}
	System.out.println ("makeList (2)");
	return new List(Definition.TITLE, List.IMPLICIT, stringArray, imageArray);
    }
  
    /**
     * Command Listener implementation
     */
    public void commandAction(Command c, Displayable d)
    {
	// reset timer
	mTimer.cancel();
	mTimerTask = new KeePassTimerTask(this);
	mTimer = new Timer();
	mTimer.schedule(mTimerTask, TIMER_DELAY);

	if (c == List.SELECT_COMMAND) {
	    System.out.println ("Select Command");
	    int i = ((List)d).getSelectedIndex();

	    if (i < mCurrentGroup.childGroups.size()) {
		// if group is selected, move to that group
		mCurrentGroup = (PwGroup)mCurrentGroup.childGroups.elementAt(i);
		mainList = makeList(mCurrentGroup);
		mainList.addCommand(CMD_EXIT);
		mainList.setCommandListener(this);
		mDisplay.setCurrent(mainList);
	    } else if (i < mCurrentGroup.childGroups.size() + mCurrentGroup.childEntries.size()) {
		// if entry is selected, show it
		PwEntry entry = (PwEntry)mCurrentGroup.childEntries.elementAt(i - mCurrentGroup.childGroups.size());
		
		 
		try {
		MessageBox box = new MessageBox (entry.title,
			 "URL  : " + entry.url + "\r\n" +
			 "user : " + entry.username + "\r\n" +						 
			 "pass : " + new String(entry.getPassword(), "UTF-8") + "\r\n" +
			 "notes: " + entry.additional,
			AlertType.INFO, this, false, mIcon[entry.imageId]);
		} catch (UnsupportedEncodingException e) {
		    doAlert (e.toString());
		}
						 
	    } else {
		// go up one
		mCurrentGroup = mCurrentGroup.parent;
		mainList = makeList(mCurrentGroup);
		mainList.addCommand(CMD_EXIT);
		mainList.setCommandListener(this);
		mDisplay.setCurrent(mainList);
	    }
	    
	    
	    //display.setCurrent(exclusiveList);
	} else if (c == CMD_EXIT) {
            destroyApp(false);
            notifyDestroyed();
	}
    }

    public void exit()
    {
	System.out.println ("Exit!");
	
	destroyApp(true);
	notifyDestroyed();
    }
    

}
