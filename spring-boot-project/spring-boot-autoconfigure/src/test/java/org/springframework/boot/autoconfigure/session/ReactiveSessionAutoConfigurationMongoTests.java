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

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.web.reactive.WebSessionIdResolverAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.http.ResponseCookie;
import org.springframework.session.data.mongo.ReactiveMongoSessionRepository;
import org.springframework.session.data.redis.ReactiveRedisSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mongo-specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Andy Wilkinson
 * @author Weix Sun
 */
class ReactiveSessionAutoConfigurationMongoTests extends AbstractSessionAutoConfigurationTests {

	private final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class))
			.withPropertyValues("spring.mongodb.embedded.version=3.5.5");

	@Test
	void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.session.store-type=mongodb")
				.withConfiguration(AutoConfigurations.of(EmbeddedMongoAutoConfiguration.class,
						MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class))
				.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner.withClassLoader(new FilteredClassLoader(ReactiveRedisSessionRepository.class))
				.withConfiguration(AutoConfigurations.of(EmbeddedMongoAutoConfiguration.class,
						MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class))
				.run(validateSpringSessionUsesMongo("sessions"));
	}

	@Test
	void defaultConfigWithCustomTimeout() {
		this.contextRunner.withPropertyValues("spring.session.store-type=mongodb", "spring.session.timeout=1m")
				.withConfiguration(AutoConfigurations.of(EmbeddedMongoAutoConfiguration.class,
						MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class))
				.run((context) -> {
					ReactiveMongoSessionRepository repository = validateSessionRepository(context,
							ReactiveMongoSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("maxInactiveIntervalInSeconds", 60);
				});
	}

	@Test
	void defaultConfigWithCustomSessionTimeout() {
		this.contextRunner.withPropertyValues("spring.session.store-type=mongodb", "server.reactive.session.timeout=1m")
				.withConfiguration(AutoConfigurations.of(EmbeddedMongoAutoConfiguration.class,
						MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class))
				.run((context) -> {
					ReactiveMongoSessionRepository repository = validateSessionRepository(context,
							ReactiveMongoSessionRepository.class);
					assertThat(repository).hasFieldOrPropertyWithValue("maxInactiveIntervalInSeconds", 60);
				});
	}

	@Test
	void mongoSessionStoreWithCustomizations() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(EmbeddedMongoAutoConfiguration.class,
						MongoAutoConfiguration.class, MongoDataAutoConfiguration.class,
						MongoReactiveAutoConfiguration.class, MongoReactiveDataAutoConfiguration.class))
				.withPropertyValues("spring.session.store-type=mongodb", "spring.session.mongodb.collection-name=foo")
				.run(validateSpringSessionUsesMongo("foo"));
	}

	@Test
	void sessionCookieConfigurationIsAppliedToAutoConfiguredWebSessionIdResolver() {
		AutoConfigurations autoConfigurations = AutoConfigurations.of(EmbeddedMongoAutoConfiguration.class,
				MongoAutoConfiguration.class, MongoDataAutoConfiguration.class, MongoReactiveAutoConfiguration.class,
				MongoReactiveDataAutoConfiguration.class, WebSessionIdResolverAutoConfiguration.class);
		this.contextRunner.withConfiguration(autoConfigurations).withUserConfiguration(Config.class)
				.withPropertyValues("spring.session.store-type=mongodb",
						"server.reactive.session.cookie.name:JSESSIONID",
						"server.reactive.session.cookie.domain:.example.com",
						"server.reactive.session.cookie.path:/example", "server.reactive.session.cookie.max-age:60",
						"server.reactive.session.cookie.http-only:false", "server.reactive.session.cookie.secure:false",
						"server.reactive.session.cookie.same-site:strict")
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
			assertThat(repository).hasFieldOrPropertyWithValue("maxInactiveIntervalInSeconds",
					ReactiveMongoSessionRepository.DEFAULT_INACTIVE_INTERVAL);
		};
	}

}
