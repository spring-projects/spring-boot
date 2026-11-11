/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.logging.logback;

import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.logging.logback.LogbackMetricsAutoConfiguration.LogbackLoggingCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Logback metrics.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass({ MeterRegistry.class, LoggerContext.class, LoggerFactory.class })
@ConditionalOnBean(MeterRegistry.class)
@Conditional(LogbackLoggingCondition.class)
public final class LogbackMetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	LogbackMetrics logbackMetrics() {
		return new LogbackMetrics();
	}

	static class LogbackLoggingCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
			ConditionMessage.Builder message = ConditionMessage.forCondition("LogbackLoggingCondition");
			if (loggerFactory instanceof LoggerContext) {
				return ConditionOutcome.match(message.because("ILoggerFactory is a Logback LoggerContext"));
			}
			return ConditionOutcome.noMatch(
					message.because("ILoggerFactory is an instance of " + loggerFactory.getClass().getCanonicalName()));
		}

	}

}
