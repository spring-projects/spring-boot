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
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.core.LifeCycle;
import org.apache.logging.log4j.core.LifeCycle2;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rolling.AbstractTriggeringPolicy;
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
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.util.Builder;
import org.jspecify.annotations.Nullable;

/**
 * {@link TriggeringPolicy} that selects one of several standard Log4j2
 * {@link TriggeringPolicy TriggeringPolicies} based on configuration attributes. The
 * supported strategies are {@code size}, {@code time}, {@code size-and-time}, and
 * {@code cron}.
 *
 * @author hojooo
 */
@Plugin(name = "SpringBootTriggeringPolicy", category = Node.CATEGORY, elementType = "TriggeringPolicy",
		deferChildren = true, printObject = true)
public final class SpringBootTriggeringPolicy extends AbstractTriggeringPolicy {

	private final TriggeringPolicy delegate;

	private SpringBootTriggeringPolicy(TriggeringPolicy delegate) {
		this.delegate = delegate;
	}

	TriggeringPolicy getDelegate() {
		return this.delegate;
	}

	@Override
	public void initialize(RollingFileManager manager) {
		this.delegate.initialize(manager);
	}

	@Override
	public boolean isTriggeringEvent(LogEvent event) {
		return this.delegate.isTriggeringEvent(event);
	}

	@Override
	public void start() {
		super.start();
		if (this.delegate instanceof LifeCycle lifecycle) {
			lifecycle.start();
		}
	}

	@Override
	public boolean stop(long timeout, TimeUnit timeUnit) {
		setStopping();
		boolean result = true;
		if (this.delegate instanceof LifeCycle2 lifecycle2) {
			result = lifecycle2.stop(timeout, timeUnit);
		}
		else if (this.delegate instanceof LifeCycle lifecycle) {
			lifecycle.stop();
		}
		setStopped();
		return result;
	}

	@Override
	public boolean isStarted() {
		if (this.delegate instanceof LifeCycle lifecycle) {
			return lifecycle.isStarted();
		}
		return super.isStarted();
	}

	@Override
	public String toString() {
		return "SpringBootTriggeringPolicy{" + this.delegate + "}";
	}

	@PluginBuilderFactory
	public static SpringBootTriggeringPolicyBuilder newBuilder() {
		return new SpringBootTriggeringPolicyBuilder();
	}

	@PluginFactory
	public static SpringBootTriggeringPolicy createPolicy(@PluginAttribute("strategy") @Nullable String strategy,
			@PluginAttribute("maxFileSize") @Nullable String maxFileSize,
			@PluginAttribute("timeInterval") @Nullable Integer timeInterval,
			@PluginAttribute("timeModulate") @Nullable Boolean timeModulate,
			@PluginAttribute("cronExpression") @Nullable String cronExpression,
			@PluginConfiguration Configuration configuration) {
		return newBuilder().setStrategy(strategy)
			.setMaxFileSize(maxFileSize)
			.setTimeInterval(timeInterval)
			.setTimeModulate(timeModulate)
			.setCronExpression(cronExpression)
			.setConfiguration(configuration)
			.build();
	}

	/**
	 * Builder for {@link SpringBootTriggeringPolicy}.
	 */
	public static class SpringBootTriggeringPolicyBuilder implements Builder<SpringBootTriggeringPolicy> {

		private static final String DEFAULT_STRATEGY = "size";

		private static final String DEFAULT_MAX_FILE_SIZE = "10MB";

		private static final int DEFAULT_TIME_INTERVAL = 1;

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
		public SpringBootTriggeringPolicy build() {
			// Read strategy from system properties first, then from attributes
			String resolvedStrategy = System.getProperty("LOG4J2_ROLLINGPOLICY_STRATEGY");
			if (resolvedStrategy == null) {
				resolvedStrategy = (this.strategy != null) ? this.strategy : DEFAULT_STRATEGY;
			}
			TriggeringPolicy policy = switch (resolvedStrategy) {
				case "time" -> createTimePolicy();
				case "size-and-time" -> CompositeTriggeringPolicy.createPolicy(createSizePolicy(), createTimePolicy());
				case "cron" -> createCronPolicy();
				case "size" -> createSizePolicy();
				default -> throw new IllegalArgumentException(
						"Unsupported rolling policy strategy '%s'".formatted(resolvedStrategy));
			};
			return new SpringBootTriggeringPolicy(policy);
		}

		private TriggeringPolicy createSizePolicy() {
			// Read from system properties first, then from attributes
			String size = System.getProperty("LOG4J2_ROLLINGPOLICY_MAX_FILE_SIZE");
			if (size == null) {
				size = (this.maxFileSize != null) ? this.maxFileSize : DEFAULT_MAX_FILE_SIZE;
			}
			return SizeBasedTriggeringPolicy.createPolicy(size);
		}

		private TriggeringPolicy createTimePolicy() {
			// Read from system properties first, then from attributes
			String intervalStr = System.getProperty("LOG4J2_ROLLINGPOLICY_TIME_INTERVAL");
			int interval = (intervalStr != null) ? Integer.parseInt(intervalStr)
					: (this.timeInterval != null) ? this.timeInterval : DEFAULT_TIME_INTERVAL;

			String modulateStr = System.getProperty("LOG4J2_ROLLINGPOLICY_TIME_MODULATE");
			boolean modulate = (modulateStr != null) ? Boolean.parseBoolean(modulateStr)
					: (this.timeModulate != null) ? this.timeModulate : false;

			return TimeBasedTriggeringPolicy.newBuilder().withInterval(interval).withModulate(modulate).build();
		}

		private TriggeringPolicy createCronPolicy() {
			Configuration configuration = Objects.requireNonNull(this.configuration, "configuration must not be null");

			// Read from system properties first, then from attributes
			String schedule = System.getProperty("LOG4J2_ROLLINGPOLICY_CRON_SCHEDULE");
			if (schedule == null) {
				schedule = (this.cronExpression != null) ? this.cronExpression : DEFAULT_CRON_EXPRESSION;
			}

			return CronTriggeringPolicy.createPolicy(configuration, null, schedule);
		}

		SpringBootTriggeringPolicyBuilder setStrategy(@Nullable String strategy) {
			this.strategy = strategy;
			return this;
		}

		SpringBootTriggeringPolicyBuilder setMaxFileSize(@Nullable String maxFileSize) {
			this.maxFileSize = maxFileSize;
			return this;
		}

		SpringBootTriggeringPolicyBuilder setTimeInterval(@Nullable Integer timeInterval) {
			this.timeInterval = timeInterval;
			return this;
		}

		SpringBootTriggeringPolicyBuilder setTimeModulate(@Nullable Boolean timeModulate) {
			this.timeModulate = timeModulate;
			return this;
		}

		SpringBootTriggeringPolicyBuilder setCronExpression(@Nullable String cronExpression) {
			this.cronExpression = cronExpression;
			return this;
		}

		SpringBootTriggeringPolicyBuilder setConfiguration(Configuration configuration) {
			this.configuration = configuration;
			return this;
		}

	}

}
