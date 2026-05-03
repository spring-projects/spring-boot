require 'formula'

class SpringBoot < Formula
  homepage 'https://spring.io/projects/spring-boot'
  url '${repo}/org/springframework/boot/spring-boot-cli/${version}/spring-boot-cli-${version}-bin.tar.gz'
  version '${version}'
  sha256 '${hash}'

  def install
    libexec.install Dir["./*"]
    (bin/"spring").write_env_script libexec/"bin/spring", {}

    bash_comp = libexec/"shell-completion/bash/spring"
    zsh_comp  = libexec/"shell-completion/zsh/_spring"

    bash_completion.install bash_comp if bash_comp.exist?
    zsh_completion.install  zsh_comp  if zsh_comp.exist?
  end
end
