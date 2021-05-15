export DEBUG=true
source ./test-functions.sh
launch_jar
await_app
curl -s http://127.0.0.1:8080/
