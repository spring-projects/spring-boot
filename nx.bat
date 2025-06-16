@ECHO OFF
SETLOCAL
SET path_to_root=%~dp0
WHERE node >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (ECHO Nx requires NodeJS to be available. To install NodeJS and NPM, see: https://nodejs.org/en/download/ . & GOTO exit)
WHERE npm >nul 2>nul
IF %ERRORLEVEL% NEQ 0 (ECHO Nx requires npm to be available. To install NodeJS and NPM, see: https://nodejs.org/en/download/ . & GOTO exit)
node %path_to_root%\.nx\nxw.js %*
:exit
  cmd /c exit /b %ERRORLEVEL%