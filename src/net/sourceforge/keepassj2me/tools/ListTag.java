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
	
	/**
	 * Constructor
	 * @param title
	 * @param listType
	 */
	public ListTag(String title, int listType) {
		super(title, listType);
		this.tags = new Vector();
	}
	
	/**
	 * Append list item
	 * @param stringPart
	 * @param imagePart
	 * @param tagPart
	 * @return index
	 */
	public int append(String stringPart, Image imagePart, Object tagPart) {
		int index = this.append(stringPart, imagePart);
		this.tags.addElement(tagPart);
		return index;
	}
	/**
	 * Append list item
	 * @param stringPart
	 * @param imagePart
	 * @param tagPart
	 * @return index
	 */
	public int append(String stringPart, Image imagePart, int tagPart) {
		return this.append(stringPart, imagePart, new Integer(tagPart));
	}
	
	/**
	 * Get tag by item index
	 * @param index
	 * @return tag
	 */
	public Object getTag(int index) {
		if (index >= this.tags.size()) throw new ArrayIndexOutOfBoundsException();
		return this.tags.elementAt(index);
	}
	/**
	 * Get tag by item index
	 * @param index
	 * @return tag
	 */
	public int getTagInt(int index) {
		return ((Integer)this.getTag(index)).intValue();
	}
	/**
	 * Get tag by item index
	 * @param index
	 * @return tag
	 */
	public String getTagString(int index) {
		return (String)this.getTag(index);
	}
	
	/**
	 * Set selected item by tag
	 * @param tag
	 * @param selected
	 */
	public void setSelectedTag(Object tag, boolean selected) {
		int index = this.tags.indexOf(tag);
		if (index >= 0) this.setSelectedIndex(index, selected);
	}
	/**
	 * Set selected item by tag
	 * @param tag
	 * @param selected
	 */
	public void setSelectedTag(int tag, boolean selected) {
		this.setSelectedTag(new Integer(tag), selected);
	}
	
	/**
	 * Get tag of selected item
	 * @return tag
	 */
	public Object getSelectedTag() {
		return this.getTag(this.getSelectedIndex());
	}
	/**
	 * Get tag of selected item
	 * @return tag
	 */
	public int getSelectedTagInt() {
		return this.getTagInt(this.getSelectedIndex());
	}
	/**
	 * Get tag of selected item
	 * @return tag
	 */
	public String getSelectedTagString() {
		return this.getTagString(this.getSelectedIndex());
	}
}
