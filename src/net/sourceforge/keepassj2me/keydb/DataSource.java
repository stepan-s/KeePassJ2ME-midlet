package net.sourceforge.keepassj2me.keydb;

import java.io.InputStream;

import javax.microedition.midlet.MIDlet;

/**
 * Data adapter
 * @author Stepan Strelets
 */
public abstract class DataSource {
	
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
}
