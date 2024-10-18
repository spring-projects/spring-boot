echo "Reclaiming Docker Disk Space"
echo

docker image ls --format "{{.Size}} {{.ID}} {{.Repository}} {{.Tag}}" | LANG=en_US sort -rh | while read line; do
	size=$( echo "$line" | cut -d' ' -f1 | sed -e 's/\.[0-9]*//' | sed -e 's/MB/000000/' | sed -e 's/GB/000000000/' )
	image=$( echo "$line" | cut -d' ' -f2 )
	repository=$( echo "$line" | cut -d' ' -f3 )
	tag=$( echo "$line" | cut -d' ' -f4 )
	if [ "$tag" != "<none>" ]; then
		if [ "$size" -gt 200000000 ]; then
			echo "Cleaning $image $repository:$size"
			docker image rm $image
		fi
	fi
done

echo "Finished cleanup, leaving the following containers:"
echo
docker image ls --format "{{.Size}} {{.ID}} {{.Repository}}:{{.Tag}}" | LANG=en_US sort -rh
echo
df -h
echo
