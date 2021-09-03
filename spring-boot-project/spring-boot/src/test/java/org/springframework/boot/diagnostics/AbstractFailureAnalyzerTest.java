package org.springframework.boot.diagnostics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractFailureAnalyzerTest {

	private FailureAnalyzerConcrete failureAnalyzerConcrete;

	@BeforeEach
	void configureFailureAnalyzer() {
		failureAnalyzerConcrete = new FailureAnalyzerConcrete();
	}

	@Test
	void findCauseExtendsThrowable() {
		ThrowableExtendsException ex = new ThrowableExtendsException();
		assertNotNull(failureAnalyzerConcrete.findCause(ex, Throwable.class).getClass());
	}

	@Test
	void findCauseExtendsOtherException() {
		ExtendsThrowableExtendsException ex = new ExtendsThrowableExtendsException();
		assertNotNull(failureAnalyzerConcrete.findCause(ex, ThrowableExtendsException.class).getClass());
	}

	@Test
	void findCauseOtherException() {
		ThrowableExtendsException ex = new ThrowableExtendsException();
		assertNull(failureAnalyzerConcrete.findCause(ex, OtherException.class));
	}

	@Test
	void findCauseNullObject() {
		assertNull(failureAnalyzerConcrete.findCause(null, Throwable.class));
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