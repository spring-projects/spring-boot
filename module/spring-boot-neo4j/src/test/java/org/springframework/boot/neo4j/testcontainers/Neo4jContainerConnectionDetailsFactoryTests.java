/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.neo4j.testcontainers;

import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthToken;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactoryHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Neo4jContainerConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 */
class Neo4jContainerConnectionDetailsFactoryTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = ContainerConnectionDetailsFactoryHints.getRegisteredHints(getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(AuthToken.class)).accepts(hints);
	}

}
