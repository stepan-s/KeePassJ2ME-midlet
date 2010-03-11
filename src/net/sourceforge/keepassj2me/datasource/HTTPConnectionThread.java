package net.sourceforge.keepassj2me.datasource;

// Java
import javax.microedition.io.*;
import javax.microedition.lcdui.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

// Bouncy Castle
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.tools.MessageBox;

//#ifdef DEBUG
	import org.bouncycastle.util.encoders.Hex;
// #endif
import org.bouncycastle.crypto.*;
import org.bouncycastle.crypto.digests.*;
import org.bouncycastle.crypto.params.*;
import org.bouncycastle.crypto.modes.*;
import org.bouncycastle.crypto.engines.*;

/**
 * Download a KDB file from site
 * @author Unknown
 * @author Stepan Strelets
 */
public class HTTPConnectionThread extends Thread {
    String mURL = null, mUserCode = null, mPassCode = null, mEncCode = null;
    Form mForm; //TODO: replace UI with event listener
    byte[] content;
	public static final byte[] ZeroIV = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	public static final int KDB_HEADER_LEN = 124;
	public static final int PASSWORD_KEY_SHA_ROUNDS = 6000;

    /**
     * Construct download thread
     * @param url
     * @param userCode
     * @param passCode
     * @param encCode
     * @param midlet
     * @param form
     */
    public HTTPConnectionThread(String url, String userCode, String passCode, String encCode, Form form) {
    	mURL = url;
    	mUserCode = userCode;
    	mPassCode = passCode;
    	mEncCode = encCode;
    	mForm = form;
    }
    /**
     * Thread run
     */
    public void run() {
		try {
		    connect(mURL, mUserCode, mPassCode, mEncCode, mForm);
		} catch (Exception e) {
		    MessageBox.showAlert("Error from connect(): " + e.toString());
		    content = null;
		}
    }
    
    /**
     * Download file
     * @param url Site URL
     * @param userCode User login
     * @param passCode User password
     * @param encCode 
     * @param form UI to display download progress
     * @throws IOException
     * @throws RecordStoreException
     * @throws KeePassException
     */
    private void connect(String url, String userCode, String passCode, String encCode, Form form)
		throws IOException, KeePassException {
    	// #ifdef DEBUG
    		System.out.println ("connect: 1");
    	// #endif
    	HttpConnection hc = null;
    	InputStream in = null;
    	String rawData = "usercode=" + userCode + "&passcode=" + passCode;
    	String type = "application/x-www-form-urlencoded";
	
    	hc = (HttpConnection)Connector.open(url);
	
    	hc.setRequestMethod(HttpConnection.POST);
    	hc.setRequestProperty("Content-Type", type);
    	hc.setRequestProperty("Content-Length", "13");

    	// #ifdef DEBUG
    		System.out.println ("connect: 2");
    	// #endif
	
    	OutputStream os = hc.openOutputStream();
		os.write(rawData.getBytes());
	
		int rc = hc.getResponseCode();
		// #ifdef DEBUG
			System.out.println ("rc = " + rc);
		// #endif
	
		if (rc != HttpConnection.HTTP_OK) {
			throw new IOException("HTTP response code: " + rc);
		}

		// #ifdef DEBUG
			System.out.println ("connect: 3");
		// #endif
	
		in = hc.openInputStream();
	
		int contentLength = (int)hc.getLength();
		content = null;
		if (contentLength > 0) {
			// length available
	    
			// #ifdef DEBUG
				System.out.println ("connect: 4, contentLength = " + contentLength);
			// #endif
			content = new byte[contentLength];
			in.read(content);
		} else {
			// length not available
	    
			// #ifdef DEBUG
				System.out.println ("connect: 5, contentLength not known");
			// #endif
			//int data;
			content = null;

			final int BUFLEN = 1024;
	    
	    
			int readLen;
			contentLength = 0;
			while (true) {
				byte[] newContent = new byte[contentLength + BUFLEN];
				if (contentLength > 0)
					System.arraycopy (content, 0, newContent, 0, contentLength);
				readLen = in.read(newContent, contentLength, BUFLEN);
				content = newContent;
				contentLength += readLen;
		
				form.append("read: " + readLen + " bytes\r\n");
				// #ifdef DEBUG
					System.out.println ("read: " + readLen + " bytes");
				// #endif
				if (readLen < BUFLEN)
					break;
		
			}
		}
		in.close();
		hc.close();
	
		// Show the response to the user.
		// #ifdef DEBUG
			System.out.println ("Downloaded " + contentLength + " bytes");
		// #endif
		form.append("Downloaded " + contentLength + " bytes\r\n");

		if (contentLength - HTTPConnectionThread.KDB_HEADER_LEN <= 0 ||
				(contentLength - HTTPConnectionThread.KDB_HEADER_LEN) % 16 != 0) {
			form.append("Wrong KDB length ... Download failed because KDB file is not on the server, network error, wrong username, or wrong passcode.\r\n");
			throw new IOException("Wrong KDB length ... Download failed because KDB file is not on the server, network error, wrong username, or wrong passcode.");
		}
	
		form.append("Generating encryption key ...\r\n");

		// decrypt KDB with enc code
		byte[] encKey = passwordKeySHA(encCode);

		form.append("Decrypting KDB ...\r\n");

		BufferedBlockCipher cipher = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
		cipher.init(false, new ParametersWithIV(new KeyParameter(encKey), HTTPConnectionThread.ZeroIV));

		// #ifdef DEBUG
			int outlen =
		// #endif
			cipher.getOutputSize(contentLength - HTTPConnectionThread.KDB_HEADER_LEN);
		
		// #ifdef DEBUG
			System.out.println ("Output size: " + outlen);
		// #endif
	
		// #ifdef DEBUG
			int size = 
		// #endif
			cipher.processBytes(content, HTTPConnectionThread.KDB_HEADER_LEN,
				       contentLength - HTTPConnectionThread.KDB_HEADER_LEN,
				       content, HTTPConnectionThread.KDB_HEADER_LEN);

		// #ifdef DEBUG
			System.out.println ("KDB decrypted length: " + size);
		// #endif
    }
    
    /**
     * Get file content
     * @return byte array with file content
     */
    public byte[] getContent() {
    	return this.content;
    }

    /**
     * Generate key from encryption code by running SHA256 multiple rounds
     * @param encCode String with code
     * @return String with encrypted code
     */
    private byte[] passwordKeySHA(String encCode) {
    	byte[] encBytes = encCode.getBytes();
    	for (int i=0; i<encBytes.length; i++)
    		encBytes[i] -= '0';
	
    	byte[] encKey;

    	SHA256Digest md = new SHA256Digest();
    	encKey = new byte[md.getDigestSize()];

    	// #ifdef DEBUG
    		System.out.println ("encBytes: " + new String(Hex.encode(encBytes)));
    	// #endif
    	md.update( encBytes, 0, encBytes.length );
    	md.doFinal(encKey, 0);
	
    	for (int i=0; i<HTTPConnectionThread.PASSWORD_KEY_SHA_ROUNDS - 1; i++) {
    		md.reset();
    		md.update( encKey, 0, encKey.length);
    		md.doFinal(encKey, 0);
    	}

    	// #ifdef DEBUG
    		System.out.println ("encKey: " + new String(Hex.encode(encKey)));
    	// #endif
	
    	return encKey;
    }
}
