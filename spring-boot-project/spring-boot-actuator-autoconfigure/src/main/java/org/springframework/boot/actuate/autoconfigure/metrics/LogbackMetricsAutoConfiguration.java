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

package org.springframework.boot.actuate.autoconfigure.metrics;

import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration.LogbackLoggingCondition;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Logback metrics.
 *
 * @author Stephane Nicoll
 * @since 2.1.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureAfter(MetricsAutoConfiguration.class)
@ConditionalOnClass({ MeterRegistry.class, LoggerContext.class, LoggerFactory.class })
@ConditionalOnBean(MeterRegistry.class)
@Conditional(LogbackLoggingCondition.class)
public class LogbackMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public LogbackMetrics logbackMetrics() {
		return new LogbackMetrics();
	}

	static class LogbackLoggingCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("LogbackLoggingCondition");
			if (loggerFactory instanceof LoggerContext) {
				return ConditionOutcome.match(
						message.because("ILoggerFactory is a Logback LoggerContext"));
			}
			return ConditionOutcome
					.noMatch(message.because("ILoggerFactory is an instance of "
							+ loggerFactory.getClass().getCanonicalName()));
		}

	}

}
