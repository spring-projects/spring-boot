SET "JAVA_HOME=C:\opt\jdk-8"
SET PATH=%PATH%;C:\Program Files\Git\usr\bin
cd git-repo
.\gradlew --no-daemon --max-workers=4 --quiet build
