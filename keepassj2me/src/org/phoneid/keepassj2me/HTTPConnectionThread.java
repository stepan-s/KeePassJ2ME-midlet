package org.phoneid.keepassj2me;

// PhoneID
import org.phoneid.*;

// Java
import javax.microedition.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.*;
import java.io.UnsupportedEncodingException;

/// record store
import javax.microedition.rms.*;

// Bouncy Castle
import org.bouncycastle1.util.encoders.Hex;
import org.bouncycastle1.crypto.*;
import org.bouncycastle1.crypto.generators.*;
import org.bouncycastle1.crypto.digests.*;
import org.bouncycastle1.crypto.params.*;
import org.bouncycastle1.crypto.paddings.*;
import org.bouncycastle1.crypto.modes.*;
import org.bouncycastle1.crypto.engines.*;
import org.bouncycastle1.util.*;
import org.bouncycastle1.util.encoders.*;

public class HTTPConnectionThread
    extends Thread
{
    String mURL = null, mUserCode = null, mPassCode = null, mEncCode = null;
    KeePassMIDlet mMIDlet;
    Form mForm;

    public HTTPConnectionThread(String url, String userCode, String passCode, String encCode, KeePassMIDlet midlet, Form form) {
	mURL = url;
	mUserCode = userCode;
	mPassCode = passCode;
	mEncCode = encCode;
	mMIDlet = midlet;
	mForm = form;
    }
    
    public void run() {
	try {
	    connect(mURL, mUserCode, mPassCode, mEncCode, mForm);
	} catch (Exception e) {
	    System.out.println ("Error from connect()");
	    MessageBox msg = new MessageBox(Definition.TITLE, "Error from connect(): " + e.toString(),
					    AlertType.ERROR, mMIDlet, false,
					    null);
	    msg.waitForDone();
	    mMIDlet.exit();
	}
    }
    
    private void connect(String url, String userCode, String passCode, String encCode, Form form)
	throws IOException, RecordStoreException, PhoneIDException
    {
	System.out.println ("connect: 1");
	HttpConnection hc = null;
	InputStream in = null;
	String rawData = "usercode=" + userCode + "&passcode=" + passCode;
	String type = "application/x-www-form-urlencoded";
	
	hc = (HttpConnection)Connector.open(url);
	
	hc.setRequestMethod(HttpConnection.POST);
	hc.setRequestProperty("Content-Type", type);
	hc.setRequestProperty("Content-Length", "13");

	System.out.println ("connect: 2");
	
	OutputStream os = hc.openOutputStream();
	os.write(rawData.getBytes());
	
	int rc = hc.getResponseCode();
	System.out.println ("rc = " + rc);
	
	if (rc != HttpConnection.HTTP_OK) {
	    throw new IOException("HTTP response code: " + rc);
	}

	System.out.println ("connect: 3");
	
	in = hc.openInputStream();
	
	int contentLength = (int)hc.getLength();
	byte[] content = null;
	if (contentLength > 0) {
	    // length available
	    
	    System.out.println ("connect: 4, contentLength = " + contentLength);
	    content = new byte[contentLength];
	    in.read(content);
	} else {
	    // length not available
	    
	    System.out.println ("connect: 5, contentLength not known");
	    int data;
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
		
		form.append("read: " + readLen + " bytes\n");
		System.out.println ("read: " + readLen + " bytes");
		if (readLen < BUFLEN)
		    break;
		
	    }
	}
	in.close();
	hc.close();
	
	// Show the response to the user.
	System.out.println ("Downloaded " + contentLength + " bytes");
	form.append("Downloaded " + contentLength + " bytes\n");
	form.append("Generating encryption key ...\n");

	// decrypt KDB with enc code
	byte[] encKey = passwordKeySHA(encCode);

	form.append("Decrypting KDB ...\n");

	BufferedBlockCipher cipher = new BufferedBlockCipher(new CBCBlockCipher(new AESEngine()));
	cipher.init(false, new ParametersWithIV(new KeyParameter(encKey), Definition.ZeroIV));

	int outlen = cipher.getOutputSize(contentLength - Definition.KDB_HEADER_LEN);
	System.out.println ("Output size: " + outlen);
	
	int size = cipher.processBytes(content, Definition.KDB_HEADER_LEN,
				       contentLength - Definition.KDB_HEADER_LEN,
				       content, Definition.KDB_HEADER_LEN);

	System.out.println ("KDB decrypted length: " + size);
	   
	mMIDlet.storeKDBInRecordStore(content, contentLength);
    }

    // generate key from encryption code by running SHA256 multiple rounds
    private byte[] passwordKeySHA(String encCode)
    {
	byte[] encBytes = encCode.getBytes();
	for (int i=0; i<encBytes.length; i++)
	    encBytes[i] -= '0';
	
	byte[] encKey;

	SHA256Digest md = new SHA256Digest();
	encKey = new byte[md.getDigestSize()];

	System.out.println ("encBytes: " + new String(Hex.encode(encBytes)));
	md.update( encBytes, 0, encBytes.length );
	md.doFinal(encKey, 0);
	
	for (int i=0; i<Definition.PASSWORD_KEY_SHA_ROUNDS - 1; i++) {
	    md.reset();
	    md.update( encKey, 0, encKey.length);
	    md.doFinal(encKey, 0);
	}

	System.out.println ("encKey: " + new String(Hex.encode(encKey)));
	
	return encKey;
    }
}
