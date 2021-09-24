/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.diagnostics;

import java.io.IOException;

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
				TestException.class)).isEqualTo(ex);
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
		protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
			return null;
		}

	}

	static class TestException extends Exception {

	}

	static class SpecificTestException extends TestException {

	}

}
