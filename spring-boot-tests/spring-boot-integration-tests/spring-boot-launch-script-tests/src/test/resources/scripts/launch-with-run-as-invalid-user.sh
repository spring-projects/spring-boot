source ./test-functions.sh
install_service

echo 'RUN_AS=johndoe' > /test-service/spring-boot-app.conf

start_service
echo "Status: $?"
