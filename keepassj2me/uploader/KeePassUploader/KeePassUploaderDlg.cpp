// KeePassUploaderDlg.cpp : implementation file
//

#include "stdafx.h"
#include "KeePassUploader.h"
#include "KeePassUploaderDlg.h"
#include "FilePickCtrl.h" // File Picker
#include "KeePassDef.h" // KeePass related definitions definition
// OpenSSL
#include <openssl/rand.h>
#include <openssl/sha.h>
#include <openssl/aes.h>
#include <openssl/evp.h>
#include <openssl/err.h>
// Windows
#include <io.h>
// for HTTP
//#include <Winhttp.h>
#include "GenericHTTPClient.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#endif


// CAboutDlg dialog used for App About

class CAboutDlg : public CDialog
{
public:
	CAboutDlg();

// Dialog Data
	enum { IDD = IDD_ABOUTBOX };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support

// Implementation
protected:
	DECLARE_MESSAGE_MAP()
};

CAboutDlg::CAboutDlg() : CDialog(CAboutDlg::IDD)
{
}

void CAboutDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
}

BEGIN_MESSAGE_MAP(CAboutDlg, CDialog)
END_MESSAGE_MAP()


// CKeePassUploaderDlg dialog




CKeePassUploaderDlg::CKeePassUploaderDlg(CWnd* pParent /*=NULL*/)
	: CDialog(CKeePassUploaderDlg::IDD, pParent)
{
	m_hIcon = AfxGetApp()->LoadIcon(IDR_MAINFRAME);

	// initializa random number generator
	while (1) 
	{
		RAND_poll();
		if (RAND_status() == 1)
			break;
	}

}

void CKeePassUploaderDlg::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	DDX_Control(pDX, IDC_EDIT_KDB, mEditKDB);
	DDX_Control(pDX, IDC_EDIT2, mEditURL);
	DDX_Control(pDX, IDC_EDIT3, mEditUsername);
	DDX_Control(pDX, IDC_EDIT4, mEditPassword);
	DDX_Control(pDX, IDC_ENCCODE, mEditEncCode);
}

BEGIN_MESSAGE_MAP(CKeePassUploaderDlg, CDialog)
	ON_WM_SYSCOMMAND()
	ON_WM_PAINT()
	ON_WM_QUERYDRAGICON()
	//}}AFX_MSG_MAP
	ON_BN_CLICKED(ID_CHOOSE, &CKeePassUploaderDlg::OnBnClickedChoose)
	ON_BN_CLICKED(ID_CANCEL, &CKeePassUploaderDlg::OnBnClickedCancel)
	ON_BN_CLICKED(ID_UPLOAD, &CKeePassUploaderDlg::OnBnClickedUpload)
END_MESSAGE_MAP()


// CKeePassUploaderDlg message handlers

BOOL CKeePassUploaderDlg::OnInitDialog()
{
	CDialog::OnInitDialog();

	// Add "About..." menu item to system menu.

	// IDM_ABOUTBOX must be in the system command range.
	ASSERT((IDM_ABOUTBOX & 0xFFF0) == IDM_ABOUTBOX);
	ASSERT(IDM_ABOUTBOX < 0xF000);

	CMenu* pSysMenu = GetSystemMenu(FALSE);
	if (pSysMenu != NULL)
	{
		CString strAboutMenu;
		strAboutMenu.LoadString(IDS_ABOUTBOX);
		if (!strAboutMenu.IsEmpty())
		{
			pSysMenu->AppendMenu(MF_SEPARATOR);
			pSysMenu->AppendMenu(MF_STRING, IDM_ABOUTBOX, strAboutMenu);
		}
	}

	// Set the icon for this dialog.  The framework does this automatically
	//  when the application's main window is not a dialog
	SetIcon(m_hIcon, TRUE);			// Set big icon
	SetIcon(m_hIcon, FALSE);		// Set small icon

	// Initialize URL text
	mEditURL.SetWindowTextA(DEFAULT_URL);

	// Initialize OpenSSL
	OpenSSL_add_all_ciphers();
    OpenSSL_add_all_digests();

	return TRUE;  // return TRUE  unless you set the focus to a control
}

void CKeePassUploaderDlg::OnSysCommand(UINT nID, LPARAM lParam)
{
	if ((nID & 0xFFF0) == IDM_ABOUTBOX)
	{
		CAboutDlg dlgAbout;
		dlgAbout.DoModal();
	}
	else
	{
		CDialog::OnSysCommand(nID, lParam);
	}
}

// If you add a minimize button to your dialog, you will need the code below
//  to draw the icon.  For MFC applications using the document/view model,
//  this is automatically done for you by the framework.

void CKeePassUploaderDlg::OnPaint()
{
	if (IsIconic())
	{
		CPaintDC dc(this); // device context for painting

		SendMessage(WM_ICONERASEBKGND, reinterpret_cast<WPARAM>(dc.GetSafeHdc()), 0);

		// Center icon in client rectangle
		int cxIcon = GetSystemMetrics(SM_CXICON);
		int cyIcon = GetSystemMetrics(SM_CYICON);
		CRect rect;
		GetClientRect(&rect);
		int x = (rect.Width() - cxIcon + 1) / 2;
		int y = (rect.Height() - cyIcon + 1) / 2;

		// Draw the icon
		dc.DrawIcon(x, y, m_hIcon);
	}
	else
	{
		CDialog::OnPaint();
	}
}

// The system calls this function to obtain the cursor to display while the user drags
//  the minimized window.
HCURSOR CKeePassUploaderDlg::OnQueryDragIcon()
{
	return static_cast<HCURSOR>(m_hIcon);
}


void CKeePassUploaderDlg::OnBnClickedChoose()
{
	//MessageBox("Hi");//, "OnBnClickedChoose()", 0, 0);

	CFilePickCtrl dlg;
	//m_pMainWnd = &dlg;
	int nResponse = dlg.DoModal("Choose KDB File");
	if (nResponse == IDOK)
	{
		char file[1024];

		dlg.GetSelectedItem(file);
		mEditKDB.SetWindowTextA(file);

	}
	else if (nResponse == IDCANCEL)
	{
		// Do nothing
	}

}

void CKeePassUploaderDlg::OnBnClickedCancel()
{
	this->DestroyWindow();
}

// run SHA256 several rounds on password to generate a key
// password must be ENCCODE_LEN length
// key must have PASSWORD_KEY_LEN length allocated
void passwordKeySHA(byte *key, byte *password)
{
	SHA256(password, ENCCODE_LEN, key);
	for (int i=0; i<ENCCODE_KEY_ROUNDS - 1; i++) {
		SHA256(key, ENCCODE_KEY_LEN, key);
	}
}

void CKeePassUploaderDlg::OnBnClickedUpload()
{
	GenericHTTPClient *pClient=new GenericHTTPClient();

    pClient->InitilizePostArguments();
    //pClient->AddPostArguments(__TAG_USRID, szUserID);
    //pClient->AddPostArguments(__TAG_JUMIN, szSocialIndex);
    //pClient->AddPostArguments(__TAG_SRC, szSource);
    //pClient->AddPostArguments(__TAG_DST, szDestination);            
    //pClient->AddPostArguments(__TAG_FORMAT, szFormat);
    //pClient->AddPostArguments(__TAG_SUBJECT, szMessage);
    //pClient->AddPostArguments(__TAG_CPCODE, szCPCode);
	pClient->AddPostArguments("kdbfile", "d:\\hi", TRUE);

	//TCHAR buf[256];
	//pClient->GetPostArguments(buf, 256);
	//MessageBox(buf);

	if(pClient->Request("http://keepassserver.info/submit.php", 
        GenericHTTPClient::RequestPostMethodMultiPartsFormData)){        
        LPCTSTR szResult=pClient->QueryHTTPResponse();

		MessageBox(szResult);
    }else{
		MessageBox("fail..");

		DWORD d = ::GetLastError();

//#ifdef    _DEBUG
        LPVOID     lpMsgBuffer;
        DWORD dwRet=FormatMessage( FORMAT_MESSAGE_ALLOCATE_BUFFER |
                      FORMAT_MESSAGE_FROM_SYSTEM,
                      NULL,
                      pClient->GetLastError(),
                      MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
                      reinterpret_cast<LPTSTR>(&lpMsgBuffer),
                      0,
                      NULL);
        //OutputDebugString(reinterpret_cast<LPTSTR>(lpMsgBuffer));
		MessageBox(reinterpret_cast<LPTSTR>(lpMsgBuffer));
        LocalFree(lpMsgBuffer);
//#endif
	}
	
  /*

	// make sure all the fields are filled
	if (mEditKDB.GetWindowTextLength() == 0 || mEditURL.GetWindowTextLength() == 0 || \
		mEditUsername.GetWindowTextLength() == 0 || mEditPassword.GetWindowTextLength() == 0) {
		MessageBox("Please fill all 4 fields");
		return;
	}

	// read KDB and check length
	byte *plainKDB = NULL;
	FILE *fp;
	CString kdbFilename;
	mEditKDB.GetWindowText(kdbFilename);
	fp = fopen(kdbFilename, "r");
	if (fp == NULL) {
		MessageBox("Cannot open KDB file");
		goto end;
	}
	int kdbLen = _filelength(fp->_file);
	if (kdbLen < KDB_HEADER_LEN || (kdbLen - KDB_HEADER_LEN) % 16 != 0) {
		MessageBox("Bad KDB len - shorter than KDB_HEADER_LEN(124), or encrypted part is not multiple of 16");
		goto end;
	}
	//char buf[256];
	//_snprintf (buf, 256, "file len %d", kdbLen);
	//MessageBox(buf);
	plainKDB = new byte[kdbLen];
	
	fread (plainKDB, sizeof(byte), kdbLen, fp);

	fclose (fp);

	// generate random encrytion code
	int rv = RAND_bytes(mEncCode, ENCCODE_LEN);
	if (rv == 0) {
		MessageBox("Random number generation failed");
		goto end;
	}
	// convert bytes to digits, string
	for (int i=0; i<ENCCODE_LEN; i++) {
		mEncCode[i] = (int)(mEncCode[i] / 25.6);
		mEncCodeStr[i] = '0' + mEncCode[i];
	}
	mEncCodeStr[ENCCODE_LEN] = NULL;

	// show enc code on window
	mEditEncCode.SetWindowText(mEncCodeStr);

	// generate key from enc code
	passwordKeySHA(mEncCodeKey, mEncCode);

	// encrypt KDB's encrypted part
	const EVP_CIPHER *cipher;
    cipher = EVP_get_cipherbyname("AES-256-CBC");
	if (cipher == NULL) {
		ERR_load_crypto_strings();

		MessageBox (ERR_error_string(ERR_get_error(), NULL));
		goto end;
	}

	EVP_CIPHER_CTX ctx;
    int outlen;
	if (cipher->key_len != 32) {
		MessageBox("AES Key Len not 32?");
		goto end;
	}
	EVP_CIPHER_CTX_init(&ctx);
    
	if (!EVP_EncryptInit_ex(&ctx, cipher, NULL, mEncCodeKey, ZeroIV))
    {
		MessageBox("EncryptInit failed");
		goto end;
	}

	EVP_CIPHER_CTX_set_padding(&ctx,0);

	if(!EVP_EncryptUpdate(&ctx, plainKDB + KDB_HEADER_LEN, &outlen, 
								plainKDB + KDB_HEADER_LEN, kdbLen - KDB_HEADER_LEN))
    {
		MessageBox("EVP_EncryptUpdate() failed");
		goto end;
	}

	//if(!EVP_EncryptFinal_ex(&ctx,out+outl,&outl2))
      //      {
        //    fprintf(stderr,"EncryptFinal failed\n");
          //  ERR_print_errors_fp(stderr);
end:
	if (plainKDB != NULL)
		delete plainKDB;
	*/
}


