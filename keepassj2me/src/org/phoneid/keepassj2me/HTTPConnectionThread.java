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

    public HTTPConnectionThread(String url, String userCode, String passCode, String encCode, KeePassMIDlet midlet) {
	mURL = url;
	mUserCode = userCode;
	mPassCode = passCode;
	mEncCode = encCode;
	mMIDlet = midlet;
    }
    
    public void run() {
	try {
	    connect(mURL, mUserCode, mPassCode, mEncCode);
	} catch (Exception e) {
	    System.out.println ("Error from connect()");
	    MessageBox msg = new MessageBox(Definition.TITLE, "Error from connect(): " + e.toString(),
					    AlertType.ERROR, mMIDlet, false,
					    null);
	    msg.waitForDone();
	    mMIDlet.exit();
	}
    }
    
    private void connect(String url, String userCode, String passCode, String encCode)
	throws IOException, RecordStoreException, PhoneIDException
    {
	HttpConnection hc = null;
	InputStream in = null;
	String rawData = "usercode=" + userCode + "&passcode=" + passCode;
	String type = "application/x-www-form-urlencoded";
	
	hc = (HttpConnection)Connector.open(url);
	
	hc.setRequestMethod(HttpConnection.POST);
	hc.setRequestProperty("Content-Type", type);
	hc.setRequestProperty("Content-Length", "13");
	OutputStream os = hc.openOutputStream();
	os.write(rawData.getBytes());
	
	int rc = hc.getResponseCode();
	System.out.println ("rc = " + rc);
	
	if (rc != HttpConnection.HTTP_OK) {
	    throw new IOException("HTTP response code: " + rc);
	}
	
	in = hc.openInputStream();
	
	int contentLength = (int)hc.getLength();
	if (contentLength == 0) {
	    throw (new PhoneIDException ("Download failed"));
	}
	
	byte[] content = new byte[contentLength];
	int length = in.read(content);
	
	in.close();
	hc.close();
	
	// Show the response to the user.
	System.out.println ("Downloaded " + contentLength + " bytes");

	// decrypt KDB with enc code
	byte[] encKey = passwordKeySHA(encCode);

	mMIDlet.storeKDBInRecordStore(content);
    }

    private byte[] passwordKeySHA(String encCode)
    {
	byte[] encBytes = encCode.getBytes();
	byte[] encKey;

	SHA256Digest md = new SHA256Digest();
	encKey = new byte[md.getDigestSize()];
	md.update( encBytes, 0, encBytes.length );
	md.doFinal(encKey, 0);
	
	for (int i=0; i<Definition.PASSWORD_KEY_SHA_ROUNDS - 1; i++) {
	    md.reset();
	    md.update( encKey, 0, encKey.length);
	    md.doFinal(encKey, 0);
	}

	return encKey;
    }
    
}
    
