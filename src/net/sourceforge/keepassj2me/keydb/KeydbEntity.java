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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * KDB entity, base for group and entry 
 * @author Stepan Strelets
 */
public abstract class KeydbEntity {
	/** Invalid or comment block, block is ignored */
	public final static short FIELD_IGNORE		= 0x0000;
	/** Entity terminator, FIELDSIZE must be 0 */
	public final static short FIELD_TERMINATOR	= (short)0xFFFF;
	
	protected KeydbDatabase db = null;
	protected boolean changed = true;
	
	/** Entity index */
	public int index;
	
	/** Whole entity offset in database */
	protected int offset;
	
	/*
	 * Offsets used for lazy fields loading
	 */
	
	/** Creation time */
	protected Date ctime;
	protected int ctimeOffset;
	/** Last modification time */
	protected Date mtime;
	protected int mtimeOffset;
	/** Last access time */
	protected Date atime;
	protected int atimeOffset;
	/** Expiration time */
	protected Date expire;
	protected int expireOffset;
	
	/**
	 * Clean object
	 */
	public void clean() {
		index = -1;
		offset = -1;
		ctime = null;
		ctimeOffset = -1;
		mtime = null;
		mtimeOffset = -1;
		atime = null;
		atimeOffset = -1;
		expire = null;
		expireOffset = -1;
	}
	
	/**
	 * Return database object
	 * @return database
	 */
	public KeydbDatabase getDB() {
		return db;
	}
	
	protected void writeByte(ByteArrayOutputStream out, byte value) {
		out.write(value);
	}
	protected void writeShort(ByteArrayOutputStream out, short value) {
		out.write((byte)(value & 0xFF));
		out.write((byte)((value >> 8) & 0xFF));
	}
	protected void writeLong(ByteArrayOutputStream out, int value) {
		out.write((byte)(value & 0xFF));
		value >>= 8;
		out.write((byte)(value & 0xFF));
		value >>= 8;
		out.write((byte)(value & 0xFF));
		value >>= 8;
		out.write((byte)(value & 0xFF));
	}
	protected void writeBytes(ByteArrayOutputStream out, byte[] value) throws IOException {
		out.write(value);
	}
	
	protected void writeField(ByteArrayOutputStream out, short field, String value) throws IOException {
		writeShort(out, field);
		if (value != null) { 
			byte[] buf = value.getBytes("UTF-8");
			writeLong(out, buf.length + 1);
			writeBytes(out, buf);
		} else {
			writeLong(out, 1);
		}
		writeByte(out, (byte)0);
	}
	protected void writeField(ByteArrayOutputStream out, short field, byte[] value) throws IOException {
		writeShort(out, field);
		if (value != null) { 
			writeLong(out, value.length);
			writeBytes(out, value);
		} else {;
			writeLong(out, 0);
		}
	}
	protected void writeField(ByteArrayOutputStream out, short field, int value) throws IOException {
		writeShort(out, field);
		writeLong(out, 4);
		writeLong(out, value);
	}
	protected void writeField(ByteArrayOutputStream out, short field, short value) throws IOException {
		writeShort(out, field);
		writeLong(out, 2);
		writeShort(out, value);
	}
	
	/*
	 * Setters & getters
	 */
	
	/**
	 * Get create time
	 * @return time
	 */
	public Date getCTime() {
		if ((ctime == null) && (ctimeOffset != -1)) {
			ctime = KeydbUtil.getDate(db.plainContent, ctimeOffset);
		}
		return ctime;
	}
	/**
	 * Set create time
	 * @param ctime
	 */
	public void setCTime(Date ctime) {
		this.ctime = ctime;
	}
	
	/**
	 * Get modify time
	 * @return time
	 */
	public Date getMTime() {
		if ((mtime == null) && (mtimeOffset != -1)) {
			mtime = KeydbUtil.getDate(db.plainContent, mtimeOffset);
		}
		return mtime;
	}
	/**
	 * Set modify time
	 * @param mtime
	 */
	public void setMTime(Date mtime) {
		this.mtime = mtime;
	}
	
	/**
	 * Get access time
	 * @return time
	 */
	public Date getATime() {
		if ((atime == null) && (atimeOffset != -1)) {
			atime = KeydbUtil.getDate(db.plainContent, atimeOffset);
		}
		return atime;
	}
	/**
	 * Set access time
	 * @param atime
	 */
	public void setATime(Date atime) {
		this.atime = atime;
	}
	
	/**
	 * Get expire time
	 * @return time
	 */
	public Date getExpire() {
		if ((expire == null) && (expireOffset != -1)) {
			expire = KeydbUtil.getDate(db.plainContent, expireOffset);
		}
		return expire;
	}
	/**
	 * Set expire time
	 * @param expire
	 */
	public void setExpire(Date expire) {
		this.expireOffset = -1;
		this.expire = expire;
	}
}
