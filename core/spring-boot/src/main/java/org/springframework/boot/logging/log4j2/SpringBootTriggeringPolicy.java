/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.logging.log4j2;

import java.util.Objects;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.CompositeTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.CronTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.RollingFileManager;
import org.apache.logging.log4j.core.appender.rolling.SizeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TimeBasedTriggeringPolicy;
import org.apache.logging.log4j.core.appender.rolling.TriggeringPolicy;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginBuilderFactory;
import org.apache.logging.log4j.core.config.plugins.PluginConfiguration;
import org.apache.logging.log4j.core.util.Builder;
import org.jspecify.annotations.Nullable;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.core.convert.ConversionException;
import org.springframework.util.Assert;

/**
 * Factory for creating a standard Log4j2 {@link TriggeringPolicy} based on configuration
 * attributes. The supported strategies are {@code size}, {@code time},
 * {@code size-and-time}, and {@code cron}.
 *
 * @author HoJoo Moon
 * @author Stephane Nicoll
 * @since 4.1.0
 */
@Plugin(name = "SpringBootTriggeringPolicy", category = Node.CATEGORY, elementType = "TriggeringPolicy",
		deferChildren = true, printObject = true)
public abstract class SpringBootTriggeringPolicy implements TriggeringPolicy {

	private SpringBootTriggeringPolicy() {
	}

	@Override
	public void initialize(RollingFileManager manager) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isTriggeringEvent(LogEvent logEvent) {
		throw new UnsupportedOperationException();
	}

	@PluginBuilderFactory
	public static SpringBootTriggeringPolicyBuilder newBuilder() {
		return new SpringBootTriggeringPolicyBuilder();
	}

	/**
	 * Builder for creating a {@link TriggeringPolicy}.
	 */
	public static class SpringBootTriggeringPolicyBuilder implements Builder<TriggeringPolicy> {

		private static final String DEFAULT_STRATEGY = "size";

		private static final String DEFAULT_MAX_FILE_SIZE = "10MB";

		private static final String DEFAULT_TIME_INTERVAL = "1";

		private static final String DEFAULT_CRON_EXPRESSION = "0 0 0 * * ?";

		@PluginAttribute("strategy")
		private @Nullable String strategy;

		@PluginAttribute("maxFileSize")
		private @Nullable String maxFileSize;

		@PluginAttribute("timeInterval")
		private @Nullable Integer timeInterval;

		@PluginAttribute("timeModulate")
		private @Nullable Boolean timeModulate;

		@PluginAttribute("cronExpression")
		private @Nullable String cronExpression;

		@PluginConfiguration
		private @Nullable Configuration configuration;

		@Override
		public TriggeringPolicy build() {
			RollingPolicyStrategy resolvedStrategy = getRollingPolicyStrategy();
			return switch (resolvedStrategy) {
				case TIME -> createTimePolicy();
				case SIZE_AND_TIME -> CompositeTriggeringPolicy.createPolicy(createSizePolicy(), createTimePolicy());
				case CRON -> createCronPolicy();
				case SIZE -> createSizePolicy();
			};
		}

		private static RollingPolicyStrategy getRollingPolicyStrategy() {
			String resolvedStrategy = getSystemProperty(RollingPolicySystemProperty.STRATEGY, DEFAULT_STRATEGY);
			try {
				return Objects.requireNonNull(ApplicationConversionService.getSharedInstance()
					.convert(resolvedStrategy, RollingPolicyStrategy.class));
			}
			catch (ConversionException ex) {
				throw new IllegalArgumentException(
						"Unsupported rolling policy strategy '%s'".formatted(resolvedStrategy));
			}
		}

		private TriggeringPolicy createSizePolicy() {
			String size = getSystemProperty(RollingPolicySystemProperty.MAX_FILE_SIZE, DEFAULT_MAX_FILE_SIZE);
			return SizeBasedTriggeringPolicy.createPolicy(size);
		}

		private TriggeringPolicy createTimePolicy() {
			int interval = Integer
				.parseInt(getSystemProperty(RollingPolicySystemProperty.TIME_INTERVAL, DEFAULT_TIME_INTERVAL));
			boolean modulate = Boolean
				.parseBoolean(getSystemProperty(RollingPolicySystemProperty.TIME_MODULATE, Boolean.FALSE.toString()));
			return TimeBasedTriggeringPolicy.newBuilder().withInterval(interval).withModulate(modulate).build();
		}

		private TriggeringPolicy createCronPolicy() {
			Assert.notNull(this.configuration, "configuration must not be null");
			Configuration configuration = this.configuration;

			String schedule = getSystemProperty(RollingPolicySystemProperty.CRON, DEFAULT_CRON_EXPRESSION);
			return CronTriggeringPolicy.createPolicy(configuration, null, schedule);
		}

		private static String getSystemProperty(RollingPolicySystemProperty property, String fallback) {
			String value = System.getProperty(property.getEnvironmentVariableName());
			return (value != null) ? value : fallback;
		}

		SpringBootTriggeringPolicyBuilder setConfiguration(Configuration configuration) {
			this.configuration = configuration;
			return this;
		}

	}

}
