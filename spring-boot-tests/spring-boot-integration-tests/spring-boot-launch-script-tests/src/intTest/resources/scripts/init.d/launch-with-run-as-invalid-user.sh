source ./test-functions.sh
install_service

echo 'RUN_AS_USER=johndoe' > /test-service/spring-boot-app.conf

start_service
echo "Status: $?"
