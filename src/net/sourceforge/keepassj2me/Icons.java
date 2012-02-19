/*
	Copyright 2008-2011 Stepan Strelets
	http://keepassj2me.sourceforge.net/

	This file is part of KeePass for J2ME.
	
	KeePass for J2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, version 2.
	
	KeePass for J2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with KeePass for J2ME.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sourceforge.keepassj2me;

import java.io.IOException;

import javax.microedition.lcdui.Image;

/**
 * Icons set manager 
 * @author Stepan Strelets
 */
public class Icons {
	private static Icons instance = null;
	
	/** Icons count */
    public static final int NUM_ICONS = 69;//in icons
	/** Extra icons count (negative indexes) */
    public static final int NUM_EXTRA_ICONS = 2;//in extraIcons
    
	/** `Open last` icon index */
    public static final int ICON_OPEN_LAST = 61;
	/** `Open` icon index */
    public static final int ICON_OPEN = 49;
	/** `Information` icon index */
	public static final int ICON_INFO = 46;
	/** `Setup` icon index */
    public static final int ICON_SETUP = 34;
	/** `Exit` icon index */
    public static final int ICON_EXIT = 45;
	/** `New` icon index */
    public static final int ICON_NEW = 10;

	/** `Directory` icon index */
    public static final int ICON_DIR = 48;
	/** `File` icon index */
	public static final int ICON_FILE = 22;
	/** `Search` icon index */
	public static final int ICON_SEARCH = 40;
	
	/** `Alert` icon index */
	public static final int ICON_ALERT = 2;
	
	/** `Back` icon index */
    public static final int ICON_BACK = -1;
	/** `Logo` icon index */
    public static final int ICON_LOGO = -2;
    
	private boolean disabled = false; 
	private Image icons[];
	private Image extraIcons[];

	private Icons() {
		disabled = Config.getInstance().isIconsDisabled();
		icons = new Image[NUM_ICONS];
		extraIcons = new Image[NUM_EXTRA_ICONS];
	}
	
	/**
	 * Get icons manager
	 * @return instance
	 */
	static public Icons getInstance() {
		if (instance == null) instance = new Icons();
		return instance;
	}
	
	private Image loadIconByName(String name) {
		Image image = null;
		try {
			image = Image.createImage("/images/" + name + ".png");
		} catch (IOException e) {
		}
		return image;
	}
	
	private void loadIcon(int index) {
		icons[index] = loadIconByName((index < 10 ? "0" : "") + index);
	}
	
	private void loadExtraIcon(int index){
		String name = null;
		switch(index) {
		case ICON_BACK: name = "back"; break;
		case ICON_LOGO: name = "icon"; break;
		}
		if (name != null) extraIcons[-index - 1] = loadIconByName(name);
	}

	/**
	 * Release loaded icons
	 */
	public void freeResources() {
		for(int i = 0; i < icons.length; ++i) icons[i] = null;
		for(int i = 0; i < extraIcons.length; ++i) extraIcons[i] = null;
	}
	
	/**
	 * Disable/enable icons
	 * @param disabled
	 */
	public void setIconsDisabled(boolean disabled) {
		this.disabled = disabled;
		if (disabled) freeResources();
	}
	
	/**
	 * Get image by index
	 * 
	 * @param index Image index
	 * @return Image or null
	 */
	public Image getImageById(int index) {
		if (!disabled) {
			if (index >= 0) {
				if (index < icons.length) {
					if (icons[index] == null) loadIcon(index);
					return icons[index];
				}
			} else {
				int i = -index - 1;
				if (i < extraIcons.length) {
					if (extraIcons[i] == null) loadExtraIcon(index);
					return extraIcons[i];
				}
			}
		};
		return null;
	}
	
	/**
	 * Get image by index, if not found try get image by <code>def</code>
	 * 
	 * @param index primary Image index
	 * @param def second Image index (if primary not found)
	 * @return Image or null
	 */
	public Image getImageById(int index, int def) {
		Image res = getImageById(index);
		if (res == null) res = getImageById(def);
		return res;
	}
}
