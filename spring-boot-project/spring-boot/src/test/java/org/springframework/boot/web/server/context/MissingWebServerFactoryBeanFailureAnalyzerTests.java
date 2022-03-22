/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.server.context;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MissingWebServerFactoryBeanFailureAnalyzer}.
 *
 * @author Guirong Hu
 */
class MissingWebServerFactoryBeanFailureAnalyzerTests {

	@Test
	void missingServletWebServerFactoryBeanFailure() {
		ApplicationContextException failure = createFailure(new ServletWebServerApplicationContext());
		assertThat(failure).isNotNull();
		FailureAnalysis analysis = new MissingWebServerFactoryBeanFailureAnalyzer().analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).isEqualTo(
				"Reason: The running web application is of type servlet, but the dependent class is missing.");
		assertThat(analysis.getAction()).isEqualTo(
				"Check your application's dependencies on supported web servers or configuration of web application type.");
	}

	@Test
	void missingReactiveWebServerFactoryBeanFailure() {
		ApplicationContextException failure = createFailure(new ReactiveWebServerApplicationContext());
		FailureAnalysis analysis = new MissingWebServerFactoryBeanFailureAnalyzer().analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).isEqualTo(
				"Reason: The running web application is of type reactive, but the dependent class is missing.");
		assertThat(analysis.getAction()).isEqualTo(
				"Check your application's dependencies on supported web servers or configuration of web application type.");
	}

	private ApplicationContextException createFailure(ConfigurableApplicationContext context) {
		try {
			context.refresh();
			context.close();
			return null;
		}
		catch (ApplicationContextException ex) {
			return ex;
		}
	}

}
