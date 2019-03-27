require 'formula'

class Springboot < Formula
  homepage 'https://projects.spring.io/spring-boot/'
  url 'https://repo.spring.io/${repo}/org/springframework/boot/spring-boot-cli/${project.version}/spring-boot-cli-${project.version}-bin.tar.gz'
  version '${project.version}'
  sha256 '${checksum}'
  head 'https://github.com/spring-projects/spring-boot.git'

  if build.head?
    depends_on 'maven' => :build
  end

  def install
    if build.head?
      Dir.chdir('spring-boot-cli') { system 'mvn -U -DskipTests=true package' }
      root = 'spring-boot-cli/target/spring-boot-cli-*-bin/spring-*'
    else
      root = '.'
    end

    bin.install Dir["#{root}/bin/spring"]
    lib.install Dir["#{root}/lib/spring-boot-cli-*.jar"]
    bash_completion.install Dir["#{root}/shell-completion/bash/spring"]
    zsh_completion.install Dir["#{root}/shell-completion/zsh/_spring"]
  end
end
