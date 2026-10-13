/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.micrometer.metrics.autoconfigure.logging.log4j2;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.logging.Log4j2Metrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.spi.LoggerContext;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.micrometer.metrics.autoconfigure.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.logging.log4j2.Log4J2MetricsAutoConfiguration.Log4JCoreLoggerContextCondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Auto-configuration for Log4J2 metrics.
 *
 * @author Andy Wilkinson
 * @since 4.0.0
 */
@AutoConfiguration(after = { MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class })
@ConditionalOnClass(value = { Log4j2Metrics.class, LogManager.class },
		name = "org.apache.logging.log4j.core.LoggerContext")
@ConditionalOnBean(MeterRegistry.class)
@Conditional(Log4JCoreLoggerContextCondition.class)
public final class Log4J2MetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	Log4j2Metrics log4j2Metrics() {
		return new Log4j2Metrics();
	}

	static class Log4JCoreLoggerContextCondition extends SpringBootCondition {

		private static final String LOGGER_CONTEXT_CLASS_NAME = "org.apache.logging.log4j.core.LoggerContext";

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {
			LoggerContext loggerContext = LogManager.getContext(false);
			try {
				if (Class.forName(LOGGER_CONTEXT_CLASS_NAME).isInstance(loggerContext)) {
					return ConditionOutcome.match("LoggerContext was an instance of " + LOGGER_CONTEXT_CLASS_NAME);
				}
			}
			catch (Throwable ex) {
				// Continue with no match
			}
			return ConditionOutcome.noMatch("LoggerContext was not an instance of " + LOGGER_CONTEXT_CLASS_NAME);
		}

	}

}
