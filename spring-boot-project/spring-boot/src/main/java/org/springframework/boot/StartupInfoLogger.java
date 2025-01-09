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

package org.springframework.boot;

import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;

import org.springframework.aot.AotDetector;
import org.springframework.boot.SpringApplication.Startup;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
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

	private final Environment environment;

	StartupInfoLogger(Class<?> sourceClass, Environment environment) {
		this.sourceClass = sourceClass;
		this.environment = environment;
	}

	void logStarting(Log applicationLog) {
		Assert.notNull(applicationLog, "'applicationLog' must not be null");
		applicationLog.info(LogMessage.of(this::getStartingMessage));
		applicationLog.debug(LogMessage.of(this::getRunningMessage));
	}

	void logStarted(Log applicationLog, Startup startup) {
		if (applicationLog.isInfoEnabled()) {
			applicationLog.info(getStartedMessage(startup));
		}
	}

	private CharSequence getStartingMessage() {
		StringBuilder message = new StringBuilder();
		message.append("Starting");
		appendAotMode(message);
		appendApplicationName(message);
		appendApplicationVersion(message);
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

	private CharSequence getStartedMessage(Startup startup) {
		StringBuilder message = new StringBuilder();
		message.append(startup.action());
		appendApplicationName(message);
		message.append(" in ");
		message.append(startup.timeTakenToStarted().toMillis() / 1000.0);
		message.append(" seconds");
		Long uptimeMs = startup.processUptime();
		if (uptimeMs != null) {
			double uptime = uptimeMs / 1000.0;
			message.append(" (process running for ").append(uptime).append(")");
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

	private void appendApplicationVersion(StringBuilder message) {
		append(message, "v", () -> this.environment.getProperty("spring.application.version"));
	}

	private void appendPid(StringBuilder message) {
		append(message, "with PID ", () -> this.environment.getProperty("spring.application.pid"));
	}

	private void appendContext(StringBuilder message) {
		StringBuilder context = new StringBuilder();
		ApplicationHome home = new ApplicationHome(this.sourceClass);
		if (home.getSource() != null) {
			context.append(home.getSource().getAbsolutePath());
		}
		append(context, "started by ", () -> System.getProperty("user.name"));
		append(context, "in ", () -> System.getProperty("user.dir"));
		if (!context.isEmpty()) {
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
			message.append((!message.isEmpty()) ? " " : "");
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
