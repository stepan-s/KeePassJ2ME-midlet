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
import javax.microedition.lcdui.TextField;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.Icons;
import net.sourceforge.keepassj2me.L10nKeys.keys;

/**
 * Provides a user interface to browse for a file.
 * 
 * @author Kevin O'Rourke.
 * @author Stepan Strelets
 *
 */
public class FileBrowser implements CommandListener {
	/** List item type - `go to parent directory` */
	public final static int ITEM_UP = 1;
	/** List item type - `directory` */
	public final static int ITEM_DIR = 2;
	/** List item type - `file` */
	public final static int ITEM_FILE = 3;
	
    private Command activatedCommand;
    private int activatedIndex = -1;
    private int activatedType;
	
	/** The full FileConnection URL of the selected file. */
	private String fileUrl = null;
	/** The currently-displayed directory. */
	private String currDir = null;
	/** The user interface for the browser. */
	private ListTag dirList = null;
	/** Title */
	private String title;
	/** Default name for new file */
	private String newFileName = null;
	
	/** Choose a file or directory. */
	private Command cmdSelect;
	/** Go up to parent directory. */
	private Command cmdUp;
	/** Go back without selecting. */
	private Command cmdCancel;
	private Command cmdNewFile;
	private Command cmdNewDir;
	
	/** Icon representing a directory. */
	private Image dirIcon;
	/** Icon representing a file. */
	private Image fileIcon;
	/** Icon representing a up dir. */
	private Image upIcon;
	
	/** The path separator string. */
	public static final String SEPARATOR = "/";
	/** The path separator as a character. */
	public static final char SEP_CHAR = '/';
	/** The display string for the parent directory. */
	public static final String UP_DIR = "../";
	/** The prefix for a FileConnection URL. */
	public static final String URL_PREFIX = "file://";
	
	/**
	 * Constructs a new FileBrowser and displays it.
	 * 
	 * @param midlet The running MIDlet.
	 * @param title List title
	 * @param dirIcon Image for directories icon
	 * @param fileIcon Image for file icon
	 * @param upIcon Image for up icon
	 */
	public FileBrowser(String title, Image dirIcon, Image fileIcon, Image upIcon) {
		this.title = title;
		this.dirIcon = dirIcon;
		this.fileIcon = fileIcon;
		this.upIcon = upIcon;
		
		// set up the commands
		cmdSelect = new Command(Config.getLocaleString(keys.SELECT), Command.SCREEN, 2);
		cmdCancel = new Command(Config.getLocaleString(keys.CANCEL), Command.SCREEN, 3);
		cmdUp = new Command(Config.getLocaleString(keys.UP), Command.SCREEN, 3);
		cmdNewFile = new Command(Config.getLocaleString(keys.NEW_FILE), Command.SCREEN, 1);
		cmdNewDir = new Command(Config.getLocaleString(keys.NEW_DIR), Command.SCREEN, 3);
	}
	
	/**
	 * Check whether the FileConnection API (part of JSR75) is available 
	 * @return true if supported
	 */
	public static boolean isSupported() {
		return System.getProperty("microedition.io.file.FileConnection.version") != null; 
	}
	
	/**
	 * Select existing file for opening
	 * @param title dialog title
	 * @param dir start path
	 * @param initial_dir initial directory or <code>null</code> for previous used
	 * @return file path or <code>null</code> on cancel
	 */
	public static String open(String title, String dir) {
		FileBrowser fileBrowser = new FileBrowser(title, Icons.getInstance().getImageById(Icons.ICON_DIR), Icons.getInstance().getImageById(Icons.ICON_FILE), Icons.getInstance().getImageById(Icons.ICON_BACK));
		if (dir != null) fileBrowser.setDir(dir);
		else fileBrowser.setDir(Config.getInstance().getLastDir());
		fileBrowser.display(false);
		String url = fileBrowser.getUrl();
		if (url != null) {
			Config.getInstance().setLastDir(url);
		};
		return url;
	}

	/**
	 * Select filename (existing or new) for saving
	 * @param title dialog title
	 * @param dir initial directory or <code>null</code> for previous used
	 * @param defaultFileName default name
	 * @return file path or <code>null</code> on cancel
	 */
	public static String save(String title, String dir, String defaultFileName) {
		FileBrowser fileBrowser = new FileBrowser(title, Icons.getInstance().getImageById(Icons.ICON_DIR), Icons.getInstance().getImageById(Icons.ICON_FILE), Icons.getInstance().getImageById(Icons.ICON_BACK));
		if (defaultFileName != null) fileBrowser.setDefaultFileName(defaultFileName);
		if (dir != null) fileBrowser.setDir(dir);
		else fileBrowser.setDir(Config.getInstance().getLastDir());
		fileBrowser.display(true);
		String url = fileBrowser.getUrl();
		if (url != null) {
			Config.getInstance().setLastDir(url);
		};
		return url;
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
	 * Set default filename for prompt
	 * @param name
	 */
	public void setDefaultFileName(String name) {
		this.newFileName = name;
	}
	
	/**
	 * Display browser and wait for choice
	 * @param save true - save dialog, false - load dialog
	 */
	public void display(boolean save) {
		DisplayStack.getInstance().pushSplash();
		showDir(currDir, save);
		boolean run = true;
		while (run) {
			try {
				activatedCommand = null;
				
				synchronized (this.dirList) {
					this.dirList.wait();
				}
				
				if (activatedCommand == null) {
					fileUrl = null;
					break;
				}

				if (activatedCommand == cmdSelect) {
					switch(activatedType) {
					case ITEM_UP:
						enterDirectory(UP_DIR);
						showDir(currDir, save);
						break;
					case ITEM_DIR:
						enterDirectory(dirList.getString(activatedIndex));
						showDir(currDir, save);
						break;
					case ITEM_FILE:
						String name = dirList.getString(activatedIndex);
						if (save) {
							FileConnection c = (FileConnection)Connector.open(currDir + name, Connector.READ);
							if (c.exists()) {
								if (MessageBox.showConfirm(Config.getLocaleString(keys.OVERWRITE_FILE_Q))) {
									// #ifdef DEBUG
									System.out.println("Try overwrite file: <"+currDir + name+">");
									// #endif
									fileUrl = currDir + name;
									run = false;
								};
							} else {
								// #ifdef DEBUG
								System.out.println("Try create file: <"+currDir + name+">");
								// #endif
								fileUrl = currDir + name;
								run = false;
							};
						} else {
							fileUrl = currDir + name;
							run = false;
						};
						break;
					};
					
				} else if (activatedCommand == cmdUp) {
					enterDirectory(UP_DIR);
					showDir(currDir, save);
					
				} else if (activatedCommand == cmdCancel) {
					fileUrl = null;
					run = false;
					
				} else if (activatedCommand == cmdNewDir) {
					InputBox ib = new InputBox(Config.getLocaleString(keys.ENTER_DIR_NAME), "", 100, TextField.ANY);
					String name = ib.getResult();
					if ((name != null) && (name.length() > 0)) {
						// #ifdef DEBUG
						System.out.println("Try create dir: <"+currDir + name + SEPARATOR+">");
						// #endif
						FileConnection c = (FileConnection)Connector.open(currDir + name + SEPARATOR, Connector.WRITE);
						c.mkdir();
						enterDirectory(name + SEPARATOR);
						showDir(currDir, save);
					};
				} else if (activatedCommand == cmdNewFile) {
					InputBox ib = new InputBox(Config.getLocaleString(keys.ENTER_DIR_NAME), newFileName == null ? "" : newFileName, 100, TextField.ANY);
					String name = ib.getResult();
					if ((name != null) && (name.length() > 0)) { 
						FileConnection c = (FileConnection)Connector.open(currDir + name, Connector.READ);
						if (c.exists()) {
							if (MessageBox.showConfirm(Config.getLocaleString(keys.OVERWRITE_FILE_Q))) {
								// #ifdef DEBUG
								System.out.println("Try overwrite file: <"+currDir + name+">");
								// #endif
								fileUrl = currDir + name;
								run = false;
							};
						} else {
							// #ifdef DEBUG
							System.out.println("Try create file: <"+currDir + name+">");
							// #endif
							fileUrl = currDir + name;
							run = false;
						};
					};
				}
			} catch (Exception e) {
				MessageBox.showAlert(e.getMessage());
				showDir(currDir, save);
			}
		}
		DisplayStack.getInstance().pop();
	}
	
	public void commandAction(Command cmd, Displayable dsp) {
		activatedCommand = cmd;
		activatedIndex = ((ListTag)dsp).getSelectedIndex();
		activatedType = ((ListTag)dsp).getSelectedTagInt();
		
		synchronized (this.dirList) {
			this.dirList.notify();
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
	 * Get current directory
	 * @return directory
	 */
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
	private void showDir(String url, boolean save) {
		Enumeration contents = null;
		FileConnection dir = null;
		
		// the user interface is provided by a List
		dirList = new ListTag(this.title, ListTag.IMPLICIT);
		dirList.addCommand(cmdSelect);
		dirList.addCommand(cmdCancel);
		dirList.addCommand(cmdUp);
		if (save) {
			dirList.addCommand(cmdNewFile);
			dirList.addCommand(cmdNewDir);
		};
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
			} catch (Exception e) {
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
			dirList.append(UP_DIR, upIcon, ITEM_UP);
		}
		
		if (contents != null) {
			while (contents.hasMoreElements()) {
				String name = (String)contents.nextElement();
				
				if (isDirectory(name)) {
					dirList.append(name, dirIcon, ITEM_DIR);
				} else {
					dirList.append(name, fileIcon, ITEM_FILE);
				}
			}
		}
		
		currDir = url;
		DisplayStack.getInstance().replaceLast(dirList);
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
