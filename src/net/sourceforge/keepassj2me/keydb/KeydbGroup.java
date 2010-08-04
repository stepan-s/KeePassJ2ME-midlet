package net.sourceforge.keepassj2me.keydb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;


/**
 * KDB Group
 * @author Stepan Strelets
 */
public class KeydbGroup extends KeydbEntity {
	/** Group ID, FIELDSIZE must be 4 bytes
	 *  It can be any 32-bit value except 0 and 0xFFFFFFFF */
	public final static short FIELD_ID			= 0x0001;
	/** Group name, FIELDDATA is an UTF-8 encoded string */
	public final static short FIELD_NAME		= 0x0002;
	/** Creation time, FIELDSIZE = 5, FIELDDATA = packed date/time */
	public final static short FIELD_CTIME		= 0x0003;
	/** Last modification time, FIELDSIZE = 5, FIELDDATA = packed date/time */
	public final static short FIELD_MTIME		= 0x0004;
	/** Last access time, FIELDSIZE = 5, FIELDDATA = packed date/time */
	public final static short FIELD_ATIME		= 0x0005;
	/** Expiration time, FIELDSIZE = 5, FIELDDATA = packed date/time */
	public final static short FIELD_EXPIRE		= 0x0006;
	/** Image ID, FIELDSIZE must be 4 bytes */
	public final static short FIELD_IMAGE		= 0x0007;
	/** Level, FIELDSIZE = 2 */
	public final static short FIELD_LEVEL		= 0x0008;
	/** Flags, 32-bit value, FIELDSIZE = 4 */
	public final static short FIELD_FLAGS		= 0x0009;

	/** Id of parent group */
	public int parentId = 0;
	
	/** Group ID, it can be any 32-bit value except 0 and 0xFFFFFFFF */
	public int id;
	/** Image ID */
	public int imageIndex;
	/** Group name */
	public String name;
	/** Level */
	public int level;       //short
	/** Flags, used by KeePass internally, don't use */
	public int flags;

	/**
	 * Constructor
	 * @param db
	 */
	public KeydbGroup(KeydbDatabase db) {
		this.db = db;
		clean();
	}
	
	public void clean() {
		super.clean();
		id = 0;
		imageIndex = 0;
		name = null;
		level = 0;
		flags = 0;
		
		parentId = 0;
	}
	
	/**
	 * Read group data from buffer
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
			case FIELD_ID:
				id = KeydbUtil.readInt(buf, offset);
				break;
			case FIELD_NAME:
				name = KeydbUtil.getString(buf, offset);
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
			case FIELD_IMAGE:
				imageIndex = KeydbUtil.readInt(buf, offset);
				break;
			case FIELD_LEVEL:
				level = KeydbUtil.readShort(buf, offset);
				break;
			case FIELD_FLAGS:
				flags = KeydbUtil.readInt(buf, offset);
				break;
			case FIELD_TERMINATOR:
				return offset - this.offset;
			}
			offset += fieldSize;
		}
	}

	/**
	 * Get packed group
	 * @return packed group
	 * @throws IOException
	 * @throws KeydbLockedException
	 */
	public byte[] getPacked() throws IOException, KeydbLockedException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		if (id == 0) id = this.db.getUniqueGroupId();
		
		writeField(bytes, FIELD_ID, id);
		writeField(bytes, FIELD_NAME, name);
		writeField(bytes, FIELD_CTIME, KeydbUtil.packTime(getCTime()));
		writeField(bytes, FIELD_MTIME, KeydbUtil.packTime(getMTime()));
		writeField(bytes, FIELD_ATIME, KeydbUtil.packTime(getATime()));
		writeField(bytes, FIELD_EXPIRE, KeydbUtil.packTime(getExpire()));
		writeField(bytes, FIELD_IMAGE, imageIndex);
		writeField(bytes, FIELD_LEVEL, (short)level);
		writeField(bytes, FIELD_FLAGS, flags);
		writeShort(bytes, FIELD_TERMINATOR);
		writeLong(bytes, 0);
		
		return bytes.toByteArray();
	}
	
	/**
	 * Save group to database
	 */
	public void save() {
		try {
			if (this.index >= 0) {
				if (this.changed) {
					Date now = new Date();
					this.setMTime(now);
					this.db.updateGroup(this.index, this.getPacked());
				};
			} else {
				Date now = new Date();
				this.setCTime(now);
				this.setMTime(now);
				this.setATime(now);
				this.index = this.db.addGroup(this.getPacked(), this.parentId);
			}
		} catch (KeydbLockedException e) {
		} catch (IOException e) {
		} catch (KeydbException e) {
		}
	}
	
	/**
	 * Delete group from database
	 */
	public void delete() {
		try {
			if (this.index >= 0) {
				this.db.deleteGroup(this.index);
				this.clean();
			};
		} catch (KeydbLockedException e) {
		}
	}
}
