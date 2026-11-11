/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.hazelcast.testcontainers;

import com.hazelcast.client.config.ClientConfig;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactoryHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HazelcastContainerConnectionDetailsFactory}.
 *
 * @author Dmytro Nosan
 */
class HazelcastContainerConnectionDetailsFactoryTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = ContainerConnectionDetailsFactoryHints.getRegisteredHints(getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(ClientConfig.class)).accepts(hints);
	}

}
