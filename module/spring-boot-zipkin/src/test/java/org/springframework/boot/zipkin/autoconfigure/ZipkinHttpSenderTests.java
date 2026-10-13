/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.zipkin.autoconfigure;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.ClosedSenderException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Abstract base test class which is used for testing the different implementations of the
 * {@link HttpSender}.
 *
 * @author Stefan Bratanov
 */
abstract class ZipkinHttpSenderTests {

	protected BytesMessageSender sender;

	abstract BytesMessageSender createSender();

	@BeforeEach
	void beforeEach() {
		this.sender = createSender();
	}

	@AfterEach
	void afterEach() throws IOException {
		this.sender.close();
	}

	@Test
	void sendShouldThrowIfCloseWasCalled() throws IOException {
		this.sender.close();
		assertThatExceptionOfType(ClosedSenderException.class)
			.isThrownBy(() -> this.sender.send(Collections.emptyList()));
	}

	protected byte[] toByteArray(String input) {
		return input.getBytes(StandardCharsets.UTF_8);
	}

}
