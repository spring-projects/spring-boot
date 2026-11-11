/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.docker.compose;

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
