source ./test-functions.sh
echo 'JAVA_OPTS="-Dserver.port=8081 -Dserver.servlet.context-path=/test"' > app.conf
launch_jar
await_app http://127.0.0.1:8081/test/
curl -s http://127.0.0.1:8081/test/
