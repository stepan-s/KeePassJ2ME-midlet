package net.sourceforge.keepassj2me.keydb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import net.sourceforge.keepassj2me.importerv3.Types;

/**
 * KDB Group
 * @author Stepan Strelets
 */
public class KeydbGroup extends KeydbEntity {
	public final static short FIELD_ID			= 0x0001; //Group ID, FIELDSIZE must be 4 bytes
														  //It can be any 32-bit value except 0 and 0xFFFFFFFF
	public final static short FIELD_NAME		= 0x0002; //Group name, FIELDDATA is an UTF-8 encoded string
	public final static short FIELD_CTIME		= 0x0003; //Creation time, FIELDSIZE = 5, FIELDDATA = packed date/time
	public final static short FIELD_MTIME		= 0x0004; //Last modification time, FIELDSIZE = 5, FIELDDATA = packed date/time
	public final static short FIELD_ATIME		= 0x0005; //Last access time, FIELDSIZE = 5, FIELDDATA = packed date/time
	public final static short FIELD_EXPIRE		= 0x0006; //Expiration time, FIELDSIZE = 5, FIELDDATA = packed date/time
	public final static short FIELD_IMAGE		= 0x0007; //Image ID, FIELDSIZE must be 4 bytes
	public final static short FIELD_LEVEL		= 0x0008; //Level, FIELDSIZE = 2
	public final static short FIELD_FLAGS		= 0x0009; //Flags, 32-bit value, FIELDSIZE = 4

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
			fieldType = (short)Types.readShort(buf, offset);
			offset += 2;
			fieldSize = Types.readInt(buf, offset);
			offset += 4;

			switch (fieldType) {
			case FIELD_IGNORE:
				// Ignore field
				break;
			case FIELD_ID:
				id = Types.readInt(buf, offset);
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
				imageIndex = Types.readInt(buf, offset);
				break;
			case FIELD_LEVEL:
				level = Types.readShort(buf, offset);
				break;
			case FIELD_FLAGS:
				flags = Types.readInt(buf, offset);
				break;
			case FIELD_TERMINATOR:
				return offset - this.offset;
			}
			offset += fieldSize;
		};
	}

	public byte[] getPacked() throws IOException {
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		
		write(bytes, FIELD_ID, id);
		write(bytes, FIELD_NAME, name);
		write(bytes, FIELD_CTIME, Types.packTime(getCTime()));
		write(bytes, FIELD_MTIME, Types.packTime(getMTime()));
		write(bytes, FIELD_ATIME, Types.packTime(getATime()));
		write(bytes, FIELD_EXPIRE, Types.packTime(getExpire()));
		write(bytes, FIELD_IMAGE, imageIndex);
		write(bytes, FIELD_LEVEL, (short)level);
		write(bytes, FIELD_FLAGS, flags);
		write(bytes, FIELD_TERMINATOR);
		bytes.write(0);
		
		return bytes.toByteArray();
	}
	
	public void save() {
		try {
			if (this.index >= 0) { 
				this.db.updateGroup(this.index, this.getPacked());
			} else {
				this.index = this.db.addGroup(this.getPacked());
			}
		} catch (KeydbLockedException e) {
		} catch (IOException e) {
		}
	}
	
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
