source /opt/concourse-java.sh

setup_symlinks

cleanup_maven_repo "org.springframework.boot"

echo 'systemProp.user.name=concourse' > ~/.gradle/gradle.properties
