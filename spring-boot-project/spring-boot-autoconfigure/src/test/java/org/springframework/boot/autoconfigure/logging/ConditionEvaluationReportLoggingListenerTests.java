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

package org.springframework.boot.autoconfigure.logging;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.http.HttpMessageConvertersAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockServletContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ConditionEvaluationReportLoggingListener}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
@ExtendWith(OutputCaptureExtension.class)
class ConditionEvaluationReportLoggingListenerTests {

	private final ConditionEvaluationReportLoggingListener initializer = new ConditionEvaluationReportLoggingListener();

	@Test
	void logsDebugOnContextRefresh(CapturedOutput output) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		this.initializer.initialize(context);
		context.register(Config.class);
		withDebugLogging(() -> context.refresh());
		assertThat(output).contains("CONDITIONS EVALUATION REPORT");
	}

	@Test
	void logsDebugOnApplicationFailedEvent(CapturedOutput output) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		this.initializer.initialize(context);
		context.register(ErrorConfig.class);
		assertThatExceptionOfType(Exception.class).isThrownBy(context::refresh)
			.satisfies((ex) -> withDebugLogging(() -> context
				.publishEvent(new ApplicationFailedEvent(new SpringApplication(), new String[0], context, ex))));
		assertThat(output).contains("CONDITIONS EVALUATION REPORT");
	}

	@Test
	void logsInfoGuidanceToEnableDebugLoggingOnApplicationFailedEvent(CapturedOutput output) {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		this.initializer.initialize(context);
		context.register(ErrorConfig.class);
		assertThatExceptionOfType(Exception.class).isThrownBy(context::refresh)
			.satisfies((ex) -> withInfoLogging(() -> context
				.publishEvent(new ApplicationFailedEvent(new SpringApplication(), new String[0], context, ex))));
		assertThat(output).doesNotContain("CONDITIONS EVALUATION REPORT")
			.contains("re-run your application with 'debug' enabled");
	}

	@Test
	void canBeUsedInApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(Config.class);
		new ConditionEvaluationReportLoggingListener().initialize(context);
		context.refresh();
		assertThat(context.getBean(ConditionEvaluationReport.class)).isNotNull();
	}

	@Test
	void canBeUsedInNonGenericApplicationContext() {
		AnnotationConfigServletWebApplicationContext context = new AnnotationConfigServletWebApplicationContext();
		context.setServletContext(new MockServletContext());
		context.register(Config.class);
		new ConditionEvaluationReportLoggingListener().initialize(context);
		context.refresh();
		assertThat(context.getBean(ConditionEvaluationReport.class)).isNotNull();
	}

	private void withDebugLogging(Runnable runnable) {
		withLoggingLevel(Level.DEBUG, runnable);
	}

	private void withInfoLogging(Runnable runnable) {
		withLoggingLevel(Level.INFO, runnable);
	}

	private void withLoggingLevel(Level logLevel, Runnable runnable) {
		Logger logger = ((LoggerContext) LoggerFactory.getILoggerFactory())
			.getLogger(ConditionEvaluationReportLogger.class);
		Level currentLevel = logger.getLevel();
		logger.setLevel(logLevel);
		try {
			runnable.run();
		}
		finally {
			logger.setLevel(currentLevel);
		}
	}

	@Configuration(proxyBeanMethods = false)
	@Import({ WebMvcAutoConfiguration.class, HttpMessageConvertersAutoConfiguration.class,
			PropertyPlaceholderAutoConfiguration.class })
	static class Config {

	}

	@Configuration(proxyBeanMethods = false)
	@Import(WebMvcAutoConfiguration.class)
	static class ErrorConfig {

		@Bean
		String iBreak() {
			throw new RuntimeException();
		}

	}

}
