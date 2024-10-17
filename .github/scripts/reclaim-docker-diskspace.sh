echo "Reclaiming Docker Disk Space"
echo

docker image ls --format "{{.Size}} {{.ID}}" | LANG=en_US sort -rh | while read line; do
	size=$( echo "$line" | cut -d' ' -f1 | sed -e 's/\.[0-9]*//' | sed -e 's/MB/000000/' | sed -e 's/MB/000000000/' )
	if [ $size -gt 200000000 ]; then
		image=$( echo "$line" | cut -d' ' -f2 )
		docker image rm -f $image
	fi
done

echo "Finished cleanup, leaving the following containers:"
echo
docker image ls --format "{{.Size}} {{.ID}} {{.Repository}}:{{.Tag}}" | LANG=en_US sort -rh
echo
df -h
echo
