/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.diagnostics.analyzer;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.annotation.AbstractNamedValueMethodArgumentResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MissingParameterNamesFailureAnalyzer}.
 *
 * @author Phillip Webb
 */
class MissingParameterNamesFailureAnalyzerTests {

	@Test
	void analyzeWhenMissingParametersExceptionReturnsFailure() throws Exception {
		MissingParameterNamesFailureAnalyzer analyzer = new MissingParameterNamesFailureAnalyzer();
		FailureAnalysis analysis = analyzer.analyze(getSpringFrameworkMissingParameterException());
		assertThat(analysis.getDescription())
			.isEqualTo(String.format("Name for argument of type [java.lang.String] not specified, and parameter name "
					+ "information not available via reflection. Ensure that the compiler uses the '-parameters' flag.:%n"));
		assertThat(analysis.getAction()).isEqualTo(MissingParameterNamesFailureAnalyzer.ACTION);
	}

	@Test
	void analyzeForMissingParametersWhenMissingParametersExceptionReturnsFailure() throws Exception {
		FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer
			.analyzeForMissingParameters(getSpringFrameworkMissingParameterException());
		assertThat(analysis.getDescription())
			.isEqualTo(String.format("Name for argument of type [java.lang.String] not specified, and parameter name "
					+ "information not available via reflection. Ensure that the compiler uses the '-parameters' flag.:%n"));
		assertThat(analysis.getAction()).isEqualTo(MissingParameterNamesFailureAnalyzer.ACTION);
	}

	@Test
	void analyzeForMissingParametersWhenInCauseReturnsFailure() throws Exception {
		RuntimeException exception = new RuntimeException("Badness", getSpringFrameworkMissingParameterException());
		FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(exception);
		assertThat(analysis.getDescription())
			.isEqualTo(String.format("Name for argument of type [java.lang.String] not specified, and parameter name "
					+ "information not available via reflection. Ensure that the compiler uses the '-parameters' flag.:%n%n"
					+ "    Resulting Failure: java.lang.RuntimeException: Badness"));
		assertThat(analysis.getAction()).isEqualTo(MissingParameterNamesFailureAnalyzer.ACTION);
	}

	@Test
	void analyzeForMissingParametersWhenInSuppressedReturnsFailure() throws Exception {
		RuntimeException exception = new RuntimeException("Badness");
		exception.addSuppressed(getSpringFrameworkMissingParameterException());
		FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(exception);
		assertThat(analysis.getDescription())
			.isEqualTo(String.format("Name for argument of type [java.lang.String] not specified, and parameter name "
					+ "information not available via reflection. Ensure that the compiler uses the '-parameters' flag.:%n%n"
					+ "    Resulting Failure: java.lang.RuntimeException: Badness"));
		assertThat(analysis.getAction()).isEqualTo(MissingParameterNamesFailureAnalyzer.ACTION);
	}

	@Test
	void analyzeForMissingParametersWhenNotPresentReturnsNull() {
		RuntimeException exception = new RuntimeException("Badness");
		FailureAnalysis analysis = MissingParameterNamesFailureAnalyzer.analyzeForMissingParameters(exception);
		assertThat(analysis).isNull();
	}

	private RuntimeException getSpringFrameworkMissingParameterException() throws Exception {
		MockResolver resolver = new MockResolver();
		Method method = getClass().getDeclaredMethod("example", String.class);
		MethodParameter parameter = new MethodParameter(method, 0);
		try {
			resolver.resolveArgument(parameter, null, null, null);
		}
		catch (RuntimeException ex) {
			return ex;
		}
		throw new AssertionError("Did not throw");
	}

	void example(String name) {
	}

	static class MockResolver extends AbstractNamedValueMethodArgumentResolver {

		@Override
		public boolean supportsParameter(MethodParameter parameter) {
			return true;
		}

		@Override
		protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
			return new NamedValueInfo("", false, null);
		}

		@Override
		protected Object resolveName(String name, MethodParameter parameter, NativeWebRequest request)
				throws Exception {
			return null;
		}

	}

}
