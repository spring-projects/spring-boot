/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mongodb.testcontainers;

import com.mongodb.ConnectionString;
import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactoryHints;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractMongoContainerConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 */
class AbstractMongoContainerConnectionDetailsFactoryTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = ContainerConnectionDetailsFactoryHints.getRegisteredHints(getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(ConnectionString.class)).accepts(hints);
	}

}
