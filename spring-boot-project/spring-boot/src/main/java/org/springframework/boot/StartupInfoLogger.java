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

package org.springframework.boot;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;

import org.springframework.boot.system.ApplicationHome;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.context.ApplicationContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;
import org.springframework.util.StringUtils;

/**
 * Logs application information on startup.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
class StartupInfoLogger {

	private final Class<?> sourceClass;

	StartupInfoLogger(Class<?> sourceClass) {
		this.sourceClass = sourceClass;
	}

	public void logStarting(Log log) {
		Assert.notNull(log, "Log must not be null");
		if (log.isInfoEnabled()) {
			log.info(getStartupMessage());
		}
		if (log.isDebugEnabled()) {
			log.debug(getRunningMessage());
		}
	}

	public void logStarted(Log log, StopWatch stopWatch) {
		if (log.isInfoEnabled()) {
			log.info(getStartedMessage(stopWatch));
		}
	}

	private String getStartupMessage() {
		StringBuilder message = new StringBuilder();
		message.append("Starting ");
		message.append(getApplicationName());
		message.append(getVersion(this.sourceClass));
		message.append(getOn());
		message.append(getPid());
		message.append(getContext());
		return message.toString();
	}

	private StringBuilder getRunningMessage() {
		StringBuilder message = new StringBuilder();
		message.append("Running with Spring Boot");
		message.append(getVersion(getClass()));
		message.append(", Spring");
		message.append(getVersion(ApplicationContext.class));
		return message;
	}

	private StringBuilder getStartedMessage(StopWatch stopWatch) {
		StringBuilder message = new StringBuilder();
		message.append("Started ");
		message.append(getApplicationName());
		message.append(" in ");
		message.append(stopWatch.getTotalTimeSeconds());
		try {
			double uptime = ManagementFactory.getRuntimeMXBean().getUptime() / 1000.0;
			message.append(" seconds (JVM running for " + uptime + ")");
		}
		catch (Throwable ex) {
			// No JVM time available
		}
		return message;
	}

	private String getApplicationName() {
		return (this.sourceClass != null) ? ClassUtils.getShortName(this.sourceClass)
				: "application";
	}

	private String getVersion(Class<?> source) {
		return getValue(" v", () -> source.getPackage().getImplementationVersion(), "");
	}

	private String getOn() {
		return getValue(" on ", () -> InetAddress.getLocalHost().getHostName());
	}

	private String getPid() {
		return getValue(" with PID ", () -> new ApplicationPid().toString());
	}

	private String getContext() {
		String startedBy = getValue("started by ", () -> System.getProperty("user.name"));
		String in = getValue("in ", () -> System.getProperty("user.dir"));
		ApplicationHome home = new ApplicationHome(this.sourceClass);
		String path = (home.getSource() != null) ? home.getSource().getAbsolutePath()
				: "";
		if (startedBy == null && path == null) {
			return "";
		}
		if (StringUtils.hasLength(startedBy) && StringUtils.hasLength(path)) {
			startedBy = " " + startedBy;
		}
		if (StringUtils.hasLength(in) && StringUtils.hasLength(startedBy)) {
			in = " " + in;
		}
		return " (" + path + startedBy + in + ")";
	}

	private String getValue(String prefix, Callable<Object> call) {
		return getValue(prefix, call, "");
	}

	private String getValue(String prefix, Callable<Object> call, String defaultValue) {
		try {
			Object value = call.call();
			if (value != null && StringUtils.hasLength(value.toString())) {
				return prefix + value;
			}
		}
		catch (Exception ex) {
			// Swallow and continue
		}
		return defaultValue;
	}

}
