package net.sourceforge.keepassj2me.keydb;

import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import net.sourceforge.keepassj2me.IProgressListener;
import net.sourceforge.keepassj2me.KeePassException;
import net.sourceforge.keepassj2me.importerv3.Util;

/**
 * KDB database
 * @author Stepan Strelets
 */
public class KeydbDatabase {
	protected IProgressListener listener = null;
	protected KeydbHeader header = null;
	
	/** KDB */
	protected byte[] plainContent = null;
	/** actual data length in plainContent */
	protected int contentSize = 0;
	
	/** each array element contain group id */
	protected int[] groupsIds = null; 
	/** each array element contain group */
	protected int[] groupsOffsets = null; 
	/** each array element contain group gid */
	protected int[] groupsGids = null;
	
	/** entries offset in plainContent */
	protected int entriesStartOffset = 0;
	
	/** each array element contain entry offset */
	protected int[] entriesOffsets = null;
	/** each array element contain entry gid */
	protected int[] entriesGids = null;
	/** each array element contain entry meta mark */
	protected byte[] entriesMeta = null;
	/** each array element contain entry search mark */
	protected byte[] entriesSearch = null;

	public KeydbDatabase() {

	}

	public void setProgressListener(IProgressListener listener) {
		this.listener = listener;
	}

	protected void setProgress(int procent, String message) throws KeydbException {
		if (this.listener != null) {
			try {
				this.listener.setProgress(procent, message);
			} catch (KeePassException e) {
				throw new KeydbException(e.getMessage());
			}
		}
	}

	public void create() {

	}

	public void open(byte[] encoded, String pass, byte[] keyfile) throws KeydbException {
		this.setProgress(5, "Open database");

		this.header = new KeydbHeader(encoded, 0);

		if ((this.header.flags & KeydbHeader.FLAG_RIJNDAEL) != 0) {
			
		} else if ((this.header.flags & KeydbHeader.FLAG_TWOFISH) != 0) {
			throw new KeydbException("TwoFish algorithm is not supported");
			
		} else {
			throw new KeydbException("Unknown algorithm");
		}

		setProgress(10, "Decrypt key");

		byte[] passHash;
		switch (((pass != null) && (pass.length() != 0) ? 1 : 0)
				| ((keyfile != null) && (keyfile.length != 0) ? 2 : 0)) {
		case 0:
			throw new KeydbException("Both password and key is empty");
		case 1:
			passHash = KeydbUtil.hash(pass);
			break;
		case 2:
			passHash = KeydbUtil.hashKeyfile(keyfile);
			break;
		case 3:
			passHash = KeydbUtil.hash(new byte[][] {pass.getBytes(), KeydbUtil.hashKeyfile(keyfile)});
			break;
		default:
			throw new KeydbException("Execution error");
		};
		
		byte[] transformedMasterKey = this.transformMasterKey(
				this.header.masterSeed2,
				passHash,
				this.header.numKeyEncRounds);
		passHash = null;
		
		// Hash the master password with the salt in the file
		byte[] finalKey = KeydbUtil.hash(new byte[][] {
				this.header.masterSeed,
				transformedMasterKey});

		setProgress(90, "Decrypt database");

		BufferedBlockCipher cipher = new BufferedBlockCipher(
				new CBCBlockCipher(new AESEngine()));

		cipher.init(false, new ParametersWithIV(new KeyParameter(finalKey),
				this.header.encryptionIV));
		
		// Decrypt! The first bytes aren't encrypted (that's the header)
		this.plainContent = new byte[encoded.length - KeydbHeader.SIZE];
		int paddedEncryptedPartSize = cipher.processBytes(encoded,
				KeydbHeader.SIZE, encoded.length - KeydbHeader.SIZE,
				this.plainContent, 0);

		//detect padding and calc content size 
		this.contentSize = 0;
		PKCS7Padding padding = new PKCS7Padding();
		try {
			this.contentSize = paddedEncryptedPartSize - padding.padCount(this.plainContent);
		} catch (InvalidCipherTextException e) {
			throw new KeydbException("Wrong password, keyfile or database corrupted (database did not decrypt correctly)");
		}
		
		if (!Util.compare(
				KeydbUtil.hash(this.plainContent, 0, this.contentSize),
				this.header.contentsHash)) {
			throw new KeydbException("Wrong password, keyfile or database corrupted (database did not decrypt correctly)");
		}

		setProgress(95, "Make indexes");
		
		this.makeGroupsIndexes();
		this.makeEntriesIndexes();
		
		setProgress(100, "Done");
	}
	
	private byte[] transformMasterKey(byte[] pKeySeed, byte[] pKey, int rounds) throws KeydbException {
		byte[] newKey = new byte[pKey.length];
		System.arraycopy(pKey, 0, newKey, 0, pKey.length);

		BufferedBlockCipher cipher = new BufferedBlockCipher(new AESEngine());
		cipher.init(true, new KeyParameter(pKeySeed));

		int procent = 10; // 10% - progress start
		int step = 5;// % step
		int roundsByStep = rounds * step / ((90 - procent)); // 90% - progress end
		int count = 0;

		for (int i = 0; i < rounds; i++) {
			cipher.processBytes(newKey, 0, newKey.length, newKey, 0);

			if (++count == roundsByStep) {
				count = 0;
				setProgress(procent += step, null);
			};
		};
		return KeydbUtil.hash(newKey);
	}
	
	public void close() {
		if (plainContent != null) {
			Util.fill(plainContent, (byte)0);
			plainContent = null;
		};
		if (groupsIds != null) groupsIds = null;
		if (groupsOffsets != null) groupsOffsets = null;
		if (groupsGids != null) groupsGids = null;
		
		if (entriesOffsets != null) entriesOffsets = null;
		if (entriesGids != null) entriesGids = null;
		if (entriesMeta != null) entriesMeta = null;
		if (entriesSearch != null) entriesSearch = null;
	}

	private void makeGroupsIndexes() {
		int offset = 0;
		int[] ids = new int[20];
		
		this.groupsIds = new int[this.header.numGroups];
		this.groupsOffsets = new int[this.header.numGroups];
		this.groupsGids = new int[this.header.numGroups];
		
		KeydbGroup group = new KeydbGroup();
		for(int i = 0; i < header.numGroups; ++i) {
			this.groupsOffsets[i] = offset;
			offset += group.read(plainContent, offset);
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
	}
	
	private void makeEntriesIndexes() {
		int offset = this.entriesStartOffset;
		
		this.entriesOffsets = new int[this.header.numEntries];
		this.entriesGids = new int[this.header.numEntries];
		this.entriesMeta = new byte[this.header.numEntries];
		this.entriesSearch = new byte[this.header.numEntries];
		
		KeydbEntry entry = new KeydbEntry();
		for(int i = 0; i < header.numEntries; ++i) {
			entry.clean();
			this.entriesOffsets[i] = offset;
			offset += entry.read(plainContent, offset);
			this.entriesGids[i] = entry.groupId;
			if (entry.title.equals("Meta-Info")
					&& entry.getUsername(this).equals("SYSTEM")
					&& entry.getUrl(this).equals("$")) {
				this.entriesMeta[i] = 1;
			} else {
				this.entriesMeta[i] = 0;
			}
		}
	};
	
	public KeydbGroup getGroup(int id) throws KeydbException {
		if (id != 0) {
			for(int i = 0; i < header.numGroups; ++i) {
				if (this.groupsIds[i] == id) {
					KeydbGroup group = new KeydbGroup();
					group.read(plainContent, this.groupsOffsets[i]);
					return group;
				};
			};
			throw new KeydbException("Group not found");
		} else {
			throw new KeydbException("Cannot get Root group");
		};
	}
	public KeydbGroup getGroupParent(int id) throws KeydbException {
		if (id != 0) {
			for(int i = 0; i < header.numGroups; ++i) {
				if (this.groupsIds[i] == id) {
					return this.getGroup(this.groupsGids[i]);
				};
			};
			throw new KeydbException("Group not found");
		} else {
			throw new KeydbException("Root group dont have parent");
		};
	}
	
	public void enumGroupContent(int id, IKeydbGroupContentRecever receiver) {
		KeydbGroup group;
		for(int i = 0; i < header.numGroups; ++i) {
			if (this.groupsGids[i] == id) {
				group = new KeydbGroup();
				group.read(plainContent, this.groupsOffsets[i]);
				receiver.addKeydbGroup(group);
			}
		}
		KeydbEntry entry;
		for(int i = 0; i < header.numEntries; ++i) {
			if ((this.entriesGids[i] == id) && (this.entriesMeta[i] == 0)) {
				entry = new KeydbEntry();
				entry.read(plainContent, this.entriesOffsets[i]);
				receiver.addKeydbEntry(entry);
			}
		}
	}

	public int searchEntriesByTitle(String begin) {
		int found = 0;
		KeydbEntry entry = new KeydbEntry();
		begin = begin.toLowerCase();
		for(int i = 0; i < header.numEntries; ++i) {
			if (this.entriesMeta[i] == 0) {
				entry.clean();
				entry.read(plainContent, this.entriesOffsets[i]);
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
	
	public void enumFoundEntries(IKeydbGroupContentRecever receiver, int start, int limit) {
		KeydbEntry entry;
		for(int i = 0; i < header.numEntries; ++i) {
			if (this.entriesSearch[i] == 1) {
				if (start > 0) {
					--start;
				} else if (limit > 0) {
					--limit;
					entry = new KeydbEntry();
					entry.read(plainContent, this.entriesOffsets[i]);
					receiver.addKeydbEntry(entry);
				} else {
					break;
				}
			}
		}
	}
	
	public KeydbEntry getFoundEntry(int index) {
		for(int i = 0; i < header.numEntries; ++i) {
			if (i == index) {
				KeydbEntry entry = new KeydbEntry();
				entry.read(plainContent, this.entriesOffsets[i]);
				return entry;
			}
		}
		return null;
	}

	public KeydbGroup getGroupByIndex(int parent, int index) {
		for(int i = 0; i < header.numGroups; ++i) {
			if (this.groupsGids[i] == parent) {
				if (index > 0) --index;
				else {
					KeydbGroup group = new KeydbGroup();
					group.read(plainContent, this.groupsOffsets[i]);
					return group;
				}
			};
		}
		return null;
	}

	public KeydbEntry getEntryByIndex(int groupId, int index) {
		for(int i = 0; i < header.numEntries; ++i) {
			if ((this.entriesGids[i] == groupId) && (this.entriesMeta[i] == 0)) {
				if (index > 0) --index;
				else {
					KeydbEntry entry = new KeydbEntry();
					entry.read(plainContent, this.entriesOffsets[i]);
					return entry;
				}
			};
		}
		return null;
	}
}
