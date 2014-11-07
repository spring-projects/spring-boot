/*
 * Copyright 2012-2014 the original author or authors.
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

import java.nio.charset.Charset;

import org.springframework.util.StringUtils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;
import ch.qos.logback.core.util.OptionHelper;

/**
 * Default logback configuration used by Spring Boot. Uses {@link LogbackConfigurator} to
 * improve startup time. See also the {@code defaults.xml}, {@code console-appender.xml}
 * and {@code file-appender.xml} files provided for classic {@code logback.xml} use.
 *
 * @author Phillip Webb
 * @since 1.1.2
 */
class DefaultLogbackConfiguration {

	private static final String CONSOLE_LOG_PATTERN = "%clr(%d{yyyy-MM-dd HH:mm:ss.SSS}){faint} "
			+ "%clr(%5p) %clr(${PID:- }){magenta} %clr(---){faint} "
			+ "%clr([%15.15t{14}]){faint} %clr(%-40.40logger{39}){cyan} "
			+ "%clr(:){faint} %m%n%wex";

	private static final String FILE_LOG_PATTERN = "%d{yyyy-MM-dd HH:mm:ss.SSS} %5p "
			+ "${PID:- } [%t] --- %-40.40logger{39} : %m%n%wex";

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private final String logFile;

	public DefaultLogbackConfiguration(String logFile) {
		this.logFile = logFile;
	}

	@SuppressWarnings("unchecked")
	public void apply(LogbackConfigurator config) {
		synchronized (config.getConfigurationLock()) {
			base(config);
			Appender<ILoggingEvent> consoleAppender = consoleAppender(config);
			if (StringUtils.hasLength(this.logFile)) {
				Appender<ILoggingEvent> fileAppender = fileAppender(config, this.logFile);
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
		LevelRemappingAppender debugRemapAppender = new LevelRemappingAppender(
				"org.springframework.boot");
		config.start(debugRemapAppender);
		config.appender("DEBUG_LEVEL_REMAPPER", debugRemapAppender);
		config.logger("", Level.ERROR);
		config.logger("org.apache.catalina.startup.DigesterFactory", Level.ERROR);
		config.logger("org.apache.catalina.util.LifecycleBase", Level.ERROR);
		config.logger("org.apache.coyote.http11.Http11NioProtocol", Level.WARN);
		config.logger("org.apache.sshd.common.util.SecurityUtils", Level.WARN);
		config.logger("org.apache.tomcat.util.net.NioSelectorPool", Level.WARN);
		config.logger("org.crsh.plugin", Level.WARN);
		config.logger("org.crsh.ssh", Level.WARN);
		config.logger("org.eclipse.jetty.util.component.AbstractLifeCycle", Level.ERROR);
		config.logger("org.hibernate.validator.internal.util.Version", Level.WARN);
		config.logger("org.springframework.boot.actuate.autoconfigure."
				+ "CrshAutoConfiguration", Level.WARN);
		config.logger("org.springframework.boot.actuate.endpoint.jmx", null, false,
				debugRemapAppender);
		config.logger("org.thymeleaf", null, false, debugRemapAppender);
	}

	private Appender<ILoggingEvent> consoleAppender(LogbackConfigurator config) {
		ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<ILoggingEvent>();
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setPattern(OptionHelper.substVars(CONSOLE_LOG_PATTERN,
				config.getContext()));
		encoder.setCharset(UTF8);
		config.start(encoder);
		appender.setEncoder(encoder);
		config.appender("CONSOLE", appender);
		return appender;
	}

	private Appender<ILoggingEvent> fileAppender(LogbackConfigurator config,
			String logFile) {
		RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<ILoggingEvent>();
		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setPattern(FILE_LOG_PATTERN);
		appender.setEncoder(encoder);
		config.start(encoder);

		appender.setFile(logFile);

		FixedWindowRollingPolicy rollingPolicy = new FixedWindowRollingPolicy();
		rollingPolicy.setFileNamePattern(logFile + ".%i");
		appender.setRollingPolicy(rollingPolicy);
		rollingPolicy.setParent(appender);
		config.start(rollingPolicy);

		SizeBasedTriggeringPolicy<ILoggingEvent> triggeringPolicy = new SizeBasedTriggeringPolicy<ILoggingEvent>();
		triggeringPolicy.setMaxFileSize("10MB");
		appender.setTriggeringPolicy(triggeringPolicy);
		config.start(triggeringPolicy);

		config.appender("FILE", appender);
		return appender;
	}

}
