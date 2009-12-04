package net.sourceforge.keepassj2me.tools;

import java.io.IOException;
import java.util.Enumeration;

import javax.microedition.io.ConnectionNotFoundException;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

/**
 * Provides a user interface to browse for a file.
 * 
 * @author Kevin O'Rourke.
 * @author Stepan Strelets
 *
 */
public class FileBrowser implements CommandListener {
	/** The full FileConnection URL of the selected file. */
	private String fileUrl = null;
	/** A flag to indicate that a file has been chosen. */
	private boolean isChosen;
	/** The currently-displayed directory. */
	private String currDir = null;
	/** The user interface for the browser. */
	private List dirList = null;
	/** Title */
	private String title;
	
	/** Choose a file or directory. */
	private Command cmdSelect;
	/** Go up to parent directory. */
	private Command cmdUp;
	/** Go back without selecting. */
	private Command cmdCancel;
	
	/** Icon representing a directory. */
	private Image dirIcon;
	/** Icon representing a file. */
	private Image fileIcon;
	/** Icon representing a up dir. */
	private Image upIcon;
	
	/** The path separator string. */
	private static final String SEPARATOR = "/";
	/** The path separator as a character. */
	private static final char SEP_CHAR = '/';
	/** The display string for the parent directory. */
	private static final String UP_DIR = "../";
	/** The prefix for a FileConnection URL. */
	private static final String URL_PREFIX = "file://";
	
	/**
	 * Constructs a new FileBrowser and displays it.
	 * 
	 * @param midlet The running MIDlet.
	 * @param title List title
	 * @param dirIcon Image for directories icon
	 * @param fileIcon Image for file icon
	 */
	public FileBrowser(String title, Image dirIcon, Image fileIcon, Image upIcon) {
		this.isChosen = false;
		
		this.title = title;
		this.dirIcon = dirIcon;
		this.fileIcon = fileIcon;
		this.upIcon = upIcon;
		
		// set up the commands
		cmdSelect = new Command("Select", Command.OK, 1);
		cmdCancel = new Command("Cancel", Command.CANCEL, 1);
		cmdUp = new Command("Up", Command.BACK, 2);
	}
	
	public static boolean isSupported() {
		// check whether the FileConnection API (part of JSR75) is available
		return System.getProperty("microedition.io.file.FileConnection.version") != null; 
	}
	
	/**
	 * Set directory for browsing
	 * 
	 * @param dir Directory name
	 */
	public void setDir(String dir) {
		this.currDir = dir;
	}
	
	/**
	 * Display browser and wait for choice
	 */
	public void display() {
		// show the browser
		DisplayStack.pushSplash();
		try {
			while (!isChosen) {
				this.showDir(this.currDir);
				synchronized (this) {
					this.wait();
				}
			}
		} catch (Exception e) {
			// #ifdef DEBUG
				System.out.println(e.toString());
			// #endif
		}
		DisplayStack.pop();
	}
	
	/* (non-Javadoc)
	 * @see javax.microedition.lcdui.CommandListener#commandAction(javax.microedition.lcdui.Command, javax.microedition.lcdui.Displayable)
	 */
	public void commandAction(Command cmd, Displayable dsp) {
		if (cmd == cmdSelect) {
			final String name = dirList.getString(dirList.getSelectedIndex());
			// #ifdef DEBUG 
				System.out.println("SELECT: " + name);
			// #endif
			if (isDirectory(name)) {
				enterDirectory(name);
			} else {
				fileUrl = currDir + name;
				
				// we're finished
				isChosen = true;
				// #ifdef DEBUG 
					System.out.println("File selected: <"+fileUrl+">");
				// #endif
			}
			
		} else if (cmd == cmdUp) {
			// #ifdef DEBUG 
				System.out.println("..");
			// #endif
			enterDirectory(UP_DIR);
			
		} else if (cmd == cmdCancel) {
			// #ifdef DEBUG
				System.out.println("Cancel");
			// #endif
			// indicate that no file was chosen
			fileUrl = null;
			isChosen = true;
			
		} else {
			// #ifdef DEBUG
				System.err.println("Unexpected Command");
			// #endif
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

	public String getDir() {
		return this.currDir;
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
	private void showDir(String url) {
		Enumeration contents = null;
		FileConnection dir = null;
		
		// the user interface is provided by a List
		dirList = new List(this.title, List.IMPLICIT);
		dirList.addCommand(cmdSelect);
		dirList.addCommand(cmdCancel);
		dirList.addCommand(cmdUp);
		dirList.setSelectCommand(cmdSelect);
		dirList.setCommandListener(this);
		
		// try open dir, if dir not found (or it is file) try go up
		while (url != null) {
			try {
				// #ifdef DEBUG
					System.out.println("Try open dir: <"+url+">");
				// #endif
				dir = (FileConnection)Connector.open(url, Connector.READ);
				if (dir.isDirectory()) {
					break;
				} else {
					// #ifdef DEBUG
						System.out.println("Dir not found: <"+url+">");
					// #endif
					dir.close();
					dir = null;
				};
				
			} catch (ConnectionNotFoundException e) {
				// there's a problem with the URL
				// #ifdef DEBUG
					System.err.println(e.toString());
				// #endif
			} catch (IOException e) {
				// some other bad thing happened
				// #ifdef DEBUG
					System.err.println(e.toString());
				// #endif
			}
			url = upDirectory(url);
		}
		
		// dir found on previous step?
		if (dir != null) {
			try {
				contents = dir.list();
			} catch (IOException e) {
				// not a lot we can do
				// #ifdef DEBUG
					System.err.println(e.toString());
				// #endif
			}
		} else {
			url = null;
			contents = FileSystemRegistry.listRoots();
		}
		
		if (url != null) {
			// include our special 'up' item
			dirList.append(UP_DIR, upIcon);
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
		DisplayStack.replaceLast(dirList);
	}
	
	/**
	 * Changes to the given subdirectory.  This must be a subdirectory of the
	 * current directory.
	 * 
	 * @param name the name of the directory to enter.
	 */
	private void enterDirectory(String name) {
		// #ifdef DEBUG
			System.out.println("enterDirectory:" + name);
		// #endif
		if (isDirectory(name)) {
			if (currDir == null) {
				if (!name.equals(UP_DIR)) {
					// #ifdef DEBUG
						System.out.println("Entering dir from fake root");
					// #endif
					// need to create a URL
					currDir = URL_PREFIX + SEPARATOR + name;
				}
			} else {
				if (name.equals(UP_DIR)) {
					// #ifdef DEBUG
						System.out.println("Going up <"+currDir+">");
					// #endif
					currDir = upDirectory(currDir);
					// #ifdef DEBUG
						System.out.println("Going up <"+currDir+">");
					// #endif
				} else {
					currDir = currDir + name;
					// #ifdef DEBUG
						System.out.println("Going down to " + name + " <"+currDir+">");
					// #endif
				}
			}
			
			synchronized (this) {
				this.notify();
			}
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
	 * @return The URL of the parent or <code>null</code> of the given directory.
	 */
	private String upDirectory(String directory) {
		String upDir = null;
		
		if (directory != null) {
			if (directory.indexOf(URL_PREFIX) == 0) directory = directory.substring(URL_PREFIX.length());
			
			// ignore the final separator
			int index = directory.lastIndexOf(SEP_CHAR, directory.length() - 2);
			
			if (index > 0) {
				// make sure we include the trailing separator
				upDir = URL_PREFIX + directory.substring(0, index + 1);
				
			} else {
				// we're back up to our fake root
				upDir = null;
			}
		}
		
		return upDir;
	}
}
