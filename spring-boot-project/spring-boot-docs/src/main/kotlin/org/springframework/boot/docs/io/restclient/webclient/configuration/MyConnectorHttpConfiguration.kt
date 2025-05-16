package org.springframework.boot.docs.io.restclient.clienthttprequestfactory.configuration

import org.springframework.boot.http.client.reactive.ClientHttpConnectorBuilder;
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.ProxySelector
import java.net.http.HttpClient

@Configuration(proxyBeanMethods = false)
class MyConnectorHttpConfiguration {

	@Bean
	fun clientHttpConnectorBuilder(proxySelector: ProxySelector): ClientHttpConnectorBuilder<*> {
		return ClientHttpConnectorBuilder.jdk().withHttpClientCustomizer { builder -> builder.proxy(proxySelector) }
	}

}