/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.docker.compose.service.connection.activemq;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ActiveMQClassicEnvironment}.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class ActiveMQClassicEnvironmentTests {

	@Test
	void getUserWhenHasNoActiveMqUser() {
		ActiveMQClassicEnvironment environment = new ActiveMQClassicEnvironment(Collections.emptyMap());
		assertThat(environment.getUser()).isNull();
	}

	@Test
	void getUserWhenHasActiveMqUser() {
		ActiveMQClassicEnvironment environment = new ActiveMQClassicEnvironment(
				Map.of("ACTIVEMQ_CONNECTION_USER", "me"));
		assertThat(environment.getUser()).isEqualTo("me");
	}

	@Test
	void getPasswordWhenHasNoActiveMqPassword() {
		ActiveMQClassicEnvironment environment = new ActiveMQClassicEnvironment(Collections.emptyMap());
		assertThat(environment.getPassword()).isNull();
	}

	@Test
	void getPasswordWhenHasActiveMqPassword() {
		ActiveMQClassicEnvironment environment = new ActiveMQClassicEnvironment(
				Map.of("ACTIVEMQ_CONNECTION_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

}
