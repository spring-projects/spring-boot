/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.artemis.docker.compose;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ArtemisEnvironment}.
 *
 * @author Eddú Meléndez
 */
class ArtemisEnvironmentTests {

	@Test
	void getUserWhenHasNoActiveMqUser() {
		ArtemisEnvironment environment = new ArtemisEnvironment(Collections.emptyMap());
		assertThat(environment.getUser()).isNull();
	}

	@Test
	void getUserWhenHasActiveMqUser() {
		ArtemisEnvironment environment = new ArtemisEnvironment(Map.of("ARTEMIS_USER", "me"));
		assertThat(environment.getUser()).isEqualTo("me");
	}

	@Test
	void getPasswordWhenHasNoActiveMqPassword() {
		ArtemisEnvironment environment = new ArtemisEnvironment(Collections.emptyMap());
		assertThat(environment.getPassword()).isNull();
	}

	@Test
	void getPasswordWhenHasActiveMqPassword() {
		ArtemisEnvironment environment = new ArtemisEnvironment(Map.of("ARTEMIS_PASSWORD", "secret"));
		assertThat(environment.getPassword()).isEqualTo("secret");
	}

}
