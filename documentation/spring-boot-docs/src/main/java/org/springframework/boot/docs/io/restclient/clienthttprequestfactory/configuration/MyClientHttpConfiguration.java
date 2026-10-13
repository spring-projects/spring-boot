/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.io.restclient.clienthttprequestfactory.configuration;

import java.net.ProxySelector;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class MyClientHttpConfiguration {

	@Bean
	ClientHttpRequestFactoryBuilder<?> clientHttpRequestFactoryBuilder(ProxySelector proxySelector) {
		return ClientHttpRequestFactoryBuilder.jdk()
			.withHttpClientCustomizer((builder) -> builder.proxy(proxySelector));
	}

}
