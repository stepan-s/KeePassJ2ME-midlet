package net.sourceforge.keepassj2me.datasource;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * @author Stepan Strelets
 */
public class UnserializeStream extends DataInputStream {
	/**
	 * Constructor
	 * @param serialized
	 */
	public UnserializeStream(byte[] serialized) {
		super(new ByteArrayInputStream(serialized));
	}
}
