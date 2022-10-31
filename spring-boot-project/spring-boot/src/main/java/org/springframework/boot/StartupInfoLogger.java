/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot;

import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;

import org.springframework.aot.AotDetector;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.ApplicationContext;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Logs application information on startup.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Moritz Halbritter
 */
class StartupInfoLogger {

	private final Class<?> sourceClass;

	StartupInfoLogger(Class<?> sourceClass) {
		this.sourceClass = sourceClass;
	}

	void logStarting(Log applicationLog) {
		Assert.notNull(applicationLog, "Log must not be null");
		applicationLog.info(LogMessage.of(this::getStartingMessage));
		applicationLog.debug(LogMessage.of(this::getRunningMessage));
	}

	void logStarted(Log applicationLog, Duration timeTakenToStartup) {
		if (applicationLog.isInfoEnabled()) {
			applicationLog.info(getStartedMessage(timeTakenToStartup));
		}
	}

	private CharSequence getStartingMessage() {
		StringBuilder message = new StringBuilder();
		message.append("Starting");
		appendAotMode(message);
		appendApplicationName(message);
		appendVersion(message, this.sourceClass);
		appendJavaVersion(message);
		appendPid(message);
		appendContext(message);
		return message;
	}

	private CharSequence getRunningMessage() {
		StringBuilder message = new StringBuilder();
		message.append("Running with Spring Boot");
		appendVersion(message, getClass());
		message.append(", Spring");
		appendVersion(message, ApplicationContext.class);
		return message;
	}

	private CharSequence getStartedMessage(Duration timeTakenToStartup) {
		StringBuilder message = new StringBuilder();
		message.append("Started");
		appendApplicationName(message);
		message.append(" in ");
		message.append(timeTakenToStartup.toMillis() / 1000.0);
		message.append(" seconds");
		try {
			double uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0;
			message.append(" (process running for ").append(uptime).append(")");
		}
		catch (Throwable ex) {
			// No JVM time available
		}
		return message;
	}

	private void appendAotMode(StringBuilder message) {
		append(message, "", () -> AotDetector.useGeneratedArtifacts() ? "AOT-processed" : null);
	}

	private void appendApplicationName(StringBuilder message) {
		append(message, "",
				() -> (this.sourceClass != null) ? ClassUtils.getShortName(this.sourceClass) : "application");
	}

	private void appendVersion(StringBuilder message, Class<?> source) {
		append(message, "v", () -> source.getPackage().getImplementationVersion());
	}

	private void appendPid(StringBuilder message) {
		append(message, "with PID ", ApplicationPid::new);
	}

	private void appendContext(StringBuilder message) {
		StringBuilder context = new StringBuilder();
		ApplicationHome home = new ApplicationHome(this.sourceClass);
		if (home.getSource() != null) {
			context.append(home.getSource().getAbsolutePath());
		}
		append(context, "started by ", () -> System.getProperty("user.name"));
		append(context, "in ", () -> System.getProperty("user.dir"));
		if (context.length() > 0) {
			message.append(" (");
			message.append(context);
			message.append(")");
		}
	}

	private void appendJavaVersion(StringBuilder message) {
		append(message, "using Java ", () -> System.getProperty("java.version"));
	}

	private void append(StringBuilder message, String prefix, Callable<Object> call) {
		append(message, prefix, call, "");
	}

	private void append(StringBuilder message, String prefix, Callable<Object> call, String defaultValue) {
		Object result = callIfPossible(call);
		String value = (result != null) ? result.toString() : null;
		if (!StringUtils.hasLength(value)) {
			value = defaultValue;
		}
		if (StringUtils.hasLength(value)) {
			message.append((message.length() > 0) ? " " : "");
			message.append(prefix);
			message.append(value);
		}
	}

	private Object callIfPossible(Callable<Object> call) {
		try {
			return call.call();
		}
		catch (Exception ex) {
			return null;
		}
	}

}
