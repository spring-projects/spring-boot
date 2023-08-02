/*
 * Copyright 2012-2023 the original author or authors.
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
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.source.InvalidConfigurationPropertyValueException;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.mongo.MongoIndexedSessionRepository;
import org.springframework.session.data.redis.RedisIndexedSessionRepository;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.data.redis.config.ConfigureNotifyKeyspaceEventsAction;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.hazelcast.HazelcastIndexedSessionRepository;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Redis specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
@Testcontainers(disabledWithoutDocker = true)
class SessionAutoConfigurationRedisTests extends AbstractSessionAutoConfigurationTests {

	@Container
	public static RedisContainer redis = new RedisContainer().withStartupAttempts(5)
		.withStartupTimeout(Duration.ofMinutes(10));

	protected final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withClassLoader(new FilteredClassLoader(HazelcastIndexedSessionRepository.class,
				JdbcIndexedSessionRepository.class, MongoIndexedSessionRepository.class))
		.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	void defaultConfig() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort())
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.run(validateSpringSessionUsesDefaultRedis("spring:session:", FlushMode.ON_SAVE,
					SaveMode.ON_SET_ATTRIBUTE));
	}

	@Test
	void invalidConfigurationPropertyValueWhenDefaultConfigIsUsedWithCustomCronCleanup() {
		this.contextRunner.withPropertyValues("spring.data.redis.host=" + redis.getHost(),
				"spring.data.redis.port=" + redis.getFirstMappedPort(), "spring.session.redis.cleanup-cron=0 0 * * * *")
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.run((context) -> {
				assertThat(context).hasFailed();
				assertThat(context.getStartupFailure())
					.hasRootCauseExactlyInstanceOf(InvalidConfigurationPropertyValueException.class);
			});
	}

	@Test
	void redisTakesPrecedenceMultipleImplementations() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withPropertyValues("spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateSpringSessionUsesDefaultRedis("spring:session:", FlushMode.ON_SAVE,
					SaveMode.ON_SET_ATTRIBUTE));
	}

	@Test
	void defaultConfigWithCustomTimeout() {
		this.contextRunner
			.withPropertyValues("spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort(), "spring.session.timeout=1m")
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.run((context) -> {
				RedisSessionRepository repository = validateSessionRepository(context, RedisSessionRepository.class);
				assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval", Duration.ofMinutes(1));
			});
	}

	@Test
	void defaultRedisSessionStoreWithCustomizations() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withPropertyValues("spring.session.redis.namespace=foo", "spring.session.redis.flush-mode=immediate",
					"spring.session.redis.save-mode=on-get-attribute", "spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateSpringSessionUsesDefaultRedis("foo:", FlushMode.IMMEDIATE, SaveMode.ON_GET_ATTRIBUTE));
	}

	@Test
	void indexedRedisSessionDefaultConfig() {
		this.contextRunner
			.withPropertyValues("spring.session.redis.repository-type=indexed",
					"spring.data.redis.host=" + redis.getHost(), "spring.data.redis.port=" + redis.getFirstMappedPort())
			.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.run(validateSpringSessionUsesIndexedRedis("spring:session:", FlushMode.ON_SAVE, SaveMode.ON_SET_ATTRIBUTE,
					"0 * * * * *"));
	}

	@Test
	void indexedRedisSessionStoreWithCustomizations() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withPropertyValues("spring.session.redis.repository-type=indexed", "spring.session.redis.namespace=foo",
					"spring.session.redis.flush-mode=immediate", "spring.session.redis.save-mode=on-get-attribute",
					"spring.session.redis.cleanup-cron=0 0 12 * * *", "spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateSpringSessionUsesIndexedRedis("foo:", FlushMode.IMMEDIATE, SaveMode.ON_GET_ATTRIBUTE,
					"0 0 12 * * *"));
	}

	@Test
	void indexedRedisSessionWithConfigureActionNone() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withPropertyValues("spring.session.redis.repository-type=indexed",
					"spring.session.redis.configure-action=none", "spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateStrategy(ConfigureRedisAction.NO_OP.getClass()));
	}

	@Test
	void indexedRedisSessionWithDefaultConfigureActionNone() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withPropertyValues("spring.session.redis.repository-type=indexed",
					"spring.data.redis.host=" + redis.getHost(), "spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateStrategy(ConfigureNotifyKeyspaceEventsAction.class, entry("notify-keyspace-events", "gxE")));
	}

	@Test
	void indexedRedisSessionWithCustomConfigureRedisActionBean() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withUserConfiguration(MaxEntriesRedisAction.class)
			.withPropertyValues("spring.session.redis.repository-type=indexed",
					"spring.data.redis.host=" + redis.getHost(), "spring.data.redis.port=" + redis.getFirstMappedPort())
			.run(validateStrategy(MaxEntriesRedisAction.class, entry("set-max-intset-entries", "1024")));

	}

	@Test
	void whenTheUserDefinesTheirOwnSessionRepositoryCustomizerThenDefaultConfigurationIsOverwritten() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withUserConfiguration(CustomizerConfiguration.class)
			.withPropertyValues("spring.session.redis.flush-mode=immediate",
					"spring.data.redis.host=" + redis.getHost(), "spring.data.redis.port=" + redis.getFirstMappedPort())
			.run((context) -> {
				RedisSessionRepository repository = validateSessionRepository(context, RedisSessionRepository.class);
				assertThat(repository).hasFieldOrPropertyWithValue("flushMode", FlushMode.ON_SAVE);
			});
	}

	@Test
	void whenIndexedAndTheUserDefinesTheirOwnSessionRepositoryCustomizerThenDefaultConfigurationIsOverwritten() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
			.withUserConfiguration(IndexedCustomizerConfiguration.class)
			.withPropertyValues("spring.session.redis.repository-type=indexed",
					"spring.session.redis.flush-mode=immediate", "spring.data.redis.host=" + redis.getHost(),
					"spring.data.redis.port=" + redis.getFirstMappedPort())
			.run((context) -> {
				RedisIndexedSessionRepository repository = validateSessionRepository(context,
						RedisIndexedSessionRepository.class);
				assertThat(repository).hasFieldOrPropertyWithValue("flushMode", FlushMode.ON_SAVE);
			});
	}

	private ContextConsumer<AssertableWebApplicationContext> validateSpringSessionUsesDefaultRedis(String keyNamespace,
			FlushMode flushMode, SaveMode saveMode) {
		return (context) -> {
			RedisSessionRepository repository = validateSessionRepository(context, RedisSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval",
					new ServerProperties().getServlet().getSession().getTimeout());
			assertThat(repository).hasFieldOrPropertyWithValue("keyNamespace", keyNamespace);
			assertThat(repository).hasFieldOrPropertyWithValue("flushMode", flushMode);
			assertThat(repository).hasFieldOrPropertyWithValue("saveMode", saveMode);
		};
	}

	private ContextConsumer<AssertableWebApplicationContext> validateSpringSessionUsesIndexedRedis(String keyNamespace,
			FlushMode flushMode, SaveMode saveMode, String cleanupCron) {
		return (context) -> {
			RedisIndexedSessionRepository repository = validateSessionRepository(context,
					RedisIndexedSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("defaultMaxInactiveInterval",
					new ServerProperties().getServlet().getSession().getTimeout());
			assertThat(repository).hasFieldOrPropertyWithValue("namespace", keyNamespace);
			assertThat(repository).hasFieldOrPropertyWithValue("flushMode", flushMode);
			assertThat(repository).hasFieldOrPropertyWithValue("saveMode", saveMode);
			assertThat(repository).hasFieldOrPropertyWithValue("cleanupCron", cleanupCron);
		};
	}

	private ContextConsumer<AssertableWebApplicationContext> validateStrategy(
			Class<? extends ConfigureRedisAction> expectedConfigureRedisActionType, Map.Entry<?, ?>... expectedConfig) {
		return (context) -> {
			assertThat(context).hasSingleBean(ConfigureRedisAction.class);
			assertThat(context).hasSingleBean(RedisConnectionFactory.class);
			assertThat(context.getBean(ConfigureRedisAction.class)).isInstanceOf(expectedConfigureRedisActionType);
			RedisConnection connection = context.getBean(RedisConnectionFactory.class).getConnection();
			if (expectedConfig.length > 0) {
				assertThat(connection.serverCommands().getConfig("*")).contains(expectedConfig);
			}
		};
	}

	static class MaxEntriesRedisAction implements ConfigureRedisAction {

		@Override
		public void configure(RedisConnection connection) {
			connection.serverCommands().setConfig("set-max-intset-entries", "1024");
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomizerConfiguration {

		@Bean
		SessionRepositoryCustomizer<RedisSessionRepository> sessionRepositoryCustomizer() {
			return (repository) -> repository.setFlushMode(FlushMode.ON_SAVE);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class IndexedCustomizerConfiguration {

		@Bean
		SessionRepositoryCustomizer<RedisIndexedSessionRepository> sessionRepositoryCustomizer() {
			return (repository) -> repository.setFlushMode(FlushMode.ON_SAVE);
		}

	}

}
