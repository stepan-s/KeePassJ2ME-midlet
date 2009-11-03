package net.sourceforge.keepassj2me.keydb;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * @author Stepan Strelets
 */
public class SerializeStream extends DataOutputStream {
	public SerializeStream() {
	    super(new ByteArrayOutputStream());
	}
	public byte[] getBytes() {
		return ((ByteArrayOutputStream)this.out).toByteArray();
	}
}
