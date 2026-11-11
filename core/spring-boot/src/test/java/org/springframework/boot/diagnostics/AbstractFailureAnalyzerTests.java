/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.diagnostics;

import java.io.IOException;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractFailureAnalyzer}.
 *
 * @author Kim Jung Bin
 * @author Stephane Nicoll
 */
class AbstractFailureAnalyzerTests {

	private final TestFailureAnalyzer failureAnalyzer = new TestFailureAnalyzer();

	@Test
	void findCauseWithNullException() {
		assertThat(this.failureAnalyzer.findCause(null, Throwable.class)).isNull();
	}

	@Test
	void findCauseWithDirectExactMatch() {
		TestException ex = new TestException();
		assertThat(this.failureAnalyzer.findCause(ex, TestException.class)).isEqualTo(ex);
	}

	@Test
	void findCauseWithDirectSubClass() {
		SpecificTestException ex = new SpecificTestException();
		assertThat(this.failureAnalyzer.findCause(ex, TestException.class)).isEqualTo(ex);
	}

	@Test
	void findCauseWitNestedAndExactMatch() {
		TestException ex = new TestException();
		assertThat(this.failureAnalyzer.findCause(new IllegalArgumentException(new IllegalStateException(ex)),
				TestException.class))
			.isEqualTo(ex);
	}

	@Test
	void findCauseWitNestedAndSubClass() {
		SpecificTestException ex = new SpecificTestException();
		assertThat(this.failureAnalyzer.findCause(new IOException(new IllegalStateException(ex)), TestException.class))
			.isEqualTo(ex);
	}

	@Test
	void findCauseWithUnrelatedException() {
		IOException ex = new IOException();
		assertThat(this.failureAnalyzer.findCause(ex, TestException.class)).isNull();
	}

	@Test
	void findCauseWithMoreSpecificException() {
		TestException ex = new TestException();
		assertThat(this.failureAnalyzer.findCause(ex, SpecificTestException.class)).isNull();
	}

	static class TestFailureAnalyzer extends AbstractFailureAnalyzer<Throwable> {

		@Override
		protected @Nullable FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
			return null;
		}

	}

	static class TestException extends Exception {

	}

	static class SpecificTestException extends TestException {

	}

}
