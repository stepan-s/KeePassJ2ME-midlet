package net.sourceforge.keepassj2me;

import java.io.IOException;

import javax.microedition.lcdui.Image;

/**
 * Icons set manager 
 * @author Stepan Strelets
 */
public class Icons {
	private static Icons instance = null;
	
    public static final int NUM_ICONS = 69;//in icons
    public static final int NUM_EXTRA_ICONS = 2;//in extraIcons
    
    public static final int ICON_OPEN_LAST_DOWNLOADED = 42;
    public static final int ICON_OPEN_FROM_JAR = 36;
    public static final int ICON_OPEN_FROM_FILE = 48;
    public static final int ICON_OPEN_FROM_INTERNET = 1;
	public static final int ICON_INFO = 46;
    public static final int ICON_SETUP = 34;
    public static final int ICON_EXIT = 45;
    
    public static final int ICON_DIR = 48;
	public static final int ICON_FILE = 22;
	public static final int ICON_SEARCH = 40;
	
	public static final int ICON_ALERT = 2;
	
    public static final int ICON_BACK = -1;
    public static final int ICON_LOGO = -2;
    
	private boolean disabled = false; 
	private Image icons[];
	private Image extraIcons[];

	public Icons() {
		disabled = Config.getInstance().isIconsDisabled();
		icons = new Image[NUM_ICONS];
		extraIcons = new Image[NUM_EXTRA_ICONS];
	}
	
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

	public void freeResources() {
		for(int i = 0; i < icons.length; ++i) icons[i] = null;
		for(int i = 0; i < extraIcons.length; ++i) extraIcons[i] = null;
	}
	
	public void setIconsDisabled(boolean disabled) {
		this.disabled = disabled;
		freeResources();
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
