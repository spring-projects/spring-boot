require 'formula'

class SpringBoot < Formula
  homepage 'https://spring.io/projects/spring-boot'
  url '${repo}/org/springframework/boot/spring-boot-cli/${version}/spring-boot-cli-${version}-bin.tar.gz'
  version '${version}'
  sha256 '${hash}'
  head 'https://github.com/spring-projects/spring-boot.git', :branch => "main"

  def install
    if build.head?
      system './gradlew cli:spring-boot-cli:tar'
      system 'tar -xzf cli/spring-boot-cli/build/distributions/spring-* -C cli/spring-boot-cli/build/distributions'
      root = 'cli/spring-boot-cli/build/distributions/spring-*'
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
