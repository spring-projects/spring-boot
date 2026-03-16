# This workflow will build a package using Maven and then publish it to GitHub packages when a release is created
# For more information see: https://github.com/actions/setup-java/blob/main/docs/advanced-usage.md#apache-maven-with-a-settings-path

name: Maven Package

on:
  release:
    types: [created]

jobs:
  build:

    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
    - uses: actions/checkout@v4
    - name: Set up JDK 11
      uses: actions/setup-java@v4
      with:
        java-version: '11'
        distribution: 'temurin'
        server-id: github # Value of the distributionManagement/repository/id field of the pom.xml
        settings-path: ${{ github.workspace }} # location for the settings.xml file

    - name: Build with Maven
      run: mvn -B package --file pom.xml

    - name: Publish to GitHub Packages Apache Maven
      run: mvn deploy -s $GITHUB_WORKSPACE/settings.xml
      env:
        GITHUB_TOKEN: ${{ github.token }}
