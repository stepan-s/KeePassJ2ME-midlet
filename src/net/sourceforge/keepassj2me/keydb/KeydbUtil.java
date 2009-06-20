package net.sourceforge.keepassj2me.keydb;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;

import net.sourceforge.keepassj2me.importerv3.Types;

/**
 * Utility
 * @author Stepan Strelets
 */
public class KeydbUtil {

	public static String getString(byte[] buf, int offset) {
		if (offset != -1) {
			try {
				return new String(buf, offset, Types.strlen(buf, offset), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		} else {
			return null;
		};
	}

	public static Date getDate(byte[] buf, int offset) {
		if (offset != -1) {
			return Types.readTime(buf, offset);
		} else {
			return null;
		};
	}

	public static byte[] getBinary(byte[] buf, int offset, int length) {
		if ((offset != -1) && (length > 0)) {
			byte bin[] = new byte[length];
			System.arraycopy(buf, offset, bin, 0, length);
			return bin;
		} else {
			return null;
		};
	}

	public static byte[] hashKeyfile(byte[] keyfile) throws KeydbException {
		if (keyfile.length == 0) {
			throw new KeydbException("Keyfile empty");
			
		} else if (keyfile.length == 32) {
			return keyfile;
			
		} else if (keyfile.length == 64) {
			return Hex.decode(keyfile);
			
		} else {
			return hash(keyfile);
		};
	}

	public static byte[] hash(byte[][] bufs) {
		SHA256Digest digest = new SHA256Digest();
		for(int i = 0; i < bufs.length; ++i)
			digest.update(bufs[i], 0, bufs[i].length);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		return hash;
	}

	public static byte[] hash(byte[] buf, int offset, int length) {
		SHA256Digest digest = new SHA256Digest();
		digest.update(buf, offset, length);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		return hash;
	}

	public static byte[] hash(byte[] buf) {
		return hash(buf, 0, buf.length);
	}

	public static byte[] hash(String str) {
		return hash(str.getBytes());
	}
}
