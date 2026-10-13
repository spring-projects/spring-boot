/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.webserver.enablemultipleconnectors.tomcat

import org.apache.catalina.connector.Connector
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyTomcatConfiguration {

	@Bean
	fun connectorCustomizer(): WebServerFactoryCustomizer<TomcatServletWebServerFactory> {
		return WebServerFactoryCustomizer { tomcat: TomcatServletWebServerFactory ->
			tomcat.addAdditionalConnectors(createConnector())
		}
	}

	private fun createConnector(): Connector {
		val connector = Connector("org.apache.coyote.http11.Http11NioProtocol")
		connector.port = 8081
		return connector
	}

}
