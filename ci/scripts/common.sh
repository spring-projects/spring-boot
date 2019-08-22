source /opt/concourse-java.sh

setup_symlinks
if [[ -d $PWD/embedmongo && ! -d $HOME/.embedmongo ]]; then
	ln -s "$PWD/embedmongo" "$HOME/.embedmongo"
fi

cleanup_maven_repo "org.springframework.boot"
