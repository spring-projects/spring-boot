/*
 * Copyright 2012-2019 the original author or authors.
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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.analyzer.NoSuchMethodFailureAnalyzer.NoSuchMethodDescriptor;
import org.springframework.boot.testsupport.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NoSuchMethodFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 * @author Stephane Nicoll
 */
@ClassPathOverrides("javax.servlet:servlet-api:2.5")
class NoSuchMethodFailureAnalyzerTests {

	@Test
	void parseJava8ErrorMessage() {
		NoSuchMethodDescriptor descriptor = new NoSuchMethodFailureAnalyzer().getNoSuchMethodDescriptor(
				"javax.servlet.ServletContext.addServlet(Ljava/lang/String;Ljavax/servlet/Servlet;)"
						+ "Ljavax/servlet/ServletRegistration$Dynamic;");
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getErrorMessage())
				.isEqualTo("javax.servlet.ServletContext.addServlet(Ljava/lang/String;Ljavax/servlet/Servlet;)"
						+ "Ljavax/servlet/ServletRegistration$Dynamic;");
		assertThat(descriptor.getClassName()).isEqualTo("javax.servlet.ServletContext");
		assertThat(descriptor.getCandidateLocations().size()).isGreaterThan(1);
		assertThat(descriptor.getActualLocation()).asString().contains("servlet-api-2.5.jar");
	}

	@Test
	void parseJavaOpenJ9ErrorMessage() {
		NoSuchMethodDescriptor descriptor = new NoSuchMethodFailureAnalyzer().getNoSuchMethodDescriptor(
				"javax/servlet/ServletContext.addServlet(Ljava/lang/String;Ljavax/servlet/Servlet;)"
						+ "Ljavax/servlet/ServletRegistration$Dynamic; (loaded from file...");
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getErrorMessage())
				.isEqualTo("javax/servlet/ServletContext.addServlet(Ljava/lang/String;Ljavax/servlet/Servlet;)"
						+ "Ljavax/servlet/ServletRegistration$Dynamic;");
		assertThat(descriptor.getClassName()).isEqualTo("javax.servlet.ServletContext");
		assertThat(descriptor.getCandidateLocations().size()).isGreaterThan(1);
		assertThat(descriptor.getActualLocation()).asString().contains("servlet-api-2.5.jar");
	}

	@Test
	void parseJavaImprovedHotspotErrorMessage() {
		NoSuchMethodDescriptor descriptor = new NoSuchMethodFailureAnalyzer().getNoSuchMethodDescriptor(
				"'javax.servlet.ServletRegistration$Dynamic javax.servlet.ServletContext.addServlet("
						+ "java.lang.String, javax.servlet.Servlet)'");
		assertThat(descriptor).isNotNull();
		assertThat(descriptor.getErrorMessage())
				.isEqualTo("'javax.servlet.ServletRegistration$Dynamic javax.servlet.ServletContext.addServlet("
						+ "java.lang.String, javax.servlet.Servlet)'");
		assertThat(descriptor.getClassName()).isEqualTo("javax.servlet.ServletContext");
		assertThat(descriptor.getCandidateLocations().size()).isGreaterThan(1);
		assertThat(descriptor.getActualLocation()).asString().contains("servlet-api-2.5.jar");
	}

	@Test
	void noSuchMethodErrorIsAnalyzed() {
		Throwable failure = createFailure();
		assertThat(failure).isNotNull();
		FailureAnalysis analysis = new NoSuchMethodFailureAnalyzer().analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription())
				.contains(NoSuchMethodFailureAnalyzerTests.class.getName() + ".createFailure(").contains("addServlet(")
				.contains("class, javax.servlet.ServletContext,");
	}

	private Throwable createFailure() {
		try {
			ServletContext servletContext = mock(ServletContext.class);
			servletContext.addServlet("example", new HttpServlet() {
			});
			return null;
		}
		catch (Throwable ex) {
			return ex;
		}
	}

}
