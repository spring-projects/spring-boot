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

package org.springframework.boot;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.concurrent.Callable;

import org.apache.commons.logging.Log;
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

	public StartupInfoLogger(Class<?> sourceClass) {
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
		return (this.sourceClass != null ? ClassUtils.getShortName(this.sourceClass)
				: "application");
	}

	private String getVersion(final Class<?> source) {
		return getValue(" v", new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return source.getPackage().getImplementationVersion();
			}
		}, "");
	}

	private String getOn() {
		return getValue(" on ", new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return InetAddress.getLocalHost().getHostName();
			}
		});
	}

	private String getPid() {
		return getValue(" with PID ", new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return System.getProperty("PID");
			}
		});
	}

	private String getContext() {
		String startedBy = getValue("started by ", new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return System.getProperty("user.name");
			}
		});
		String in = getValue("in ", new Callable<Object>() {
			@Override
			public Object call() throws Exception {
				return System.getProperty("user.dir");
			}
		});
		File codeSourceLocation = getCodeSourceLocation();
		String path = (codeSourceLocation == null ? "" : codeSourceLocation
				.getAbsolutePath());
		if (startedBy == null && codeSourceLocation == null) {
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

	private File getCodeSourceLocation() {
		try {
			ProtectionDomain protectionDomain = (this.sourceClass == null ? getClass()
					: this.sourceClass).getProtectionDomain();
			CodeSource codeSource = protectionDomain.getCodeSource();
			if (codeSource == null) {
				return null;
			}
			URL location = protectionDomain.getCodeSource().getLocation();
			URLConnection connection = location.openConnection();
			File file = getFile(location, connection);
			if (file.exists()) {
				return file;
			}
		}
		catch (Exception ex) {
		}
		return null;
	}

	private File getFile(URL location, URLConnection connection) throws IOException {
		if (connection instanceof JarURLConnection) {
			return new File(((JarURLConnection) connection).getJarFile().getName());
		}
		return new File(location.getPath());
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
