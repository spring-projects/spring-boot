#!/bin/bash
command -v node >/dev/null 2>&1 || { echo >&2 "Nx requires NodeJS to be available. To install NodeJS and NPM, see: https://nodejs.org/en/download/ ."; exit 1; }
command -v npm >/dev/null 2>&1 || { echo >&2 "Nx requires npm to be available. To install NodeJS and NPM, see: https://nodejs.org/en/download/ ."; exit 1; }
path_to_root=$(dirname $BASH_SOURCE)
node $path_to_root/.nx/nxw.js $@