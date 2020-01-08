/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.UndertowServletWebServerFactoryCustomizer.RequestLimiterHandlerWrapper;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServer;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.context.support.TestPropertySourceUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UndertowServletWebServerFactoryCustomizer}.
 *
 * @author Chris Bono
 */
class UndertowServletWebServerFactoryCustomizerTests {

	private UndertowServletWebServerFactoryCustomizer customizer;

	private MockEnvironment environment;

	private ServerProperties serverProperties;

	@BeforeEach
	void setup() {
		this.environment = new MockEnvironment();
		this.serverProperties = new ServerProperties();
		ConfigurationPropertySources.attach(this.environment);
		this.customizer = new UndertowServletWebServerFactoryCustomizer(this.serverProperties);
	}

	@Test
	void eagerFilterInitEnabled() {
		bind("server.undertow.eager-filter-init=true");
		UndertowServletWebServer server = customizeAndGetServer();
		assertThat(server.getDeploymentManager().getDeployment().getDeploymentInfo().isEagerFilterInit()).isTrue();
	}

	@Test
	void eagerFilterInitDisabled() {
		bind("server.undertow.eager-filter-init=false");
		UndertowServletWebServer server = customizeAndGetServer();
		assertThat(server.getDeploymentManager().getDeployment().getDeploymentInfo().isEagerFilterInit()).isFalse();
	}

	@Test
	void limiterNotAddedWhenMaxRequestsNotSet() {
		bind("server.undertow.max-requests=");
		UndertowServletWebServer server = customizeAndGetServer();
		assertThat(server.getDeploymentManager().getDeployment().getDeploymentInfo().getInitialHandlerChainWrappers()
				.stream().anyMatch(RequestLimiterHandlerWrapper.class::isInstance)).isFalse();
	}

	@Test
	void limiterAddedWhenMaxRequestSetWithDefaultMaxQueueCapacity() {
		bind("server.undertow.max-requests=200");
		UndertowServletWebServer server = customizeAndGetServer();
		RequestLimiterHandlerWrapper requestLimiter = server.getDeploymentManager().getDeployment().getDeploymentInfo()
				.getInitialHandlerChainWrappers().stream().filter(RequestLimiterHandlerWrapper.class::isInstance)
				.findFirst().map(RequestLimiterHandlerWrapper.class::cast).orElse(null);
		assertThat(requestLimiter).isNotNull();
		assertThat(requestLimiter.maxRequests).isEqualTo(200);
		assertThat(requestLimiter.maxQueueCapacity).isEqualTo(-1);
	}

	@Test
	void limiterAddedWhenMaxRequestSetWithCustomMaxQueueCapacity() {
		bind("server.undertow.max-requests=200", "server.undertow.max-queue-capacity=100");
		UndertowServletWebServer server = customizeAndGetServer();
		RequestLimiterHandlerWrapper requestLimiter = server.getDeploymentManager().getDeployment().getDeploymentInfo()
				.getInitialHandlerChainWrappers().stream().filter(RequestLimiterHandlerWrapper.class::isInstance)
				.findFirst().map(RequestLimiterHandlerWrapper.class::cast).orElse(null);
		assertThat(requestLimiter).isNotNull();
		assertThat(requestLimiter.maxRequests).isEqualTo(200);
		assertThat(requestLimiter.maxQueueCapacity).isEqualTo(100);
	}

	private void bind(String... inlinedProperties) {
		TestPropertySourceUtils.addInlinedPropertiesToEnvironment(this.environment, inlinedProperties);
		new Binder(ConfigurationPropertySources.get(this.environment)).bind("server",
				Bindable.ofInstance(this.serverProperties));
	}

	private UndertowServletWebServer customizeAndGetServer() {
		UndertowServletWebServerFactory factory = customizeAndGetFactory();
		return (UndertowServletWebServer) factory.getWebServer();
	}

	private UndertowServletWebServerFactory customizeAndGetFactory() {
		UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory(0);
		this.customizer.customize(factory);
		return factory;
	}

}
