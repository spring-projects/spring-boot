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

package org.springframework.boot.docker.compose.service.connection.rabbit;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class RabbitEnvironmentTests {

	@Test
	void getUsernameWhenNoRabbitmqDefaultUser() {
		RabbitEnvironment environment = new RabbitEnvironment(Collections.emptyMap());
		assertThat(environment.getUsername()).isEqualTo("guest");
	}

	@Test
	void getUsernameWhenHasRabbitmqDefaultUser() {
		RabbitEnvironment environment = new RabbitEnvironment(Map.of("RABBITMQ_DEFAULT_USER", "me"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getUsernameWhenNoRabbitmqDefaultPass() {
		RabbitEnvironment environment = new RabbitEnvironment(Collections.emptyMap());
		assertThat(environment.getPassword()).isEqualTo("guest");
	}

	@Test
	void getUsernameWhenHasRabbitmqDefaultPass() {
		RabbitEnvironment environment = new RabbitEnvironment(Map.of("RABBITMQ_DEFAULT_PASS", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

}
