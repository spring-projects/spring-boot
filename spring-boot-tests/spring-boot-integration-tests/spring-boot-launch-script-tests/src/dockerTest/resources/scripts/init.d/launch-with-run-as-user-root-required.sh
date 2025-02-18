source ./test-functions.sh
install_service

useradd wagner
echo 'RUN_AS_USER=wagner' > /test-service/spring-boot-app.conf
echo "JAVA_HOME='$JAVA_HOME'" >> /test-service/spring-boot-app.conf

su - wagner -c "$(which service) spring-boot-app start"
echo "Status: $?"
