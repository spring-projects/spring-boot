/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.test.system;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OutputCaptureExtension} when used through
 * {@link ExtendWith @ExtendWith}.
 *
 * @author Madhura Bhave
 */
@ExtendWith(OutputCaptureExtension.class)
@ExtendWith(OutputExtensionExtendWithTests.BeforeAllExtension.class)
@ExtendWith(OutputExtensionExtendWithTests.BeforeEachExtension.class)
class OutputExtensionExtendWithTests {

	@Test
	void captureShouldReturnOutputCapturedBeforeAllTestMethod(CapturedOutput output) {
		assertThat(output).contains("Before all").doesNotContain("Hello");
	}

	@Test
	void captureShouldReturnOutputCapturedBeforeEachTestMethod(CapturedOutput output) {
		assertThat(output).contains("Before each").doesNotContain("Hello");
	}

	@Test
	void captureShouldReturnAllCapturedOutput(CapturedOutput output) {
		System.out.println("Hello World");
		System.err.println("Error!!!");
		assertThat(output).contains("Before all").contains("Before each").contains("Hello World").contains("Error!!!");
	}

	static class BeforeAllExtension implements BeforeAllCallback {

		@Override
		public void beforeAll(ExtensionContext context) {
			System.out.println("Before all");
		}

	}

	static class BeforeEachExtension implements BeforeEachCallback {

		@Override
		public void beforeEach(ExtensionContext context) {
			System.out.println("Before each");
		}

	}

}
