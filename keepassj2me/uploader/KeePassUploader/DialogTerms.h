#pragma once
#include "afxwin.h"
#include "afxcmn.h"

// DialogTerms dialog

class DialogTerms : public CDialog
{
	DECLARE_DYNAMIC(DialogTerms)

public:
	DialogTerms(CWnd* pParent = NULL);   // standard constructor
	virtual ~DialogTerms();

// Dialog Data
	enum { IDD = IDD_TERMS };

protected:
	virtual void DoDataExchange(CDataExchange* pDX);    // DDX/DDV support

	DECLARE_MESSAGE_MAP()
public:
	virtual BOOL OnInitDialog();
public:
	CListBox mList;
};
