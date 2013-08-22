#!/bin/bash -e

if [ "$1" == "-x" ]; then set -x && shift; fi

if [ -e "$HOME/.rvm/scripts/rvm" ]; then
    source "$HOME/.rvm/scripts/rvm"
    rvm 1.9.3
fi

# function to convert a plain .md file to one that renders nicely in gh-pages
function convert {
    # sed - convert links with *.md to *.html (assumed relative links in local pages)
    sed -e 's/(\(.*\)\.md\(#*.*\))/(\1.html\2)/g' -e 's,(\(spring-boot[^)}.]*\)),({{site.github}}/tree/master/\1),g' -e 's/^\(```[a-zA-Z0-9]+\)/\n\1/' "$1" > _temp && mv _temp "$1"
}

# function to add the jekyll header to a .md file
function add_header {
    dir=`echo ${1%/*} | sed -e "s,[^/]*,..,g"`
    cat $PAGES_HOME/_includes/header.txt | sed -e "s,^home: .*$,home: ${dir}/," > _temp
    cat "$1" >> _temp && mv _temp "$1"
}

if [ -z $VERSION ] && [ -f pom.xml ]; then
    VERSION=`grep '<version>' pom.xml | head -1 | sed -e 's/[^>]*>//' -e 's/<.*//'`
fi 

# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
	ls=`ls -ld "$PRG"`
	link=`expr "$ls" : '.*-> \(.*\)$'`
	if expr "$link" : '/.*' > /dev/null; then
		PRG="$link"
	else
		PRG=`dirname "$PRG"`"/$link"
	fi
done
SAVED="`pwd`"
cd "`dirname \"$PRG\"`" >&-
PAGES_HOME="`pwd -P`"
cd "$SAVED" >&-

function fail {
    echo Cannot build pages. The most likely explanation
    echo is missing ruby or gem dependencies.
    echo Please ensure that you have installed ruby 1.9.3
    echo and execute
    echo "    $ (cd $PAGES_HOME; bundle)"
    echo before trying again
}

trap fail EXIT

if ! [ -e $PAGES_HOME/Gemfile.lock ]; then
    exit 1
fi

TARGET=target/pages
mkdir -p $TARGET/_includes
cp -rf $PAGES_HOME/* $TARGET

cp README.md $TARGET/_includes
convert $TARGET/_includes/README.md

# Sync the files in all docs/* directories
###################################################################
(git ls-tree -r --name-only master | grep 'docs/' | egrep -v 'src/pages' | xargs tar -cf - ) | (cd $TARGET; tar -xvf -)

# Add all the module .md files if there are any
###################################################################
(git ls-tree -r --name-only master | grep '.md$' | egrep -v 'src/pages' | xargs tar -cf - ) | (cd $TARGET; tar -xvf -)
for file in `git ls-tree -r --name-only master | grep '.md$' | egrep -v 'src/pages'`
do
    [ -f ${file} ] && convert $TARGET/"${file}"
    [ -f ${file} ] && add_header $TARGET/"${file}"
done

cd $TARGET

if [ -f _config.yml ] && [ ! -z $VERSION ]; then
    sed -i -e "s/version: .*/version: $VERSION/" _config.yml
fi

[ "$1" == "build" ] || [ "$1" == "serve" ] || ARGS="serve -w"

jekyll $ARGS $*
