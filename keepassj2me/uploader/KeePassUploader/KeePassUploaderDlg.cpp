// KeePassUploaderDlg.cpp : implementation file
//

#include "stdafx.h"
#include "KeePassUploader.h"
#include "KeePassUploaderDlg.h"
#include "FilePickCtrl.h" // File Picker
#include "KeePassDef.h" // KeePass related definitions definition
// OpenSSL
#include <openssl/rand.h>

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

	// NI
	mEditURL.SetWindowTextA(DEFAULT_URL);

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

void CKeePassUploaderDlg::OnBnClickedUpload()
{
	// make sure all the fields are filled
	if (mEditKDB.GetWindowTextLength() == 0 || mEditURL.GetWindowTextLength() == 0 || \
		mEditUsername.GetWindowTextLength() == 0 || mEditPassword.GetWindowTextLength() == 0) {
		MessageBox("Please fill all 4 fields");
		return;
	}

	// generate random encrytion code
	int rv = RAND_bytes(encCode, ENCCODE_LEN);
	if (rv == 0) {
		MessageBox("Random number generation failed");
		return;
	}
	// convert bytes to digits, string
	for (int i=0; i<ENCCODE_LEN; i++) {
		encCode[i] = (int)(encCode[i] / 25.6);
		encCodeStr[i] = '0' + encCode[i];
	}
	encCodeStr[ENCCODE_LEN] = NULL;
	//MessageBox(encCodeStr);

	mEditEncCode.SetWindowText(encCodeStr);

	/* test 
#define TEST_NUM 100000

	byte randomNums[TEST_NUM];
	rv = RAND_bytes(randomNums, TEST_NUM);
	if (rv == 0) {
		MessageBox("Random number generation failed");
		return;
	}

	int digitNum[10];
	int digit;
	for (int i=0; i<10; i++)
		digitNum[i] = 0;
	for (int i=0; i<TEST_NUM; i++) {
		digit = (int)(randomNums[i] / 25.6);
		digitNum[digit]++;
	}

	char buf[256];
	_snprintf(buf, 256, "%d %d %d %d %d %d %d %d %d %d", 
		digitNum[0], digitNum[1], digitNum[2], digitNum[3], digitNum[4], 
		digitNum[5], digitNum[6], digitNum[7], digitNum[8], digitNum[9]); 

	MessageBox(buf);
	*/


}

