// KeePassUploaderDlg.h : header file
//

#pragma once
#include "afxwin.h"
#include "KeePassDef.h"

// CKeePassUploaderDlg dialog
class CKeePassUploaderDlg : public CDialog
{
// Construction
public:
	CKeePassUploaderDlg(CWnd* pParent = NULL);	// standard constructor

// Dialog Data
	enum { IDD = IDD_KEEPASSUPLOADER_DIALOG };

	protected:
	virtual void DoDataExchange(CDataExchange* pDX);	// DDX/DDV support


// Implementation
protected:
	HICON m_hIcon;

	// Generated message map functions
	virtual BOOL OnInitDialog();
	afx_msg void OnSysCommand(UINT nID, LPARAM lParam);
	afx_msg void OnPaint();
	afx_msg HCURSOR OnQueryDragIcon();
	DECLARE_MESSAGE_MAP()
public:
	afx_msg void OnBnClickedChoose();
public:
	afx_msg void OnBnClickedCancel();
public:
	CEdit mEditKDB;
public:
	CEdit mEditURL;
public:
	afx_msg void OnBnClickedUpload();
public:
	CEdit mEditUsername;
public:
	CEdit mEditPassword;
public:
	CStatic mEditEncCode;

private:
	byte encCode[ENCCODE_LEN];
	char encCodeStr[ENCCODE_LEN + 1];
};
