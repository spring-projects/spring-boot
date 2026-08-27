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

package org.springframework.boot.test.autoconfigure.kafka;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@KafkaListenerTest(KafkaListenerTestIntegrationTests.ExampleListener.class)
@Import(KafkaListenerTestIntegrationTests.ExampleListener.class)
class KafkaListenerTestIntegrationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void shouldIncludeConfiguredListener() {
		assertThat(this.applicationContext.getBean(ExampleListener.class)).isNotNull();
	}

	@Test
	void shouldExcludeUnconfiguredListener() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(AnotherListener.class));
	}

	@Test
	void shouldExcludeStandardService() {
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
			.isThrownBy(() -> this.applicationContext.getBean(ExampleService.class));
	}

	@Component
	static class ExampleListener {

		@KafkaListener(topics = "test-topic", groupId = "test-group")
		void listen(String message) {
			assertThat(message).isNotNull();
		}

	}

	@Component
	static class AnotherListener {

		@KafkaListener(topics = "another-topic", groupId = "test-group")
		void listen(String message) {
			assertThat(message).isNotNull();
		}

	}

	@Service
	static class ExampleService {

	}

	@SpringBootApplication
	static class TestApplication {

	}

}
