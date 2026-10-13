/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.amqp.docker.compose;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitEnvironment}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
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
	void getUsernameWhenHasRabbitmqUsername() {
		RabbitEnvironment environment = new RabbitEnvironment(Map.of("RABBITMQ_USERNAME", "me"));
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

	@Test
	void getUsernameWhenHasRabbitmqPassword() {
		RabbitEnvironment environment = new RabbitEnvironment(Map.of("RABBITMQ_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

	@Test
	void getUsernameWhenRabbitmqDefaultUserHasNoValue() {
		Map<String, @Nullable String> env = new HashMap<>();
		env.put("RABBITMQ_DEFAULT_USER", null);
		RabbitEnvironment environment = new RabbitEnvironment(env);
		assertThat(environment.getUsername()).isEqualTo("guest");
	}

	@Test
	void getPasswordWhenRabbitmqDefaultPassHasNoValue() {
		Map<String, @Nullable String> env = new HashMap<>();
		env.put("RABBITMQ_DEFAULT_PASS", null);
		RabbitEnvironment environment = new RabbitEnvironment(env);
		assertThat(environment.getPassword()).isEqualTo("guest");
	}

	@Test
	void getUsernameWhenRabbitmqDefaultUserHasNoValueAndHasRabbitmqUsername() {
		Map<String, @Nullable String> env = new HashMap<>();
		env.put("RABBITMQ_DEFAULT_USER", null);
		env.put("RABBITMQ_USERNAME", "me");
		RabbitEnvironment environment = new RabbitEnvironment(env);
		assertThat(environment.getUsername()).isEqualTo("me");
	}

}
