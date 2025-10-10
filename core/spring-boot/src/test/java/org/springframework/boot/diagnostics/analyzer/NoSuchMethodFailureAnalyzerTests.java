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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.NoSuchMethodFailureAnalyzer.ClassDescriptor;
import org.springframework.boot.diagnostics.analyzer.NoSuchMethodFailureAnalyzer.NoSuchMethodDescriptor;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;
import org.springframework.data.r2dbc.mapping.R2dbcMappingContext;
import org.springframework.util.MimeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoSuchMethodFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 * @author Scott Frederick
 */
@ClassPathOverrides({ "org.springframework:spring-core:5.3.12",
		"org.springframework.data:spring-data-relational:1.1.7.RELEASE",
		"org.springframework.data:spring-data-r2dbc:3.4.7" })
class NoSuchMethodFailureAnalyzerTests {

	@Test
	void parseHotspotErrorMessage() {
		NoSuchMethodDescriptor descriptor = new NoSuchMethodFailureAnalyzer().getNoSuchMethodDescriptor(
				"'boolean org.springframework.util.MimeType.isMoreSpecific(org.springframework.util.MimeType)'");
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getErrorMessage())
			.isEqualTo("'boolean org.springframework.util.MimeType.isMoreSpecific(org.springframework.util.MimeType)'");
		assertThat(descriptor.getClassName()).isEqualTo("org.springframework.util.MimeType");
		assertThat(descriptor.getCandidateLocations()).hasSizeGreaterThan(1);
		List<ClassDescriptor> typeHierarchy = descriptor.getTypeHierarchy();
		assertThat(typeHierarchy).hasSize(1);
		assertThat(typeHierarchy.get(0).getLocation()).asString().contains("spring-core-5.3.12.jar");
	}

	@Test
	void parseOpenJ9ErrorMessage() {
		NoSuchMethodDescriptor descriptor = new NoSuchMethodFailureAnalyzer().getNoSuchMethodDescriptor(
				"org/springframework/util/MimeType.isMoreSpecific(Lorg/springframework/util/MimeType;)Z "
						+ "(loaded from ...) "
						+ "called from class org.springframework.boot.diagnostics.analyzer.NoSuchMethodFailureAnalyzerTests "
						+ "(loaded from ... "
						+ "by org.springframework.boot.testsupport.classpath.ModifiedClassPathClassLoader@e0ce6310).");
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getErrorMessage())
			.isEqualTo("org/springframework/util/MimeType.isMoreSpecific(Lorg/springframework/util/MimeType;)Z");
		assertThat(descriptor.getClassName()).isEqualTo("org.springframework.util.MimeType");
		assertThat(descriptor.getCandidateLocations()).hasSizeGreaterThan(1);
		List<ClassDescriptor> typeHierarchy = descriptor.getTypeHierarchy();
		assertThat(typeHierarchy).hasSize(1);
		assertThat(typeHierarchy.get(0).getLocation()).asString().contains("spring-core-5.3.12.jar");
	}

	@Test
	void whenAMethodOnAClassIsMissingThenNoSuchMethodErrorIsAnalyzed() {
		Throwable failure = createFailureForMissingMethod();
		FailureAnalysis analysis = new NoSuchMethodFailureAnalyzer().analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription())
			.contains(NoSuchMethodFailureAnalyzerTests.class.getName() + ".createFailureForMissingMethod(")
			.contains("isMoreSpecific(")
			.contains("calling method's class, " + NoSuchMethodFailureAnalyzerTests.class.getName() + ",")
			.contains("called method's class, org.springframework.util.MimeType,");
		assertThat(analysis.getAction()).contains(NoSuchMethodFailureAnalyzerTests.class.getName())
			.contains("org.springframework.util.MimeType");
	}

	@Test
	void whenAnInheritedMethodIsMissingThenNoSuchMethodErrorIsAnalyzed() {
		Throwable failure = createFailureForMissingInheritedMethod();
		FailureAnalysis analysis = new NoSuchMethodFailureAnalyzer().analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).contains(R2dbcMappingContext.class.getName() + ".<init>(")
			.contains(R2dbcMappingContext.class.getName() + ".setForceQuote(")
			.contains("calling method's class, org.springframework.data.r2dbc.mapping.R2dbcMappingContext,")
			.contains("called method's class, org.springframework.data.r2dbc.mapping.R2dbcMappingContext,")
			.contains("    org.springframework.data.r2dbc.mapping.R2dbcMappingContext")
			.contains("    org.springframework.data.relational.core.mapping.RelationalMappingContext")
			.contains("    org.springframework.data.mapping.context.AbstractMappingContext");
		assertThat(analysis.getAction()).contains("org.springframework.data.r2dbc.mapping.R2dbcMappingContext");
	}

	@SuppressWarnings("NullAway") // Deliberately trigger exception
	private Throwable createFailureForMissingMethod() {
		try {
			MimeType mimeType = new MimeType("application", "json");
			mimeType.isMoreSpecific(null);
			throw new AssertionError("Should not be reached");
		}
		catch (Throwable ex) {
			return ex;
		}
	}

	private Throwable createFailureForMissingInheritedMethod() {
		try {
			new R2dbcMappingContext();
			throw new AssertionError("Should not be reached");
		}
		catch (Throwable ex) {
			return ex;
		}
	}

}
