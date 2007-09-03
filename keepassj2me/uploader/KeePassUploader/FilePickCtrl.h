#if !defined(AFX_FILEPICKCTRL_H__2773A142_C6A1_11D3_BC5C_00A0CC3A0C54__INCLUDED_)
#define AFX_FILEPICKCTRL_H__2773A142_C6A1_11D3_BC5C_00A0CC3A0C54__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000
// FilePickCtrl.h : header file
//

#include "resource.h"

/////////////////////////////////////////////////////////////////////////////
// CFilePickCtrl dialog

class CFilePickCtrl : public CDialog
{
// Construction
public:
	CFilePickCtrl(CWnd* pParent = NULL);   // standard constructor
	virtual ~CFilePickCtrl();

// Dialog Data
	//{{AFX_DATA(CFilePickCtrl)
	enum { IDD = IDD_FILEPICKCTRL_DIALOG };
	CComboBoxEx		m_drivesCtrl;
	CEdit			m_currentDirCtrl;
	CComboBox		m_typesCtrl;
	CListCtrl		m_filesCtrl;
	CTreeCtrl		m_driveTreeCtrl;
	//}}AFX_DATA


	void		SetLargeDriveIcons(BOOL bLargeDriveIcons);
	void		SetLargeFilesIcons(BOOL bLargeFilesIcons);
	void		SetDirOnly(BOOL bDirOnly);
	void		SetShowPath(BOOL bShowPath);
	void		SetFileMask(char *description, char *filemask);
	void		SetStartPath(char *startPath);
	BOOL		AddFilemask(char *filemask);
	void		DeleteFilemasks();
	void		GetSelectedItem(char *buf);


// Overrides
	// ClassWizard generated virtual function overrides
	//{{AFX_VIRTUAL(CFilePickCtrl)
	public:
	virtual int DoModal(char *title);
	protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support
	//}}AFX_VIRTUAL

// Implementation
protected:
	BOOL		m_bLargeDriveIcons;
	BOOL		m_bLargeFilesIcons;
	BOOL		m_bDirOnly;
	BOOL		m_bShowPath;
	char		m_title[1024];
	char		m_startPath[1024];
	char		m_selectedFile[1024];
	char		m_lastStartPath[1024];
	char		*m_pDriveString;
	DWORD		m_nDriveBitmask;
	int			m_nCurrentDrivesSel;
	int			m_nCurrentTypesSel;
	int			m_nLongestStringWidth;
	int			m_nItems;
	int			m_nDirItems;
	int			m_nFileItems;

struct FILESCTRL_STRUCT
{
	int							iconIndex;
	LPARAM						lParam;
	char						text[MAX_PATH];
	struct FILESCTRL_STRUCT		*next;
} dirData, fileData, *pDirData, *pFileData;

struct FILEMASKS_STRUCT
{
	char						text[MAX_PATH];
	struct FILEMASKS_STRUCT		*next;
} filemasks, *pFilemasks;


	void		SetDropDownSize(CComboBox *box, UINT LinesToDisplay);
	int			GetIconIndex(char *filename, BOOL bDirectory);
	void		StrToCapitalizedWords(char *s);
	void		PopulateFilePickCtrl();
	void		SortDataStructs();
	void		AddDataStructs();
	void		DeleteDataStructs();

	
	// Generated message map functions
	//{{AFX_MSG(CFilePickCtrl)
	virtual BOOL OnInitDialog();
	afx_msg void OnSelChangeFilePickCtrlTypes();
	afx_msg void OnSelChangeFilePickCtrlDrives();
	afx_msg void OnDblclkFilePickCtrlFiles(NMHDR* pNMHDR, LRESULT* pResult);
	afx_msg void OnDestroy();
	virtual void OnOK();
	virtual void OnCancel();
	//}}AFX_MSG
	DECLARE_MESSAGE_MAP()
};

//{{AFX_INSERT_LOCATION}}
// Microsoft Visual C++ will insert additional declarations immediately before the previous line.

#endif // !defined(AFX_FILEPICKCTRL_H__2773A142_C6A1_11D3_BC5C_00A0CC3A0C54__INCLUDED_)
