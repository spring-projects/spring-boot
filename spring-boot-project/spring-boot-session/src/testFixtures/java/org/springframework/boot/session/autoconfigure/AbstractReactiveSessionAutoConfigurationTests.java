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

package org.springframework.boot.session.autoconfigure;

import java.util.Collections;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.web.context.reactive.ReactiveWebApplicationContext;
import org.springframework.boot.web.server.reactive.MockReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.session.ReactiveMapSessionRepository;
import org.springframework.session.ReactiveSessionRepository;
import org.springframework.session.config.annotation.web.server.EnableSpringWebSession;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for Spring Session auto-configuration tests when the backing store is
 * reactive.
 *
 * @author Andy Wilkinson
 */
public abstract class AbstractReactiveSessionAutoConfigurationTests {

	private static final MockReactiveWebServerFactory mockReactiveWebServerFactory = new MockReactiveWebServerFactory();

	protected ReactiveWebApplicationContextRunner contextRunner;

	@Test
	void backOffIfReactiveSessionRepositoryIsPresent() {
		this.contextRunner.withUserConfiguration(ReactiveSessionRepositoryConfiguration.class).run((context) -> {
			ReactiveMapSessionRepository repository = validateSessionRepository(context,
					ReactiveMapSessionRepository.class);
			assertThat(context).getBean("mySessionRepository").isSameAs(repository);
		});
	}

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

	protected <T extends ReactiveSessionRepository<?>> T validateSessionRepository(
			AssertableReactiveWebApplicationContext context, Class<T> type) {
		assertThat(context).hasSingleBean(WebSessionManager.class);
		assertThat(context).hasSingleBean(ReactiveSessionRepository.class);
		ReactiveSessionRepository<?> repository = context.getBean(ReactiveSessionRepository.class);
		assertThat(repository).as("Wrong session repository type").isInstanceOf(type);
		return type.cast(repository);
	}

	@Configuration(proxyBeanMethods = false)
	protected static class ReactiveWebServerConfiguration {

		@Bean
		MockReactiveWebServerFactory mockReactiveWebServerFactory() {
			return mockReactiveWebServerFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableSpringWebSession
	static class ReactiveSessionRepositoryConfiguration {

		@Bean
		ReactiveMapSessionRepository mySessionRepository() {
			return new ReactiveMapSessionRepository(Collections.emptyMap());
		}

	}

}
