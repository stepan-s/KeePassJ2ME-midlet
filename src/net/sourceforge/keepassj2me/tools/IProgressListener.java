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

import net.sourceforge.keepassj2me.KeePassException;

/**
 * Progress listener interface 
 * @author Stepan Strelets
 */
public interface IProgressListener {
	/**
	 * Set progress position and text message
	 * @param procent Current progress 0-100
	 * @param message Current message, if <code>null</code> does not change
	 * @throws KeePassException throw on cancel
	 */
	public void setProgress(int procent, String message) throws KeePassException;
}
