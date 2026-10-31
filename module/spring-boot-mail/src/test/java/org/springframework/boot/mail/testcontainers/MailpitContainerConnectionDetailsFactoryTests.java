/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.mail.testcontainers;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.boot.mail.autoconfigure.MailConnectionDetails;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionDetailsFactoryHints;
import org.springframework.boot.testcontainers.service.connection.ContainerConnectionSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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

	@Test
	@SuppressWarnings("unchecked")
	void shouldBackOffWhenMailIsNotAvailable() {
		ContainerConnectionSource<Container<?>> source = mock(ContainerConnectionSource.class);
		given(source.accepts("axllent/mailpit", Container.class, MailConnectionDetails.class)).willReturn(true);
		MailpitContainerConnectionDetailsFactory factory = new MailpitContainerConnectionDetailsFactory();
		assertThat(factory.getConnectionDetails(source)).isNotNull();
		new ApplicationContextRunner().withClassLoader(new FilteredClassLoader(JavaMailSender.class)).run((context) -> {
			assertThat(ClassUtils.isPresent(JavaMailSender.class.getName(), null)).isFalse();
			assertThat(factory.getConnectionDetails(source)).isNull();
		});
		assertThat(factory.getConnectionDetails(source)).isNotNull();
	}

}
