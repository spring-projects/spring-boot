/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.httpclients.webclientreactornettycustomization

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ClientHttpConnector
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.client.ReactorResourceFactory
import reactor.netty.http.client.HttpClient

@Configuration(proxyBeanMethods = false)
class MyReactorNettyClientConfiguration {

	@Bean
	fun clientHttpConnector(resourceFactory: ReactorResourceFactory): ClientHttpConnector {
		val httpClient = HttpClient.create(resourceFactory.connectionProvider)
			.runOn(resourceFactory.loopResources)
			.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
			.doOnConnected { connection ->
				connection.addHandlerLast(ReadTimeoutHandler(60))
			}
		return ReactorClientHttpConnector(httpClient)
	}

}

