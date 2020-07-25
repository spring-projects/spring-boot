SET "JAVA_HOME=C:\opt\jdk-8"
SET PATH=%PATH%;C:\Program Files\Git\usr\bin
cd git-repo

echo ".\mvnw clean install" > build.log
.\mvnw clean install -U  -Duser.name=concourse >> build.log 2>&1 || (sleep 1 && tail -n 3000 build.log && exit 1)