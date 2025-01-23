/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.logging.logback;

import java.nio.charset.Charset;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy;
import ch.qos.logback.core.spi.ScanException;
import ch.qos.logback.core.util.FileSize;
import ch.qos.logback.core.util.OptionHelper;

import org.springframework.boot.ansi.AnsiColor;
import org.springframework.boot.ansi.AnsiElement;
import org.springframework.boot.ansi.AnsiStyle;
import org.springframework.boot.logging.LogFile;
import org.springframework.util.StringUtils;

/**
 * Default logback configuration used by Spring Boot. Uses {@link LogbackConfigurator} to
 * improve startup time. See also the {@code base.xml}, {@code defaults.xml},
 * {@code console-appender.xml} and {@code file-appender.xml} files provided for classic
 * {@code logback.xml} use.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author Vedran Pavic
 * @author Robert Thornton
 * @author Scott Frederick
 * @author Jonatan Ivanov
 * @author Moritz Halbritter
 */
class DefaultLogbackConfiguration {

	private static final String DEFAULT_CHARSET = Charset.defaultCharset().name();

	private static final String NAME_AND_GROUP = "%esb(){APPLICATION_NAME}%esb{APPLICATION_GROUP}";

	private static final String DATETIME = "%d{${LOG_DATEFORMAT_PATTERN:-yyyy-MM-dd'T'HH:mm:ss.SSSXXX}}";

	private static final String DEFAULT_CONSOLE_LOG_PATTERN = faint(DATETIME) + " "
			+ colorByLevel("${LOG_LEVEL_PATTERN:-%5p}") + " " + magenta("${PID:-}") + " "
			+ faint("--- " + NAME_AND_GROUP + "[%15.15t] ${LOG_CORRELATION_PATTERN:-}") + cyan("%-40.40logger{39}")
			+ " " + faint(":") + " %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}";

	static final String CONSOLE_LOG_PATTERN = "${CONSOLE_LOG_PATTERN:-" + DEFAULT_CONSOLE_LOG_PATTERN;

	private static final String DEFAULT_FILE_LOG_PATTERN = DATETIME + " ${LOG_LEVEL_PATTERN:-%5p} ${PID:-} --- "
			+ NAME_AND_GROUP + "[%t] ${LOG_CORRELATION_PATTERN:-}"
			+ "%-40.40logger{39} : %m%n${LOG_EXCEPTION_CONVERSION_WORD:-%wEx}}";

	static final String FILE_LOG_PATTERN = "${FILE_LOG_PATTERN:-" + DEFAULT_FILE_LOG_PATTERN;

	private final LogFile logFile;

	DefaultLogbackConfiguration(LogFile logFile) {
		this.logFile = logFile;
	}

	void apply(LogbackConfigurator config) {
		config.getConfigurationLock().lock();
		try {
			defaults(config);
			Appender<ILoggingEvent> consoleAppender = consoleAppender(config);
			if (this.logFile != null) {
				Appender<ILoggingEvent> fileAppender = fileAppender(config, this.logFile.toString());
				config.root(Level.INFO, consoleAppender, fileAppender);
			}
			else {
				config.root(Level.INFO, consoleAppender);
			}
		}
		finally {
			config.getConfigurationLock().unlock();
		}
	}

	private void defaults(LogbackConfigurator config) {
		deprecatedDefaults(config);
		config.conversionRule("clr", ColorConverter.class, ColorConverter::new);
		config.conversionRule("correlationId", CorrelationIdConverter.class, CorrelationIdConverter::new);
		config.conversionRule("esb", EnclosedInSquareBracketsConverter.class, EnclosedInSquareBracketsConverter::new);
		config.conversionRule("wex", WhitespaceThrowableProxyConverter.class, WhitespaceThrowableProxyConverter::new);
		config.conversionRule("wEx", ExtendedWhitespaceThrowableProxyConverter.class,
				ExtendedWhitespaceThrowableProxyConverter::new);
		putProperty(config, "CONSOLE_LOG_PATTERN", CONSOLE_LOG_PATTERN);
		putProperty(config, "CONSOLE_LOG_CHARSET", "${CONSOLE_LOG_CHARSET:-" + DEFAULT_CHARSET + "}");
		putProperty(config, "CONSOLE_LOG_THRESHOLD", "${CONSOLE_LOG_THRESHOLD:-TRACE}");
		putProperty(config, "CONSOLE_LOG_STRUCTURED_FORMAT", "${CONSOLE_LOG_STRUCTURED_FORMAT:-}");
		putProperty(config, "FILE_LOG_PATTERN", FILE_LOG_PATTERN);
		putProperty(config, "FILE_LOG_CHARSET", "${FILE_LOG_CHARSET:-" + DEFAULT_CHARSET + "}");
		putProperty(config, "FILE_LOG_THRESHOLD", "${FILE_LOG_THRESHOLD:-TRACE}");
		putProperty(config, "FILE_LOG_STRUCTURED_FORMAT", "${FILE_LOG_STRUCTURED_FORMAT:-}");
		config.logger("org.apache.catalina.startup.DigesterFactory", Level.ERROR);
		config.logger("org.apache.catalina.util.LifecycleBase", Level.ERROR);
		config.logger("org.apache.coyote.http11.Http11NioProtocol", Level.WARN);
		config.logger("org.apache.sshd.common.util.SecurityUtils", Level.WARN);
		config.logger("org.apache.tomcat.util.net.NioSelectorPool", Level.WARN);
		config.logger("org.eclipse.jetty.util.component.AbstractLifeCycle", Level.ERROR);
		config.logger("org.hibernate.validator.internal.util.Version", Level.WARN);
		config.logger("org.springframework.boot.actuate.endpoint.jmx", Level.WARN);
	}

	@SuppressWarnings("removal")
	private void deprecatedDefaults(LogbackConfigurator config) {
		config.conversionRule("applicationName", ApplicationNameConverter.class, ApplicationNameConverter::new);
	}

	void putProperty(LogbackConfigurator config, String name, String val) {
		config.getContext().putProperty(name, resolve(config, val));
	}

	private Appender<ILoggingEvent> consoleAppender(LogbackConfigurator config) {
		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
		createAppender(config, appender, "CONSOLE");
		config.appender("CONSOLE", appender);
		return appender;
	}

	private Appender<ILoggingEvent> fileAppender(LogbackConfigurator config, String logFile) {
		RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
		createAppender(config, appender, "FILE");
		appender.setFile(logFile);
		setRollingPolicy(appender, config);
		config.appender("FILE", appender);
		return appender;
	}

	private void createAppender(LogbackConfigurator config, OutputStreamAppender<ILoggingEvent> appender, String type) {
		appender.addFilter(createThresholdFilter(config, type));
		Encoder<ILoggingEvent> encoder = createEncoder(config, type);
		appender.setEncoder(encoder);
		config.start(encoder);
	}

	private ThresholdFilter createThresholdFilter(LogbackConfigurator config, String type) {
		ThresholdFilter filter = new ThresholdFilter();
		filter.setLevel(resolve(config, "${" + type + "_LOG_THRESHOLD}"));
		filter.start();
		return filter;
	}

	private Encoder<ILoggingEvent> createEncoder(LogbackConfigurator config, String type) {
		Charset charset = resolveCharset(config, "${" + type + "_LOG_CHARSET}");
		String structuredLogFormat = resolve(config, "${" + type + "_LOG_STRUCTURED_FORMAT}");
		if (StringUtils.hasLength(structuredLogFormat)) {
			StructuredLogEncoder encoder = createStructuredLogEncoder(structuredLogFormat);
			encoder.setCharset(charset);
			return encoder;
		}
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setCharset(charset);
		encoder.setPattern(resolve(config, "${" + type + "_LOG_PATTERN}"));
		return encoder;
	}

	private StructuredLogEncoder createStructuredLogEncoder(String format) {
		StructuredLogEncoder encoder = new StructuredLogEncoder();
		encoder.setFormat(format);
		return encoder;
	}

	private void setRollingPolicy(RollingFileAppender<ILoggingEvent> appender, LogbackConfigurator config) {
		SizeAndTimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new SizeAndTimeBasedRollingPolicy<>();
		rollingPolicy.setContext(config.getContext());
		rollingPolicy.setFileNamePattern(
				resolve(config, "${LOGBACK_ROLLINGPOLICY_FILE_NAME_PATTERN:-${LOG_FILE}.%d{yyyy-MM-dd}.%i.gz}"));
		rollingPolicy
			.setCleanHistoryOnStart(resolveBoolean(config, "${LOGBACK_ROLLINGPOLICY_CLEAN_HISTORY_ON_START:-false}"));
		rollingPolicy.setMaxFileSize(resolveFileSize(config, "${LOGBACK_ROLLINGPOLICY_MAX_FILE_SIZE:-10MB}"));
		rollingPolicy.setTotalSizeCap(resolveFileSize(config, "${LOGBACK_ROLLINGPOLICY_TOTAL_SIZE_CAP:-0}"));
		rollingPolicy.setMaxHistory(resolveInt(config, "${LOGBACK_ROLLINGPOLICY_MAX_HISTORY:-7}"));
		appender.setRollingPolicy(rollingPolicy);
		rollingPolicy.setParent(appender);
		config.start(rollingPolicy);
	}

	private boolean resolveBoolean(LogbackConfigurator config, String val) {
		return Boolean.parseBoolean(resolve(config, val));
	}

	private int resolveInt(LogbackConfigurator config, String val) {
		return Integer.parseInt(resolve(config, val));
	}

	private FileSize resolveFileSize(LogbackConfigurator config, String val) {
		return FileSize.valueOf(resolve(config, val));
	}

	private Charset resolveCharset(LogbackConfigurator config, String val) {
		return Charset.forName(resolve(config, val));
	}

	private String resolve(LogbackConfigurator config, String val) {
		try {
			return OptionHelper.substVars(val, config.getContext());
		}
		catch (ScanException ex) {
			throw new RuntimeException(ex);
		}
	}

	private static String faint(String value) {
		return color(value, AnsiStyle.FAINT);
	}

	private static String cyan(String value) {
		return color(value, AnsiColor.CYAN);
	}

	private static String magenta(String value) {
		return color(value, AnsiColor.MAGENTA);
	}

	private static String colorByLevel(String value) {
		return "%clr(" + value + "){}";
	}

	private static String color(String value, AnsiElement ansiElement) {
		return "%clr(" + value + "){" + ColorConverter.getName(ansiElement) + "}";
	}

}
