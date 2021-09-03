/*
 * Copyright 2012-2020 the original author or authors.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractFailureAnalyzerTest {

	private FailureAnalyzerConcrete failureAnalyzerConcrete;

	@BeforeEach
	void configureFailureAnalyzer() {
		this.failureAnalyzerConcrete = new FailureAnalyzerConcrete();
	}

	@Test
	void findCauseExtendsThrowable() {
		ThrowableExtendsException ex = new ThrowableExtendsException();
		assertThat(this.failureAnalyzerConcrete.findCause(ex, Throwable.class).getClass()).isNotNull();
	}

	@Test
	void findCauseExtendsOtherException() {
		ExtendsThrowableExtendsException ex = new ExtendsThrowableExtendsException();
		assertThat(this.failureAnalyzerConcrete.findCause(ex, ThrowableExtendsException.class).getClass()).isNotNull();
	}

	@Test
	void findCauseOtherException() {
		ThrowableExtendsException ex = new ThrowableExtendsException();
		assertThat(this.failureAnalyzerConcrete.findCause(ex, OtherException.class)).isNull();
	}

	@Test
	void findCauseNullObject() {
		assertThat(this.failureAnalyzerConcrete.findCause(null, Throwable.class)).isNull();
	}

	static class FailureAnalyzerConcrete extends AbstractFailureAnalyzer<Throwable> {

		@Override
		protected FailureAnalysis analyze(Throwable rootFailure, Throwable cause) {
			return null;
		}

	}

	static class ThrowableExtendsException extends Throwable {

	}

	static class ExtendsThrowableExtendsException extends ThrowableExtendsException {

	}

	static class OtherException extends Throwable {

	}

}
