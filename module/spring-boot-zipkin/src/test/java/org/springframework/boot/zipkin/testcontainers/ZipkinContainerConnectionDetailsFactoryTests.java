/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.testcontainers;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactoryHints;
import org.springframework.boot.zipkin.autoconfigure.ZipkinAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipkinContainerConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 */
class ZipkinContainerConnectionDetailsFactoryTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = ContainerConnectionDetailsFactoryHints.getRegisteredHints(getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(ZipkinAutoConfiguration.class)).accepts(hints);
	}

}
