/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.docker.compose;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ActiveMQEnvironment}.
 *
 * @author Stephane Nicoll
 */
class ActiveMQEnvironmentTests {

	@Test
	void getUserWhenHasNoActiveMqUser() {
		ActiveMQEnvironment environment = new ActiveMQEnvironment(Collections.emptyMap());
		assertThat(environment.getUser()).isNull();
	}

	@Test
	void getUserWhenHasActiveMqUser() {
		ActiveMQEnvironment environment = new ActiveMQEnvironment(Map.of("ACTIVEMQ_USERNAME", "me"));
		assertThat(environment.getUser()).isEqualTo("me");
	}

	@Test
	void getPasswordWhenHasNoActiveMqPassword() {
		ActiveMQEnvironment environment = new ActiveMQEnvironment(Collections.emptyMap());
		assertThat(environment.getPassword()).isNull();
	}

	@Test
	void getPasswordWhenHasActiveMqPassword() {
		ActiveMQEnvironment environment = new ActiveMQEnvironment(Map.of("ACTIVEMQ_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

}
