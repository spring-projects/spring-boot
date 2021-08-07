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

import java.time.Duration;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.session.SessionAutoConfiguration.ReactiveSessionConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.MockReactiveWebServerFactory;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.boot.web.reactive.context.ReactiveWebApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.ResponseCookie;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebSession;
import org.springframework.web.server.session.WebSessionManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reactive Tests for {@link ReactiveSessionConfiguration}.
 *
 * @author Weix Sun
 */
@Testcontainers(disabledWithoutDocker = true)
class ReactiveSessionConfigurationTests {

	@Container
	public static RedisContainer redis = new RedisContainer().withStartupAttempts(5)
			.withStartupTimeout(Duration.ofMinutes(10));

	private static final MockReactiveWebServerFactory mockReactiveWebServerFactory = new MockReactiveWebServerFactory();

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class, RedisAutoConfiguration.class,
					RedisReactiveAutoConfiguration.class))
			.withUserConfiguration(Config.class).withPropertyValues("spring.session.store-type=redis",
					"spring.redis.host=" + redis.getHost(), "spring.redis.port=" + redis.getFirstMappedPort());

	@Test
	void sessionCookieConfigurationIsAppliedToAutoConfiguredWebSessionIdResolver() {
		this.contextRunner.withPropertyValues("spring.webflux.session.cookie.name:JSESSIONID",
				"spring.webflux.session.cookie.domain:.example.com", "spring.webflux.session.cookie.path:/example",
				"spring.webflux.session.cookie.max-age:60", "spring.webflux.session.cookie.http-only:false",
				"spring.webflux.session.cookie.secure:false", "spring.webflux.session.cookie.same-site:strict")
				.run(assertExchangeWithSession((exchange) -> {
					List<ResponseCookie> cookies = exchange.getResponse().getCookies().get("JSESSIONID");
					assertThat(cookies).isNotEmpty();
					assertThat(cookies).allMatch((cookie) -> cookie.getDomain().equals(".example.com"));
					assertThat(cookies).allMatch((cookie) -> cookie.getPath().equals("/example"));
					assertThat(cookies).allMatch((cookie) -> cookie.getMaxAge().equals(Duration.ofSeconds(60)));
					assertThat(cookies).allMatch((cookie) -> !cookie.isHttpOnly());
					assertThat(cookies).allMatch((cookie) -> !cookie.isSecure());
					assertThat(cookies).allMatch((cookie) -> cookie.getSameSite().equals("Strict"));
				}));
	}

	private ContextConsumer<ReactiveWebApplicationContext> assertExchangeWithSession(
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

	@Configuration(proxyBeanMethods = false)
	static class Config {

		@Bean
		MockReactiveWebServerFactory mockReactiveWebServerFactory() {
			return mockReactiveWebServerFactory;
		}

	}

}
