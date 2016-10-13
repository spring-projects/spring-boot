install_service() {
  mkdir /test-service
  mv /spring-boot-launch-script-tests-*.jar /test-service/spring-boot-app.jar
  chmod +x /test-service/spring-boot-app.jar
  ln -s /test-service/spring-boot-app.jar /etc/init.d/spring-boot-app
}

start_service() {
  service spring-boot-app start $@
}

restart_service() {
  service spring-boot-app restart
}

status_service() {
  service spring-boot-app status
}

stop_service() {
  service spring-boot-app stop
}

await_app() {
  if [ -z $1 ]
  then
    url=http://127.0.0.1:8080
  else
    url=$1
  fi
  end=$(date +%s)
  let "end+=30"
  until curl -s $url > /dev/null
  do
    now=$(date +%s)
    if [[ $now -ge $end ]]; then
      break
    fi
    sleep 1
  done
}
