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

package org.springframework.boot.data.redis.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataRedisUrlSyntaxFailureAnalyzer}.
 *
 * @author Scott Frederick
 */
class RedisUrlSyntaxFailureAnalyzerTests {

	@Test
	void analyzeInvalidUrlSyntax() {
		DataRedisUrlSyntaxException exception = new DataRedisUrlSyntaxException("redis://invalid");
		FailureAnalysis analysis = new DataRedisUrlSyntaxFailureAnalyzer().analyze(exception);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).contains("The URL 'redis://invalid' is not valid");
		assertThat(analysis.getAction()).contains("Review the value of the property 'spring.data.redis.url'");
	}

	@Test
	void analyzeRedisHttpUrl() {
		DataRedisUrlSyntaxException exception = new DataRedisUrlSyntaxException("http://127.0.0.1:26379/mymaster");
		FailureAnalysis analysis = new DataRedisUrlSyntaxFailureAnalyzer().analyze(exception);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).contains("The URL 'http://127.0.0.1:26379/mymaster' is not valid")
			.contains("The scheme 'http' is not supported");
		assertThat(analysis.getAction()).contains("Use the scheme 'redis://' for insecure or 'rediss://' for secure");
	}

	@Test
	void analyzeRedisSentinelUrl() {
		DataRedisUrlSyntaxException exception = new DataRedisUrlSyntaxException(
				"redis-sentinel://username:password@127.0.0.1:26379,127.0.0.1:26380/mymaster");
		FailureAnalysis analysis = new DataRedisUrlSyntaxFailureAnalyzer().analyze(exception);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).contains(
				"The URL 'redis-sentinel://username:password@127.0.0.1:26379,127.0.0.1:26380/mymaster' is not valid")
			.contains("The scheme 'redis-sentinel' is not supported");
		assertThat(analysis.getAction()).contains("Use spring.data.redis.sentinel properties");
	}

	@Test
	void analyzeRedisSocketUrl() {
		DataRedisUrlSyntaxException exception = new DataRedisUrlSyntaxException("redis-socket:///redis/redis.sock");
		FailureAnalysis analysis = new DataRedisUrlSyntaxFailureAnalyzer().analyze(exception);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).contains("The URL 'redis-socket:///redis/redis.sock' is not valid")
			.contains("The scheme 'redis-socket' is not supported");
		assertThat(analysis.getAction()).contains("Configure the appropriate Spring Data Redis connection beans");
	}

}
