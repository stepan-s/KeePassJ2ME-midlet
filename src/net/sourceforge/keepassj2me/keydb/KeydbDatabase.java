/*
	Copyright 2008-2011 Stepan Strelets
	http://keepassj2me.sourceforge.net/

	This file is part of KeePass for J2ME.
	
	KeePass for J2ME is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, version 2.
	
	KeePass for J2ME is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.
	
	You should have received a copy of the GNU General Public License
	along with KeePass for J2ME.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sourceforge.keepassj2me.keydb;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.prng.DigestRandomGenerator;
import org.bouncycastle.crypto.prng.RandomGenerator;

import net.sourceforge.keepassj2me.Config;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.L10nKeys.keys;
import net.sourceforge.keepassj2me.tools.DisplayStack;
import net.sourceforge.keepassj2me.tools.IProgressListener;
import net.sourceforge.keepassj2me.tools.IWatchDogTimerTarget;
import net.sourceforge.keepassj2me.tools.WatchDogTimer;

/**
 * KDB database
 * @author Stepan Strelets
 */
public class KeydbDatabase implements IWatchDogTimerTarget {
	/** Search in title */
	public static final byte SEARCHBYTITLE = 1;
	/** Search in URL */
	public static final byte SEARCHBYURL = 2;
	/** Search in user name */
	public static final byte SEARCHBYUSERNAME = 4;
	/** Search in note */
	public static final byte SEARCHBYNOTE = 8;
	/** Mask for search flags */
	public static final byte SEARCHBY_MASK = 0xF;
	
	private IProgressListener listener = null;
	private long TIMER_DELAY = 600000; //10 min
	private WatchDogTimer watchDog = null;
	
	/* KDB */
	
	private KeydbHeader header = null;
	private byte[] encodedContent = null;
	private byte[] key = null;
	protected byte[] plainContent = null;
	/** actual data length in plainContent */
	private int contentSize = 0;
	private boolean changed = false;
	
	/* Indexes */
	
	/** each array element contain group id */
	private int[] groupsIds = null; 
	/** each array element contain group */
	private int[] groupsOffsets = null; 
	/** each array element contain group gid */
	private int[] groupsGids = null;
	
	/** entries offset in plainContent */
	private int entriesStartOffset = 0;
	
	/** each array element contain entry offset */
	private int[] entriesOffsets = null;
	/** each array element contain entry gid */
	private int[] entriesGids = null;
	/** each array element contain entry meta mark */
	private byte[] entriesMeta = null;
	/** each array element contain entry search mark */
	private byte[] entriesSearch = null;

	// #ifdef DEBUG
	java.util.Vector groupsHashes;
	java.util.Vector entriesHashes;
	// #endif
	
	/**
	 * Create database object
	 */
	public KeydbDatabase() {
		this.TIMER_DELAY = 60000 * Config.getInstance().getWatchDogTimeOut();
		this.watchDog = new WatchDogTimer(this);
	}

	/**
	 * Set progress listener
	 * @param listener
	 */
	public void setProgressListener(IProgressListener listener) {
		this.listener = listener;
	}

	/**
	 * Proxy method for setting progress state
	 * @param procent
	 * @param message
	 * @throws KeydbException
	 */
	private void setProgress(int procent, String message) throws KeydbException {
		if (this.listener != null) {
			try {
				this.listener.setProgress(procent, message);
			} catch (KeePassException e) {
				throw new KeydbException(e.getMessage());
			}
		}
	}

	/**
	 * Create empty database
	 * @param pass
	 * @param keyfile 
	 * @param rounds 
	 * @throws KeydbException 
	 */
	public void create(String pass, byte[] keyfile, int rounds) throws KeydbException {
		this.close();
		
		this.header = new KeydbHeader(rounds);
		
		this.setProgress(5, Config.getLocaleString(keys.KD_GENERATE_KEY));
		this.key = this.makeMasterKey(pass, keyfile, 5, 95);
		
		this.setProgress(95, Config.getLocaleString(keys.KD_PREPARE_STRUCTURE));
		this.plainContent = new byte[4096];
		this.contentSize = 0;
		this.makeGroupsIndexes();
		this.makeEntriesIndexes();
		
		setProgress(100, Config.getLocaleString(keys.KD_DONE));
		
		watchDog.setTimer(TIMER_DELAY);
	}

	/**
	 * Change password, key file, encryption rounds
	 * @param pass
	 * @param keyfile
	 * @param rounds
	 * @throws KeydbException
	 */
	public void changeMasterKey(String pass, byte[] keyfile, int rounds) throws KeydbException {
		watchDog.cancelTimer();
		passLock();
		
		this.header.reinitialize(rounds);
		
		this.setProgress(5, Config.getLocaleString(keys.KD_GENERATE_KEY));
		this.key = this.makeMasterKey(pass, keyfile, 5, 100);
		setProgress(100, Config.getLocaleString(keys.KD_DONE));
		this.changed = true;
		
		watchDog.setTimer(TIMER_DELAY);
	}
	
	/**
	 * Decode database
	 * @param encoded
	 * @param pass
	 * @param keyfile
	 * @throws KeydbException
	 */
	public void open(byte[] encoded, String pass, byte[] keyfile) throws KeydbException {
		this.close();
		
		this.setProgress(5, Config.getLocaleString(keys.KD_OPEN_DB));
		
		this.header = new KeydbHeader(encoded, 0);

		if ((this.header.flags & KeydbHeader.FLAG_RIJNDAEL) != 0) {
			
		} else if ((this.header.flags & KeydbHeader.FLAG_TWOFISH) != 0) {
			throw new KeydbException(Config.getLocaleString(keys.KD_TWOFISH_NOT_SUPPORTED));
			
		} else {
			throw new KeydbException(Config.getLocaleString(keys.KD_UNKNOWN_ALGO));
		}

		setProgress(10, Config.getLocaleString(keys.KD_DECRYPT_KEY));
		this.key = this.makeMasterKey(pass, keyfile, 10, 90);

		setProgress(90, Config.getLocaleString(keys.KD_DECRYPT_DB));
		this.decrypt(encoded, KeydbHeader.SIZE, encoded.length - KeydbHeader.SIZE);

		setProgress(95, Config.getLocaleString(keys.KD_MAKE_INDEXES));
		this.makeGroupsIndexes();
		this.makeEntriesIndexes();
		
		setProgress(100, Config.getLocaleString(keys.KD_DONE));
		watchDog.setTimer(TIMER_DELAY);
	}
	
	/**
	 * Encode database
	 * @return encoded database
	 * @throws KeydbException
	 */
	public byte[] getEncoded() throws KeydbException {//Encrypt content
		if (isLocked()) return this.encodedContent;
		
		if ((this.header.numGroups == 0) && (this.header.numEntries == 0))
			throw new KeydbException(Config.getLocaleString(keys.KD_NOTHING_SAVE));
		
		BufferedBlockCipher cipher = new BufferedBlockCipher(
				new CBCBlockCipher(new AESEngine()));
		
		//calc padding size
		int block_size = cipher.getBlockSize();
		int pad_size = block_size - this.contentSize % block_size;
		
		// #ifdef DEBUG
		System.out.println("contentSize: " + this.contentSize);
		System.out.println("block_size: " + block_size);
		System.out.println("pad_size: " + pad_size);
		// #endif
		
		//add padding to content
		byte temp[] = new byte[this.contentSize + pad_size];
		System.arraycopy(this.plainContent, 0, temp, 0, this.contentSize);
		KeydbUtil.fill(this.plainContent, (byte)0);
		this.plainContent = temp;
		temp = null;
		PKCS7Padding padding = new PKCS7Padding();
		padding.addPadding(this.plainContent, this.contentSize);
		
		byte encoded[] = new byte[KeydbHeader.SIZE + this.contentSize + pad_size];
		
		//encode
		cipher.init(true, new ParametersWithIV(new KeyParameter(this.key),
				this.header.encryptionIV));
		
		int paddedEncryptedPartSize = cipher.processBytes(
				this.plainContent, 0, this.plainContent.length,
				encoded, KeydbHeader.SIZE);
		
		if (paddedEncryptedPartSize != this.plainContent.length) {
			// #ifdef DEBUG
			System.out.println("Encoding: " + paddedEncryptedPartSize + " != " + this.plainContent.length);
			// #endif
			throw new KeydbException(Config.getLocaleString(keys.KD_ENCRYPTING_FAILED));
		}
		
		//Set header
		this.header.contentsHash = KeydbUtil.hash(this.plainContent, 0, this.contentSize);
		this.header.write(encoded, 0);
		
		return encoded;
	}
	
	private byte[] makeMasterKey(String pass, byte[] keyfile, int start_procent, int end_procent) throws KeydbException {
		byte[] passHash;
		switch (((pass != null) && (pass.length() != 0) ? 1 : 0)
				| ((keyfile != null) && (keyfile.length != 0) ? 2 : 0)) {
		case 0:
			throw new KeydbException(Config.getLocaleString(keys.KD_PASS_KEY_EMPTY));
		case 1:
			passHash = KeydbUtil.hash(pass);
			break;
		case 2:
			passHash = KeydbUtil.hashKeyfile(keyfile);
			break;
		case 3:
			passHash = KeydbUtil.hash(new byte[][] {KeydbUtil.hash(pass.getBytes()), KeydbUtil.hashKeyfile(keyfile)});
			break;
		default:
			throw new KeydbException("Execution error");
		};
		
		byte[] transformedMasterKey = this.transformMasterKey(
				this.header.masterSeed2,
				passHash,
				this.header.numKeyEncRounds, start_procent, end_procent);
		KeydbUtil.fill(passHash, (byte)0);
		
		// Hash the master password with the salt in the file
		byte[] masterKey = KeydbUtil.hash(new byte[][] {
				this.header.masterSeed,
				transformedMasterKey});
		KeydbUtil.fill(transformedMasterKey, (byte)0);
		
		return masterKey;
	}
	
	/**
	 * Decrypt master key
	 * @param pKeySeed
	 * @param pKey
	 * @param rounds
	 * @return
	 * @throws KeydbException
	 */
	private byte[] transformMasterKey(byte[] pKeySeed, byte[] pKey, int rounds, int start_procent, int end_procent) throws KeydbException {
		byte[] newKey = new byte[pKey.length];
		System.arraycopy(pKey, 0, newKey, 0, pKey.length);

		BufferedBlockCipher cipher = new BufferedBlockCipher(new AESEngine());
		cipher.init(true, new KeyParameter(pKeySeed));

		int procent = start_procent; // start_procent% - progress start
		int step = 5;// % step
		int roundsByStep = rounds * step / ((end_procent - procent)); // end_procent% - progress end
		int count = 0;

		for (int i = 0; i < rounds; i++) {
			cipher.processBytes(newKey, 0, newKey.length, newKey, 0);

			if (++count == roundsByStep) {
				count = 0;
				setProgress(procent += step, null);
			};
		};
		
		byte[] transformedMasterKey = KeydbUtil.hash(newKey);
		KeydbUtil.fill(newKey, (byte)0);
		
		return transformedMasterKey;
	}
	
	private void decrypt(byte[] encoded, int offset, int length) throws KeydbException {
		BufferedBlockCipher cipher = new BufferedBlockCipher(
				new CBCBlockCipher(new AESEngine()));

		cipher.init(false, new ParametersWithIV(new KeyParameter(this.key),
				this.header.encryptionIV));
		
		// Decrypt! The first bytes aren't encrypted (that's the header)
		this.plainContent = new byte[encoded.length - KeydbHeader.SIZE];
		int paddedEncryptedPartSize = cipher.processBytes(encoded,
				offset, length,
				this.plainContent, 0);

		//detect padding and calc content size 
		this.contentSize = 0;
		PKCS7Padding padding = new PKCS7Padding();
		try {
			this.contentSize = paddedEncryptedPartSize - padding.padCount(this.plainContent);
		} catch (InvalidCipherTextException e) {
			throw new KeydbException(Config.getLocaleString(keys.KD_DB_DECRYPT_ERR, new String[] {"1"}));
		}
		
		if (!KeydbUtil.compare(
				KeydbUtil.hash(this.plainContent, 0, this.contentSize),
				this.header.contentsHash)) {
			throw new KeydbException(Config.getLocaleString(keys.KD_DB_DECRYPT_ERR, new String[] {"2"}));
		}
	}
	
	/**
	 * Close database
	 */
	public void close() {
		watchDog.cancelTimer();
		header = null;
		encodedContent = null;
		if (plainContent != null) {
			KeydbUtil.fill(plainContent, (byte)0);
			plainContent = null;
		};
		if (key != null) {
			KeydbUtil.fill(key, (byte)0);
			key = null;
		};
		if (groupsIds != null) groupsIds = null;
		if (groupsOffsets != null) groupsOffsets = null;
		if (groupsGids != null) groupsGids = null;
		
		if (entriesOffsets != null) entriesOffsets = null;
		if (entriesGids != null) entriesGids = null;
		if (entriesMeta != null) entriesMeta = null;
		if (entriesSearch != null) entriesSearch = null;
	}

	/**
	 * Prepare structures for speedup group operations
	 */
	private void makeGroupsIndexes() {
		int offset = 0;
		int length;
		int[] ids = new int[20];
		
		this.groupsIds = new int[this.header.numGroups];
		this.groupsOffsets = new int[this.header.numGroups];
		this.groupsGids = new int[this.header.numGroups];
		
		// #ifdef DEBUG
		java.util.Vector groupsHashes = new java.util.Vector(); 
		// #endif
		
		KeydbGroup group = new KeydbGroup(this);
		for(int i = 0; i < header.numGroups; ++i) {
			this.groupsOffsets[i] = offset;
			length = group.read(offset, i);
			// #ifdef DEBUG
			String h = KeydbUtil.hashToString(KeydbUtil.hash(this.plainContent, offset, length));
			groupsHashes.addElement(h);
			if (this.groupsHashes != null) {
				if (!this.groupsHashes.contains(h)) {
					System.out.println("new group-"+i+": "+h);
				}
			}
			// #endif
			offset += length;
			this.groupsIds[i] = group.id;
			
			//get parent
			this.groupsGids[i] = (group.level > 0) ? ids[group.level - 1] : 0;
			
			//check depth availability
			if (group.level >= ids.length) {
				int[] new_ids = new int[ids.length + 20];
				System.arraycopy(ids, 0, new_ids, 0, ids.length);
				ids = new_ids;
			};
			//set self
			ids[group.level] = group.id;
		}
		this.entriesStartOffset = offset;

		// #ifdef DEBUG
		if (this.groupsHashes != null) {
			for(int i = 0; i < this.groupsHashes.size(); ++i) {
				if (!groupsHashes.contains(this.groupsHashes.elementAt(i))) {
					System.out.println("del group-"+i+": "+this.groupsHashes.elementAt(i));
				}
			};
		};
		this.groupsHashes = groupsHashes; 
		// #endif
	}
	
	/**
	 * Prepare structures for speedup enties operations
	 */
	private void makeEntriesIndexes() {
		int offset = this.entriesStartOffset;
		int length;
		
		this.entriesOffsets = new int[this.header.numEntries];
		this.entriesGids = new int[this.header.numEntries];
		this.entriesMeta = new byte[this.header.numEntries];
		this.entriesSearch = new byte[this.header.numEntries];
		
		// #ifdef DEBUG
		java.util.Vector entriesHashes = new java.util.Vector(); 
		// #endif
		
		KeydbEntry entry = new KeydbEntry(this);
		for(int i = 0; i < header.numEntries; ++i) {
			entry.clean();
			this.entriesOffsets[i] = offset;
			length = entry.read(offset, i);
			// #ifdef DEBUG
			String h = KeydbUtil.hashToString(KeydbUtil.hash(this.plainContent, offset, length));
			entriesHashes.addElement(h);
			if (this.entriesHashes != null) {
				if (!this.entriesHashes.contains(h)) {
					System.out.println("new entry-"+i+": "+h);
				}
			}
			// #endif
			offset += length;
			this.entriesGids[i] = entry.groupId;
			if (entry.title.equals("Meta-Info")
					&& entry.getUsername().equals("SYSTEM")
					&& entry.getUrl().equals("$")) {
				this.entriesMeta[i] = 1;
			} else {
				this.entriesMeta[i] = 0;
			}
		}
		
		// #ifdef DEBUG
		if (this.entriesHashes != null) {
			for(int i = 0; i < this.entriesHashes.size(); ++i) {
				if (!entriesHashes.contains(this.entriesHashes.elementAt(i))) {
					System.out.println("del entry-"+i+": "+this.entriesHashes.elementAt(i));
				}
			};
		};
		this.entriesHashes = entriesHashes; 
		// #endif
	};
	
	/**
	 * Get database header
	 * @return header object
	 */
	public KeydbHeader getHeader() {
		return header;
	}
	
	/**
	 * Get group by id
	 * @param id
	 * @return group
	 * @throws KeydbException 
	 */
	public KeydbGroup getGroup(int id) throws KeydbException {
		passLock();
		
		if (id != 0) {
			for(int i = 0; i < header.numGroups; ++i) {
				if (this.groupsIds[i] == id) {
					KeydbGroup group = new KeydbGroup(this);
					group.read(this.groupsOffsets[i], i);
					return group;
				};
			};
			throw new KeydbException(Config.getLocaleString(keys.KD_GROUP_NOT_FOUND));
		} else {
			throw new KeydbException(Config.getLocaleString(keys.KD_CANT_GET_ROOT_GROUP));
		}
	}
	
	/**
	 * Get parent group of child group identified by id
	 * @param id
	 * @return group
	 * @throws KeydbException
	 */
	public KeydbGroup getGroupParent(int id) throws KeydbException {
		passLock();
		
		if (id != 0) {
			for(int i = 0; i < header.numGroups; ++i) {
				if (this.groupsIds[i] == id) {
					return this.getGroup(this.groupsGids[i]);
				};
			};
			throw new KeydbException(Config.getLocaleString(keys.KD_GROUP_NOT_FOUND));
		} else {
			throw new KeydbException(Config.getLocaleString(keys.KD_ROOT_GROUP_PARENT));
		}
	}
	
	/**
	 * Enumerate group content (subgroups and entries)
	 * @param id
	 * @param receiver
	 * @param start
	 * @param limit
	 * @return items count
	 * @throws KeydbLockedException 
	 */
	public int enumGroupContent(int id, IKeydbGroupContentRecever receiver, int start, int limit) throws KeydbLockedException {
		passLock();
		
		int total = 0;
		KeydbGroup group;
		for(int i = 0; i < header.numGroups; ++i) {
			if (this.groupsGids[i] == id) {
				if (start > 0) {
					--start;
				} else if (limit > 0) {
					--limit;
					group = new KeydbGroup(this);
					group.read(this.groupsOffsets[i], i);
					receiver.addKeydbGroup(group);
				};
				++total;
			}
		}
		receiver.totalGroups(total);
		KeydbEntry entry;
		for(int i = 0; i < header.numEntries; ++i) {
			if ((this.entriesGids[i] == id) && (this.entriesMeta[i] == 0)) {
				if (start > 0) {
					--start;
				} else if (limit > 0) {
					--limit;
					entry = new KeydbEntry(this);
					entry.read(this.entriesOffsets[i], i);
					receiver.addKeydbEntry(entry);
				};
				++total;
			}
		}
		return total;
	}
	
	/**
	 * Get page number on which the group is
	 * @param parent
	 * @param id
	 * @param size
	 * @return page index
	 * @throws KeydbLockedException 
	 */
	public int getGroupPage(int parent, int id, int size) throws KeydbLockedException {
		passLock();
		
		int page = 0;
		int index = 0;
		for(int i = 0; i < header.numGroups; ++i) {
			if (this.groupsGids[i] == parent) {
				if (this.groupsIds[i] == id) break;
			
				if (++index >= size) {
					index = 0;
					++page;
				}
			}
		}
		return page;
	}

	/**
	 * Search for entries with the title beginning with substring
	 * @param begin
	 * @return items count
	 * @throws KeydbLockedException 
	 */
	public int searchEntriesByTitle(String begin) throws KeydbLockedException {
		passLock();
		
		int found = 0;
		KeydbEntry entry = new KeydbEntry(this);
		begin = begin.toLowerCase();
		for(int i = 0; i < header.numEntries; ++i) {
			if (this.entriesMeta[i] == 0) {
				entry.clean();
				entry.read(this.entriesOffsets[i], i);
				if (entry.title.toLowerCase().startsWith(begin)) {
					this.entriesSearch[i] = 1;
					++found;
				} else {
					this.entriesSearch[i] = 0;
				}
			} else {
				this.entriesSearch[i] = 0;
			}
		}
		return found;
	}
	
	/**
	 * Find entries with parameter containing substring
	 * @param value
	 * @param search_by
	 * @return items count
	 * @throws KeydbLockedException 
	 */
	public int searchEntriesByTextFields(String value, byte search_by) throws KeydbLockedException {
		passLock();
		
		int found = 0;
		KeydbEntry entry = new KeydbEntry(this);
		value = value.toLowerCase();
		for(int i = 0; i < header.numEntries; ++i) {
			if (this.entriesMeta[i] == 0) {
				entry.clean();
				entry.read(this.entriesOffsets[i], i);
				if (
						(((search_by & SEARCHBYTITLE) != 0) && (entry.title.toLowerCase().indexOf(value, 0) >= 0))
						|| (((search_by & SEARCHBYURL) != 0) && (entry.getUrl().toLowerCase().indexOf(value, 0) >= 0))
						|| (((search_by & SEARCHBYUSERNAME) != 0) && (entry.getUsername().toLowerCase().indexOf(value, 0) >= 0))
						|| (((search_by & SEARCHBYNOTE) != 0) && (entry.getNote().toLowerCase().indexOf(value, 0) >= 0))
						) {
					this.entriesSearch[i] = 1;
					++found;
				} else {
					this.entriesSearch[i] = 0;
				}
			} else {
				this.entriesSearch[i] = 0;
			}
		}
		return found;
	}
	
	/**
	 * Enumerate entries in search result
	 * @param receiver
	 * @param start
	 * @param limit
	 * @throws KeydbLockedException 
	 */
	public void enumFoundEntries(IKeydbGroupContentRecever receiver, int start, int limit) throws KeydbLockedException {
		passLock();
		
		KeydbEntry entry;
		for(int i = 0; i < header.numEntries; ++i) {
			if (this.entriesSearch[i] == 1) {
				if (start > 0) {
					--start;
				} else if (limit > 0) {
					--limit;
					entry = new KeydbEntry(this);
					entry.read(this.entriesOffsets[i], i);
					receiver.addKeydbEntry(entry);
				} else {
					break;
				}
			}
		}
	}
	
	/**
	 * Get entry by index in search result
	 * @param index
	 * @return entry
	 * @throws KeydbLockedException 
	 */
	public KeydbEntry getFoundEntry(int index) throws KeydbLockedException {
		passLock();
		
		for(int i = 0; i < header.numEntries; ++i) {
			if (this.entriesSearch[i] == 1) {
				if (index > 0) --index;
				else {
					KeydbEntry entry = new KeydbEntry(this);
					entry.read(this.entriesOffsets[i], i);
					return entry;
				}
			};
		}
		return null;
	}

	/**
	 * Get group by index in group
	 * @param parent
	 * @param index
	 * @return group
	 * @throws KeydbLockedException 
	 */
	public KeydbGroup getGroupByIndex(int parent, int index) throws KeydbLockedException {
		passLock();
		
		for(int i = 0; i < header.numGroups; ++i) {
			if (this.groupsGids[i] == parent) {
				if (index > 0) --index;
				else {
					KeydbGroup group = new KeydbGroup(this);
					group.read(this.groupsOffsets[i], i);
					return group;
				}
			};
		}
		return null;
	}

	/**
	 * Get entry by index in group
	 * @param groupId
	 * @param index
	 * @return entry
	 * @throws KeydbLockedException 
	 */
	public KeydbEntry getEntryByIndex(int groupId, int index) throws KeydbLockedException {
		passLock();
		
		for(int i = 0; i < header.numEntries; ++i) {
			if ((this.entriesGids[i] == groupId) && (this.entriesMeta[i] == 0)) {
				if (index > 0) --index;
				else {
					KeydbEntry entry = new KeydbEntry(this);
					entry.read(this.entriesOffsets[i], i);
					return entry;
				}
			};
		}
		return null;
	}
	
	/**
	 * Get group data length in database
	 * @param index group index in database
	 * @return length
	 */
	private int getGroupDataLength(int index) {
		return (((index + 1) < this.groupsOffsets.length) ? this.groupsOffsets[index + 1] : this.entriesStartOffset)
				- this.groupsOffsets[index];
	}
	
	/**
	 * Get entry data length in database
	 * @param index entry index in database
	 * @return length
	 */
	private int getEntryDataLength(int index) {
		return (((index + 1) < this.entriesOffsets.length) ? this.entriesOffsets[index + 1] : this.contentSize)
				- this.entriesOffsets[index];
	}
	
	/**
	 * Delete marked groups and entries at once
	 * @param groups marked groups
	 * @param entries marked entries
	 */
	private void purge(byte[] groups, byte[] entries) {
		int pos = 0;
		int offset, length;
		
		//copy all alive groups to begin 
		int numGroups = 0;
		for(int i = 0; i < groups.length; ++i) {
			if (groups[i] == 0) {
				offset = this.groupsOffsets[i];
				length = this.getGroupDataLength(i);
				if (offset > pos) {
					System.arraycopy(	this.plainContent, offset,
										this.plainContent, pos,
										length);
				};
				pos += length;
				++numGroups;
			};
		};
		this.header.numGroups = numGroups;
		
		//copy all alive entries to begin
		int numEntries = 0;
		for(int i = 0; i < entries.length; ++i) {
			if (entries[i] == 0) {
				offset = this.entriesOffsets[i];
				length = this.getEntryDataLength(i);
				if (offset > pos) {
					System.arraycopy(	this.plainContent, offset,
										this.plainContent, pos,
										length);
				};
				pos += length;
				++numEntries;
			};
		};
		this.header.numEntries = numEntries;
		
		this.contentSize = pos;
		this.changed = true;
		
		this.makeGroupsIndexes();
		this.makeEntriesIndexes();
	}
	
	/**
	 * Mark groups and entries as deleted recursively
	 * @param index group index
	 * @param groups groups marks
	 * @param entries entries marks
	 */
	private void markGroupDeleted(int index, byte[] groups, byte[] entries) {
		groups[index] = 1;
		int id = this.groupsIds[index];
		for(int i = 0; i < header.numGroups; ++i) {
			if (id == this.groupsGids[i]) {
				this.markGroupDeleted(i, groups, entries);
			};
		};
		for(int i = 0; i < header.numEntries; ++i) {
			if (id == this.entriesGids[i]) entries[i] = 1;
		};
	}
	
	/**
	 * Delete group from database recursively 
	 * @param index group index
	 * @throws KeydbLockedException 
	 */
	public void deleteGroup(int index) throws KeydbLockedException {
		passLock();
		
		byte[] groups = new byte[this.header.numGroups];
		byte[] entries = new byte [this.header.numEntries];
		KeydbUtil.fill(groups, (byte)0);
		KeydbUtil.fill(entries, (byte)0);
		this.markGroupDeleted(index, groups, entries);
		this.purge(groups, entries);
	}
	
	/**
	 * Delete entry from database
	 * @param index entry index
	 * @throws KeydbLockedException 
	 */
	public void deleteEntry(int index) throws KeydbLockedException {
		passLock();
		
		int offset = this.entriesOffsets[index];
		int length = this.getEntryDataLength(index);
		int size = this.contentSize - (offset + length);
		if (size > 0)
			System.arraycopy(	this.plainContent, offset + length,
								this.plainContent, offset,
								size); 
		
		this.contentSize -= length;
		this.header.numEntries -= 1;
		this.changed = true;
		
		this.makeGroupsIndexes();
		this.makeEntriesIndexes();
	}
	
	/**
	 * Add group to database
	 * @param groupContent packed group
	 * @param gid group id
	 * @return group index
	 * @throws KeydbException 
	 */
	public int addGroup(byte[] groupContent, int gid) throws KeydbException {
		passLock();
		
		int pos = -1;
		if (gid != 0) {
			KeydbGroup group = this.getGroup(gid);
			int level = group.level;
			//find end of childs
			int i;
			for(i = group.index + 1; i < header.numGroups; ++i) {
				group.read(this.groupsOffsets[i], i);
				if (group.level <= level) break; 
			};
			if (i < header.numGroups) pos = i;
		};
		
		this.replaceBlock(pos == -1 ? this.entriesStartOffset : this.groupsOffsets[pos], 0, groupContent);
		this.header.numGroups += 1;
		
		this.makeGroupsIndexes();
		this.makeEntriesIndexes();
		
		return (pos == -1 ? this.header.numGroups - 1 : pos);
	}
	
	/**
	 * Add entry to database
	 * @param entryContent packed entry
	 * @return entry index
	 * @throws KeydbLockedException 
	 */
	public int addEntry(byte[] entryContent) throws KeydbLockedException {
		passLock();
		
		this.replaceBlock(this.contentSize, 0, entryContent);
		this.header.numEntries += 1;
		
		this.makeGroupsIndexes();
		this.makeEntriesIndexes();
		
		return this.header.numEntries - 1;
	}

	/**
	 * Replace data in database with block data, space managed automatically
	 * @param offset offset data for replace
	 * @param size size data for replace, if size is zero then data inserted
	 * @param block new data
	 */
	private void replaceBlock(int offset, int size, byte[] block) {
		if ((this.plainContent.length - this.contentSize) >= (block.length - size)) {
			//enough space
			//move tail
			System.arraycopy(	this.plainContent, offset + size,
								this.plainContent, offset + block.length,
								this.contentSize - (offset + size));
		} else {
			//need allocate enough space
			byte tmp[] = new byte[this.contentSize + (block.length - size)];
			//move head
			System.arraycopy(	this.plainContent, 0,
								tmp, 0,
								offset);
			//move tail
			System.arraycopy(	this.plainContent, offset + size,
								tmp, offset + block.length,
								this.contentSize - (offset + size));
			KeydbUtil.fill(this.plainContent, (byte)0);
			this.plainContent = tmp;
		};
		//place body
		System.arraycopy(	block, 0,
							this.plainContent, offset,
							block.length);
		
		this.contentSize += block.length - size;
		this.changed = true;
	}
	
	/**
	 * Replace group data with updated data
	 * @param index group index
	 * @param groupContent updated data
	 * @throws KeydbLockedException 
	 */
	public void updateGroup(int index, byte[] groupContent) throws KeydbLockedException {
		passLock();
		
		this.replaceBlock(this.groupsOffsets[index], this.getGroupDataLength(index), groupContent);
		
		this.makeGroupsIndexes();
		this.makeEntriesIndexes();
	}
	
	/**
	 * Replace entry data with updated data
	 * @param index entry index
	 * @param entryContent updated data
	 * @throws KeydbLockedException 
	 */
	public void updateEntry(int index, byte[] entryContent) throws KeydbLockedException {
		passLock();
		
		this.replaceBlock(this.entriesOffsets[index], this.getEntryDataLength(index), entryContent);
		
		this.makeGroupsIndexes();
		this.makeEntriesIndexes();
	}
	
	/**
	 * Create a unique group id
	 * @return group id
	 * @throws KeydbLockedException 
	 */
	public int getUniqueGroupId() throws KeydbLockedException {
		passLock();

		int id, i;
		while (true) {//FIXME: need more efficient method
			id = this.getRandom();
			for(i = 0; i < header.numGroups; ++i) {
				if (this.groupsIds[i] == id) break;
			};
			if (i >= header.numGroups) {
				//not match
				break;
			};
		};
		return id;
	}
	protected int getRandom() {
		byte[] id = new byte[4];
		RandomGenerator rnd = new DigestRandomGenerator(new SHA1Digest());
		rnd.addSeedMaterial(System.currentTimeMillis());
		rnd.nextBytes(id);
		return (id[0] | (id[1] << 8) | (id[2] << 16) | (id[3] << 24)); 
	}
	
	/**
	 * Check database changes
	 * @return true if changed
	 */
	public boolean isChanged() {
		return this.changed;
	}
	/**
	 * Reset change indicator
	 */
	public void resetChangeIndicator() {
		this.changed = false;
	}

	// WATCH DOG
	
	public void invokeByWatchDog() {
		// #ifdef DEBUG
		System.out.println("invokeByWatchDog");
		// #endif
		this.lock();
	}
	/**
	 * Reset watch dog timeout
	 * @throws KeydbLockedException
	 */
	public void reassureWatchDog() throws KeydbLockedException {
		passLock();
		watchDog.setTimer(TIMER_DELAY);
		// #ifdef DEBUG
		System.out.println("reassureWatchDog");
		// #endif
	}
	/**
	 * Encrypt database content
	 */
	public void lock() {
		if (!isLocked()) {
			try {
				byte[] encodedContent = getEncoded();
				this.close();
				this.encodedContent = encodedContent;
				
			} catch (KeydbException e) {
				this.close();
			}
			
			DisplayStack.notifyUI();
			
			// #ifdef DEBUG
			System.out.println("Database locked");
			// #endif
		};
	}
	/**
	 * Decrypt database content
	 * @param pass
	 * @param keyfile
	 * @throws KeydbException
	 */
	public void unlock(String pass, byte[] keyfile) throws KeydbException {
		if (isLocked()) {
			byte[] encoded = this.encodedContent;
			try {
				open(encoded, pass, keyfile);
				this.encodedContent = null;
			} catch (KeydbException e) {
				this.encodedContent = encoded;
				throw e;
			}
			
			// #ifdef DEBUG
			System.out.println("Database unlocked");
			// #endif
		};
	}
	/**
	 * This method must be added to all public methods
	 * @throws KeydbLockedException
	 */
	private void passLock() throws KeydbLockedException {
		if (isLocked()) throw new KeydbLockedException();
	}
	/**
	 * Get database status locked/unlocked
	 * @return true if encrypted
	 */
	public boolean isLocked() {
		return this.encodedContent != null;
	}

	/**
	 * Get database size
	 * @return size in bytes
	 */
	public int getSize() {
		if (this.isLocked()) return this.encodedContent.length + KeydbHeader.SIZE; 
		else return this.contentSize + KeydbHeader.SIZE;//TODO: add padding
	}
}
