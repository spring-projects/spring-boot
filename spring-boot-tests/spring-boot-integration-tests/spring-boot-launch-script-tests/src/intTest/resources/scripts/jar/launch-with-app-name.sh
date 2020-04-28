source ./test-functions.sh
source ./jar/test-functions.sh
export APP_NAME="my-new-app"
export MODE=service
./app.jar start
TEST_LOG_FILE="/var/log/$APP_NAME.log"
if [[ (! (-e "$TEST_LOG_FILE")) || (! (-f "$TEST_LOG_FILE")) ]]; then
  echo Log file "$TEST_LOG_FILE" doesn\'t exists.
  exit 2
else
  echo Test for a log file is passed.
fi
TEST_PID_FOLDER="/var/run/$APP_NAME"
if [[ (! (-e "$TEST_PID_FOLDER")) || (! (-d "$TEST_PID_FOLDER")) ]]; then
  echo PID folder "$TEST_PID_FOLDER" doesn\'t exists.
  exit 2
else
  echo Test for a PID folder is passed.
fi
TEST_PID_FILE="$TEST_PID_FOLDER/$APP_NAME.pid"
if [[ (! (-e "$TEST_PID_FILE")) || (! (-f "$TEST_PID_FILE"))]]; then
  echo PID file "$TEST_PID_FILE" doesn\'t exists.
  exit 2
else
  echo Test for a PID file is passed.
fi
echo All tests are passed.
