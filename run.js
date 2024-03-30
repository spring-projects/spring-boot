;(function () {
  'use strict'

  const childProcess = require('child_process')
  const fs = require('fs');

  async function main() {
    try {
      checkout(process.argv.includes('--no-checkout'))
      if (!process.argv.includes('--only-checkout')) {
        install(process.argv.includes('--no-install'))
        run(process.argv.includes('--no-run'))
      }
    } catch (error) {
      console.log("Unexpected error")
      process.exitCode = (error.exitCode) ? error.exitCode : 1
    }
  }

  function checkout(skip) {
    if (skip) return
    console.log('Checking out Antora package.json files from `main`')
    const packageJson = childProcess.execSync('git show main:antora/package.json', {env: process.env})
    const packageLockJson = childProcess.execSync('git show main:antora/package-lock.json', {env: process.env})
    fs.writeFileSync('package.json', packageJson)
    fs.writeFileSync('package-lock.json', packageLockJson)
  }

  function install(skip) {
    if (skip) return
    console.log('Installing modules')
    childProcess.execSync('npm ci --silent --no-progress', {stdio: 'inherit', env: process.env})
  }

  function run(skip) {
    const packageJson = JSON.parse(fs.readFileSync('package.json', 'utf8'))
    const uiBundleUrl = packageJson.config['ui-bundle-url'];
    const command = `npx antora antora-playbook.yml --stacktrace --ui-bundle-url ${uiBundleUrl}`
    if (uiBundleUrl.includes('/latest/')) {
      console.log('Refusing to run Antora with development build of UI')
      console.log(`$ ${command}`)
      process.exitCode = 1
      return
    }
    console.log((!skip) ? 'Running Antora' : 'Use the following command to run Antora')
    console.log(`$ ${command}`)
    if (!skip) childProcess.execSync(command, {stdio: 'inherit', env: process.env})
  }

  main()

})()