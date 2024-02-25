/*
 * Copyright 2012-2023 the original author or authors.
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

	/**
	 * Constructs a new instance of StartupInfoLogger with the specified source class.
	 * @param sourceClass the class from which the logger is being instantiated
	 */
	StartupInfoLogger(Class<?> sourceClass) {
		this.sourceClass = sourceClass;
	}

	/**
	 * Logs the starting information using the provided application log.
	 * @param applicationLog the log to be used for logging the starting information (must
	 * not be null)
	 * @throws IllegalArgumentException if the applicationLog is null
	 */
	void logStarting(Log applicationLog) {
		Assert.notNull(applicationLog, "Log must not be null");
		applicationLog.info(LogMessage.of(this::getStartingMessage));
		applicationLog.debug(LogMessage.of(this::getRunningMessage));
	}

	/**
	 * Logs the startup information of the application.
	 * @param applicationLog the log object used for logging
	 * @param startup the startup object containing the startup information
	 */
	void logStarted(Log applicationLog, Startup startup) {
		if (applicationLog.isInfoEnabled()) {
			applicationLog.info(getStartedMessage(startup));
		}
	}

	/**
	 * Returns the starting message for the StartupInfoLogger class.
	 * @return the starting message
	 */
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

	/**
	 * Returns a CharSequence containing the running message.
	 * @return the running message
	 */
	private CharSequence getRunningMessage() {
		StringBuilder message = new StringBuilder();
		message.append("Running with Spring Boot");
		appendVersion(message, getClass());
		message.append(", Spring");
		appendVersion(message, ApplicationContext.class);
		return message;
	}

	/**
	 * Returns a CharSequence representing the get started message for a given startup.
	 * @param startup the Startup object representing the startup information
	 * @return a CharSequence representing the get started message
	 */
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

	/**
	 * Appends the AOT mode information to the given message.
	 * @param message the StringBuilder object to append the AOT mode information to
	 */
	private void appendAotMode(StringBuilder message) {
		append(message, "", () -> AotDetector.useGeneratedArtifacts() ? "AOT-processed" : null);
	}

	/**
	 * Appends the application name to the given message.
	 * @param message the StringBuilder to append the application name to
	 */
	private void appendApplicationName(StringBuilder message) {
		append(message, "",
				() -> (this.sourceClass != null) ? ClassUtils.getShortName(this.sourceClass) : "application");
	}

	/**
	 * Appends the version information of the specified source class to the given message.
	 * @param message the StringBuilder object to append the version information to
	 * @param source the Class object representing the source class
	 */
	private void appendVersion(StringBuilder message, Class<?> source) {
		append(message, "v", () -> source.getPackage().getImplementationVersion());
	}

	/**
	 * Appends the process ID (PID) to the given message.
	 * @param message the StringBuilder to append the PID to
	 */
	private void appendPid(StringBuilder message) {
		append(message, "with PID ", ApplicationPid::new);
	}

	/**
	 * Appends the context information to the given message.
	 * @param message the message to append the context information to
	 */
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

	/**
	 * Appends the Java version to the given message.
	 * @param message the StringBuilder to append the Java version to
	 */
	private void appendJavaVersion(StringBuilder message) {
		append(message, "using Java ", () -> System.getProperty("java.version"));
	}

	/**
	 * Appends the result of a Callable to a StringBuilder with a given prefix.
	 * @param message the StringBuilder to append the result to
	 * @param prefix the prefix to prepend to the result
	 * @param call the Callable to execute and append its result
	 */
	private void append(StringBuilder message, String prefix, Callable<Object> call) {
		append(message, prefix, call, "");
	}

	/**
	 * Appends a message to a StringBuilder object with a given prefix and value. If the
	 * value is null or empty, it will be replaced with a default value.
	 * @param message the StringBuilder object to append the message to
	 * @param prefix the prefix to prepend to the value
	 * @param call the Callable object to retrieve the value from
	 * @param defaultValue the default value to use if the retrieved value is null or
	 * empty
	 */
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

	/**
	 * Executes the given Callable object and returns the result if successful.
	 * @param call the Callable object to be executed
	 * @return the result of the Callable object if successful, null otherwise
	 */
	private Object callIfPossible(Callable<Object> call) {
		try {
			return call.call();
		}
		catch (Exception ex) {
			return null;
		}
	}

}
