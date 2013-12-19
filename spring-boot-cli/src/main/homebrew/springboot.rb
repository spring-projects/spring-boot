require 'formula'

class Springboot < Formula
	homepage 'http://projects.spring.io/spring-boot/'
	url 'https://repo.spring.io/${repo}/org/springframework/boot/spring-boot-cli/${version}/spring-boot-cli-${version}-bin.tar.gz'
	version '${version}'
	sha1 '${checksum}'

	def install
		bin.install 'bin/spring'
		lib.install 'lib/spring-boot-cli-${version}.jar'
		bash_completion.install 'shell-completion/bash/spring'
		zsh_completion.install 'shell-completion/zsh/_spring'
	end
end
