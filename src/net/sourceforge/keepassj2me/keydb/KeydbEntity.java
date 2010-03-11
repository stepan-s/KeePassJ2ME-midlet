package net.sourceforge.keepassj2me.keydb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * KDB entity, base for group and entry 
 * @author Stepan Strelets
 */
public abstract class KeydbEntity {
	public final static short FIELD_IGNORE		= 0x0000; //Invalid or comment block, block is ignored
	public final static short FIELD_TERMINATOR	= (short)0xFFFF; //Entity terminator, FIELDSIZE must be 0
	
	protected KeydbDatabase db = null;
	protected boolean changed = true;
	
	/** Entity index */
	public int index;
	
	/** Whole entity offset in database */
	public int offset;
	
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
	
	public Date getCTime() {
		if ((ctime == null) && (ctimeOffset != -1)) {
			ctime = KeydbUtil.getDate(db.plainContent, ctimeOffset);
		}
		return ctime;
	}
	public void setCTime(Date ctime) {
		this.ctime = ctime;
	}
	
	public Date getMTime() {
		if ((mtime == null) && (mtimeOffset != -1)) {
			mtime = KeydbUtil.getDate(db.plainContent, mtimeOffset);
		}
		return mtime;
	}
	public void setMTime(Date mtime) {
		this.mtime = mtime;
	}
	
	public Date getATime() {
		if ((atime == null) && (atimeOffset != -1)) {
			atime = KeydbUtil.getDate(db.plainContent, atimeOffset);
		}
		return atime;
	}
	public void setATime(Date atime) {
		this.atime = atime;
	}
	
	public Date getExpire() {
		if ((expire == null) && (expireOffset != -1)) {
			expire = KeydbUtil.getDate(db.plainContent, expireOffset);
		}
		return expire;
	}
	public void setExpire(Date expire) {
		this.expireOffset = -1;
		this.expire = expire;
	}
}
