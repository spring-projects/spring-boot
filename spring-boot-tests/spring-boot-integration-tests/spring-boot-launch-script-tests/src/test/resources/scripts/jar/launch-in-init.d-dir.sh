source ./test-functions.sh
source ./jar/test-functions.sh
cd "init.d" || exit 1
ln -s ../spring-boot-launch-script-tests.jar some_app
./some_app
