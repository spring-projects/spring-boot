/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.testcontainers;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactoryHints;
import org.springframework.boot.testsupport.classpath.ClassPathExclusions;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ZipkinContainerConnectionDetailsFactory}.
 *
 * @author Moritz Halbritter
 */
@ClassPathExclusions("spring-boot-actuator-*")
class ZipkinContainerConnectionDetailsFactoryWithoutActuatorTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = ContainerConnectionDetailsFactoryHints.getRegisteredHints(getClass().getClassLoader());
		assertThat(hints).isNotNull();
	}

}
