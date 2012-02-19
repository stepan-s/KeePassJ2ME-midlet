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

package net.sourceforge.keepassj2me.keydb;

/**
 * @author Stepan Strelets
 */
public interface IKeydbGroupContentRecever {
	/**
	 * Receive group
	 * @param group
	 */
	public void addKeydbGroup(KeydbGroup group);
	/**
	 * Groups count
	 * @param count
	 */
	public void totalGroups(int count);
	/**
	 * Receive entry
	 * @param entry
	 */
	public void addKeydbEntry(KeydbEntry entry);
}
