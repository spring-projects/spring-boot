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

package org.springframework.boot.diagnostics.analyzer;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.context.ApplicationContextException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReactiveMissingWebServerFactoryFailureAnalyzer}
 *
 * @author Zhao Xianming
 */
public class ReactiveMissingWebServerFactoryFailureAnalyzerTests {

	@Test
	void missingWebServerFactoryFailure() {
		ApplicationContextException ex = createFailureMissingBean();
		FailureAnalysis failureAnalysis = new ReactiveMissingWebServerFactoryFailureAnalyzer().analyze(null, ex);
		assertThat(failureAnalysis).isNull();
	}

	@Test
	void multipleWebServerFactoryFailure() {
		ApplicationContextException ex = createFailureMultipleBean();
		FailureAnalysis failureAnalysis = new ReactiveMissingWebServerFactoryFailureAnalyzer().analyze(null, ex);
		assertThat(failureAnalysis).isNull();
	}

	private ApplicationContextException createFailureMissingBean() {
		return new ApplicationContextException(
				"Unable to start ReactiveWebApplicationContext due to missing ReactiveWebServerFactory bean.");
	}

	private ApplicationContextException createFailureMultipleBean() {
		return new ApplicationContextException(
				"Unable to start ReactiveWebApplicationContext due to multiple ReactiveWebServerFactory beans.");
	}

}
