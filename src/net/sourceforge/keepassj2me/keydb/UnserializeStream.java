package net.sourceforge.keepassj2me.keydb;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * @author Stepan Strelets
 */
public class UnserializeStream extends DataInputStream {
	public UnserializeStream(byte[] serialized) {
		super(new ByteArrayInputStream(serialized));
	}
}
