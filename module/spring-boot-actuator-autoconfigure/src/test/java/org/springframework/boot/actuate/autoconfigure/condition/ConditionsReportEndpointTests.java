/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.autoconfigure.condition;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpoint.ContextConditionsDescriptor;
import org.springframework.boot.autoconfigure.condition.ConditionEvaluationReport;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ConditionsReportEndpoint}.
 *
 * @author Greg Turnquist
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class ConditionsReportEndpointTests {

	@Test
	void invoke() {
		new ApplicationContextRunner().withUserConfiguration(Config.class).run((context) -> {
			ContextConditionsDescriptor report = context.getBean(ConditionsReportEndpoint.class)
				.conditions()
				.getContexts()
				.get(context.getId());
			assertThat(report).isNotNull();
			assertThat(report.getPositiveMatches()).isEmpty();
			assertThat(report.getNegativeMatches()).containsKey("a");
			assertThat(report.getUnconditionalClasses()).contains("b");
			assertThat(report.getExclusions()).contains("com.foo.Bar");
		});
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties
	static class Config {

		private final ConfigurableApplicationContext context;

		Config(ConfigurableApplicationContext context) {
			this.context = context;
			setupAutoConfigurationReport();
		}

		private void setupAutoConfigurationReport() {
			ConditionEvaluationReport report = ConditionEvaluationReport.get(this.context.getBeanFactory());
			report.recordEvaluationCandidates(Arrays.asList("a", "b"));
			report.recordConditionEvaluation("a", mock(Condition.class), mock(ConditionOutcome.class));
			report.recordExclusions(Collections.singletonList("com.foo.Bar"));
		}

		@Bean
		ConditionsReportEndpoint endpoint() {
			return new ConditionsReportEndpoint(this.context);
		}

	}

}
