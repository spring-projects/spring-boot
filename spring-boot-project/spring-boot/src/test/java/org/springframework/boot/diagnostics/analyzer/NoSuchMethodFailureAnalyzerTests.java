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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.testsupport.runner.classpath.ClassPathOverrides;
import org.springframework.boot.testsupport.runner.classpath.ModifiedClassPathRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link NoSuchMethodFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
@RunWith(ModifiedClassPathRunner.class)
@ClassPathOverrides("javax.servlet:servlet-api:2.5")
public class NoSuchMethodFailureAnalyzerTests {

	@Test
	public void noSuchMethodErrorIsAnalyzed() {
		Throwable failure = createFailure();
		assertThat(failure).isNotNull();
		FailureAnalysis analysis = new NoSuchMethodFailureAnalyzer().analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription())
				.contains(NoSuchMethodFailureAnalyzerTests.class.getName()
						+ ".createFailure(")
				.contains("javax.servlet.ServletContext.addServlet"
						+ "(Ljava/lang/String;Ljavax/servlet/Servlet;)"
						+ "Ljavax/servlet/ServletRegistration$Dynamic;")
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
