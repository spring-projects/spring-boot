/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.webclient.test.autoconfigure;

import java.io.IOException;
import java.io.UncheckedIOException;

import okhttp3.mockwebserver.MockWebServer;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.webclient.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Configuration for a {@link MockWebServer}.
 *
 * @author Andy Wilkinson
 */
@Configuration(proxyBeanMethods = false)
public class MockWebServerConfiguration implements DisposableBean, WebClientCustomizer {

	private final MockWebServer mockWebServer = new MockWebServer();

	MockWebServerConfiguration() {
		try {
			this.mockWebServer.start();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public void destroy() {
		try {
			this.mockWebServer.shutdown();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	@Override
	public void customize(WebClient.Builder webClientBuilder) {
		webClientBuilder.baseUrl(this.mockWebServer.url("/").toString());
	}

	@Bean
	MockWebServer mockWebServer() {
		return this.mockWebServer;
	}

}
