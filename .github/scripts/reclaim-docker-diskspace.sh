#!/bin/bash

echo "Reclaiming Docker Disk Space"
echo

docker image ls --format "{{.Size}} {{.ID}} {{.Repository}} {{.Tag}}" | LANG=en_US sort -rh | while read line; do
	size=$( echo "$line" | cut -d' ' -f1 | sed -e 's/\.[0-9]*//' | sed -e 's/MB/000000/' | sed -e 's/GB/000000000/' )
	image=$( echo "$line" | cut -d' ' -f2 )
	repository=$( echo "$line" | cut -d' ' -f3 )
	tag=$( echo "$line" | cut -d' ' -f4 )
	echo "Considering $image $repository:$tag $size"
	if [[ "$tag" =~ ^[a-f0-9]{32}$ ]]; then
		echo "Ignoring GitHub action image $image $repository:$tag"
	elif [[ "$tag" == "<none>" ]]; then
		echo "Ignoring untagged image $image $repository:$tag"
	elif [[ "$size" -lt 200000000 ]]; then
		echo "Ignoring small image $image $repository:$tag"
	else
		echo "Cleaning $image $repository:$tag"
		docker image rm $image
	fi
done

echo "Finished cleanup, leaving the following containers:"
echo
docker image ls --format "{{.Size}} {{.ID}} {{.Repository}}:{{.Tag}}" | LANG=en_US sort -rh
echo
df -h
echo
