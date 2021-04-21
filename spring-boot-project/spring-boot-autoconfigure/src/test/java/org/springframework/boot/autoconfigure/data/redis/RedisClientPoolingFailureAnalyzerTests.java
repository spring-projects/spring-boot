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

package org.springframework.boot.autoconfigure.data.redis;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties.ClientType;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisClientPoolingFailureAnalyzer}.
 *
 * @author Weix Sun
 */
class RedisClientPoolingFailureAnalyzerTests {

	@Test
	void analyzeJedisSentinelButPoolNotEnabled() {
		RedisClientPoolingException exception = new RedisClientPoolingException(ClientType.JEDIS);
		FailureAnalysis analysis = new RedisClientPoolingFailureAnalyzer().analyze(exception);
		assertThat(analysis.getDescription()).contains("Jedis Sentinel cannot operate without a pool.");
		assertThat(analysis.getAction()).contains(
				"Set spring.redis.jedis.pool.enabled=true instead of spring.redis.jedis.pool.enabled=false or delete this configuration item.(Default: true)");
	}

	@Test
	void analyzeLettucePoolEnabledButPool2NotExist() {
		RedisClientPoolingException exception = new RedisClientPoolingException(ClientType.LETTUCE);
		FailureAnalysis analysis = new RedisClientPoolingFailureAnalyzer().analyze(exception);
		assertThat(analysis.getDescription())
				.contains("Lettuce pool cannot enable if \"commons-pool2\" don't exists on the classpath");
		assertThat(analysis.getAction()).contains("Add \"commons-pool2\" dependency to the classpath.");
	}

	@Test
	void analyzeOtherError() {
		RedisClientPoolingException exception = new RedisClientPoolingException(ClientType.JEDIS,
				"Jedis Pooling Error.");
		FailureAnalysis analysis = new RedisClientPoolingFailureAnalyzer().analyze(exception);
		assertThat(analysis.getDescription()).contains("Jedis Pooling Error.");
		assertThat(analysis.getAction()).contains("");
	}

}
