package org.springframework.boot.docs.io.restclient.clienthttprequestfactory.configuration

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.ProxySelector
import java.net.http.HttpClient

@Configuration(proxyBeanMethods = false)
class MyClientHttpConfiguration {

	@Bean
	fun clientHttpRequestFactoryBuilder(proxySelector: ProxySelector): ClientHttpRequestFactoryBuilder<*> {
		return ClientHttpRequestFactoryBuilder.jdk()
				.withHttpClientCustomizer { builder -> builder.proxy(proxySelector) }
	}

}