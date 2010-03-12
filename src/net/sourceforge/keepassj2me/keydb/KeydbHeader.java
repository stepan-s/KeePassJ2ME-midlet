package net.sourceforge.keepassj2me.keydb;

import net.sourceforge.keepassj2me.importerv3.Types;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.prng.RandomGenerator;

/**
 * KDB Header
 * @author Stepan Strelets
 */
public class KeydbHeader {
	/*
	 * HEADER CONSTANT
	 */
	/** Size of byte buffer needed to hold this struct. */
	public static final int SIZE = 124;
	
	// DB sig from KeePass 1.03
	public static final int SIGNATURE_1 = 0x9AA2D903;
	public static final int SIGNATURE_2 = 0xB54BFB65;
	// DB ver from KeePass 1.03
	public static final int VERSION = 0x00030002;
	
	public static final int FLAG_SHA2 = 1;
	public static final int FLAG_RIJNDAEL = 2;
	public static final int FLAG_ARCFOUR = 4;
	public static final int FLAG_TWOFISH = 8;
	public static final int FLAG_MASK = 15;

	public static final int ALGO_AES = 0;
	public static final int ALGO_TWOFISH = 1;

	/*
	 * HEADER STRUCTURE
	 */
	protected int signature1; // = SIGNATURE_1
	protected int signature2; // = SIGNATURE_2
	protected int flags;
	protected int version;
	/** Seed that gets hashed with the userkey to form the final key */
	protected byte masterSeed[] = new byte[16];
	/** IV used for content encryption */
	protected byte encryptionIV[] = new byte[16];
	/** Number of groups in the database */
	protected int numGroups = 0;
	/** Number of entries in the database */
	protected int numEntries = 0;
	/** SHA-256 hash of the database, used for integrity check */
	protected byte contentsHash[] = new byte[32];
	/** Used for the dwKeyEncRounds AES transformations */
	protected byte masterSeed2[] = new byte[32];
	protected int numKeyEncRounds;

	public KeydbHeader(int rounds) {
		signature1 = SIGNATURE_1;
		signature2 = SIGNATURE_2;
		flags = FLAG_SHA2 | FLAG_RIJNDAEL;
		version = VERSION;
		reinitialize(rounds);
	}
	
	/**
	 * Parse given buf, as read from file.
	 * 
	 * @param buf
	 * @param offset
	 * @throws KeydbException 
	 */
	public KeydbHeader(byte buf[], int offset) throws KeydbException {
		this.read(buf, offset);
	}
	
	public void reinitialize(int rounds) {
		numKeyEncRounds = rounds;
		//SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
		//rnd.setSeed(System.currentTimeMillis());
		RandomGenerator rnd = new DigestRandomGenerator(new SHA1Digest());
		rnd.addSeedMaterial(System.currentTimeMillis());
		rnd.nextBytes(masterSeed);
		rnd.nextBytes(encryptionIV);
		rnd.nextBytes(masterSeed2);
	}
	
	public void read(byte buf[], int offset) throws KeydbException {
		if (buf.length < KeydbHeader.SIZE) {
			throw new KeydbException("Incorrect database structure");
		};
		this.signature1 = Types.readInt(buf, offset + 0);
		this.signature2 = Types.readInt(buf, offset + 4);
		if ((this.signature1 != KeydbHeader.SIGNATURE_1) || (this.signature2 != KeydbHeader.SIGNATURE_2)) {
			throw new KeydbException("Incorrect database structure");
		};
		flags = Types.readInt(buf, offset + 8);
		version = Types.readInt(buf, offset + 12);
		if (this.version != KeydbHeader.VERSION) {
			throw new KeydbException("Unsupported database version");
		};
		System.arraycopy(buf, offset + 16, masterSeed, 0, 16);
		System.arraycopy(buf, offset + 32, encryptionIV, 0, 16);
		numGroups = Types.readInt(buf, offset + 48);
		numEntries = Types.readInt(buf, offset + 52);
		System.arraycopy(buf, offset + 56, contentsHash, 0, 32);
		System.arraycopy(buf, offset + 88, masterSeed2, 0, 32);
		numKeyEncRounds = Types.readInt(buf, offset + 120);
	}
	
	public void write(byte[] buf, int offset) {
		Types.writeInt(buf, offset + 0, signature1);
		Types.writeInt(buf, offset + 4, signature2);
		Types.writeInt(buf, offset + 8, flags);
		Types.writeInt(buf, offset + 12, version);
		System.arraycopy(masterSeed, 0, buf, offset + 16, 16);
		System.arraycopy(encryptionIV, 0, buf, offset + 32, 16);
		Types.writeInt(buf, offset + 48, numGroups);
		Types.writeInt(buf, offset + 52, numEntries);
		System.arraycopy(contentsHash, 0, buf, offset + 56, 32);
		System.arraycopy(masterSeed2, 0, buf, offset + 88, 32);
		Types.writeInt(buf, offset + 120, numKeyEncRounds);
	}
	
	public int getEncryptionRounds() {
		return numKeyEncRounds;
	}
	public int getGroupsCount() {
		return numGroups;
	}
	public int getEntriesCount() {
		return numEntries;
	}
}
