"use strict";
// This file should be committed to your repository! It wraps Nx and ensures
// that your local installation matches nx.json.
// See: https://nx.dev/recipes/installation/install-non-javascript for more info.




Object.defineProperty(exports, "__esModule", { value: true });
const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const installationPath = path.join(__dirname, 'installation', 'package.json');
function matchesCurrentNxInstall(currentInstallation, nxJsonInstallation) {
    if (!currentInstallation.devDependencies ||
        !Object.keys(currentInstallation.devDependencies).length) {
        return false;
    }
    try {
        if (currentInstallation.devDependencies['nx'] !==
            nxJsonInstallation.version ||
            require(path.join(path.dirname(installationPath), 'node_modules', 'nx', 'package.json')).version !== nxJsonInstallation.version) {
            return false;
        }
        for (const [plugin, desiredVersion] of Object.entries(nxJsonInstallation.plugins || {})) {
            if (currentInstallation.devDependencies[plugin] !== desiredVersion) {
                return false;
            }
        }
        return true;
    }
    catch {
        return false;
    }
}
function ensureDir(p) {
    if (!fs.existsSync(p)) {
        fs.mkdirSync(p, { recursive: true });
    }
}
function getCurrentInstallation() {
    try {
        return require(installationPath);
    }
    catch {
        return {
            name: 'nx-installation',
            version: '0.0.0',
            devDependencies: {},
        };
    }
}
function performInstallation(currentInstallation, nxJson) {
    fs.writeFileSync(installationPath, JSON.stringify({
        name: 'nx-installation',
        devDependencies: {
            nx: nxJson.installation.version,
            ...nxJson.installation.plugins,
        },
    }));
    try {
        cp.execSync('npm i', {
            cwd: path.dirname(installationPath),
            stdio: 'inherit',
            windowsHide: false,
        });
    }
    catch (e) {
        // revert possible changes to the current installation
        fs.writeFileSync(installationPath, JSON.stringify(currentInstallation));
        // rethrow
        throw e;
    }
}
function ensureUpToDateInstallation() {
    const nxJsonPath = path.join(__dirname, '..', 'nx.json');
    let nxJson;
    try {
        nxJson = require(nxJsonPath);
        if (!nxJson.installation) {
            console.error('[NX]: The "installation" entry in the "nx.json" file is required when running the nx wrapper. See https://nx.dev/recipes/installation/install-non-javascript');
            process.exit(1);
        }
    }
    catch {
        console.error('[NX]: The "nx.json" file is required when running the nx wrapper. See https://nx.dev/recipes/installation/install-non-javascript');
        process.exit(1);
    }
    try {
        ensureDir(path.join(__dirname, 'installation'));
        const currentInstallation = getCurrentInstallation();
        if (!matchesCurrentNxInstall(currentInstallation, nxJson.installation)) {
            performInstallation(currentInstallation, nxJson);
        }
    }
    catch (e) {
        const messageLines = [
            '[NX]: Nx wrapper failed to synchronize installation.',
        ];
        if (e instanceof Error) {
            messageLines.push('');
            messageLines.push(e.message);
            messageLines.push(e.stack);
        }
        else {
            messageLines.push(e.toString());
        }
        console.error(messageLines.join('\n'));
        process.exit(1);
    }
}
if (!process.env.NX_WRAPPER_SKIP_INSTALL) {
    ensureUpToDateInstallation();
}

require('./installation/node_modules/nx/bin/nx');
