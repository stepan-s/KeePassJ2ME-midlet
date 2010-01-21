package net.sourceforge.keepassj2me.keydb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.prng.RandomGenerator;

import net.sourceforge.keepassj2me.importerv3.Types;

/**
 * KDB Entry
 * @author Stepan Strelets
 */
public class KeydbEntry {
	public final static short FIELD_IGNORE 		= 0x0000; //Invalid or comment block, block is ignored
	public final static short FIELD_UUID 		= 0x0001; //UUID, uniquely identifying an entry, FIELDSIZE must be 16
	public final static short FIELD_GID 		= 0x0002; //Group ID, identifying the group of the entry, FIELDSIZE = 4
														  //It can be any 32-bit value except 0 and 0xFFFFFFFF
	public final static short FIELD_IMAGE 		= 0x0003; //Image ID, identifying the image/icon of the entry, FIELDSIZE = 4
	public final static short FIELD_TITLE 		= 0x0004; //Title of the entry, FIELDDATA is an UTF-8 encoded string
	public final static short FIELD_URL 		= 0x0005; //URL string, FIELDDATA is an UTF-8 encoded string
	public final static short FIELD_USER 		= 0x0006; //UserName string, FIELDDATA is an UTF-8 encoded string
	public final static short FIELD_PASSWORD 	= 0x0007; //Password string, FIELDDATA is an UTF-8 encoded string
	public final static short FIELD_NOTE 		= 0x0008; //Notes string, FIELDDATA is an UTF-8 encoded string
	public final static short FIELD_CTIME 		= 0x0009; //Creation time, FIELDSIZE = 5, FIELDDATA = packed date/time
	public final static short FIELD_MTIME 		= 0x000A; //Last modification time, FIELDSIZE = 5, FIELDDATA = packed date/time
	public final static short FIELD_ATIME 		= 0x000B; //Last access time, FIELDSIZE = 5, FIELDDATA = packed date/time
	public final static short FIELD_EXPIRE 		= 0x000C; //Expiration time, FIELDSIZE = 5, FIELDDATA = packed date/time
	public final static short FIELD_BINDESC 	= 0x000D; //Binary description UTF-8 encoded string
	public final static short FIELD_BINDATA 	= 0x000E; //Binary data
	public final static short FIELD_TERMINATOR	= (short)0xFFFF; //Entry terminator, FIELDSIZE must be 0

	KeydbDatabase db = null;
	
	/** whole entry offset in database */
	private int offset;
	
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
	/** Creation time */
	private Date ctime;
	private int ctimeOffset;
	/** Last modification time */
	private Date mtime;
	private int mtimeOffset;
	/** Last access time */
	private Date atime;
	private int atimeOffset;
	/** Expiration time */
	private Date expire;
	private int expireOffset;
	/** Binary description string */
	private String binaryDesc;
	private int binaryDescOffset;
	/** Binary data */
	private byte[] binaryData;
	private int binaryDataOffset;
	public int binaryDataLength;
	
	public KeydbEntry(KeydbDatabase db) {
		this.db = db;
		clean();
	}
	
	/**
	 * Reset all fields
	 */
	public void clean() {
		offset = -1;
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
		ctime = null;
		ctimeOffset = -1;
		mtime = null;
		mtimeOffset = -1;
		atime = null;
		atimeOffset = -1;
		expire = null;
		expireOffset = -1;
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
	protected int read(int offset) {
		byte[] buf = db.plainContent;
		this.offset = offset;
		short fieldType;
		int fieldSize;
		while(true) {
			fieldType = (short)Types.readShort(buf, offset);
			offset += 2;
			fieldSize = Types.readInt(buf, offset);
			offset += 4;

			switch (fieldType) {
			case FIELD_IGNORE:
				// Ignore field
				break;
			case FIELD_UUID:
				uuidOffset = offset;
				break;
			case FIELD_GID:
				groupId = Types.readInt(buf, offset);
				break;
			case FIELD_IMAGE:
				imageIndex = Types.readInt(buf, offset);
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
		};
	}
	
	public byte[] getPacked() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		byte[] uuid = this.getUUID();
		if (uuid == null) {
			this.setUUID(this.createUUID());
			uuid = this.getUUID();
		}
		write(bytes, FIELD_UUID, uuid);
		write(bytes, FIELD_GID, groupId);
		write(bytes, FIELD_IMAGE, imageIndex);
		write(bytes, FIELD_TITLE, title);
		write(bytes, FIELD_URL, getUrl());
		write(bytes, FIELD_USER, getUsername());
		write(bytes, FIELD_PASSWORD, getPassword());
		write(bytes, FIELD_NOTE, getNote());
		write(bytes, FIELD_CTIME, Types.packTime(getCTime()));
		write(bytes, FIELD_MTIME, Types.packTime(getMTime()));
		write(bytes, FIELD_ATIME, Types.packTime(getATime()));
		write(bytes, FIELD_EXPIRE, Types.packTime(getExpire()));
		write(bytes, FIELD_BINDESC, getBinaryDesc());
		write(bytes, FIELD_BINDATA, getBinaryData());
		write(bytes, FIELD_TERMINATOR);
		
		return bytes.toByteArray();
	}
	private void write(ByteArrayOutputStream out, short field) {
		out.write((byte)(field & 0xFF));
		out.write((byte)((field >> 8) & 0xFF));
	}
	private void write(ByteArrayOutputStream out, short field, String value) throws IOException {
		if (value != null) { 
			write(out, field);
			out.write(value.getBytes().length + 1);
			out.write(value.getBytes());
			out.write((byte)0);
		};
	}
	private void write(ByteArrayOutputStream out, short field, byte[] value) throws IOException {
		if (value != null) { 
			write(out, field);
			out.write(value.length);
			out.write(value);
		};
	}
	private void write(ByteArrayOutputStream out, short field, int value) throws IOException {
		write(out, field);
		out.write(4);
		out.write(value);
	}
	
	public byte[] createUUID() {
		byte[] uuid = new byte[16];
		RandomGenerator rnd = new DigestRandomGenerator(new SHA1Digest());
		rnd.addSeedMaterial(System.currentTimeMillis());
		rnd.nextBytes(uuid);
		return uuid;
	}
	public byte[] getUUID() {
		if ((uuid == null) && (uuidOffset != -1)) {
			uuid = new byte[16];
			System.arraycopy(db.plainContent, uuidOffset, uuid, 0, 16);
		};
		return uuid;
	}
	public void setUUID(byte[] uuid) {
		if (uuid.length == 16) this.uuid = uuid;
	}
	public String getUrl() {
		if ((url == null) && (urlOffset != -1)) {
			url = KeydbUtil.getString(db.plainContent, urlOffset); 
		}
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUsername() {
		if ((username == null) && (usernameOffset != -1)) {
			username = KeydbUtil.getString(db.plainContent, usernameOffset);
		}
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getNote() {
		if ((note == null) && (noteOffset != -1)) {
			note = KeydbUtil.getString(db.plainContent, noteOffset);
		}
		return note;
	}
	public void setNote(String note) {
		this.note = note;
	}
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
	public String getBinaryDesc() {
		if ((binaryDesc == null) && (binaryDescOffset != -1)) {
			binaryDesc = KeydbUtil.getString(db.plainContent, binaryDescOffset);
		}
		return binaryDesc;
	}
	public void setBinaryDesc(String binaryDesc) {
		this.binaryDesc = binaryDesc;
	}
	public byte[] getBinaryData() {
		if ((binaryData == null) && (binaryDataOffset != -1)) {
			binaryData = KeydbUtil.getBinary(db.plainContent, binaryDataOffset, binaryDataLength);
		}
		return binaryData;
	}
	public void setBinaryData(byte[] binaryData) {
		this.binaryData = binaryData;
	}
	public byte[] getPasswordBin() {
		if (password != null) {
			return password.getBytes();
			
		} else if (passwordOffset != -1) {
			return KeydbUtil.getBinary(db.plainContent, passwordOffset, Types.strlen(db.plainContent, passwordOffset));
			 
		} else {
			return null;
		};
	}
	public String getPassword() {
		if ((password == null) && (passwordOffset != -1)) {
			password = KeydbUtil.getString(db.plainContent, passwordOffset);
		}
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
}
