/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.session;

import java.util.Collections;
import java.util.function.Consumer;

import org.springframework.boot.autoconfigure.web.reactive.MockReactiveWebServerFactory;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.session.MapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.SessionRepository;
import org.springframework.session.config.annotation.web.http.EnableSpringHttpSession;
import org.springframework.session.web.http.SessionRepositoryFilter;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Shared test utilities for {@link SessionAutoConfiguration} tests.
 *
 * @author Stephane Nicoll
 * @author Weix Sun
 */
public abstract class AbstractSessionAutoConfigurationTests {

	private static final MockReactiveWebServerFactory mockReactiveWebServerFactory = new MockReactiveWebServerFactory();

	protected ContextConsumer<ReactiveWebApplicationContext> assertExchangeWithSession(
			Consumer<MockServerWebExchange> exchange) {
		return (context) -> {
			MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
			MockServerWebExchange webExchange = MockServerWebExchange.from(request);
			WebSessionManager webSessionManager = context.getBean(WebSessionManager.class);
			WebSession webSession = webSessionManager.getSession(webExchange).block();
			webSession.start();
			webExchange.getResponse().setComplete().block();
			exchange.accept(webExchange);
		};
	}

	protected <T extends SessionRepository<?>> T validateSessionRepository(AssertableWebApplicationContext context,
			Class<T> type) {
		assertThat(context).hasSingleBean(SessionRepositoryFilter.class);
		assertThat(context).hasSingleBean(SessionRepository.class);
		SessionRepository<?> repository = context.getBean(SessionRepository.class);
		assertThat(repository).as("Wrong session repository type").isInstanceOf(type);
		return type.cast(repository);
	}

	protected <T extends ReactiveSessionRepository<?>> T validateSessionRepository(
			AssertableReactiveWebApplicationContext context, Class<T> type) {
		assertThat(context).hasSingleBean(WebSessionManager.class);
		assertThat(context).hasSingleBean(ReactiveSessionRepository.class);
		ReactiveSessionRepository<?> repository = context.getBean(ReactiveSessionRepository.class);
		assertThat(repository).as("Wrong session repository type").isInstanceOf(type);
		return type.cast(repository);
	}

	@Configuration
	@EnableSpringHttpSession
	static class SessionRepositoryConfiguration {

		@Bean
		MapSessionRepository mySessionRepository() {
			return new MapSessionRepository(Collections.emptyMap());
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		MockReactiveWebServerFactory mockReactiveWebServerFactory() {
			return mockReactiveWebServerFactory;
		}

	}

}
