// DialogTerms.cpp : implementation file
//

#include "stdafx.h"
#include "KeePassUploader.h"
#include "DialogTerms.h"
#include "KeePassDef.h"
// Windows
#include <sys/types.h> 
#include <sys/stat.h>

// DialogTerms dialog

IMPLEMENT_DYNAMIC(DialogTerms, CDialog)

DialogTerms::DialogTerms(CWnd* pParent /*=NULL*/)
	: CDialog(DialogTerms::IDD, pParent)
{
}

DialogTerms::~DialogTerms()
{
}

void DialogTerms::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	DDX_Control(pDX, IDC_LIST1, mList);
}


BEGIN_MESSAGE_MAP(DialogTerms, CDialog)
END_MESSAGE_MAP()


// DialogTerms message handlers

BOOL DialogTerms::OnInitDialog()
{
	CDialog::OnInitDialog();
	
	// show Terms of Service
	//struct stat stbuf;
	//stat(PATH_TERMS_OF_SERVICE, &stbuf);
	//int sizeTerms = stbuf.st_size;

	//TCHAR *strTerms = NULL;
	//strTerms = new TCHAR[sizeTerms];
	TCHAR line[1024];

	FILE *fpTerms = fopen(PATH_TERMS_OF_SERVICE, "r");
	if (fpTerms == NULL) {
		MessageBox ("No Terms of Service file");
		goto end;
	}
	//fread(strTerms, sizeof(TCHAR), sizeTerms, fpTerms);

	while (fgets(line, 1024, fpTerms)) {
		mList.InsertString(-1, line);
	}
	
	fclose (fpTerms);

end:

	return TRUE;  // return TRUE unless you set the focus to a control
	// EXCEPTION: OCX Property Pages should return FALSE
}
