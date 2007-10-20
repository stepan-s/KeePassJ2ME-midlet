#ifndef _KEEPASS_DEF_H_
#define _KEEPASS_DEF_H_

// definition

#define DEFAULT_URL "https://keepassserver.info/"
#define URL_SUBMIT "submit.php"
#define PATH_TERMS_OF_SERVICE "TermsOfService.txt"
#define ENCCODE_LEN 16
#define USER_CODE_LEN 4
#define PASS_CODE_LEN 4
#define ENCCODE_KEY_ROUNDS 6000
#define ENCCODE_KEY_LEN 32
#define KDB_HEADER_LEN 124
#define BUFLEN 1024
#define ENC_KDB_FILE "enckdb.tmp"

const byte ZeroIV[] = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

#endif // _KEEPASS_DEF_H_
