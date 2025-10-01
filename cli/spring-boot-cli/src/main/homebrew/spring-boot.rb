require 'formula'

class SpringBoot < Formula
  homepage 'https://spring.io/projects/spring-boot'
  url 'https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-cli/3.5.6/spring-boot-cli-3.5.6-bin.tar.gz'
  version '3.5.6'
  sha256 '3ac9314100c474ddad1c4ae04a85404383817d6f748820980e26ccbe55393bbe'
  head 'https://github.com/spring-projects/spring-boot.git', :branch => "main"

  def install
    if build.head?
      system './gradlew spring-boot-project:spring-boot-tools:spring-boot-cli:tar'
      system 'tar -xzf spring-boot-project/spring-boot-tools/spring-boot-cli/build/distributions/spring-* -C spring-boot-project/spring-boot-tools/spring-boot-cli/build/distributions'
      root = 'spring-boot-project/spring-boot-tools/spring-boot-cli/build/distributions/spring-*'
    else
      root = '.'
    end

    libexec.install Dir["#{root}/*"]
    
	(bin/"spring").write_env_script libexec/"bin/spring", {}

	bash_comp = libexec/"shell-completion/bash/spring"
	zsh_comp  = libexec/"shell-completion/zsh/_spring"

	bash_completion.install bash_comp if bash_comp.exist?
	zsh_completion.install  zsh_comp  if zsh_comp.exist?
  end
end
