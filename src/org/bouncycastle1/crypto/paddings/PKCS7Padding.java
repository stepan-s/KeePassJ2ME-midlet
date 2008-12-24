package org.bouncycastle1.crypto.paddings;

import org.bouncycastle1.crypto.SecureRandom;

import org.bouncycastle1.crypto.InvalidCipherTextException;

import org.phoneid.keepassj2me.KeePassMIDlet;

/**
 * A padder that adds PKCS7/PKCS5 padding to a block.
 */
public class PKCS7Padding
    implements BlockCipherPadding
{
    /**
     * Initialise the padder.
     *
     * @param random - a SecureRandom if available.
     */
    public void init(org.bouncycastle1.crypto.SecureRandom random)
        throws IllegalArgumentException
    {
        // nothing to do.
    }

    /**
     * Return the name of the algorithm the padder implements.
     *
     * @return the name of the algorithm the padder implements.
     */
    public String getPaddingName()
    {
        return "PKCS7";
    }

    /**
     * add the pad bytes to the passed in block, returning the
     * number of bytes added.
     */
    public int addPadding(
        byte[]  in,
        int     inOff)
    {
        byte code = (byte)(in.length - inOff);

        while (inOff < in.length)
        {
            in[inOff] = code;
            inOff++;
        }

        return code;
    }

    /**
     * return the number of pad bytes present in the block.
     */
    public int padCount(byte[] in)
        throws InvalidCipherTextException
    {
        int count = in[in.length - 1] & 0xff;

	KeePassMIDlet.logS("PKCS7Padding: in.length = " + in.length);
	KeePassMIDlet.logS("PKCS7Padding: last byte = " + in[in.length - 1]);
	KeePassMIDlet.logS("PKCS7Padding: padCount = " + count);

        if (count > in.length)
        {
            throw new InvalidCipherTextException("pad block corrupted");
        }
        
        for (int i = 1; i <= count; i++)
        {
            if (in[in.length - i] != count)
            {
                throw new InvalidCipherTextException("pad block corrupted");
            }
        }

        return count;
    }
}
