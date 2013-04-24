package org.springframework.bootstrap.grapes

// Spring stuff needs to be on the system classloader apparently (when using @Configuration)
@GrabResolver(name='spring-snapshot', root='http://repo.springframework.org/snapshot')
@GrabConfig(systemClassLoader=true)
@Grab("org.springframework:spring-context:@@dependency.springframework.version@@")
@Grab("org.springframework.bootstrap:spring-bootstrap:@@version@@")
@GrabExclude("commons-logging:commons-logging")
@Grab("org.slf4j:jcl-over-slf4j:1.6.1")
@Grab("org.slf4j:slf4j-jdk14:1.6.1")
class BootstrapGrapes { 
}

import org.springframework.bootstrap.context.annotation.EnableAutoConfiguration
import org.springframework.context.annotation.Configuration

@Configuration
// @EnableAutoConfiguration
class BootstrapAutoConfiguration { 
}