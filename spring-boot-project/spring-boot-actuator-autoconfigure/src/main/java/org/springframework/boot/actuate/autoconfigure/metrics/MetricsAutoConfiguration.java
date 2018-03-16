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

package org.springframework.boot.actuate.autoconfigure.metrics;

import ch.qos.logback.classic.LoggerContext;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.logging.LogbackMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Micrometer-based metrics.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @since 2.0.0
 */
@Configuration
@ConditionalOnClass(Timed.class)
@EnableConfigurationProperties(MetricsProperties.class)
@AutoConfigureBefore(CompositeMeterRegistryAutoConfiguration.class)
public class MetricsAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public Clock micrometerClock() {
		return Clock.SYSTEM;
	}

	@Bean
	public static MeterRegistryPostProcessor meterRegistryPostProcessor(
			ApplicationContext context) {
		return new MeterRegistryPostProcessor(context);
	}

	@Bean
	@Order(0)
	public PropertiesMeterFilter propertiesMeterFilter(MetricsProperties properties) {
		return new PropertiesMeterFilter(properties);
	}

	@Configuration
	@ConditionalOnProperty(value = "management.metrics.binders.jvm.enabled", matchIfMissing = true)
	static class JvmMeterBindersConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public JvmGcMetrics jvmGcMetrics() {
			return new JvmGcMetrics();
		}

		@Bean
		@ConditionalOnMissingBean
		public JvmMemoryMetrics jvmMemoryMetrics() {
			return new JvmMemoryMetrics();
		}

		@Bean
		@ConditionalOnMissingBean
		public JvmThreadMetrics jvmThreadMetrics() {
			return new JvmThreadMetrics();
		}

		@Bean
		@ConditionalOnMissingBean
		public ClassLoaderMetrics classLoaderMetrics() {
			return new ClassLoaderMetrics();
		}

	}

	@Configuration
	static class MeterBindersConfiguration {

		@Bean
		@ConditionalOnClass(name = { "ch.qos.logback.classic.LoggerContext",
				"org.slf4j.LoggerFactory" })
		@Conditional(LogbackLoggingCondition.class)
		@ConditionalOnMissingBean(LogbackMetrics.class)
		@ConditionalOnProperty(value = "management.metrics.binders.logback.enabled", matchIfMissing = true)
		public LogbackMetrics logbackMetrics() {
			return new LogbackMetrics();
		}

		@Bean
		@ConditionalOnProperty(value = "management.metrics.binders.uptime.enabled", matchIfMissing = true)
		@ConditionalOnMissingBean
		public UptimeMetrics uptimeMetrics() {
			return new UptimeMetrics();
		}

		@Bean
		@ConditionalOnProperty(value = "management.metrics.binders.processor.enabled", matchIfMissing = true)
		@ConditionalOnMissingBean
		public ProcessorMetrics processorMetrics() {
			return new ProcessorMetrics();
		}

		@Bean
		@ConditionalOnProperty(name = "management.metrics.binders.files.enabled", matchIfMissing = true)
		@ConditionalOnMissingBean
		public FileDescriptorMetrics fileDescriptorMetrics() {
			return new FileDescriptorMetrics();
		}

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
