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

package org.springframework.boot.amqp.docker.compose;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitMqEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 */
class RabbitMqEnvironmentTests {

	@Test
	void getUsernameWhenNoRabbitMqDefaultUser() {
		RabbitMqEnvironment environment = new RabbitMqEnvironment(Collections.emptyMap());
		assertThat(environment.getUsername()).isEqualTo("guest");
	}

	@Test
	void getUsernameWhenHasRabbitMqDefaultUser() {
		RabbitMqEnvironment environment = new RabbitMqEnvironment(Map.of("RABBITMQ_DEFAULT_USER", "me"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getUsernameWhenHasRabbitMqUsername() {
		RabbitMqEnvironment environment = new RabbitMqEnvironment(Map.of("RABBITMQ_USERNAME", "me"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getPasswordWhenNoRabbitMqDefaultPass() {
		RabbitMqEnvironment environment = new RabbitMqEnvironment(Collections.emptyMap());
		assertThat(environment.getPassword()).isEqualTo("guest");
	}

	@Test
	void getPasswordWhenHasRabbitMqDefaultPass() {
		RabbitMqEnvironment environment = new RabbitMqEnvironment(Map.of("RABBITMQ_DEFAULT_PASS", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getPasswordWhenHasRabbitMqPassword() {
		RabbitMqEnvironment environment = new RabbitMqEnvironment(Map.of("RABBITMQ_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

}
