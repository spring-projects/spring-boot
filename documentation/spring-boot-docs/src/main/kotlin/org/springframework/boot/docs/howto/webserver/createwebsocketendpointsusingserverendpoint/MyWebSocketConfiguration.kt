/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.webserver.createwebsocketendpointsusingserverendpoint

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.server.standard.ServerEndpointExporter

@Configuration(proxyBeanMethods = false)
class MyWebSocketConfiguration {

	@Bean
	fun serverEndpointExporter(): ServerEndpointExporter {
		return ServerEndpointExporter()
	}

}

