/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.autoconfigure.condition;

import java.util.Arrays;
import java.util.Collections;

import javax.annotation.PostConstruct;

import org.junit.Test;

import org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpoint.ContextConditionEvaluation;
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
public class ConditionsReportEndpointTests {

	@Test
	public void invoke() {
		new ApplicationContextRunner().withUserConfiguration(Config.class)
				.run((context) -> {
					ContextConditionEvaluation report = context
							.getBean(ConditionsReportEndpoint.class)
							.applicationConditionEvaluation().getContexts()
							.get(context.getId());
					assertThat(report.getPositiveMatches()).isEmpty();
					assertThat(report.getNegativeMatches()).containsKey("a");
					assertThat(report.getUnconditionalClasses()).contains("b");
					assertThat(report.getExclusions()).contains("com.foo.Bar");
				});
	}

	@Configuration
	@EnableConfigurationProperties
	public static class Config {

		private final ConfigurableApplicationContext context;

		public Config(ConfigurableApplicationContext context) {
			this.context = context;
		}

		@PostConstruct
		public void setupAutoConfigurationReport() {
			ConditionEvaluationReport report = ConditionEvaluationReport
					.get(this.context.getBeanFactory());
			report.recordEvaluationCandidates(Arrays.asList("a", "b"));
			report.recordConditionEvaluation("a", mock(Condition.class),
					mock(ConditionOutcome.class));
			report.recordExclusions(Collections.singletonList("com.foo.Bar"));
		}

		@Bean
		public ConditionsReportEndpoint endpoint() {
			return new ConditionsReportEndpoint(this.context);
		}

	}

}
