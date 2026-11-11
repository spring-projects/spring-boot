/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docs.howto.httpclients.webclientreactornettycustomization;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;

@Configuration(proxyBeanMethods = false)
public class MyReactorNettyClientConfiguration {

	@Bean
	ClientHttpConnector clientHttpConnector(ReactorResourceFactory resourceFactory) {
		// @formatter:off
		HttpClient httpClient = HttpClient.create(resourceFactory.getConnectionProvider())
				.runOn(resourceFactory.getLoopResources())
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
				.doOnConnected((connection) -> connection.addHandlerLast(new ReadTimeoutHandler(60)));
		return new ReactorClientHttpConnector(httpClient);
		// @formatter:on
	}

}
