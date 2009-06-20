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
	
	protected byte[] plainContent = null;
	protected int contentSize = 0;
	protected int entriesOffset = 0;

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

		int offset = 0;
		KeydbGroup group = new KeydbGroup();
		for(int i = 0; i < header.numGroups; ++i) {
			offset += group.read(plainContent, offset);
		}
		this.entriesOffset = offset;
		
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
	}

	public KeydbGroup getGroupParent(int id) throws KeydbException {
		if (id != 0) {
			int offset = 0;
			int[] ids = new int[20];
			KeydbGroup group = new KeydbGroup();
			
			for(int i = 0; i < header.numGroups; ++i) {
				group.clean();
				offset += group.read(plainContent, offset);
				if (group.id == id) {
					if (group.level == 0) {
						return null;
					} else {
						int parentOffset = ids[group.level - 1];
						group.clean();
						group.read(plainContent, parentOffset);
						return group;
					}
				} else {
					if (group.level >= ids.length) {
						int[] new_ids = new int[ids.length + 20];
						System.arraycopy(ids, 0, new_ids, 0, ids.length);
						ids = new_ids;
					};
					ids[group.level] = group.offset;
				}
			}
			throw new KeydbException("Group not found");
		} else {
			throw new KeydbException("Root dont have parent");
		};
	}
	
	public void enumGroupContent(int id, IKeydbGroupContentRecever receiver) {
		int offset = 0;
		int level = -1;
		int i = 0;
		KeydbGroup group = new KeydbGroup();
		if (id != 0) {
			//find parent group and childs level
			while(i < header.numGroups) {
				group.clean();
				offset += group.read(plainContent, offset);
				++i;
				if (group.id == id) {
					level = group.level + 1;
					break;
				};
			}
		} else {
			level = 0;
		};
		//scan for child groups at level
		while(i < header.numGroups) {
			group.clean();
			offset += group.read(plainContent, offset);
			++i;
			if (group.level == level) {
				receiver.addKeydbGroup(group);
				group = new KeydbGroup();
			} else if (group.level < level) {
				break;
			}
		}
		
		if (id != 0) {
			i = 0;
			offset = this.entriesOffset;
			//scan for child entries
			KeydbEntry entry = new KeydbEntry();
			while(i < header.numEntries) {
				entry.clean();
				offset += entry.read(plainContent, offset);
				++i;
				if (entry.groupId == id) {
					receiver.addKeydbEntry(entry);
					entry = new KeydbEntry();
				};
			}
		};
	}
	
	public KeydbEntry getEntryByOffset(int offset) {
		KeydbEntry entry = new KeydbEntry();
		entry.read(plainContent, offset);
		return entry;
	}
	
	public void enumEntriesByTitle(String begin, IKeydbGroupContentRecever receiver, int max) {
		int offset = this.entriesOffset;
		KeydbEntry entry = new KeydbEntry();
		begin = begin.toLowerCase();
		for(int i = 0; i < header.numEntries; ++i) {
			entry.clean();
			offset += entry.read(plainContent, offset);
			if (entry.title.toLowerCase().startsWith(begin)) {
				receiver.addKeydbEntry(entry);
				if (--max <= 0) break;
				entry = new KeydbEntry();
			};
		}
	}
	
	public KeydbEntry getEntryByTitle(String begin, int index) {
		int offset = this.entriesOffset;
		KeydbEntry entry = new KeydbEntry();
		begin = begin.toLowerCase();
		for(int i = 0; i < header.numEntries; ++i) {
			entry.clean();
			offset += entry.read(plainContent, offset);
			if (entry.title.toLowerCase().startsWith(begin)) {
				if (index > 0) --index;
				else return entry;
			};
		}
		return null;
	}

	public KeydbGroup getGroupByIndex(int parent, int index) {
		int offset = 0;
		int level = -1;
		int i = 0;
		KeydbGroup group = new KeydbGroup();
		if (parent != 0) {
			//find parent group and childs level
			while(i < header.numGroups) {
				group.clean();
				offset += group.read(plainContent, offset);
				++i;
				if (group.id == parent) {
					level = group.level + 1;
					break;
				};
			}
		} else {
			level = 0;
		};
		//scan for child groups at level
		while(i < header.numGroups) {
			group.clean();
			offset += group.read(plainContent, offset);
			++i;
			if (group.level == level) {
				if (index > 0) --index;
				else return group;
			} else if (group.level < level) {
				break;
			}
		}
		return null;
	}

	public KeydbEntry getEntryByIndex(int groupId, int index) {
		int offset = this.entriesOffset;
		KeydbEntry entry = new KeydbEntry();
		for(int i = 0; i < header.numEntries; ++i) {
			entry.clean();
			offset += entry.read(plainContent, offset);
			if (entry.groupId == groupId) {
				if (index > 0) --index;
				else return entry;
			};
		}
		return null;
	}
}
