require 'formula'

class Springboot < Formula
	homepage 'http://projects.spring.io/spring-boot/'
	url 'https://repo.spring.io/${repo}/org/springframework/boot/spring-boot-cli/${project.version}/spring-boot-cli-${project.version}-bin.tar.gz'
	version '${project.version}'
	sha1 '${checksum}'

	def install
		bin.install 'bin/spring'
		lib.install 'lib/spring-boot-cli-${project.version}.jar'
		bash_completion.install 'shell-completion/bash/spring'
		zsh_completion.install 'shell-completion/zsh/_spring'
	end
end
