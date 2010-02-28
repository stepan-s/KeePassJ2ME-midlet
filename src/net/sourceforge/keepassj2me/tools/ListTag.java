package net.sourceforge.keepassj2me.tools;

import java.util.Vector;

import javax.microedition.lcdui.Image;
import javax.microedition.lcdui.List;

/**
 * ListTag extends List adding tagged object to each item
 * @author Stepan Strelets
 */
public class ListTag extends List {
	private Vector tags = null;
	
	public ListTag(String title, int listType) {
		super(title, listType);
		this.tags = new Vector();
	}
	
	public int append(String stringPart, Image imagePart, Object tagPart) {
		int index = this.append(stringPart, imagePart);
		this.tags.addElement(tagPart);
		return index;
	}
	public int append(String stringPart, Image imagePart, int tagPart) {
		return this.append(stringPart, imagePart, new Integer(tagPart));
	}
	
	public Object getTag(int index) {
		if (index >= this.tags.size()) throw new ArrayIndexOutOfBoundsException();
		return this.tags.elementAt(index);
	}
	public int getTagInt(int index) {
		return ((Integer)this.getTag(index)).intValue();
	}
	public String getTagString(int index) {
		return (String)this.getTag(index);
	}
	
	public void setSelectedTag(Object tag, boolean selected) {
		int index = this.tags.indexOf(tag);
		if (index >= 0) this.setSelectedIndex(index, selected);
	}
	public void setSelectedTag(int tag, boolean selected) {
		this.setSelectedTag(new Integer(tag), selected);
	}
	
	public Object getSelectedTag() {
		return this.getTag(this.getSelectedIndex());
	}
	public int getSelectedTagInt() {
		return this.getTagInt(this.getSelectedIndex());
	}
	public String getSelectedTagString() {
		return this.getTagString(this.getSelectedIndex());
	}
}
