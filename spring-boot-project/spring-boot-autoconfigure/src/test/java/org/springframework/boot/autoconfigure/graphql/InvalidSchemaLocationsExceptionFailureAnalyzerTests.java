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

package org.springframework.boot.autoconfigure.graphql;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InvalidSchemaLocationsExceptionFailureAnalyzer}
 *
 * @author Brian Clozel
 */
class InvalidSchemaLocationsExceptionFailureAnalyzerTests {

	private final InvalidSchemaLocationsExceptionFailureAnalyzer analyzer = new InvalidSchemaLocationsExceptionFailureAnalyzer();

	private final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

	private final String[] missingLocation = new String[] {
			"classpath:org/springframework/boot/autoconfigure/graphql/missing/" };

	private final String[] existingLocation = new String[] {
			"classpath:org/springframework/boot/autoconfigure/graphql/" };

	@Test
	void shouldReportCause() {
		InvalidSchemaLocationsException exception = new InvalidSchemaLocationsException(this.existingLocation,
				this.resolver);
		FailureAnalysis analysis = this.analyzer.analyze(exception);
		assertThat(analysis.getCause()).isInstanceOf(InvalidSchemaLocationsException.class);
		assertThat(analysis.getAction()).contains("Check that the following locations contain schema files:");
	}

	@Test
	void shouldListUnresolvableLocation() {
		InvalidSchemaLocationsException exception = new InvalidSchemaLocationsException(this.missingLocation,
				this.resolver);
		FailureAnalysis analysis = this.analyzer.analyze(exception);
		assertThat(analysis.getAction()).contains(this.existingLocation[0]).doesNotContain("file:");
	}

	@Test
	void shouldListExistingLocation() {
		InvalidSchemaLocationsException exception = new InvalidSchemaLocationsException(this.existingLocation,
				this.resolver);
		FailureAnalysis analysis = this.analyzer.analyze(exception);
		assertThat(analysis.getAction()).contains(this.existingLocation[0]).contains("file:");
	}

}
