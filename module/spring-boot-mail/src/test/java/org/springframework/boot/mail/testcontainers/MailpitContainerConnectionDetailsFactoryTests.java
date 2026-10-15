/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.testcontainers;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactoryHints;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MailpitContainerConnectionDetailsFactory}.
 *
 * @author Van Vo
 */
class MailpitContainerConnectionDetailsFactoryTests {

	@Test
	void shouldRegisterHints() {
		RuntimeHints hints = ContainerConnectionDetailsFactoryHints.getRegisteredHints(getClass().getClassLoader());
		assertThat(RuntimeHintsPredicates.reflection().onType(JavaMailSender.class)).accepts(hints);
	}

}
