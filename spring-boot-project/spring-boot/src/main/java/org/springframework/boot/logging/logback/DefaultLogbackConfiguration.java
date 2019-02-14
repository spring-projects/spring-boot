/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;

import org.springframework.boot.logging.LogFile;
import org.springframework.boot.logging.LoggingInitializationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertyResolver;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;

/**
 * Default logback configuration used by Spring Boot. Uses {@link LogbackConfigurator} to
 * improve startup time. See also the {@code defaults.xml}, {@code console-appender.xml}
 * and {@code file-appender.xml} files provided for classic {@code logback.xml} use.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @since 1.1.2
 */
class DefaultLogbackConfiguration {

	private static final String CONSOLE_LOG_PATTERN = "%clr(%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}}){faint} "
			+ "%clr(${LOG_LEVEL_PATTERN:-%5p}) %clr(${PID:- }){magenta} %clr(---){faint} "
			+ "%clr([%15.15t]){faint} %clr(%-40.40logger{39}){cyan} "
			+ "%clr(:){faint} %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}";

	private static final String FILE_LOG_PATTERN = "%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd HH:mm:ss.SSS}} "
			+ "${LOG_LEVEL_PATTERN:-%5p} ${PID:- } --- [%t] %-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}";

	private static final Pattern SIZE_PATTERN = Pattern
			.compile("^([+\\-]?\\d+)\\s*([A-Z]{0,2})S?$", Pattern.CASE_INSENSITIVE);

	private static final String MAX_FILE_SIZE = "10MB";

	private final PropertyResolver patterns;

	private final LogFile logFile;

	DefaultLogbackConfiguration(LoggingInitializationContext initializationContext,
			LogFile logFile) {
		this.patterns = getPatternsResolver(initializationContext.getEnvironment());
		this.logFile = logFile;
	}

	private PropertyResolver getPatternsResolver(Environment environment) {
		if (environment == null) {
			return new PropertySourcesPropertyResolver(null);
		}
		if (environment instanceof ConfigurableEnvironment) {
			PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(
					((ConfigurableEnvironment) environment).getPropertySources());
			resolver.setIgnoreUnresolvableNestedPlaceholders(true);
			return resolver;
		}
		return environment;
	}

	public void apply(LogbackConfigurator config) {
		synchronized (config.getConfigurationLock()) {
			base(config);
			Appender<ILoggingEvent> consoleAppender = consoleAppender(config);
			if (this.logFile != null) {
				Appender<ILoggingEvent> fileAppender = fileAppender(config,
						this.logFile.toString());
				config.root(Level.INFO, consoleAppender, fileAppender);
			}
			else {
				config.root(Level.INFO, consoleAppender);
			}
		}
	}

	private void base(LogbackConfigurator config) {
		config.conversionRule("clr", ColorConverter.class);
		config.conversionRule("wex", WhitespaceThrowableProxyConverter.class);
		config.conversionRule("wEx", ExtendedWhitespaceThrowableProxyConverter.class);
		config.logger("org.apache.catalina.startup.DigesterFactory", Level.ERROR);
		config.logger("org.apache.catalina.util.LifecycleBase", Level.ERROR);
		config.logger("org.apache.coyote.http11.Http11NioProtocol", Level.WARN);
		config.logger("org.apache.sshd.common.util.SecurityUtils", Level.WARN);
		config.logger("org.apache.tomcat.util.net.NioSelectorPool", Level.WARN);
		config.logger("org.eclipse.jetty.util.component.AbstractLifeCycle", Level.ERROR);
		config.logger("org.hibernate.validator.internal.util.Version", Level.WARN);
	}

	private Appender<ILoggingEvent> consoleAppender(LogbackConfigurator config) {
		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		String logPattern = this.patterns.getProperty("logging.pattern.console",
				CONSOLE_LOG_PATTERN);
		encoder.setPattern(OptionHelper.substVars(logPattern, config.getContext()));
		config.start(encoder);
		appender.setEncoder(encoder);
		config.appender("CONSOLE", appender);
		return appender;
	}

	private Appender<ILoggingEvent> fileAppender(LogbackConfigurator config,
			String logFile) {
		RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		String logPattern = this.patterns.getProperty("logging.pattern.file",
				FILE_LOG_PATTERN);
		encoder.setPattern(OptionHelper.substVars(logPattern, config.getContext()));
		appender.setEncoder(encoder);
		config.start(encoder);
		appender.setFile(logFile);
		setRollingPolicy(appender, config, logFile);
		config.appender("FILE", appender);
		return appender;
	}

	private void setRollingPolicy(RollingFileAppender<ILoggingEvent> appender,
			LogbackConfigurator config, String logFile) {
		SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
		rollingPolicy.setCleanHistoryOnStart(this.patterns.getProperty(
				"logging.file.clean-history-on-start", Boolean.class, false));
		rollingPolicy.setFileNamePattern(logFile + ".%d{yyyy-MM-dd}.%i.gz");
		setMaxFileSize(rollingPolicy, getDataSize(
				this.patterns.getProperty("logging.file.max-size", MAX_FILE_SIZE)));
		setTotalSizeCap(rollingPolicy,
				getDataSize(this.patterns.getProperty("logging.file.total-size-cap",
						String.valueOf(CoreConstants.UNBOUNDED_TOTAL_SIZE_CAP))));
		rollingPolicy.setMaxHistory(this.patterns.getProperty("logging.file.max-history",
				Integer.class, CoreConstants.UNBOUND_HISTORY));
		appender.setRollingPolicy(rollingPolicy);
		rollingPolicy.setParent(appender);
		config.start(rollingPolicy);
	}

	private void setTotalSizeCap(
			SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy,
			DataSize totalSizeCap) {
		Assert.isTrue(!totalSizeCap.isNegative(), "TotalSizeCap must not be a negative");
		rollingPolicy.setTotalSizeCap(new FileSize(totalSizeCap.toBytes()));
	}

	private void setMaxFileSize(
			SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy,
			DataSize maxFileSize) {
		Assert.isTrue(!maxFileSize.isNegative(), "MaxFileSize must not be a negative");
		try {
			rollingPolicy.setMaxFileSize(new FileSize(maxFileSize.toBytes()));
		}
		catch (NoSuchMethodError ex) {
			// Logback < 1.1.8 used String configuration
			Method method = ReflectionUtils.findMethod(
					SizeAndTimeBasedRollingPolicy.class, "setMaxFileSize", String.class);
			ReflectionUtils.invokeMethod(method, rollingPolicy,
					Objects.toString(maxFileSize.toBytes()));
		}
	}

	private static DataSize getDataSize(String value) {
		Matcher matcher = SIZE_PATTERN.matcher(value);
		if (matcher.matches()) {
			String size = matcher.group(1);
			String unit = matcher.group(2);
			return StringUtils.hasText(unit) ? DataSize.parse(size + unit.toUpperCase())
					: DataSize.parse(size);
		}
		throw new IllegalArgumentException(
				"'" + value + "' does not match data size pattern");
	}

}
