/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jooq.autoconfigure;

import org.jooq.DSLContext;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.r2dbc.autoconfigure.R2dbcAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoDslContextBeanFailureAnalyzer}.
 *
 * @author Andy Wilkinson
 */
class NoDslContextBeanFailureAnalyzerTests {

	@Test
	void noAnalysisWithoutR2dbcAutoConfiguration() {
		new ApplicationContextRunner().run((context) -> {
			NoDslContextBeanFailureAnalyzer failureAnalyzer = new NoDslContextBeanFailureAnalyzer(
					context.getBeanFactory());
			assertThat(failureAnalyzer.analyze(new NoSuchBeanDefinitionException(DSLContext.class))).isNull();
		});
	}

	@Test
	void analysisWithR2dbcAutoConfiguration() {
		new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(R2dbcAutoConfiguration.class))
			.run((context) -> {
				NoDslContextBeanFailureAnalyzer failureAnalyzer = new NoDslContextBeanFailureAnalyzer(
						context.getBeanFactory());
				assertThat(failureAnalyzer.analyze(new NoSuchBeanDefinitionException(DSLContext.class))).isNotNull();
			});
	}

}
