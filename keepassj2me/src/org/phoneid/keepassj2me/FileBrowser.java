package org.phoneid.keepassj2me;

import java.io.IOException;
import java.util.Enumeration;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;
import javax.microedition.midlet.MIDlet;

/**
 * Provides a user interface to browse for a file.
 * 
 * @author Kevin O'Rourke.
 *
 */
public class FileBrowser implements CommandListener {
	/** The MIDlet this FileBrowser belongs to. */
	protected MIDlet midlet = null;
	/** The full FileConnection URL of the selected file. */
	private String fileUrl = null;
	/** A flag to indicate that a file has been chosen. */
	private boolean isChosen;
	/** The currently-displayed directory. */
	private String currDir = null;
	/** The user interface for the browser. */
	private List dirList = null;
	
	/** Choose a file or directory. */
	private Command cmdSelect;
	/** Go up to parent directory. */
	private Command cmdUp;
	/** Go back without selecting. */
	private Command cmdBack;
	
	/** Icon representing a directory. */
	private Image dirIcon;
	/** Icon representing a file. */
	private Image fileIcon;
	
	
	/** The path separator string. */
	private static final String SEPARATOR = "/";
	/** The path sepatator as a character. */
	private static final char SEP_CHAR = '/';
	/** The display string for the parent directory. */
	private static final String UP_DIR = "../";
	/** The path to the directory icon resource. */
	private static final String DIR_ICON_RES = "/images/48_gt.png";
	/** The path to the file icon resource. */
	private static final String FILE_ICON_RES = "/images/22_gt.png";
	/** The prefix for a FileConnection URL. */
	private static final String URL_PREFIX = "file:///";
	
	/**
	 * Constructs a new FileBrowser and displays it.
	 * 
	 * @param midlet The running MIDlet.
	 */
	public FileBrowser(MIDlet midlet) {
		this.midlet = midlet;
		this.isChosen = false;
		
		// load the icon images
		try {
			dirIcon = Image.createImage(DIR_ICON_RES);
		} catch (IOException e) {
			// do without an icon
			dirIcon = null;
		}
		try {
			fileIcon = Image.createImage(FILE_ICON_RES);
		} catch (IOException e) {
			// do without an icon
			fileIcon = null;
		}
		
		// the user interface is provided by a List
		dirList = new List("Select KDB file", List.IMPLICIT);
		
		// set up the commands
		cmdSelect = new Command("Select", Command.ITEM, 1);
		cmdBack = new Command("Back", Command.BACK, 1);
		cmdUp = new Command("Up", Command.ITEM, 2);
		dirList.addCommand(cmdSelect);
		dirList.addCommand(cmdBack);
		dirList.addCommand(cmdUp);
		dirList.setSelectCommand(cmdSelect);
		
		dirList.setCommandListener(this);
		
		// show the browser
		System.out.println("About to display file browser");
		Display.getDisplay(midlet).setCurrent(dirList);
		System.out.println("Displaying file browser");
	}
	
	
	/* (non-Javadoc)
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
	 */
	public void commandAction(Command cmd, Displayable dsp) {
		if (cmd == cmdSelect) {
			final String name = dirList.getString(dirList.getSelectedIndex());
			System.out.println("SELECT: "+name);
			if (isDirectory(name)) {
				new Thread(new Runnable() {
					public void run() {
						enterDirectory(name);
					}
				}).start();
			} else {
				fileUrl = currDir+name;
				
				// we're finished
				isChosen = true;
			}
		} else if (cmd == cmdUp) {
			System.out.println("..");
			enterDirectory(UP_DIR);
		} else if (cmd == cmdBack) {
			System.out.println("BACK");
			// indicate that no file was chosen
			fileUrl = null;
			isChosen = true;
		} else {
			System.err.println("Unexpected Command");
		}
		
		if (isChosen) {
			synchronized (this) {
				this.notify();
			}
		}
	}
	
	/**
	 * Returns the file URL selected by the user.
	 * 
	 * @return the file URL or <code>null</code> if no file was selected.
	 */
	public String getUrl() {
		return fileUrl;
	}
	
	/**
	 * Blocks until either a file has been chosen or the user has chosen to
	 * go back.
	 */
    public void waitForDone() {
		try {
			while (!isChosen) {
				synchronized (this) {
					this.wait();
				}
			}
		} catch (Exception e) {
			System.out.println(e.toString());
		}
	}

	/**
	 * Fills the list with the contents of the given URL.
	 * 
	 * If the URL is <code>null</code> then the filesystem roots will be
	 * listed instead.
	 * 
	 * If the URL refers to a file then the directory containing that file will
	 * be listed.
	 * 
	 * @param url
	 *            the directory to show.
	 */
	public void showDir(String url) {
		Enumeration contents = null;
		FileConnection dir = null;
		
		// remove previous contents
		dirList.deleteAll();
		
		if (url != null) {
			// is it a file or a directory?
			try {
				dir = (FileConnection)Connector.open(url);
				
				if (!dir.isDirectory()) {
					// get the URL for the parent directory
					url = URL_PREFIX+dir.getPath();
				}
				
				dir.close();
				dir = null;
			} catch (ConnectionNotFoundException e) {
				// there's a problem with the URL
				System.err.println(e.toString());
				url = null;
			} catch (IOException e) {
				// some other bad thing happened
				System.err.println(e.toString());
				url = null;
			}
		}
	
		if (url == null) {
			// show the file system roots
			contents = FileSystemRegistry.listRoots();
		} else {
			// show the directory contents
			try {
				dir = (FileConnection)Connector.open(url);
				contents = dir.list();
				// include our special 'up' item
				dirList.append(UP_DIR, dirIcon);
			} catch (IOException e) {
				// not a lot we can do
				System.err.println(e.toString());
			}
		}
		
		if (contents != null) {
			while (contents.hasMoreElements()) {
				String name = (String)contents.nextElement();
				
				if (isDirectory(name)) {
					dirList.append(name, dirIcon);
				} else {
					dirList.append(name, fileIcon);
				}
			}
		}
		
		currDir = url;
	}
	
	/**
	 * Changes to the given subdirectory.  This must be a subdirectory of the
	 * current directory.
	 * 
	 * @param name the name of the directory to enter.
	 */
	private void enterDirectory(String name) {
		System.out.println("enterDirectory:"+name);
		if (isDirectory(name)) {
			if (currDir == null) {
				if (!name.equals(UP_DIR)) {
					System.out.println("Entering dir from fake root");
					// need to create a URL
					currDir = URL_PREFIX+name;
				}
			} else {
				if (name.equals(UP_DIR)) {
					System.out.println("Going up");
					currDir = upDirectory(currDir);
				} else {
					System.out.println("Going down to "+name);
					currDir = currDir+name;
				}
			}
			
			showDir(currDir);
		}
	}

	/**
	 * Checks whether a string represents a file or a directory.
	 * 
	 * This is based on directories ending with the separator character '/'.
	 * 
	 * @param name The filename to check.
	 * @return <code>true</code> if the name represents a directory,
	 * <code>false</code> if it represents a file.
	 */
	private boolean isDirectory(String name) {
		return name.endsWith(SEPARATOR);
	}
	
	/**
	 * Returns the URL of the parent of the given directory.
	 * 
	 * @param directory The directory to start from.
	 * @return The URL of the parent of the given directory.
	 */
	private String upDirectory(String directory) {
		String upDir = null;
		
		if (currDir != null) {
			// ignore the final separator
			int index = currDir.lastIndexOf(SEP_CHAR, currDir.length() -2);
			
			if (index > 0) {
				// make sure we include the trailing separator
				upDir = currDir.substring(0, index+1);
			}
			
			if (upDir.equals(URL_PREFIX)) {
				// we're back up to our fake root
				upDir = null;
			}
		}
		
		return upDir;
	}
}
