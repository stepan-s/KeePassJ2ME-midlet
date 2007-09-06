#ifndef _KEEPASS_DEF_H_
#define _KEEPASS_DEF_H_

// definition

#define DEFAULT_URL "http://keepassserver.info"
#define ENCCODE_LEN 16
#define ENCCODE_KEY_ROUNDS 6000
#define ENCCODE_KEY_LEN 32
#define KDB_HEADER_LEN 124

const byte ZeroIV[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

#endif // _KEEPASS_DEF_H_
