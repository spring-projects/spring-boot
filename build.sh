#!/bin/bash

if [ "$1" == "-x" ]; then set -x && shift; fi

# function to convert a plain .md file to one that renders nicely in gh-pages
function convert {
    # sed - convert links with *.md to *.html (assumed relative links in local pages)
    sed -e 's/(\(.*\)\.md\(#*.*\))/(\1.html\2)/g' -e 's,(\(spring-boot[^)}.]*\)),({{ site.github_repo_url }}/tree/master/\1),g' -e 's/^\(```[a-zA-Z0-9]+\)/\n\1/' "$1" > _temp && mv _temp "$1"
}

# function to add the jekyll header to a .md file
function add_header {
    dir=`echo ${1%/*} | sed -e "s,[^/]*,..,g"`
    cat _includes/header.txt | sed -e "s,^home: .*$,home: ${dir}/," > _temp
    cat "$1" >> _temp && mv _temp "$1"
}

if [ ! -e  code ]; then
    echo "Cannot find master symlink, please 'ln -s' to the spring boot code repo"
    echo "Alternatively use the following git commands to copy from master:"
    echo "git read-tree --prefix=code master -u"
    echo "git reset HEAD code"
    exit 1
fi

if [ -z $VERSION ] && [ -f code/pom.xml ]; then
    VERSION=`grep '<version>' code/pom.xml | head -1 | sed -e 's/[^>]*>//' -e 's/<.*//'`
fi 

if ! [ -e Gemfile.lock ]; then
    exit 1
fi

echo "Rebuilding pages using v$VERSION code"


echo "...updating version in _config"
if [ -f _config.yml ] && [ ! -z $VERSION ]; then
    sed -i ".bak" -e "s/version: .*/version: $VERSION/" _config.yml
fi

echo "...copying source files"
rm -fr docs
mkdir docs
rsync -avm --include '*/' --include '**/*.md' --exclude '*' code/ docs/ 

echo "...converting"
for file in `find docs`
do
    [ -f ${file} ] && convert "${file}"
    [ -f ${file} ] && add_header "${file}"
done

echo
echo "DONE"
echo
