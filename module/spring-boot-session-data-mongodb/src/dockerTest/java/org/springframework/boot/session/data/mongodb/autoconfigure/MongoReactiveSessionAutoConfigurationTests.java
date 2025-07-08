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

package org.springframework.boot.session.data.mongodb.autoconfigure;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.mongodb.autoconfigure.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.mongodb.autoconfigure.MongoReactiveAutoConfiguration;
import org.springframework.boot.session.autoconfigure.AbstractReactiveSessionAutoConfigurationTests;
import org.springframework.boot.session.autoconfigure.SessionAutoConfiguration;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.boot.webflux.autoconfigure.WebSessionIdResolverAutoConfiguration;
import org.springframework.http.ResponseCookie;
import org.springframework.session.MapSession;
import org.springframework.session.data.mongo.ReactiveMongoSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for the reactive support of {@link MongoSessionAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Weix Sun
 */
@Testcontainers(disabledWithoutDocker = true)
class MongoReactiveSessionAutoConfigurationTests extends AbstractReactiveSessionAutoConfigurationTests {

	@Container
	static final MongoDBContainer mongoDb = TestImage.container(MongoDBContainer.class);

	@BeforeEach
	void prepareContextRunner() {
		this.contextRunner = new ReactiveWebApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(SessionAutoConfiguration.class, MongoSessionAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class));
	}

	@Test
	void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl())
			.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	void defaultConfigWithCustomTimeout() {
		this.contextRunner
			.withPropertyValues("spring.session.timeout=1m", "spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl())
			.run((context) -> {
				ReactiveMongoSessionRepository repository = validateSessionRepository(context,
						ReactiveMongoSessionRepository.class);
				assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval", Duration.ofMinutes(1));
			});
	}

	@Test
	void defaultConfigWithCustomSessionTimeout() {
		this.contextRunner
			.withPropertyValues("server.reactive.session.timeout=1m",
					"spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl())
			.run((context) -> {
				ReactiveMongoSessionRepository repository = validateSessionRepository(context,
						ReactiveMongoSessionRepository.class);
				assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval", Duration.ofMinutes(1));
			});
	}

	@Test
	void mongoSessionStoreWithCustomizations() {
		this.contextRunner
			.withPropertyValues("spring.session.mongodb.collection-name=foo",
					"spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl())
			.run(validateSpringSessionUsesMongo("foo"));
	}

	@Test
	void sessionCookieConfigurationIsAppliedToAutoConfiguredWebSessionIdResolver() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(WebSessionIdResolverAutoConfiguration.class))
			.withUserConfiguration(ReactiveWebServerConfiguration.class)
			.withPropertyValues("server.reactive.session.cookie.name:JSESSIONID",
					"server.reactive.session.cookie.domain:.example.com",
					"server.reactive.session.cookie.path:/example", "server.reactive.session.cookie.max-age:60",
					"server.reactive.session.cookie.http-only:false", "server.reactive.session.cookie.secure:false",
					"server.reactive.session.cookie.same-site:strict",
					"spring.data.mongodb.uri=" + mongoDb.getReplicaSetUrl())
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

	private ContextConsumer<AssertableReactiveWebApplicationContext> validateSpringSessionUsesMongo(
			String collectionName) {
		return (context) -> {
			ReactiveMongoSessionRepository repository = validateSessionRepository(context,
					ReactiveMongoSessionRepository.class);
			assertThat(repository.getCollectionName()).isEqualTo(collectionName);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval",
					MapSession.DEFAULT_MAX_INACTIVE_INTERVAL);
		};
	}

}
