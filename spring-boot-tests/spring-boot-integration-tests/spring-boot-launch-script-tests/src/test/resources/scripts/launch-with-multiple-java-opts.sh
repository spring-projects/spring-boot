source ./test-functions.sh
install_service
echo 'JAVA_OPTS="-Dserver.port=8081 -Dserver.servlet.context-path=/test"' > /test-service/spring-boot-app.conf
start_service
await_app http://127.0.0.1:8081/test/
curl -s http://127.0.0.1:8081/test/
