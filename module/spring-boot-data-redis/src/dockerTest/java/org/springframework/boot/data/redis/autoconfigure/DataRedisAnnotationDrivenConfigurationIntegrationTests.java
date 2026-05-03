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

import java.util.ArrayList;
import java.util.List;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.data.redis.annotation.RedisListener;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration tests for {@link DataRedisAnnotationDrivenConfiguration}.
 *
 * @author Stephane Nicoll
 */
@Testcontainers(disabledWithoutDocker = true)
class DataRedisAnnotationDrivenConfigurationIntegrationTests {

	@Container
	static final RedisContainer redis = TestImage.container(RedisContainer.class);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataRedisAutoConfiguration.class))
		.withPropertyValues("spring.data.redis.host=" + redis.getHost(),
				"spring.data.redis.port=" + redis.getFirstMappedPort());

	@Test
	void annotatedListenerShouldReceiveMessages() {
		this.contextRunner.withUserConfiguration(TestListener.class).run((context) -> {
			StringRedisTemplate redisTemplate = context.getBean(StringRedisTemplate.class);
			TestListener testListener = context.getBean(TestListener.class);
			redisTemplate.convertAndSend("test-channel", "hello-world");
			await().untilAsserted(() -> assertThat(testListener.messages).contains("hello-world"));
		});
	}

	static class TestListener {

		private final List<String> messages = new ArrayList<>();

		@RedisListener("test-channel")
		void process(String message) {
			this.messages.add(message);
		}

	}

}
