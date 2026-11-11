/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.webserver.enablemultipleconnectors.jetty

import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.springframework.boot.jetty.ConfigurableJettyWebServerFactory
import org.springframework.boot.jetty.JettyServerCustomizer
import org.springframework.boot.web.server.WebServerFactoryCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class MyJettyConfiguration {

	@Bean
	fun connectorCustomizer(): WebServerFactoryCustomizer<ConfigurableJettyWebServerFactory> {
		return WebServerFactoryCustomizer { jetty: ConfigurableJettyWebServerFactory ->
			jetty.addServerCustomizers(JettyServerCustomizer { server: Server ->
				val connector = ServerConnector(server)
				connector.setPort(8081)
				server.addConnector(connector)
			})
		}
	}

}
