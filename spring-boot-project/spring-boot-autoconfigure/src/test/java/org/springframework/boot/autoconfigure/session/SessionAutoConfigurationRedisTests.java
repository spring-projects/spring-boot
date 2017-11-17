/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.session;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.session.RedisSessionConfiguration.SpringBootRedisHttpSessionConfiguration;
import org.springframework.boot.test.context.HideClassesClassLoader;
import org.springframework.boot.test.context.assertj.AssertableWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.testsupport.rule.RedisTestServer;
import org.springframework.session.data.mongo.MongoOperationsSessionRepository;
import org.springframework.session.data.redis.RedisFlushMode;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.hazelcast.HazelcastSessionRepository;
import org.springframework.session.jdbc.JdbcOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Vedran Pavic
 */
public class SessionAutoConfigurationRedisTests
		extends AbstractSessionAutoConfigurationTests {

	@Rule
	public final RedisTestServer redis = new RedisTestServer();

	protected final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	public void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.session.store-type=redis")
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.run(validateSpringSessionUsesRedis("spring:session:event:created:",
						RedisFlushMode.ON_SAVE, "0 * * * * *"));
	}

	@Test
	public void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner
				.withClassLoader(
						new HideClassesClassLoader(HazelcastSessionRepository.class,
								JdbcOperationsSessionRepository.class,
								MongoOperationsSessionRepository.class))
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.run(validateSpringSessionUsesRedis("spring:session:event:created:",
						RedisFlushMode.ON_SAVE, "0 * * * * *"));
	}

	@Test
	public void redisSessionStoreWithCustomizations() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class))
				.withPropertyValues("spring.session.store-type=redis",
						"spring.session.redis.namespace=foo",
						"spring.session.redis.flush-mode=immediate",
						"spring.session.redis.cleanup-cron=0 0 12 * * *")
				.run(validateSpringSessionUsesRedis("spring:session:foo:event:created:",
						RedisFlushMode.IMMEDIATE, "0 0 12 * * *"));
	}

	private ContextConsumer<AssertableWebApplicationContext> validateSpringSessionUsesRedis(
			String sessionCreatedChannelPrefix, RedisFlushMode flushMode,
			String cleanupCron) {
		return (context) -> {
			RedisOperationsSessionRepository repository = validateSessionRepository(
					context, RedisOperationsSessionRepository.class);
			assertThat(repository.getSessionCreatedChannelPrefix())
					.isEqualTo(sessionCreatedChannelPrefix);
			assertThat(new DirectFieldAccessor(repository)
					.getPropertyValue("redisFlushMode")).isEqualTo(flushMode);
			SpringBootRedisHttpSessionConfiguration configuration = context
					.getBean(SpringBootRedisHttpSessionConfiguration.class);
			assertThat(new DirectFieldAccessor(configuration)
					.getPropertyValue("cleanupCron")).isEqualTo(cleanupCron);
		};
	}

}
