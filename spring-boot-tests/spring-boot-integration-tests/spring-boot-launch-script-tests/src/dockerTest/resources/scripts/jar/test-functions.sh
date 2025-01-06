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

launch_jar() {
  ./app.jar $@ &
}
