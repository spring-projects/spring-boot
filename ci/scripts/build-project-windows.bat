SET "JAVA_HOME=C:\opt\jdk-17"
SET PATH=%PATH%;C:\Program Files\Git\usr\bin
cd git-repo
.\gradlew -Dorg.gradle.internal.launcher.welcomeMessageEnabled=false --no-daemon --max-workers=4 build
