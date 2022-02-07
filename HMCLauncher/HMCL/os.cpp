#include "stdafx.h"
#include "os.h"

LSTATUS MyRegQueryValue(HKEY hKey, LPCWSTR subKey, DWORD dwType,
                        std::wstring &out) {
  DWORD dwSize = 0;
  LSTATUS ret = RegQueryValueEx(hKey, subKey, 0, &dwType, NULL, &dwSize);
  if (ret != ERROR_SUCCESS) return ret;
  WCHAR *buffer = new WCHAR[dwSize];
  ret = RegQueryValueEx(hKey, subKey, 0, &dwType, (LPBYTE)buffer, &dwSize);
  if (ret != ERROR_SUCCESS) return ret;
  out = buffer;
  delete[] buffer;
  return ERROR_SUCCESS;
}

LSTATUS MyGetModuleFileName(HMODULE hModule, std::wstring &out) {
  DWORD res, size = MAX_PATH;
  out = std::wstring();
  out.resize(size);
  while ((res = GetModuleFileName(hModule, &out[0], size)) == size) {
    out.resize(size += MAX_PATH);
  }
  if (res == 0)
    return GetLastError();
  else {
    out.resize(size - MAX_PATH + res);
    return ERROR_SUCCESS;
  }
}

LSTATUS MyGetEnvironmentVariable(LPCWSTR name, std::wstring &out) {
  DWORD res, size = MAX_PATH;
  out = std::wstring();
  out.resize(size);
  while ((res = GetEnvironmentVariable(name, &out[0], size)) == size) {
    out.resize(size += MAX_PATH);
  }
  if (res == 0)
    return GetLastError();
  else {
    out.resize(size - MAX_PATH + res);
    return ERROR_SUCCESS;
  }
}

bool MyCreateProcess(const std::wstring &command, const std::wstring &workdir) {
  std::wstring writable_command = command;
  STARTUPINFO si;
  PROCESS_INFORMATION pi;
  si.cb = sizeof(si);
  ZeroMemory(&si, sizeof(si));
  ZeroMemory(&pi, sizeof(pi));

  if (workdir.empty()) {
    return CreateProcess(NULL, &writable_command[0], NULL, NULL, false,
                         NORMAL_PRIORITY_CLASS, NULL, NULL, &si, &pi);
  } else {
    return CreateProcess(NULL, &writable_command[0], NULL, NULL, false,
                         NORMAL_PRIORITY_CLASS, NULL, workdir.c_str(), &si,
                         &pi);
  }
}

bool FindFirstFileExists(LPCWSTR lpPath, DWORD dwFilter) {
  WIN32_FIND_DATA fd;
  HANDLE hFind = FindFirstFile(lpPath, &fd);
  bool bFilter = (false == dwFilter) ? true : fd.dwFileAttributes & dwFilter;
  bool ret = ((hFind != INVALID_HANDLE_VALUE) && bFilter) ? true : false;
  FindClose(hFind);
  return ret;
}

bool MyGetFileVersionInfo(const std::wstring &filePath, Version &version) {
  DWORD verHandle = 0;
  UINT size = 0;
  LPBYTE lpBuffer = NULL;
  VS_FIXEDFILEINFO *pFileInfo;
  DWORD dwSize = GetFileVersionInfoSize(filePath.c_str(), NULL);

  if (!dwSize) return false;

  LPBYTE data = new BYTE[dwSize];
  if (!GetFileVersionInfo(filePath.c_str(), 0, dwSize, data)) {
    delete[] data;
    return false;
  }

  if (!VerQueryValue(data, TEXT("\\"), (LPVOID *)&pFileInfo, &size)) {
    delete[] data;
    return false;
  }

  version = Version{(pFileInfo->dwFileVersionMS >> 16) & 0xFFFF,
                    (pFileInfo->dwFileVersionMS >> 0) & 0xFFFF,
                    (pFileInfo->dwFileVersionLS >> 16) & 0xFFFF,
                    (pFileInfo->dwFileVersionLS >> 0) & 0xFFFF};
  return true;
}

HRESULT MySHGetFolderPath(int csidl, std::wstring &out) {
  out = std::wstring();
  out.resize(MAX_PATH);

  HRESULT res = SHGetFolderPath(NULL, CSIDL_APPDATA, NULL, 0, &out[0]);
  if (SUCCEEDED(res)) {
    out.resize(wcslen(&out[0]));
  } else {
    out.resize(0);
  }
  return res;
}

void MyPathAppend(std::wstring &filePath, const std::wstring &more) {
  if (filePath.back() != L'\\') {
    filePath += L'\\';
  }

  filePath += more;
}

void MyPathAddBackslash(std::wstring &filePath) {
  if (filePath.back() != L'\\') {
    filePath += L'\\';
  }
}

LSTATUS MyGetTempPath(std::wstring &out) {
  out = std::wstring();
  out.resize(MAX_PATH);

  DWORD res = GetTempPath(MAX_PATH, &out[0]);
  if (res == 0) {
    return GetLastError();
  }
  out.resize(res);
  return ERROR_SUCCESS;
}

LSTATUS MyGetTempFileName(const std::wstring &pathName, const std::wstring &prefixString, std::wstring &out) {
  out = std::wstring();
  out.resize(MAX_PATH);

  if (GetTempFileName(pathName.c_str(), prefixString.c_str(), 0, &out[0]) == 0) {
    out.resize(0);
    return GetLastError();
  }
  out.resize(wcslen(&out[0]));
  return ERROR_SUCCESS;
}

void MyAppendPathToCommandLine(std::wstring &commandLine, const std::wstring &path) {
  commandLine += L'"';
  for (WCHAR ch : path) {
    if (ch == L'\\') {
      commandLine += L"\\\\";
    } else if (ch == L'"') {
      commandLine += L"\\\"";
    } else {
      commandLine += ch;
    }
  }
  commandLine += L'"';
}