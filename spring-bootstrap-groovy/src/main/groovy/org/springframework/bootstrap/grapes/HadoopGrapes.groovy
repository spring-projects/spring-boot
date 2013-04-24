package org.springframework.bootstrap.grapes

@GrabConfig(systemClassLoader=true)
@Grab("org.springframework.data:spring-data-hadoop:1.0.0.RELEASE")
@Grab("org.springframework.bootstrap:spring-bootstrap:@@version@@")
@Grab("org.springframework:spring-context:4.0.0.BOOTSTRAP-SNAPSHOT")
@Grab("org.apache.hadoop:hadoop-examples:1.0.4")
@GrabExclude("org.mortbay.jetty:sevlet-api-2.5")
@GrabExclude("org.mortbay.jetty:jetty")
@GrabExclude("org.mortbay.jetty:jetty-util")
@GrabExclude("org.mortbay.jetty:jsp-2.1")
@GrabExclude("org.mortbay.jetty:jsp-api-2.1")
@GrabExclude("tomcat:jasper-runtime")
@GrabExclude("tomcat:jasper-compiler")
class HadoopGrapes { 
}

import org.springframework.bootstrap.context.annotation.ConditionalOnMissingBean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ImportResource

@Configuration
@ConditionalOnMissingBean(org.apache.hadoop.conf.Configuration)
@ImportResource("hadoop-context.xml")
class HadoopContext {
}
