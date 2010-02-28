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
	
	protected void write(ByteArrayOutputStream out, short field) {
		out.write((byte)(field & 0xFF));
		out.write((byte)((field >> 8) & 0xFF));
	}
	protected void write(ByteArrayOutputStream out, short field, String value) throws IOException {
		if (value != null) { 
			write(out, field);
			out.write(value.getBytes().length + 1);
			out.write(value.getBytes());
			out.write((byte)0);
		};
	}
	protected void write(ByteArrayOutputStream out, short field, byte[] value) throws IOException {
		if (value != null) { 
			write(out, field);
			out.write(value.length);
			out.write(value);
		};
	}
	protected void write(ByteArrayOutputStream out, short field, int value) throws IOException {
		write(out, field);
		out.write(4);
		out.write(value);
	}
	protected void write(ByteArrayOutputStream out, short field, short value) throws IOException {
		write(out, field);
		out.write(2);
		write(out, value);
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
		this.expire = expire;
	}
}
