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
import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.prng.RandomGenerator;


/**
 * KDB Entry
 * @author Stepan Strelets
 */
public class KeydbEntry extends KeydbEntity {
	/**UUID, uniquely identifying an entry, FIELDSIZE must be 16 */
	public final static short FIELD_UUID 		= 0x0001;
	/** Group ID, identifying the group of the entry, FIELDSIZE = 4
	 *  It can be any 32-bit value except 0 and 0xFFFFFFFF */
	public final static short FIELD_GID 		= 0x0002;
	/** Image ID, identifying the image/icon of the entry, FIELDSIZE = 4 */
	public final static short FIELD_IMAGE 		= 0x0003;
	/** Title of the entry, FIELDDATA is an UTF-8 encoded string */
	public final static short FIELD_TITLE 		= 0x0004;
	/** URL string, FIELDDATA is an UTF-8 encoded string */
	public final static short FIELD_URL 		= 0x0005;
	/** UserName string, FIELDDATA is an UTF-8 encoded string */
	public final static short FIELD_USER 		= 0x0006;
	/** Password string, FIELDDATA is an UTF-8 encoded string */
	public final static short FIELD_PASSWORD 	= 0x0007;
	/** Notes string, FIELDDATA is an UTF-8 encoded string */
	public final static short FIELD_NOTE 		= 0x0008;
	/** Creation time, FIELDSIZE = 5, FIELDDATA = packed date/time */
	public final static short FIELD_CTIME 		= 0x0009;
	/** Last modification time, FIELDSIZE = 5, FIELDDATA = packed date/time */
	public final static short FIELD_MTIME 		= 0x000A;
	/** Last access time, FIELDSIZE = 5, FIELDDATA = packed date/time */
	public final static short FIELD_ATIME 		= 0x000B;
	/** Expiration time, FIELDSIZE = 5, FIELDDATA = packed date/time */
	public final static short FIELD_EXPIRE 		= 0x000C;
	/** Binary description UTF-8 encoded string */
	public final static short FIELD_BINDESC 	= 0x000D;
	/** Binary data */
	public final static short FIELD_BINDATA 	= 0x000E;

	/*
	 * Offsets used for lazy fields loading
	 */
	
	/** UUID, uniquely identifying an entry */
	private byte[] uuid; 
	private int uuidOffset;
	/** Group ID, identifying the group of the entry */
	public int groupId;
	/** Image ID, identifying the image/icon of the entry */
	public int imageIndex;
	/** Title of the entry */
	public String title;
	/** URL string */
	private String url;
	private int urlOffset;
	/** UserName string */
	private String username;
	private int usernameOffset;
	/** Password string */
	private String password;
	private int passwordOffset;
	/** Notes string */
	private String note;
	private int noteOffset;
	/** Binary description string */
	private String binaryDesc;
	private int binaryDescOffset;
	/** Binary data */
	private byte[] binaryData;
	private int binaryDataOffset;
	/** Size of attachment */
	public int binaryDataLength;
	
	/**
	 * Constructor
	 * @param db
	 */
	public KeydbEntry(KeydbDatabase db) {
		this.db = db;
		clean();
	}
	
	/**
	 * Reset all fields
	 */
	public void clean() {
		super.clean();
		uuid = null;
		uuidOffset = -1;
		groupId = 0;
		imageIndex = 0;
		title = null;
		url = null;
		urlOffset = -1;
		username = null;
		usernameOffset = -1;
		password = null; //TODO: clean value
		passwordOffset = -1;
		note = null;
		noteOffset = -1;
		binaryDesc = null;
		binaryDescOffset = -1;
		binaryData = null;
		binaryDataOffset = -1;
		binaryDataLength = 0;
	}
	
	/**
	 * Read entry data from buffer
	 * @param buf
	 * @param offset
	 * @return bytes readed
	 */
	protected int read(int offset, int index) {
		byte[] buf = db.plainContent;
		this.index = index;
		this.offset = offset;
		short fieldType;
		int fieldSize;
		while(true) {
			fieldType = (short)KeydbUtil.readShort(buf, offset);
			offset += 2;
			fieldSize = KeydbUtil.readInt(buf, offset);
			offset += 4;

			switch (fieldType) {
			case FIELD_IGNORE:
				// Ignore field
				break;
			case FIELD_UUID:
				uuidOffset = offset;
				break;
			case FIELD_GID:
				groupId = KeydbUtil.readInt(buf, offset);
				break;
			case FIELD_IMAGE:
				imageIndex = KeydbUtil.readInt(buf, offset);
				break;
			case FIELD_TITLE:
				title = KeydbUtil.getString(buf, offset);
				break;
			case FIELD_URL:
				urlOffset = offset;
				break;
			case FIELD_USER:
				usernameOffset = offset;
				break;
			case FIELD_PASSWORD:
				passwordOffset = offset;
				break;
			case FIELD_NOTE:
				noteOffset = offset;
				break;
			case FIELD_CTIME:
				ctimeOffset = offset;
				break;
			case FIELD_MTIME:
				mtimeOffset = offset;
				break;
			case FIELD_ATIME:
				atimeOffset = offset;
				break;
			case FIELD_EXPIRE:
				expireOffset = offset;
				break;
			case FIELD_BINDESC:
				binaryDescOffset = offset;
				break;
			case FIELD_BINDATA:
				binaryDataOffset = offset;
				binaryDataLength = fieldSize;
				break;
			case FIELD_TERMINATOR:
				return offset - this.offset;
			}
			offset += fieldSize;
		}
	}
	
	/**
	 * Get packed entry
	 * @return packed entry
	 * @throws IOException
	 */
	public byte[] getPacked() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		writeField(bytes, FIELD_UUID, this.getUUID());
		writeField(bytes, FIELD_GID, groupId);
		writeField(bytes, FIELD_IMAGE, imageIndex);
		writeField(bytes, FIELD_TITLE, title);
		writeField(bytes, FIELD_URL, getUrl());
		writeField(bytes, FIELD_USER, getUsername());
		writeField(bytes, FIELD_PASSWORD, getPassword());
		writeField(bytes, FIELD_NOTE, getNote());
		writeField(bytes, FIELD_CTIME, KeydbUtil.packTime(getCTime()));
		writeField(bytes, FIELD_MTIME, KeydbUtil.packTime(getMTime()));
		writeField(bytes, FIELD_ATIME, KeydbUtil.packTime(getATime()));
		writeField(bytes, FIELD_EXPIRE, KeydbUtil.packTime(getExpire()));
		writeField(bytes, FIELD_BINDESC, getBinaryDesc());
		writeField(bytes, FIELD_BINDATA, getBinaryData());
		writeShort(bytes, FIELD_TERMINATOR);
		writeLong(bytes, 0);
		
		return bytes.toByteArray();
	}
	
	/**
	 * Generate entry uuid
	 * @return uuid
	 */
	public byte[] createUUID() {//FIXME: make sure this is unique
		byte[] uuid = new byte[16];
		RandomGenerator rnd = new DigestRandomGenerator(new SHA1Digest());
		rnd.addSeedMaterial(System.currentTimeMillis());
		rnd.nextBytes(uuid);
		return uuid;
	}
	/**
	 * Get currect uuid or create new
	 * @return uuid
	 */
	public byte[] getUUID() {
		if (uuid == null) {
			if (uuidOffset != -1) {
				uuid = new byte[16];
				System.arraycopy(db.plainContent, uuidOffset, uuid, 0, 16);
			} else {
				uuid = this.createUUID();
			}
		};
		return uuid;
	}
	/**
	 * Set uuid
	 * @param uuid
	 */
	public void setUUID(byte[] uuid) {
		if (uuid.length == 16) this.uuid = uuid;
	}
	/**
	 * Get URL
	 * @return url
	 */
	public String getUrl() {
		if (url == null) {
			if (urlOffset != -1) {
				url = KeydbUtil.getString(db.plainContent, urlOffset);
			} else {
				url = "";			
			}
		}
		return url;
	}
	/**
	 * Set URL
	 * @param url
	 */
	public void setUrl(String url) {
		this.url = url;
	}
	/**
	 * Get user name
	 * @return user name
	 */
	public String getUsername() {
		if (username == null) {
			if (usernameOffset != -1) {
				username = KeydbUtil.getString(db.plainContent, usernameOffset);
			} else {
				username = "";
			}
		}
		return username;
	}
	/**
	 * Set user name
	 * @param username
	 */
	public void setUsername(String username) {
		this.username = username;
	}
	/**
	 * Get note
	 * @return note
	 */
	public String getNote() {
		if (note == null) {
			if (noteOffset != -1) {
				note = KeydbUtil.getString(db.plainContent, noteOffset);
			} else {
				note = "";
			}
		}
		return note;
	}
	/**
	 * Set note
	 * @param note
	 */
	public void setNote(String note) {
		this.note = note;
	}
	/**
	 * Get attachment description
	 * @return description
	 */
	public String getBinaryDesc() {
		if (binaryDesc == null) {
			if (binaryDescOffset != -1) {
				binaryDesc = KeydbUtil.getString(db.plainContent, binaryDescOffset);
			} else {
				binaryDesc = "";
			}
		}
		return binaryDesc;
	}
	/**
	 * Set attachment description
	 * @param binaryDesc
	 */
	public void setBinaryDesc(String binaryDesc) {
		this.binaryDesc = binaryDesc;
		this.binaryDescOffset = -1;
	}
	/**
	 * Get attachement
	 * @return data
	 */
	public byte[] getBinaryData() {
		if ((binaryData == null) && (binaryDataOffset != -1)) {
			binaryData = KeydbUtil.getBinary(db.plainContent, binaryDataOffset, binaryDataLength);
		}
		return binaryData;
	}
	/**
	 * Set attachement
	 * @param binaryData
	 */
	public void setBinaryData(byte[] binaryData) {
		this.binaryData = binaryData;
		this.binaryDataOffset = -1;
		if (binaryData != null) {
			this.binaryDataLength = binaryData.length;
		} else {
			this.binaryDataLength = 0;
		}
	}
	/**
	 * Get password as binary
	 * @return password
	 */
	public byte[] getPasswordBin() {
		if (password != null) {
			try {
				return password.getBytes("UTF-8");
			} catch (UnsupportedEncodingException e) {
			}
			
		} else if (passwordOffset != -1) {
			return KeydbUtil.getBinary(db.plainContent, passwordOffset, KeydbUtil.strlen(db.plainContent, passwordOffset));
			 
		};
		return null;
	}
	/**
	 * Get password
	 * @return password
	 */
	public String getPassword() {
		if (password == null) {
			if (passwordOffset != -1) {
				password = KeydbUtil.getString(db.plainContent, passwordOffset);
			} else {
				password = "";
			}
		}
		return password;
	}
	/**
	 * Set password
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}
	
	/**
	 * Save entry to database
	 */
	public void save() {
		try {
			if (this.index >= 0) { 
				if (this.changed) {
					Date now = new Date();
					this.setMTime(now);
					this.db.updateEntry(this.index, this.getPacked());
				};
			} else {
				Date now = new Date();
				this.setCTime(now);
				this.setMTime(now);
				this.setATime(now);
				this.index = this.db.addEntry(this.getPacked());
			}
		} catch (KeydbLockedException e) {
		} catch (IOException e) {
		}
	}
	
	/**
	 * Delete entry from database
	 */
	public void delete() {
		try {
			if (this.index >= 0) {
				this.db.deleteEntry(this.index);
				this.clean();
			};
		} catch (KeydbLockedException e) {
		}
	}
	
	/**
	 * Get entry type meta or regular entry
	 * @return boolean this entry is meta
	 */
	public boolean isMeta() {
		if (this.binaryDataOffset == -1) return false;
		if (this.noteOffset == -1) return false;
		if (!this.getBinaryDesc().equals("bin-stream")) return false;
		if (this.title == null) return false;
		if (!this.title.equals("Meta-Info")) return false;
		if (!this.getUsername().equals("SYSTEM")) return false;
		if (!this.getUrl().equals("$")) return false;
		if (this.imageIndex != 0) return false;
		return true;
	}
}
