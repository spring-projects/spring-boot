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

import org.springframework.boot.docker.compose.core.RunningService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ActiveMqAmqpDockerComposeConnectionDetailsFactory}.
 *
 * @author Stephane Nicoll
 */
class ActiveMqAmqpDockerComposeConnectionDetailsFactoryTests {

	@Test
	void getUserWhenHasNoActiveMqUser() {
		AmqpEnvironment environment = getAmqpEnvironment(Collections.emptyMap());
		assertThat(environment.getUsername()).isNull();
	}

	@Test
	void getUserWhenHasActiveMqUser() {
		AmqpEnvironment environment = getAmqpEnvironment(Map.of("ACTIVEMQ_CONNECTION_USER", "me"));
		assertThat(environment.getUsername()).isEqualTo("me");
	}

	@Test
	void getPasswordWhenHasNoActiveMqPassword() {
		AmqpEnvironment environment = getAmqpEnvironment(Collections.emptyMap());
		assertThat(environment.getPassword()).isNull();
	}

	@Test
	void getPasswordWhenHasActiveMqPassword() {
		AmqpEnvironment environment = getAmqpEnvironment(Map.of("ACTIVEMQ_CONNECTION_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	private AmqpEnvironment getAmqpEnvironment(Map<String, String> env) {
		RunningService runningService = mock(RunningService.class);
		given(runningService.env()).willReturn(env);
		return new ActiveMqAmqpDockerComposeConnectionDetailsFactory().getAmqpEnvironment(runningService);
	}

}
