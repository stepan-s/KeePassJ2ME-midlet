package net.sourceforge.keepassj2me.importerv3;

/**
 * @author unknown
 */
public class Util {

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
	return;
    }
}

