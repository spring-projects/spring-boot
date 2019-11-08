source ./test-functions.sh
install_service
start_service
echo "PID: $(cat /var/run/spring-boot-app/spring-boot-app.pid)"
start_service
echo "Status: $?"
