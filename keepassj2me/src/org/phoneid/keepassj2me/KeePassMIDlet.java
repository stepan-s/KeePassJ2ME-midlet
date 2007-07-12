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

public class KeePassMIDlet
    extends MIDlet
    implements CommandListener {
    static KeePassMIDlet myself = null;
    //private Form mMainForm;
    private List mainList;
    private boolean firstTime = true;
    private Display mDisplay;
    protected Command CMD_EXIT = new Command("Exit", Command.EXIT, 1);
    PwManager mPwManager = null;
    PwGroup mCurrentGroup;
    Image mIcon[];

    // constants
    private final int NUM_ICONS = 65;
    private final String TITLE = new String ("KeePass for J2ME");
    private final int SECRET_CODE_LEN = 8;
    
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

    public void openDatabaseAndDisplay()
    {
	// open database
	try {
	    Form form = new Form(TITLE);
	    form.append("Reading Key Database ...\r\n");
	    form.append("Please Wait");
	    mDisplay.setCurrent(form);
	    
	    PasswordBox pwb = new PasswordBox (TITLE, "Please enter KDB password", 64, this, true, TextField.PASSWORD);
	    
	    // read key database file
	    InputStream is = getClass( ).getResourceAsStream("/Database.kdb");
	    if (is == null) {
		System.out.println ("InputStream is null ... file probably not found");
		doAlert("InputStream is null.  Database.kdb is not found or not readable");
		return;
	    }
	    // open and decrypt database
	    mPwManager = new ImporterV3().openDatabase(is, pwb.getResult());
	    if (mPwManager != null)
		System.out.println ("pwManager created");

	} catch (Exception e) {
	    doAlert(e.toString());
	    return;
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
	mDisplay.setCurrent(mainList);

	mTimerTask = new KeePassTimerTask(this);
	mTimer.schedule(mTimerTask, TIMER_DELAY);
    }

    // Ask user whether I should download KDB from web server,
    // or use the one stored locally in .jar.
    // Then get KDB
    private void obtainKDB()
	throws IOException
    {
	// Ask user KDB download preference
	KDBSelection kdbSelection = new KDBSelection(this);
	kdbSelection.waitForDone();
	
	if (kdbSelection.getResult() == 0) {
	    // download from HTML
	    System.out.println ("Download KDB from web server");
	    String secretCode = null;
	    while (true) {
		PasswordBox pwb = new PasswordBox(TITLE, "Please enter secret code for KDB download", SECRET_CODE_LEN, this, true, TextField.NUMERIC);
		secretCode = pwb.getResult();
		if (secretCode.length() == SECRET_CODE_LEN) {
		    break;
		} else {
		    MessageBox msg = new MessageBox(TITLE,
						    new String("Secret code length must be " + SECRET_CODE_LEN),
						    AlertType.ERROR, this, false);
		    msg.waitForDone();
		}
	    }
	    
	    // got secret code
	    // now download kdb from web server
	    Form waitForm = new Form(TITLE);
	    waitForm.append("Downloading ...");
	    mDisplay.setCurrent(waitForm);
	    HTTPConnectionThread t =  new HTTPConnectionThread(secretCode, this); /* {
		public void run() {
			try {
			    connect(secretCode);
			} catch (IOException e) {
			    doAlert(e.toString());
			}
			}
			};*/
	    t.start();

	    try {
		t.join();
	    } catch (InterruptedException e) {
		System.out.println (e.toString());
	    }
	    
	    } else {
		System.out.println ("Use local KDB");
	    }
	    
    }
    
    public void startApp()
    {
	mDisplay = Display.getDisplay(this);
	
	if (firstTime) {
            try {
                // load the images
		mIcon = new Image[NUM_ICONS];
                for (int i=0; i<NUM_ICONS; i++) {
		    mIcon[i] = Image.createImage("/images/" + i + "_gt.png");
		}
		// TODO: check if kdb is loaded.  If so, skip
		obtainKDB();
		openDatabaseAndDisplay();
		firstTime = false;

	    } catch (IOException e) {
                // ignore the image loading failure the application can recover.
		doAlert(e.toString());
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
	Alert alert = new Alert( TITLE );
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
	return new List(TITLE, List.IMPLICIT, stringArray, imageArray);
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
			AlertType.INFO, this, false);
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

    /*
    private void connect(String secretCode)
	throws IOException
    {
	HttpConnection hc = null;
	InputStream in = null;
	String url = "http://www.keepassserver.info/download2.php";
	String rawData = "code=" + secretCode;
	// String agent = "Mozilla/4.0";
	String type = "application/x-www-form-urlencoded";
    
	try {
	    hc = (HttpConnection)Connector.open(url);

	    hc.setRequestMethod(HttpConnection.POST);
	    // hc.setRequestProperty("User-Agent", agent);
	    hc.setRequestProperty("Content-Type", type);
	    hc.setRequestProperty("Content-Length", "13");
	    OutputStream os = hc.openOutputStream();
	    os.write(rawData.getBytes());

	    int rc = hc.getResponseCode();
	    System.out.println ("rc = " + rc);
		    
	    if (rc != HttpConnection.HTTP_OK) {
                throw new IOException("HTTP response code: " + rc);
            }

	    
	    in = hc.openInputStream();
	    
	    int contentLength = (int)hc.getLength();
	    byte[] raw = new byte[contentLength];
	    int length = in.read(raw);
	    
	    in.close();
	    hc.close();
	    
	    // Show the response to the user.
	    // String s = new String(raw, 0, length);
	    System.out.println ("Downloaded " + contentLength + " bytes");
	}
	catch (IOException ioe) {
	    System.out.println (ioe.toString());
	}
	// mDisplay.setCurrent(mMainForm);
    }
    */
   
}
