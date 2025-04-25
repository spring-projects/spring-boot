@echo off

set /p registryUrl=

if "%registryUrl%" == "user.example.com" (
    echo {
    echo   "ServerURL": "%registryUrl%",
    echo   "Username": "username",
    echo   "Secret": "secret"
    echo }
    exit /b 0
)

if "%registryUrl%" == "token.example.com" (
    echo {
    echo   "ServerURL": "%registryUrl%",
    echo   "Username": "<token>",
    echo   "Secret": "secret"
    echo }
    exit /b 0
)

if "%registryUrl%" == "url.missing.example.com" (
    echo no credentials server URL >&2
    exit /b 1
)

if "%registryUrl%" == "username.missing.example.com" (
    echo no credentials username >&2
    exit /b 1
)

if "%registryUrl%" == "credentials.missing.example.com" (
    echo credentials not found in native keychain >&2
    exit /b 1
)

echo Unknown error >&2
exit /b 1
