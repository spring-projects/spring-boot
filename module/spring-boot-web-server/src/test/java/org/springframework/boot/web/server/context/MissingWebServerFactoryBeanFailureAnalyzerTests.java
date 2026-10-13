/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server.context;

import org.junit.jupiter.api.Test;

import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.web.server.reactive.ReactiveWebServerFactory;
import org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContext;
import org.springframework.boot.web.server.servlet.ServletWebServerFactory;
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MissingWebServerFactoryBeanFailureAnalyzer}.
 *
 * @author Guirong Hu
 * @author Andy Wilkinson
 */
class MissingWebServerFactoryBeanFailureAnalyzerTests {

	@Test
	void missingServletWebServerFactoryBeanFailure() {
		ApplicationContextException failure = createFailure(new ServletWebServerApplicationContext());
		assertThat(failure).isNotNull();
		FailureAnalysis analysis = new MissingWebServerFactoryBeanFailureAnalyzer().analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).isEqualTo("Web application could not be started as there was no "
				+ ServletWebServerFactory.class.getName() + " bean defined in the context.");
		assertThat(analysis.getAction()).isEqualTo(
				"Check your application's dependencies for a supported servlet web server.\nCheck the configured web "
						+ "application type.");
	}

	@Test
	void missingReactiveWebServerFactoryBeanFailure() {
		ApplicationContextException failure = createFailure(new ReactiveWebServerApplicationContext());
		FailureAnalysis analysis = new MissingWebServerFactoryBeanFailureAnalyzer().analyze(failure);
		assertThat(analysis).isNotNull();
		assertThat(analysis.getDescription()).isEqualTo("Web application could not be started as there was no "
				+ ReactiveWebServerFactory.class.getName() + " bean defined in the context.");
		assertThat(analysis.getAction()).isEqualTo(
				"Check your application's dependencies for a supported reactive web server.\nCheck the configured web "
						+ "application type.");
	}

	private ApplicationContextException createFailure(ConfigurableApplicationContext context) {
		try {
			context.refresh();
			context.close();
			throw new AssertionError("Should not be reached");
		}
		catch (ApplicationContextException ex) {
			return ex;
		}
	}

}
