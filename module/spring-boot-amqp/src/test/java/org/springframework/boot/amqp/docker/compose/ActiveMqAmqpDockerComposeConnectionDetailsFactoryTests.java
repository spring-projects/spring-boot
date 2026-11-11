/*
 * Copyright 2012-present the original author or authors.
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
