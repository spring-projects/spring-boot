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

package org.springframework.boot.autoconfigure.session;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.session.RedisSessionConfiguration.SpringBootRedisHttpSessionConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.testcontainers.DisabledWithoutDockerTestcontainers;
import org.springframework.boot.testsupport.testcontainers.RedisContainer;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.session.data.mongo.MongoOperationsSessionRepository;
import org.springframework.session.data.redis.RedisFlushMode;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.data.redis.config.ConfigureNotifyKeyspaceEventsAction;
import org.springframework.session.data.redis.config.ConfigureRedisAction;
import org.springframework.session.hazelcast.HazelcastSessionRepository;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Redis specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
@DisabledWithoutDockerTestcontainers
class SessionAutoConfigurationRedisTests extends AbstractSessionAutoConfigurationTests {

	@Container
	public static RedisContainer redis = new RedisContainer();

	protected final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	void defaultConfig() {
		this.contextRunner
				.withPropertyValues("spring.session.store-type=redis",
						"spring.redis.port=" + redis.getFirstMappedPort())
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.run(validateSpringSessionUsesRedis("spring:session:event:0:created:", RedisFlushMode.ON_SAVE,
						"0 * * * * *"));
	}

	@Test
	void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader(HazelcastSessionRepository.class,
						JdbcOperationsSessionRepository.class, MongoOperationsSessionRepository.class))
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.withPropertyValues("spring.redis.port=" + redis.getFirstMappedPort())
				.run(validateSpringSessionUsesRedis("spring:session:event:0:created:", RedisFlushMode.ON_SAVE,
						"0 * * * * *"));
	}

	@Test
	void redisSessionStoreWithCustomizations() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.withPropertyValues("spring.session.store-type=redis", "spring.session.redis.namespace=foo",
						"spring.session.redis.flush-mode=immediate", "spring.session.redis.cleanup-cron=0 0 12 * * *",
						"spring.redis.port=" + redis.getFirstMappedPort())
				.run(validateSpringSessionUsesRedis("foo:event:0:created:", RedisFlushMode.IMMEDIATE, "0 0 12 * * *"));
	}

	@Test
	void redisSessionWithConfigureActionNone() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.withPropertyValues("spring.session.store-type=redis", "spring.session.redis.configure-action=none",
						"spring.redis.port=" + redis.getFirstMappedPort())
				.run(validateStrategy(ConfigureRedisAction.NO_OP.getClass()));
	}

	@Test
	void redisSessionWithDefaultConfigureActionNone() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.withPropertyValues("spring.session.store-type=redis",
						"spring.redis.port=" + redis.getFirstMappedPort())
				.run(validateStrategy(ConfigureNotifyKeyspaceEventsAction.class,
						entry("notify-keyspace-events", "gxE")));
	}

	@Test
	void redisSessionWithCustomConfigureRedisActionBean() {
		this.contextRunner.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.withUserConfiguration(MaxEntriesRedisAction.class)
				.withPropertyValues("spring.session.store-type=redis",
						"spring.redis.port=" + redis.getFirstMappedPort())
				.run(validateStrategy(MaxEntriesRedisAction.class, entry("set-max-intset-entries", "1024")));

	}

	private ContextConsumer<AssertableWebApplicationContext> validateSpringSessionUsesRedis(
			String sessionCreatedChannelPrefix, RedisFlushMode flushMode, String cleanupCron) {
		return (context) -> {
			RedisOperationsSessionRepository repository = validateSessionRepository(context,
					RedisOperationsSessionRepository.class);
			assertThat(repository.getSessionCreatedChannelPrefix()).isEqualTo(sessionCreatedChannelPrefix);
			assertThat(repository).hasFieldOrPropertyWithValue("redisFlushMode", flushMode);
			SpringBootRedisHttpSessionConfiguration configuration = context
					.getBean(SpringBootRedisHttpSessionConfiguration.class);
			assertThat(configuration).hasFieldOrPropertyWithValue("cleanupCron", cleanupCron);
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
				assertThat(connection.getConfig("*")).contains(expectedConfig);
			}
		};
	}

	static class MaxEntriesRedisAction implements ConfigureRedisAction {

		@Override
		public void configure(RedisConnection connection) {
			connection.setConfig("set-max-intset-entries", "1024");
		}

	}

}
