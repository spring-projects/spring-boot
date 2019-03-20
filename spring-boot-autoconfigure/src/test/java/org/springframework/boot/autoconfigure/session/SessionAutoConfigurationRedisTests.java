/*
 * Copyright 2012-2017 the original author or authors.
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

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.redis.RedisTestServer;
import org.springframework.session.data.redis.RedisFlushMode;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Redis specific tests for {@link SessionAutoConfiguration}.
 *
 * @author Stephane Nicoll
 */
public class SessionAutoConfigurationRedisTests
		extends AbstractSessionAutoConfigurationTests {

	@Rule
	public final RedisTestServer redis = new RedisTestServer();

	@Test
	public void redisSessionStore() {
		load(Collections.<Class<?>>singletonList(RedisAutoConfiguration.class),
				"spring.session.store-type=redis");
		validateSpringSessionUsesRedis();
	}

	private void validateSpringSessionUsesRedis() {
		RedisOperationsSessionRepository repository = validateSessionRepository(
				RedisOperationsSessionRepository.class);
		assertThat(repository.getSessionCreatedChannelPrefix())
				.isEqualTo("spring:session:event:created:");
		assertThat(new DirectFieldAccessor(repository).getPropertyValue("redisFlushMode"))
				.isEqualTo(RedisFlushMode.ON_SAVE);
	}

	@Test
	public void redisSessionStoreWithCustomizations() {
		load(Collections.<Class<?>>singletonList(RedisAutoConfiguration.class),
				"spring.session.store-type=redis", "spring.session.redis.namespace=foo",
				"spring.session.redis.flush-mode=immediate");
		RedisOperationsSessionRepository repository = validateSessionRepository(
				RedisOperationsSessionRepository.class);
		assertThat(repository.getSessionCreatedChannelPrefix())
				.isEqualTo("spring:session:foo:event:created:");
		assertThat(new DirectFieldAccessor(repository).getPropertyValue("redisFlushMode"))
				.isEqualTo(RedisFlushMode.IMMEDIATE);
	}

}
