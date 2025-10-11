/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
