/*
 * Copyright 2012-2018 the original author or authors.
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

import org.junit.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.assertj.AssertableReactiveWebApplicationContext;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.session.data.mongo.ReactiveMongoOperationsSessionRepository;
import org.springframework.session.data.redis.ReactiveRedisOperationsSessionRepository;
import org.springframework.session.data.redis.RedisFlushMode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reactive Redis-specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Vedran Pavic
 */
public class ReactiveSessionAutoConfigurationRedisTests
		extends AbstractSessionAutoConfigurationTests {

	protected final ReactiveWebApplicationContextRunner contextRunner = new ReactiveWebApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(SessionAutoConfiguration.class));

	@Test
	public void defaultConfig() {
		this.contextRunner.withPropertyValues("spring.session.store-type=redis")
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class,
						RedisReactiveAutoConfiguration.class))
				.run(validateSpringSessionUsesRedis("spring:session:",
						RedisFlushMode.ON_SAVE));
	}

	@Test
	public void defaultConfigWithUniqueStoreImplementation() {
		this.contextRunner
				.withClassLoader(new FilteredClassLoader(
						ReactiveMongoOperationsSessionRepository.class))
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class,
						RedisReactiveAutoConfiguration.class))
				.run(validateSpringSessionUsesRedis("spring:session:",
						RedisFlushMode.ON_SAVE));
	}

	@Test
	public void redisSessionStoreWithCustomizations() {
		this.contextRunner
				.withConfiguration(AutoConfigurations.of(RedisAutoConfiguration.class,
						RedisReactiveAutoConfiguration.class))
				.withPropertyValues("spring.session.store-type=redis",
						"spring.session.redis.namespace=foo",
						"spring.session.redis.flush-mode=immediate")
				.run(validateSpringSessionUsesRedis("foo:", RedisFlushMode.IMMEDIATE));
	}

	private ContextConsumer<AssertableReactiveWebApplicationContext> validateSpringSessionUsesRedis(
			String namespace, RedisFlushMode flushMode) {
		return (context) -> {
			ReactiveRedisOperationsSessionRepository repository = validateSessionRepository(
					context, ReactiveRedisOperationsSessionRepository.class);
			assertThat(repository).hasFieldOrPropertyWithValue("namespace", namespace);
			assertThat(repository).hasFieldOrPropertyWithValue("redisFlushMode",
					flushMode);
		};
	}

}
