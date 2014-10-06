/*
 * Copyright 2010-2014 the original author or authors.
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

package org.springframework.boot.actuate.system;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ApplicationPid;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.SystemPropertyUtils;

/**
 * An {@link org.springframework.context.ApplicationListener} that saves application PID
 * into file. This application listener will be triggered exactly once per JVM, and the
 * file name can be overridden at runtime with a System property named "PIDFILE" (or
 * "pidfile") or an environment property name "{@value #CONFIGURED_PID_FILE}".
 *
 * @author Jakub Kubrynski
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.0.2
 */
public class ApplicationPidListener implements
		ApplicationListener<SpringApplicationEvent>, Ordered {

	private static final Log logger = LogFactory.getLog(ApplicationPidListener.class);

	private static final String DEFAULT_FILE_NAME = "application.pid";
	private static final String CONFIGURED_PID_FILE = "spring.application.pid-file";

	private static final AtomicBoolean created = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE + 13;

	private final File file;
	private final boolean withOverrides;

	/**
	 * Create a new {@link ApplicationPidListener} instance with the default PID file name
	 * "{@value #DEFAULT_FILE_NAME}".
	 *
	 * The default can be overridden by providing a system property "PIDFILE" or an
	 * {@link Environment} property named "{@value #CONFIGURED_PID_FILE}".
	 *
	 * The environment property will override the system property if both are specified.
	 */
	public ApplicationPidListener() {
		this.file = new File(DEFAULT_FILE_NAME);
		this.withOverrides = true;
	}

	/**
	 * Create a new {@link ApplicationPidListener} instance with a specified filename.
	 * System and environment properties will not be honored.
	 *
	 * @param filename the name of file containing pid
	 */
	public ApplicationPidListener(String filename) {
		Assert.notNull(filename, "Filename must not be null");
		this.file = new File(filename);
		this.withOverrides = false;
	}

	/**
	 * Create a new {@link ApplicationPidListener} instance with a specified file. System
	 * and environment properties will not be honored.
	 *
	 * @param file the file containing pid
	 */
	public ApplicationPidListener(File file) {
		Assert.notNull(file, "File must not be null");
		this.file = file;
		this.withOverrides = false;
	}

	@Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		writePidFile(configurePidFile(event));
	}

	private File configurePidFile(final SpringApplicationEvent event) {
		// set the file to the listener defaults
		File pidFile = this.file;

		// only run configuration overrides if the PID file location was not specified
		// explicitly
		if (!withOverrides) {
			return pidFile;
		}

		// try to resolve from system property, overriding the default
		String configSource = "system property";

		String actual = SystemPropertyUtils.resolvePlaceholders("${PIDFILE}", true);
		actual = !actual.contains("$") ? actual : SystemPropertyUtils
				.resolvePlaceholders("${pidfile}", true);
		actual = !actual.contains("$") ? actual : pidFile.getAbsolutePath();
		pidFile = new File(actual);

		if (!(event instanceof ApplicationEnvironmentPreparedEvent)) {
			return pidFile;
		}

		// try to resolve from environment, overriding the system property and the default
		Environment env = ((ApplicationEnvironmentPreparedEvent) event).getEnvironment();
		if (env.containsProperty(CONFIGURED_PID_FILE)) {
			configSource = "environment";

			String location = env.getProperty(CONFIGURED_PID_FILE);
			pidFile = new File(location);
		}

		logger.debug(String.format("Configured PID file from %s: %s", configSource,
				pidFile.getAbsolutePath()));
		return pidFile;
	}

	private void writePidFile(final File pidFile) {
		if (created.compareAndSet(false, true)) {
			try {
				new ApplicationPid().write(pidFile);
				this.file.deleteOnExit();
			}
			catch (Exception ex) {
				logger.warn(String.format("Cannot create pid file %s", this.file));
			}
		}
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

	/**
	 * Reset the created flag for testing purposes.
	 */
	static void reset() {
		created.set(false);
	}
}
