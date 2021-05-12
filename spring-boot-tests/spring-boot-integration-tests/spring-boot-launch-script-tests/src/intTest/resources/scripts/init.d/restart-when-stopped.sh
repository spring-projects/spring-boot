source ./test-functions.sh
install_service
restart_service
echo "Status: $?"
echo "PID: $(cat /var/run/spring-boot-app/spring-boot-app.pid)"
