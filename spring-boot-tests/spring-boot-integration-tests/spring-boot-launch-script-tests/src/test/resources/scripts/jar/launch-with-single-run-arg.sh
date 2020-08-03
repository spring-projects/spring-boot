source ./test-functions.sh
echo 'RUN_ARGS=--server.port=8081' > /spring-boot-launch-script-tests.conf
launch_jar
await_app http://127.0.0.1:8081/
curl -s http://127.0.0.1:8081/
