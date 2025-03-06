/*
 * Copyright 2012-2024 the original author or authors.
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
import java.util.Map;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.reactive.WebSessionIdResolverAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.redis.connection.ReactiveRedisConnection;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.http.ResponseCookie;
import org.springframework.session.MapSession;
import org.springframework.session.SaveMode;
import org.springframework.session.data.mongo.ReactiveMongoSessionRepository;
import org.springframework.session.data.redis.ReactiveRedisIndexedSessionRepository;
import org.springframework.session.data.redis.ReactiveRedisSessionRepository;
import org.springframework.session.data.redis.config.ConfigureReactiveRedisAction;
import org.springframework.session.data.redis.config.annotation.ConfigureNotifyKeyspaceEventsReactiveAction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Reactive Redis-specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Vedran Pavic
 * @author Weix Sun
 */
@Testcontainers(disabledWithoutDocker = true)
class ReactiveSessionAutoConfigurationRedisTests extends AbstractSessionAutoConfigurationTests {

	@Container
	public static RedisContainer redis = TestImage.container(RedisContainer.class);

	protected final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
		.withClassLoader(new FilteredClassLoader(ReactiveMongoSessionRepository.class))
		.withConfiguration(
				AutoConfigurations.of(SessionAutoConfiguration.class, WebSessionIdResolverAutoConfiguration.class,
						RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class));

	@Test
	void defaultConfig() {
		this.contextRunner.run(validateSpringSessionUsesRedis("spring:session:", SaveMode.ON_SET_ATTRIBUTE));
	}

	@Test
	void redisTakesPrecedenceMultipleImplementations() {
		ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner().withConfiguration(
				AutoConfigurations.of(SessionAutoConfiguration.class, WebSessionIdResolverAutoConfiguration.class,
						RedisAutoConfiguration.class, RedisReactiveAutoConfiguration.class));
		contextRunner.run(validateSpringSessionUsesRedis("spring:session:", SaveMode.ON_SET_ATTRIBUTE));
	}

	@Test
	void defaultConfigWithCustomTimeout() {
		this.contextRunner.withPropertyValues("spring.session.timeout=1m").run((context) -> {
			ReactiveRedisSessionRepository repository = validateSessionRepository(context,
					ReactiveRedisSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval", Duration.ofMinutes(1));
		});
	}

	@Test
	void defaultConfigWithCustomWebFluxTimeout() {
		this.contextRunner.withPropertyValues("server.reactive.session.timeout=1m").run((context) -> {
			ReactiveRedisSessionRepository repository = validateSessionRepository(context,
					ReactiveRedisSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval", Duration.ofMinutes(1));
		});
	}

	@Test
	void redisSessionStoreWithCustomizations() {
		this.contextRunner
			.withPropertyValues("spring.session.redis.namespace=foo", "spring.session.redis.save-mode=on-get-attribute")
			.run(validateSpringSessionUsesRedis("foo:", SaveMode.ON_GET_ATTRIBUTE));
	}

	@Test
	void sessionCookieConfigurationIsAppliedToAutoConfiguredWebSessionIdResolver() {
		this.contextRunner.withUserConfiguration(Config.class)
			.withPropertyValues("spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort(),
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

	@Test
	void indexedRedisSessionDefaultConfig() {
		this.contextRunner
			.withPropertyValues("spring.session.redis.repository-type=indexed",
					"spring.data.redis.host=" + redis.getHost(), "spring.data.redis.port=" + redis.getFirstMappedPort())
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.run(validateSpringSessionUsesIndexedRedis("spring:session:", SaveMode.ON_SET_ATTRIBUTE));
	}

	@Test
	void indexedRedisSessionStoreWithCustomizations() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withPropertyValues("spring.session.redis.repository-type=indexed", "spring.session.redis.namespace=foo",
					"spring.session.redis.save-mode=on-get-attribute", "spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateSpringSessionUsesIndexedRedis("foo:", SaveMode.ON_GET_ATTRIBUTE));
	}

	@Test
	void indexedRedisSessionWithConfigureActionNone() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withPropertyValues("spring.session.redis.repository-type=indexed",
					"spring.session.redis.configure-action=none", "spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateStrategy(ConfigureReactiveRedisAction.NO_OP.getClass()));
	}

	@Test
	void indexedRedisSessionWithDefaultConfigureActionNone() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withPropertyValues("spring.session.redis.repository-type=indexed",
					"spring.data.redis.host=" + redis.getHost(), "spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateStrategy(ConfigureNotifyKeyspaceEventsReactiveAction.class,
					entry("notify-keyspace-events", "gxE")));
	}

	@Test
	void indexedRedisSessionWithCustomConfigureReactiveRedisActionBean() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withUserConfiguration(MaxEntriesReactiveRedisAction.class)
			.withPropertyValues("spring.session.redis.repository-type=indexed",
					"spring.data.redis.host=" + redis.getHost(), "spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateStrategy(MaxEntriesReactiveRedisAction.class, entry("set-max-intset-entries", "1024")));

	}

	private ContextConsumer<AssertableReactiveWebApplicationContext> validateSpringSessionUsesRedis(String namespace,
			SaveMode saveMode) {
		return (context) -> {
			ReactiveRedisSessionRepository repository = validateSessionRepository(context,
					ReactiveRedisSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval",
					MapSession.DEFAULT_MAX_INACTIVE_INTERVAL);
			assertThat(repository).hasFieldOrPropertyWithValue("namespace", namespace);
			assertThat(repository).hasFieldOrPropertyWithValue("saveMode", saveMode);
		};
	}

	private ContextConsumer<AssertableReactiveWebApplicationContext> validateSpringSessionUsesIndexedRedis(
			String keyNamespace, SaveMode saveMode) {
		return (context) -> {
			ReactiveRedisIndexedSessionRepository repository = validateSessionRepository(context,
					ReactiveRedisIndexedSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval",
					new ServerProperties().getReactive().getSession().getTimeout());
			assertThat(repository).hasFieldOrPropertyWithValue("namespace", keyNamespace);
			assertThat(repository).hasFieldOrPropertyWithValue("saveMode", saveMode);
		};
	}

	private ContextConsumer<AssertableReactiveWebApplicationContext> validateStrategy(
			Class<? extends ConfigureReactiveRedisAction> expectedConfigureReactiveRedisActionType,
			Map.Entry<?, ?>... expectedConfig) {
		return (context) -> {
			assertThat(context).hasSingleBean(ConfigureReactiveRedisAction.class);
			assertThat(context).hasSingleBean(RedisConnectionFactory.class);
			assertThat(context.getBean(ConfigureReactiveRedisAction.class))
				.isInstanceOf(expectedConfigureReactiveRedisActionType);
			ReactiveRedisConnection connection = context.getBean(ReactiveRedisConnectionFactory.class)
				.getReactiveConnection();
			if (expectedConfig.length > 0) {
				assertThat(connection.serverCommands().getConfig("*").block(Duration.ofSeconds(30)))
					.contains(expectedConfig);
			}
		};
	}

	static class MaxEntriesReactiveRedisAction implements ConfigureReactiveRedisAction {

		@Override
		public Mono<Void> configure(ReactiveRedisConnection connection) {
			return Mono.when(connection.serverCommands().setConfig("set-max-intset-entries", "1024"));
		}

	}

}
