source ./test-functions.sh
launch_jar --server.port=8081 --server.servlet.context-path=/test
await_app http://127.0.0.1:8081/test/
curl -s http://127.0.0.1:8081/test/
