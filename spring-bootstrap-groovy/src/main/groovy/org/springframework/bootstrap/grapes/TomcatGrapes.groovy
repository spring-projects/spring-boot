package org.springframework.bootstrap.grapes

@GrabConfig(systemClassLoader=true)
// Grab some Tomcat dependencies
@Grab("org.apache.tomcat.embed:tomcat-embed-core:7.0.32")
// JULI logging has sensible defaults in JAVA_HOME, so no need for user to create it
@Grab("org.apache.tomcat.embed:tomcat-embed-logging-juli:7.0.32")
class TomcatGrapes { 
}