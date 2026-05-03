#!/bin/sh

read -r registryUrl

if [ "$registryUrl" = "user.example.com" ]; then
	cat <<EOF
{
  "ServerURL": "${registryUrl}",
  "Username": "username",
  "Secret": "secret"
}
EOF
	exit 0
fi

if [ "$registryUrl" = "token.example.com" ]; then
	cat <<EOF
{
  "ServerURL": "${registryUrl}",
  "Username": "<token>",
  "Secret": "secret"
}
EOF
	exit 0
fi

if [ "$registryUrl" = "url.missing.example.com" ]; then
  echo "no credentials server URL" >&2
	exit 1
fi

if [ "$registryUrl" = "username.missing.example.com" ]; then
  echo "no credentials username" >&2
	exit 1
fi

if [ "$registryUrl" = "credentials.missing.example.com" ]; then
  echo "credentials not found in native keychain" >&2
	exit 1
fi

echo "Unknown error" >&2
exit 1
