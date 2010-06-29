package net.sourceforge.keepassj2me.keydb;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;

import net.sourceforge.keepassj2me.importerv3.Types;

/**
 * Utility
 * @author Stepan Strelets
 */
public class KeydbUtil {

	/**
	 * Read UTF-8 string from buffer 
	 * @param buf
	 * @param offset
	 * @return string or null
	 */
	public static String getString(byte[] buf, int offset) {
		//FIXME: this code works with null terminated strings (look Types.strlen), but all string in kdb have length - need use for reliability
		if (offset != -1) {
			try {
				return new String(buf, offset, Types.strlen(buf, offset), "UTF-8");
			} catch (UnsupportedEncodingException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Read datetime from buffer
	 * @param buf
	 * @param offset
	 * @return datetime
	 */
	public static Date getDate(byte[] buf, int offset) {
		if (offset != -1) {
			return Types.readTime(buf, offset);
		} else {
			return null;
		}
	}

	/**
	 * Read attachment from buffer
	 * @param buf
	 * @param offset
	 * @param length
	 * @return attachment
	 */
	public static byte[] getBinary(byte[] buf, int offset, int length) {
		if ((offset != -1) && (length > 0)) {
			byte bin[] = new byte[length];
			System.arraycopy(buf, offset, bin, 0, length);
			return bin;
		} else {
			return null;
		}
	}

	/**
	 * Check buffer for hex encoded value
	 * @param buffer
	 * @throws KeydbException
	 */
	public static void checkHex(byte[] buffer) throws KeydbException {
		for (int i = 0; i < buffer.length; ++i) {
			byte b = buffer[i];
			if (
					((b >= 0x30) && (b <= 0x39))	// b is 0-9
					|| ((b >= 0x41) && (b <= 0x46))	// b is A-F
					|| ((b >= 0x61) && (b <= 0x66)) // b is a-f
				) {
				//pass
			} else {
				throw new KeydbException("Wrong HEX");
			}
		}
	}
	
	/**
	 * Calculate hash of key in buffer
	 * @param keyfile
	 * @return hash
	 * @throws KeydbException
	 */
	public static byte[] hashKeyfile(byte[] keyfile) throws KeydbException {
		if (keyfile.length == 0) {
			throw new KeydbException("Keyfile empty");
			
		} else if (keyfile.length == 32) {
			return keyfile;
			
		} else if (keyfile.length == 64) {
			try {
				checkHex(keyfile);
				return Hex.decode(keyfile);
				
			} catch (Exception e) {
				//if hex decode failed try it as binary key
				return hash(keyfile);
			}
			
		} else {
			return hash(keyfile);
		}
	}

	/**
	 * Calculate hash of key in file
	 * @param filename
	 * @return hash
	 * @throws KeydbException
	 */
	public static byte[] hashKeyFile(String filename) throws KeydbException {
		FileConnection conn = null;
		try {
			conn = (FileConnection) Connector.open(filename, Connector.READ);
			if (!conn.exists()) {
				throw new KeydbException("Key file does not exist: " + filename);
			};
			return hash(conn.openInputStream(), (int)conn.fileSize());
			
		} catch (IOException e) {
			throw new KeydbException("Key file access error");
		}
	}
	
	/**
	 * hash data from input stream
	 * @param is input stream
	 * @param size data size or -1
	 * @return hash
	 * @throws KeydbException
	 */
	public static byte[] hash(InputStream is, int size) throws KeydbException {
		try {
			byte[] buf;
			if (size == -1) size = is.available();
			
			switch(size) {
			case 0:
				throw new KeydbException("Key is empty");
			case 32:
				buf = new byte[32]; 
				is.read(buf, 0, 32);
				return buf;
			case 64:
				buf = new byte[64]; 
				is.read(buf, 0, 64);
				try {
					checkHex(buf);
					return Hex.decode(buf);
				} catch (Exception e) {
					return hash(buf);
				}
			default:
				buf = new byte[4096];
				SHA256Digest digest = new SHA256Digest();
				while(size > 0) {
					int len = (size > buf.length ? buf.length : size);
					is.read(buf, 0, len);
					digest.update(buf, 0, len);
					size -= len;
				};
				byte[] hash = new byte[digest.getDigestSize()];
				digest.doFinal(hash, 0);
				return hash;
			}
		} catch (IOException e) {
			throw new KeydbException("Key read error");
		}
	}
	
	/**
	 * Get hash of binary chanks
	 * @param bufs
	 * @return hash
	 */
	public static byte[] hash(byte[][] bufs) {
		SHA256Digest digest = new SHA256Digest();
		for(int i = 0; i < bufs.length; ++i)
			digest.update(bufs[i], 0, bufs[i].length);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		return hash;
	}

	/**
	 * Get hash of buffer part
	 * @param buf
	 * @param offset
	 * @param length
	 * @return hash
	 */
	public static byte[] hash(byte[] buf, int offset, int length) {
		SHA256Digest digest = new SHA256Digest();
		digest.update(buf, offset, length);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		return hash;
	}

	/**
	 * Get hash of buffer
	 * @param buf
	 * @return hash
	 */
	public static byte[] hash(byte[] buf) {
		return hash(buf, 0, buf.length);
	}

	/**
	 * Get hash of string
	 * @param str
	 * @return hash
	 */
	public static byte[] hash(String str) {
		return hash(str.getBytes());
	}
	
	/**
	 * Get hex representation of hash
	 * @param hash
	 * @return hex string
	 */
	public static String hashToString(byte[] hash) {
		String out = "";
		for(int i = 0; i < hash.length; ++i) {
			byte b = hash[i];
			out += Integer.toHexString((b >> 4) & 0xF) + Integer.toHexString(b & 0xF); 
		}
		return out;
	}
}
