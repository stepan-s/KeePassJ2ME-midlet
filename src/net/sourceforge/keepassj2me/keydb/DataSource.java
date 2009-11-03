package net.sourceforge.keepassj2me.keydb;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.midlet.MIDlet;

/**
 * Data adapter
 * @author Stepan Strelets
 */
public abstract class DataSource {
	public static final byte uid = 0;
	
	public abstract void select(MIDlet midlet, String caption) throws KeydbException;

	public abstract InputStream getInputStream() throws KeydbException;

	public abstract byte[] load() throws KeydbException;

	public abstract void save(byte[] content) throws KeydbException;
	
	public static boolean canLoad() {
		return true;
	}
	
	public static boolean canSave() {
		return false;
	}
	
	public abstract void serialize(SerializeStream out) throws IOException;
	public abstract void unserialize(UnserializeStream in) throws IOException;
}
