package net.sourceforge.keepassj2me.keydb;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.rms.RecordStore;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.L10nKeys.keys;

import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.util.encoders.Hex;


/**
 * Utility
 * @author Stepan Strelets
 * @author Bill Zwicky <wrzwicky@pobox.com>
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
				return new String(buf, offset, KeydbUtil.strlen(buf, offset), "UTF-8");
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
			return KeydbUtil.readTime(buf, offset);
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
				throw new KeydbException(Config.getLocaleString(keys.KD_WRONG_HEX));
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
			throw new KeydbException(Config.getLocaleString(keys.KD_KEYFILE_EMPTY));
			
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
				throw new KeydbException(Config.getLocaleString(keys.KD_KEYFILE_NOT_EXIST, new String[] {filename}));
			};
			return hash(conn.openInputStream(), (int)conn.fileSize());
			
		} catch (IOException e) {
			throw new KeydbException(Config.getLocaleString(keys.KD_KEYFILE_ACCESS_ERR));
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
				throw new KeydbException(Config.getLocaleString(keys.KD_KEYFILE_EMPTY));
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
			throw new KeydbException(Config.getLocaleString(keys.KD_KEYFILE_READ_ERR));
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

	/**
	 * Unpack date from 5 byte format.
	 * The five bytes at 'offset' are unpacked to a java.util.Date instance.
	 * @param buf 
	 * @param offset 
	 * @return date
	 */
	public static Date readTime( byte[] buf, int offset ) {
		int dw1 = KeydbUtil.readUByte( buf, offset );
	    int dw2 = KeydbUtil.readUByte( buf, offset + 1 );
	    int dw3 = KeydbUtil.readUByte( buf, offset + 2 );
	    int dw4 = KeydbUtil.readUByte( buf, offset + 3 );
	    int dw5 = KeydbUtil.readUByte( buf, offset + 4 );
	
	    //System.out.println("Packed date: "+dw1+", "+dw2+", "+dw3+", "+dw4+", "+dw5);
	    
	    // Unpack 5 byte structure to date and time
	    int year   =  (dw1 << 6) | (dw2 >> 2);
	    int month  = ((dw2 & 0x00000003) << 2) | (dw3 >> 6);
	    int day    =  (dw3 >> 1) & 0x0000001F;
	    int hour   = ((dw3 & 0x00000001) << 4) | (dw4 >> 4);
	    int minute = ((dw4 & 0x0000000F) << 2) | (dw5 >> 6);
	    int second =   dw5 & 0x0000003F;
	
	    //System.out.println("Unpacked date: "+year+"-"+month+"-"+day+" "+hour+":"+minute+":"+second);
	    
	    //magic date i.e. unset
	    if ((year == 2999)
	    	&& (month == 12)
	    	&& (day == 28)
	    	&& (hour == 23)
	    	&& (minute == 59)
	    	&& (second == 59)) return null;
	    
	    try {
		    Calendar time = Calendar.getInstance();
		    time.set(Calendar.YEAR, year);
		    time.set(Calendar.MONTH, month - 1);
		    time.set(Calendar.DATE, day);	
		    time.set(Calendar.HOUR_OF_DAY, hour);
		    time.set(Calendar.MINUTE, minute);
		    time.set(Calendar.SECOND, second);
		    return time.getTime();
		    
	    } catch (Exception e) {
	    	return null;
	    }
	}

	/*
	 * 76543210 76543210 76543210 76543210 76543210
	 * \      Y      /\ M /\ D /\ H  /\  i  /\  s /
	 */
	/**
	 * Pack time
	 * @param time
	 * @return packed time
	 */
	public static byte[] packTime(Date time) {
		byte[] buf = new byte[5];
		int year, month, day, hour, minute, second;
		
		if (time == null) {
			year = 2999;
		    month = 12;
		    day = 28;
		    hour = 23;
		    minute = 59;
		    second = 59;
		} else {
			Calendar c = Calendar.getInstance();
			c.setTime(time);
			year = c.get(Calendar.YEAR);
		    month = c.get(Calendar.MONTH) + 1;
		    day = c.get(Calendar.DATE);
		    hour = c.get(Calendar.HOUR_OF_DAY);
		    minute = c.get(Calendar.MINUTE);
		    second = c.get(Calendar.SECOND);
		};
		
		buf[0] = (byte)(year >> 6);
		buf[1] = (byte)(((year & 0x3F) << 2) | ((month & 0xF) >> 2));
		buf[2] = (byte)(((month & 0x3) << 6) | ((day & 0x1F) << 1) | ((hour & 0x1F) >> 4));
		buf[3] = (byte)(((hour & 0xF) << 4) | ((minute & 0x3F) >> 2));
		buf[4] = (byte)(((minute & 0x3) << 6) | (second & 0x3F));
		
		return buf;
	}

	/**
	   * Read a 32-bit value.
	   * 
	   * @param buf
	   * @param offset
	   * @return value
	   */
	  public static int readInt( byte buf[], int offset ) {
	    return (buf[offset + 0] & 0xFF) + ((buf[offset + 1] & 0xFF) << 8) + ((buf[offset + 2] & 0xFF) << 16)
	           + ((buf[offset + 3] & 0xFF) << 24);
	  }

	/**
	   * Write a 32-bit value.
	   * 
	   * @param val
	   * @param buf
	   * @param offset
	   */
	  public static void writeInt(byte[] buf, int offset, int val) {
	    buf[offset + 0] = (byte)(val & 0xFF);
	    buf[offset + 1] = (byte)((val >>> 8) & 0xFF);
	    buf[offset + 2] = (byte)((val >>> 16) & 0xFF);
	    buf[offset + 3] = (byte)((val >>> 24) & 0xFF);
	  }

	/**
	   * Read an unsigned 16-bit value.
	   * 
	   * @param buf
	   * @param offset
	   * @return value
	   */
	  public static int readShort( byte[] buf, int offset ) {
	    return (buf[offset + 0] & 0xFF) + ((buf[offset + 1] & 0xFF) << 8);
	  }

	/** Read an unsigned byte 
	 * @param buf 
	 * @param offset 
	 * @return byte */
	  public static int readUByte( byte[] buf, int offset ) {
	    return ((int)buf[offset] & 0xFF);
	  }

	/**
	   * Return len of null-terminated string (i.e. distance to null)
	   * within a byte buffer.
	   * 
	   * @param buf
	   * @param offset
	   * @return length
	   */
	  public static int strlen( byte[] buf, int offset ) {
	    int len = 0;
	    while( buf[offset + len] != 0 )
	      len++;
	    return len;
	  }

	/**
	   * Copy a sequence of bytes into a new array.
	   * 
	   * @param b - source array
	   * @param offset - first byte
	   * @param len - number of bytes
	   * @return new byte[len]
	   */
	  public static byte[] extract( byte[] b, int offset, int len ) {
	    byte[] b2 = new byte[len];
	    System.arraycopy( b, offset, b2, 0, len );
	    return b2;
	  }

	/**
	* Compare byte arrays
	 * @param array1 
	 * @param array2 
	 * @return false on different
	*/
	public static boolean compare(byte[] array1, byte[] array2) 
	{
	if (array1.length != array2.length)
	    return false;
	
	for (int i=0; i<array1.length; i++)
	    if (array1[i] != array2[i])
		return false;
	
	return true;
	}

	/**
	 * fill byte array
	 * @param array 
	 * @param value 
	 */
	public static void fill(byte[] array, byte value)
	{
	for (int i=0; i<array.length; i++)
	    array[i] = value;
	}
	
	/**
	 * Format size to pretty form
	 * @param size 
	 * @return formatted string
	 */
	public static String toPrettySize(long size) {
		if (size < 1024) {
			return size + " B";
		} else if (size < 1048576) {
			return size / 1024 + "." + size % 1024 / 102 + " kB";
		} else {
			return size / 1048576 + "." + size % 1048576 / 104857 + " MB";
		}
	}
	
	/**
	 * Get device information
	 * @return strings array - {platform, locale, configuration, profiles, free memory, total memory, recordstore used, recordstore available}
	 */
	public static String [] getHWInfo() {
		int rs_memory_used = -1;
		int rs_memory_available = -1;
		try {
			String [] rsnames = RecordStore.listRecordStores();
			if ((rsnames != null) && (rsnames.length > 0)) {
				RecordStore rs = RecordStore.openRecordStore(rsnames[0], false);
				rs_memory_used = rs.getSize();
				rs_memory_available = rs.getSizeAvailable();
			}
		} catch (Exception e) {
		}
		return new String [] {
			java.lang.System.getProperty("microedition.platform")+"\r\n",
			java.lang.System.getProperty("microedition.locale")+"\r\n",
			java.lang.System.getProperty("microedition.configuration")+"\r\n",
			java.lang.System.getProperty("microedition.profiles")+"\r\n",
			KeydbUtil.toPrettySize(java.lang.Runtime.getRuntime().freeMemory())+"\r\n",
			KeydbUtil.toPrettySize(java.lang.Runtime.getRuntime().totalMemory())+"\r\n",
			(rs_memory_used >= 0 ? KeydbUtil.toPrettySize(rs_memory_used) : "?")+"\r\n",
			(rs_memory_available >= 0 ? KeydbUtil.toPrettySize(rs_memory_available) : "?")
		};
	}
}
