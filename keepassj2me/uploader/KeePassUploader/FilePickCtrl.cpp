// FilePickCtrl.cpp : implementation file
//

#include "stdafx.h"
#include "FilePickCtrl.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/////////////////////////////////////////////////////////////////////////////
// CFilePickCtrl dialog


CFilePickCtrl::CFilePickCtrl(CWnd* pParent /*=NULL*/)
	: CDialog(CFilePickCtrl::IDD, pParent)
{
	//{{AFX_DATA_INIT(CFilePickCtrl)
	//}}AFX_DATA_INIT

	m_bLargeDriveIcons = FALSE;
	m_bLargeFilesIcons = FALSE;
	m_bDirOnly = FALSE;
	m_bShowPath = TRUE;
	m_nCurrentDrivesSel = -1;
	m_nCurrentTypesSel = 0;
	strcpy(m_startPath, "C:\\");
	filemasks.next = NULL;
}


CFilePickCtrl::~CFilePickCtrl()
{
}


void CFilePickCtrl::DoDataExchange(CDataExchange* pDX)
{
	CDialog::DoDataExchange(pDX);
	//{{AFX_DATA_MAP(CFilePickCtrl)
	DDX_Control(pDX, IDC_FILEPICKCTRL_DRIVES, m_drivesCtrl);
	DDX_Control(pDX, IDC_FILEPICKCTRL_CURRENTDIR, m_currentDirCtrl);
	DDX_Control(pDX, IDC_FILEPICKCTRL_FILETYPES, m_typesCtrl);
	DDX_Control(pDX, IDC_FILEPICKCTRL_FILES, m_filesCtrl);
	//}}AFX_DATA_MAP
}


BEGIN_MESSAGE_MAP(CFilePickCtrl, CDialog)
	//{{AFX_MSG_MAP(CFilePickCtrl)
	ON_CBN_SELCHANGE(IDC_FILEPICKCTRL_FILETYPES, OnSelChangeFilePickCtrlTypes)
	ON_CBN_SELCHANGE(IDC_FILEPICKCTRL_DRIVES, OnSelChangeFilePickCtrlDrives)
	ON_NOTIFY(NM_DBLCLK, IDC_FILEPICKCTRL_FILES, OnDblclkFilePickCtrlFiles)
	ON_WM_DESTROY()
	//}}AFX_MSG_MAP
END_MESSAGE_MAP()

/////////////////////////////////////////////////////////////////////////////
// CFilePickCtrl message handlers

BOOL CFilePickCtrl::OnInitDialog() 
{
	CDialog::OnInitDialog();

	m_nDriveBitmask = GetLogicalDrives();
	SetWindowText(m_title);
	GetDlgItem(IDC_STATIC_FILEPICKCTRL_CURRENTDIR)->ShowWindow(m_bShowPath);
	GetDlgItem(IDC_FILEPICKCTRL_CURRENTDIR)->ShowWindow(m_bShowPath);

	if (m_bDirOnly)
	{
		GetDlgItem(IDC_STATIC_FILEPICKCTRL_FILETYPES)->ShowWindow(FALSE);
		GetDlgItem(IDC_FILEPICKCTRL_FILETYPES)->ShowWindow(FALSE);
	}
	else
	{
		m_typesCtrl.AddString("All files (*.*)");
		pFilemasks = filemasks.next;
		while (pFilemasks != NULL)
		{
			m_typesCtrl.AddString(pFilemasks->text);
			pFilemasks = pFilemasks->next;
		}
	}
	SetDropDownSize(&m_typesCtrl, m_typesCtrl.GetCount());
	m_typesCtrl.SetCurSel(m_nCurrentTypesSel);

	COMBOBOXEXITEM	cbexi;
	cbexi.mask = CBEIF_IMAGE | CBEIF_SELECTEDIMAGE | CBEIF_TEXT | CBEIF_LPARAM;
	cbexi.iItem = -1;					// Insert at end

	int			i = 0;
	DWORD		bitmask = 1;
	for (char driveLetter = 'A'; driveLetter <= 'Z'; driveLetter++)
	{
		if (m_nDriveBitmask & bitmask)
			i++;
		bitmask <<= 1;
	}

	char *p = m_pDriveString = new char [i*4 + 1];
	GetLogicalDriveStrings(i*4 + 1, m_pDriveString);

	i = 0;
	while (*p != 0)
	{
		char buf[8];
		if (*p > 'Z')
			*p -= 32;			// convert it from lower case to upper case
		if (*p == m_startPath[0])
			m_nCurrentDrivesSel = i;
		sprintf(buf, "(%c:)", *p);
		cbexi.pszText = (char *) &buf;
		cbexi.lParam = *p;
		cbexi.iImage = cbexi.iSelectedImage = GetIconIndex(p, TRUE);
		if (m_drivesCtrl.InsertItem(&cbexi) == -1)
			MessageBox("Error adding driveCtrl item", "", MB_OK);
		else
			m_drivesCtrl.SetItemDataPtr(i++, p);
		p += 4;
	}


// Set up image list pointers.
	SHFILEINFO		ssfi;
	CImageList		smallSystemImageList, largeSystemImageList;
	
	smallSystemImageList.Attach((HIMAGELIST) SHGetFileInfo(_T("C:\\"), 0, &ssfi, sizeof(SHFILEINFO), SHGFI_USEFILEATTRIBUTES | SHGFI_SYSICONINDEX | SHGFI_SMALLICON));
	largeSystemImageList.Attach((HIMAGELIST) SHGetFileInfo(_T("C:\\"), 0, &ssfi, sizeof(SHFILEINFO), SHGFI_USEFILEATTRIBUTES | SHGFI_SYSICONINDEX | SHGFI_LARGEICON));

// Set up control image lists.
	if (m_bLargeDriveIcons)
		m_drivesCtrl.SetImageList(&largeSystemImageList);
	else
		m_drivesCtrl.SetImageList(&smallSystemImageList);

	if (m_bLargeFilesIcons)
	{
		m_filesCtrl.SetImageList(&largeSystemImageList, LVSIL_NORMAL);
		m_filesCtrl.ModifyStyle(LVS_TYPEMASK | WS_VSCROLL, LVS_ICON, 0);
	}
	else
	{
		m_filesCtrl.SetImageList(&smallSystemImageList, LVSIL_SMALL);
		m_filesCtrl.ModifyStyle(LVS_TYPEMASK | WS_VSCROLL, LVS_SMALLICON, 0);
	}

	smallSystemImageList.Detach();
	largeSystemImageList.Detach();

	SetDropDownSize(&m_drivesCtrl, m_drivesCtrl.GetCount());
	m_drivesCtrl.SetCurSel(m_nCurrentDrivesSel);

	strcpy(m_lastStartPath, m_startPath);
	PopulateFilePickCtrl();

	return TRUE;  // return TRUE unless you set the focus to a control
	              // EXCEPTION: OCX Property Pages should return FALSE
}


int CFilePickCtrl::DoModal(char *title) 
{
	m_selectedFile[0] = 0;
	strcpy(m_title, title);
	
	return CDialog::DoModal();
}


void CFilePickCtrl::OnDestroy() 
{
    DeleteFilemasks();
    delete [] m_pDriveString;

	CDialog::OnDestroy();
}


void CFilePickCtrl::OnCancel() 
{
	strcpy(m_startPath, m_lastStartPath);

	CDialog::OnCancel();
}


void CFilePickCtrl::OnOK() 
{
	BOOL		b = FALSE;
	int			i = (int) m_filesCtrl.GetFirstSelectedItemPosition();

	if (i != NULL)
	{
		i--;
		if (m_filesCtrl.GetItemData(i) & FILE_ATTRIBUTE_DIRECTORY)
		{
			if (m_bDirOnly)
			{
// It's the "[Up one directory]" folder ...
				if (m_filesCtrl.GetItemData(i) == -1)
				{
// Strip off the deepest directory name by searching for the 2nd to last occurence of a backslash,
// and put a null terminator after it.
					strcpy(m_selectedFile, m_startPath);
					char	*p = strrchr(m_selectedFile, '\\');
					*p = 0;
					p = strrchr(m_selectedFile, '\\');
					*(p + 1) = 0;
				}
// It's the "[Root]" folder ...
				else if (m_filesCtrl.GetItemData(i) == -2)
				{
					strcpy(m_selectedFile, m_startPath);
				}
// Otherwise it's a normal directory ...
				else
				{
					char	buf[1024];
					m_filesCtrl.GetItemText(i, 0, buf, 1024);
					sprintf(m_selectedFile, "%s%s\\", m_startPath, buf);
				}
			}
			else
				b = TRUE;

			strcpy(m_startPath, m_selectedFile);
		}
// It must be a filename then ...
		else
		{
			char	buf[1024];
			m_filesCtrl.GetItemText(i, 0, buf, 1024);
			sprintf(m_selectedFile, "%s%s", m_startPath, buf);
		}
	}
	else
		b = TRUE;

/*
	if (b)
		EndDialog(IDCANCEL);
	else
		CDialog::OnOK();
*/

	if (b)
	{
		m_currentDirCtrl.GetWindowText(m_selectedFile, 1024);
		m_selectedFile[strlen(m_selectedFile)] = 0;
	}

	CDialog::OnOK();
}


void CFilePickCtrl::SetDropDownSize(CComboBox *box, UINT LinesToDisplay)
{
	CRect	cbSize;										// current size of combo box
	int		Height;										// new height for drop-down portion of combo box
    
	box->GetClientRect(cbSize);
	Height = box->GetItemHeight(-1);					// start with size of the edit-box portion
	Height += box->GetItemHeight(0) * LinesToDisplay;	// + height of lines 

	// Note: The use of SM_CYEDGE assumes that we're using Windows '95
	// Now add on the height of the border of the edit box
	Height += GetSystemMetrics(SM_CYEDGE) * 2;			// top & bottom edges

	// The height of the border of the drop-down box
	Height += GetSystemMetrics(SM_CYEDGE) * 2;			// top & bottom edges

	// Now set the size of the window
	box->SetWindowPos(NULL,								// not relative to any other windows
		0, 0,											// TopLeft corner doesn't change
		cbSize.right, Height,							// existing width, new height
		SWP_NOMOVE | SWP_NOZORDER);						// don't move box or change z-ordering.
}


void CFilePickCtrl::SetLargeDriveIcons(BOOL bLargeDriveIcons)
{
	m_bLargeDriveIcons = bLargeDriveIcons;
}


void CFilePickCtrl::SetLargeFilesIcons(BOOL bLargeFilesIcons)
{
	m_bLargeFilesIcons = bLargeFilesIcons;
}


void CFilePickCtrl::SetDirOnly(BOOL bDirOnly)
{
	m_bDirOnly = bDirOnly;
	if (m_bDirOnly)
		m_nCurrentTypesSel = 0;
}


void CFilePickCtrl::SetShowPath(BOOL bShowPath)
{
	m_bShowPath = bShowPath;
}


void CFilePickCtrl::SetStartPath(char *startPath)
{
	strcpy(m_startPath, startPath);
}

	
BOOL CFilePickCtrl::AddFilemask(char *filemask)
{
	if (m_bDirOnly)
		return(FALSE);

	char	buf[MAX_PATH], *p;

	strcpy(buf, filemask);
	p = strrchr(buf, ')');
	if (p == NULL)
		return(FALSE);
	else
		*p = 0;
	p = strrchr(buf, '(');
	if (p == NULL)
		return(FALSE);
	else
	{
		pFilemasks = &filemasks;
		while (pFilemasks->next != NULL)
			pFilemasks = pFilemasks->next;
		pFilemasks->next = new struct FILEMASKS_STRUCT;
		pFilemasks = pFilemasks->next;
		pFilemasks->next = NULL;
		strcpy(pFilemasks->text, filemask);
	}
	
	return (TRUE);
}


void CFilePickCtrl::DeleteFilemasks()
{
	struct FILEMASKS_STRUCT		*pTempFilemasks;
	pFilemasks = filemasks.next;
	while (pFilemasks != NULL)
	{
		pTempFilemasks = pFilemasks->next;
		delete pFilemasks;
		pFilemasks = pTempFilemasks;
	}
	filemasks.next = NULL;
	m_nCurrentTypesSel = 0;
}


void CFilePickCtrl::OnSelChangeFilePickCtrlTypes() 
{
	m_nCurrentTypesSel = m_typesCtrl.GetCurSel();
	PopulateFilePickCtrl();
}


void CFilePickCtrl::OnSelChangeFilePickCtrlDrives() 
{
	m_nCurrentDrivesSel = m_drivesCtrl.GetCurSel();
	strcpy(m_startPath, (char *) m_drivesCtrl.GetItemDataPtr(m_nCurrentDrivesSel));
	PopulateFilePickCtrl();
}


void CFilePickCtrl::PopulateFilePickCtrl()
{
	LVITEM				lvi;
	WIN32_FIND_DATA		findData;
	char				searchPath[MAX_PATH];
	char				buf[MAX_PATH];
	int					folderIconIndex;

	m_nItems = 0;
	dirData.next = NULL;
	m_nDirItems = 0;
	fileData.next = NULL;
	m_nFileItems = 0;
	m_nLongestStringWidth = 0;
	m_filesCtrl.DeleteAllItems();
	sprintf(searchPath, "%s*", m_startPath);
	GetDlgItem(IDC_FILEPICKCTRL_CURRENTDIR)->SetWindowText(m_startPath);

// If it's the root of a drive, then don't add an "Up one directory" item.
	GetWindowsDirectory(buf, MAX_PATH);
	folderIconIndex = GetIconIndex(buf, TRUE);

	lvi.mask = LVIF_TEXT | LVIF_IMAGE | LVIF_PARAM;
	lvi.iSubItem = 0;
	lvi.iItem = m_nItems;
	lvi.iImage = folderIconIndex;
	if (strlen(m_startPath) != 3)
	{
		lvi.pszText = "[Up one directory]";
		lvi.lParam = -1;
		if (m_filesCtrl.InsertItem(&lvi) == -1)
			AfxMessageBox("Error adding item!");
		else
		{
			m_nItems++;
			if (m_filesCtrl.GetStringWidth("[Up one directory]") > m_nLongestStringWidth)
				m_nLongestStringWidth = m_filesCtrl.GetStringWidth("[Up one directory]");
		}
	}
	else
	{
		lvi.pszText = "[Root]";
		lvi.lParam = -2;
		if (m_filesCtrl.InsertItem(&lvi) == -1)
			AfxMessageBox("Error adding item!");
		else
		{
			m_nItems++;
			if (m_filesCtrl.GetStringWidth("[Root]") > m_nLongestStringWidth)
				m_nLongestStringWidth = m_filesCtrl.GetStringWidth("[Root]");
		}
	}

	HANDLE h = FindFirstFile(searchPath, &findData);
	if (h != INVALID_HANDLE_VALUE)
	{
		pDirData = &dirData;

		do
		{
			if ((findData.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY) && findData.cFileName[0] != '.')
			{
				pDirData->next = new struct FILESCTRL_STRUCT;
				pDirData = pDirData->next;
				pDirData->next = NULL;
				pDirData->lParam = findData.dwFileAttributes;
				pDirData->iconIndex = folderIconIndex;
				strcpy(pDirData->text, findData.cFileName);
				StrToCapitalizedWords(pDirData->text);
				m_nDirItems++;
			}
		}
		while (FindNextFile(h, &findData));
	}
	FindClose(h);

// Now add files of the selected filemask type.
	if (!m_bDirOnly)
	{
		m_typesCtrl.GetLBText(m_typesCtrl.GetCurSel(), buf);
// All filemasks should be in the format "some text (filename.extension)", i.e. "All files (*.*)".
		char *p = strrchr(buf, '(');
		sprintf(searchPath, "%s%s", m_startPath, p + 1);
		p = strrchr(searchPath, ')');
		*p = 0;

		h = FindFirstFile(searchPath, &findData);
		if (h != INVALID_HANDLE_VALUE)
		{
			pFileData = &fileData;

			do
			{
				if (!(findData.dwFileAttributes & FILE_ATTRIBUTE_DIRECTORY))
				{
					pFileData->next = new struct FILESCTRL_STRUCT;
					pFileData = pFileData->next;
					pFileData->next = NULL;
					pFileData->lParam = findData.dwFileAttributes;
					sprintf(searchPath, "%s%s", m_startPath, findData.cFileName);
					pFileData->iconIndex = GetIconIndex(searchPath, FALSE);
					strcpy(pFileData->text, findData.cFileName);
					StrToCapitalizedWords(pFileData->text);
					m_nFileItems++;
				}
			}
			while (FindNextFile(h, &findData));
		}
	
		FindClose(h);
	}

	SortDataStructs();
	AddDataStructs();
	DeleteDataStructs();
}


int CFilePickCtrl::GetIconIndex(char *filename, BOOL bDirectory)
{               
	SHFILEINFO		ssfi;
	DWORD			attr;

	if (bDirectory)
		attr = FILE_ATTRIBUTE_DIRECTORY;
	else
		attr = FILE_ATTRIBUTE_NORMAL;

	if (m_bLargeFilesIcons)
		SHGetFileInfo(filename, attr, &ssfi, sizeof(SHFILEINFO), SHGFI_USEFILEATTRIBUTES | SHGFI_SYSICONINDEX | SHGFI_LARGEICON);
	else
		SHGetFileInfo(filename, attr, &ssfi, sizeof(SHFILEINFO), SHGFI_USEFILEATTRIBUTES | SHGFI_SYSICONINDEX | SHGFI_SMALLICON);

	return ssfi.iIcon;
}


void CFilePickCtrl::OnDblclkFilePickCtrlFiles(NMHDR *pNMHDR, LRESULT *pResult) 
{
	NMLISTVIEW *pNMLISTVIEW = (NMLISTVIEW *) pNMHDR;

	*pResult = 0;

	// check if an item is selected 
	if (pNMLISTVIEW->iItem == -1) 
		return;

// If the user double-clicked on a directory, then change to it and repopulate the file control.
	if (m_filesCtrl.GetItemData(pNMLISTVIEW->iItem) & FILE_ATTRIBUTE_DIRECTORY)
	{
// It's the "[Up one directory]" folder ...
		if (m_filesCtrl.GetItemData(pNMLISTVIEW->iItem) == -1)
		{
// Strip off the deepest directory name by searching for the 2nd to last occurence of a backslash,
// and put a null terminator after it.
			char	*p = strrchr(m_startPath, '\\');
			*p = 0;
			p = strrchr(m_startPath, '\\');
			*(p + 1) = 0;
		}
// If it's not the "[Root]" folder ...
		else if (m_filesCtrl.GetItemData(pNMLISTVIEW->iItem) != -2)
		{
			char	buf[MAX_PATH];
			m_filesCtrl.GetItemText(pNMLISTVIEW->iItem, 0, buf, MAX_PATH);
			strcat(m_startPath, buf);
			strcat(m_startPath, "\\");
		}

		PopulateFilePickCtrl();
	}
}


void CFilePickCtrl::StrToCapitalizedWords(char *s)
{
	strlwr(s);
	char *p = s;
	BOOL b = TRUE;

	while (*p != 0)
	{
		if (b)
		{
			if (*p >= 'a' && *p <= 'z')
				*p -= 32;
			b = FALSE;
		}
		p++;
		if (*(p-1) == ' ')
			b = TRUE;
	}
}


void CFilePickCtrl::SortDataStructs()
{
	int		i, j;

// Bubble sort the Directory items
	for (i = m_nDirItems - 1; i > 0; i--)
	{
		pDirData = dirData.next;
		for (j = 0; j < i; j++)
		{
			if (strcmp(pDirData->text, pDirData->next->text) > 0)
			{
				dirData.iconIndex = pDirData->iconIndex;
				dirData.lParam = pDirData->lParam;
				strcpy(dirData.text, pDirData->text);

				pDirData->iconIndex = pDirData->next->iconIndex;
				pDirData->lParam = pDirData->next->lParam;
				strcpy(pDirData->text, pDirData->next->text);

				pDirData->next->iconIndex = dirData.iconIndex;
				pDirData->next->lParam = dirData.lParam;
				strcpy(pDirData->next->text, dirData.text);
			}
			pDirData = pDirData->next;
		}
	}

// Bubble sort the File items
	if (!m_bDirOnly && m_nFileItems > 0)
	{
		for (i = m_nFileItems - 1; i > 0; i--)
		{
			pFileData = fileData.next;
			for (j = 0; j < i; j++)
			{
				if (strcmp(pFileData->text, pFileData->next->text) > 0)
				{
					fileData.iconIndex = pFileData->iconIndex;
					fileData.lParam = pFileData->lParam;
					strcpy(fileData.text, pFileData->text);

					pFileData->iconIndex = pFileData->next->iconIndex;
					pFileData->lParam = pFileData->next->lParam;
					strcpy(pFileData->text, pFileData->next->text);

					pFileData->next->iconIndex = fileData.iconIndex;
					pFileData->next->lParam = fileData.lParam;
					strcpy(pFileData->next->text, fileData.text);
				}
				pFileData = pFileData->next;
			}
		}
	}
}


void CFilePickCtrl::AddDataStructs()
{
	LVITEM				lvi;

	lvi.mask = LVIF_TEXT | LVIF_IMAGE | LVIF_PARAM;
	lvi.iSubItem = 0;

// First add the Directory items
	pDirData = dirData.next;
	while (pDirData != NULL)
	{
		lvi.iItem = m_nItems;
		lvi.iImage = pDirData->iconIndex;
		lvi.lParam = pDirData->lParam;
		lvi.pszText = pDirData->text;
		if (m_filesCtrl.InsertItem(&lvi) == -1)
		{
			AfxMessageBox("Error adding item!");
			break;
		}
		else
		{
			if (m_filesCtrl.GetStringWidth(pDirData->text) > m_nLongestStringWidth)
				m_nLongestStringWidth = m_filesCtrl.GetStringWidth(pDirData->text);
			m_nItems++;
			pDirData = pDirData->next;
		}
	}

// Then add the File items
	if (!m_bDirOnly)
	{
		pFileData = fileData.next;
		while (pFileData != NULL)
		{
			lvi.iItem = m_nItems;
			lvi.iImage = pFileData->iconIndex;
			lvi.lParam = pFileData->lParam;
			lvi.pszText = pFileData->text;
			if (m_filesCtrl.InsertItem(&lvi) == -1)
			{
				AfxMessageBox("Error adding item!");
				break;
			}
			else
			{
				if (m_filesCtrl.GetStringWidth(pFileData->text) > m_nLongestStringWidth)
					m_nLongestStringWidth = m_filesCtrl.GetStringWidth(pFileData->text);
				m_nItems++;
				pFileData = pFileData->next;
			}
		}
	}

	m_filesCtrl.SetColumnWidth(-1, 35 + m_nLongestStringWidth);
}


void CFilePickCtrl::DeleteDataStructs()
{
	struct FILESCTRL_STRUCT		*pTempFilesCtrl;

	m_nItems = 0;

	pDirData = dirData.next;
	while (pDirData != NULL)
	{
		pTempFilesCtrl = pDirData->next;
		delete pDirData;
		pDirData = pTempFilesCtrl;
	}
	dirData.next = NULL;
	m_nDirItems = 0;

	pFileData = fileData.next;
	while (pFileData != NULL)
	{
		pTempFilesCtrl = pFileData->next;
		delete pFileData;
		pFileData = pTempFilesCtrl;
	}
	fileData.next = NULL;
	m_nFileItems = 0;
}


void CFilePickCtrl::GetSelectedItem(char *buf)
{
	strcpy(buf, m_selectedFile);
}
