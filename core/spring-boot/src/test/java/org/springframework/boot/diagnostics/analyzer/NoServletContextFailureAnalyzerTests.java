/*
 * Copyright 2012-present the original author or authors.
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

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.diagnostics.FailureAnalysis;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoServletContextFailureAnalyzer}.
 *
 * @author xoruddl taeyun1411@gmail.com
 */
class NoServletContextFailureAnalyzerTests {

	private final NoServletContextFailureAnalyzer analyzer = new NoServletContextFailureAnalyzer();

	@Test
	void analyzesMissingServletContextForResourceHandlerMapping() {
		BeanCreationException failure = new BeanCreationException("resourceHandlerMapping", "Failed to create bean",
				new IllegalStateException("No ServletContext set"));
		FailureAnalysis analysis = this.analyzer.analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).contains("Spring MVC infrastructure");
		assertThat(analysis.getAction()).contains("@EnableWebMvc").contains("@WebMvcTest");
	}

	@Test
	void doesNotAnalyzeMissingServletContextForAnotherBean() {
		BeanCreationException failure = new BeanCreationException("anotherBean", "Failed to create bean",
				new IllegalStateException("No ServletContext set"));
		assertThat(this.analyzer.analyze(failure)).isNull();
	}

}
