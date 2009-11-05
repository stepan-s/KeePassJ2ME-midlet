package net.sourceforge.keepassj2me.datasource;

import java.io.IOException;
import java.io.InputStream;

import javax.microedition.io.file.FileConnection;

/**
 * This HACK for FileConnection InputStream (available() does not return filesize)
 * @author Stepan Strelets
 */
public class FileInputStream extends InputStream {
	private InputStream is;
	private long size; 
	
	public FileInputStream(FileConnection conn) throws IOException {
		this.is = conn.openInputStream();
		this.size = conn.fileSize();
	}
	public long skip(long n) throws IOException {
		return is.skip(n);
	}
	public synchronized void reset() throws IOException {
		is.reset();
	}
	public int read(byte[] b) throws IOException {
		return is.read(b);
	}
	public int read(byte[] arg0, int arg1, int arg2) throws IOException {
		return is.read(arg0, arg1, arg2);
	}
	public int read() throws IOException {
		return is.read();
	}
	public boolean markSupported() {
		return is.markSupported();
	}
	public synchronized void mark(int readlimit) {
		is.mark(readlimit);
	}
	public void close() throws IOException {
		is.close();
	}
	public int available() throws IOException {
		return (int)size;
	}
}
