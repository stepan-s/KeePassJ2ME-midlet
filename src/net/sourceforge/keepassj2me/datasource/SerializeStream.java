package net.sourceforge.keepassj2me.datasource;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

/**
 * @author Stepan Strelets
 */
public class SerializeStream extends DataOutputStream {
	/**
	 * Constructor
	 */
	public SerializeStream() {
	    super(new ByteArrayOutputStream());
	}
	/**
	 * Get serialized data
	 * @return data
	 */
	public byte[] getBytes() {
		return ((ByteArrayOutputStream)this.out).toByteArray();
	}
}
