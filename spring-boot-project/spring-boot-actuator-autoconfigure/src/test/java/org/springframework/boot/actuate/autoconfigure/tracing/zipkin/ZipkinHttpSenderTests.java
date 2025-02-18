/*
 * Copyright 2012-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.tracing.zipkin;

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
