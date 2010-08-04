package net.sourceforge.keepassj2me.tools;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.L10nConstants.keys;

/**
 * Jar content browser
 * 
 * @author Stepan Strelets
 */
public class JarBrowser implements CommandListener {
	private String currDir = null;
	private String fileUrl = null;
	private boolean isChosen;
	
	private List dirList = null;
	private String title;
	private Command cmdSelect;
	private Command cmdCancel;
	
	private Image fileIcon;

	/**
	 * Create jar browser
	 * 
	 * @param midlet parent midlet
	 * @param title List title
	 * @param fileIcon icon for file
	 */
	public JarBrowser(String title, Image fileIcon) {
		this.title = title;
		this.isChosen = false;
		this.fileIcon = fileIcon;
		
		cmdSelect = new Command(Config.getLocaleString(keys.SELECT), Command.OK, 1);
		cmdCancel = new Command(Config.getLocaleString(keys.CANCEL), Command.CANCEL, 1);
	}
	
	/**
	 * Check if jar contain content
	 * @param url base path in jar
	 * @return true if content exist
	 */
	public static boolean contentExists(String url) {
		return (null != (new Object()).getClass().getResourceAsStream(url+"/ls"));
	}
	
	/**
	 * Set directory for browsing
	 * 
	 * @param dir
	 */
	public void setDir(String dir) {
		this.currDir = dir;
	}
	
	/**
	 * Get selected file 
	 * @return <code>String</code> or <code>null</code> if cancel
	 */
	public String getUrl() {
		return fileUrl;
	}
	
	/**
	 * Display browser and wait for user
	 * @throws KeePassException 
	 */
	public void display() throws KeePassException {
		DisplayStack.getInstance().pushSplash();
		try {
			while (!isChosen) {
				this.showDir(this.currDir);
				try {
					synchronized (this) {
						this.wait();
					}
				} catch (Exception e) {
					// #ifdef DEBUG
						System.out.println(e.toString());
					// #endif
				}
			}
		} finally {
			DisplayStack.getInstance().pop();
		};
	}
	
	/**
	 * Construct list for dir <code>url</code>
	 * @param url Directory
	 */
	private void showDir(String url) throws KeePassException {
		dirList = new List(this.title, List.IMPLICIT);
		dirList.addCommand(cmdSelect);
		dirList.addCommand(cmdCancel);
		dirList.setSelectCommand(cmdSelect);
		dirList.setCommandListener(this);
		
		InputStream is = getClass().getResourceAsStream(url+"/ls");
		if (is != null) {
			try {
				byte buf[] = new byte[is.available()];
				is.read(buf);
				is.close();
				String ls = new String(buf, "UTF-8");
				// #ifdef DEBUG
					System.out.println("LS: "+ls);
				// #endif
				String name;
				do {
					int i = ls.indexOf(0x0A);
					if (i > 0) {
						name = ls.substring(0, i).trim();
						ls = ls.substring(i + 1);
					} else {
						name = ls.trim();
						ls = "";
					};
					if (name.length() > 0) {
						// #ifdef DEBUG
							System.out.println("ADD: '"+name+"'");
						// #endif
						dirList.append(name, this.fileIcon);
					};
				} while (ls.length() > 0);
			} catch (IOException e) {
				// #ifdef DEBUG
					System.out.println(e.toString());
				// #endif
			};
		};
		if (dirList.size() == 0) {
			throw new KeePassException(Config.getLocaleString(keys.MIDLET_NOT_CONTAIN_KDB));
		}
		DisplayStack.getInstance().replaceLast(dirList);
	}
	
	public void commandAction(Command cmd, Displayable dsp) {
		if (cmd == cmdSelect) {
			final String name = Integer.toString(dirList.getSelectedIndex());
			// #ifdef DEBUG 
				System.out.println("SELECT: " + name + " ("+dirList.getString(dirList.getSelectedIndex())+")");
			// #endif
			fileUrl = currDir + '/' + name;
			
			// we're finished
			isChosen = true;
			// #ifdef DEBUG 
				System.out.println("File selected: <"+fileUrl+">");
			// #endif
			
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
}
