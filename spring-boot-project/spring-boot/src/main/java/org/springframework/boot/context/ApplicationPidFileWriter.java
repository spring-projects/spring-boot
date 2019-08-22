/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.context;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.boot.system.ApplicationPid;
import org.springframework.boot.system.SystemProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * An {@link ApplicationListener} that saves application PID into file. This application
 * listener will be triggered exactly once per JVM, and the file name can be overridden at
 * runtime with a System property or environment variable named "PIDFILE" (or "pidfile")
 * or using a {@code spring.pid.file} property in the Spring {@link Environment}.
 * <p>
 * If PID file can not be created no exception is reported. This behavior can be changed
 * by assigning {@code true} to System property or environment variable named
 * {@code PID_FAIL_ON_WRITE_ERROR} (or "pid_fail_on_write_error") or to
 * {@code spring.pid.fail-on-write-error} property in the Spring {@link Environment}.
 * <p>
 * Note: access to the Spring {@link Environment} is only possible when the
 * {@link #setTriggerEventType(Class) triggerEventType} is set to
 * {@link ApplicationEnvironmentPreparedEvent}, {@link ApplicationReadyEvent}, or
 * {@link ApplicationPreparedEvent}.
 *
 * @author Jakub Kubrynski
 * @author Dave Syer
 * @author Phillip Webb
 * @author Tomasz Przybyla
 * @author Madhura Bhave
 * @since 2.0.0
 */
public class ApplicationPidFileWriter implements ApplicationListener<SpringApplicationEvent>, Ordered {

	private static final Log logger = LogFactory.getLog(ApplicationPidFileWriter.class);

	private static final String DEFAULT_FILE_NAME = "application.pid";

	private static final List<Property> FILE_PROPERTIES;

	static {
		List<Property> properties = new ArrayList<>();
		properties.add(new SpringProperty("spring.pid.", "file"));
		properties.add(new SpringProperty("spring.", "pidfile"));
		properties.add(new SystemProperty("PIDFILE"));
		FILE_PROPERTIES = Collections.unmodifiableList(properties);
	}

	private static final List<Property> FAIL_ON_WRITE_ERROR_PROPERTIES;

	static {
		List<Property> properties = new ArrayList<>();
		properties.add(new SpringProperty("spring.pid.", "fail-on-write-error"));
		properties.add(new SystemProperty("PID_FAIL_ON_WRITE_ERROR"));
		FAIL_ON_WRITE_ERROR_PROPERTIES = Collections.unmodifiableList(properties);
	}

	private static final AtomicBoolean created = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE + 13;

	private final File file;

	private Class<? extends SpringApplicationEvent> triggerEventType = ApplicationPreparedEvent.class;

	/**
	 * Create a new {@link ApplicationPidFileWriter} instance using the filename
	 * 'application.pid'.
	 */
	public ApplicationPidFileWriter() {
		this(new File(DEFAULT_FILE_NAME));
	}

	/**
	 * Create a new {@link ApplicationPidFileWriter} instance with a specified filename.
	 * @param filename the name of file containing pid
	 */
	public ApplicationPidFileWriter(String filename) {
		this(new File(filename));
	}

	/**
	 * Create a new {@link ApplicationPidFileWriter} instance with a specified file.
	 * @param file the file containing pid
	 */
	public ApplicationPidFileWriter(File file) {
		Assert.notNull(file, "File must not be null");
		this.file = file;
	}

	/**
	 * Sets the type of application event that will trigger writing of the PID file.
	 * Defaults to {@link ApplicationPreparedEvent}. NOTE: If you use the
	 * {@link org.springframework.boot.context.event.ApplicationStartingEvent} to trigger
	 * the write, you will not be able to specify the PID filename in the Spring
	 * {@link Environment}.
	 * @param triggerEventType the trigger event type
	 */
	public void setTriggerEventType(Class<? extends SpringApplicationEvent> triggerEventType) {
		Assert.notNull(triggerEventType, "Trigger event type must not be null");
		this.triggerEventType = triggerEventType;
	}

	@Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		if (this.triggerEventType.isInstance(event) && created.compareAndSet(false, true)) {
			try {
				writePidFile(event);
			}
			catch (Exception ex) {
				String message = String.format("Cannot create pid file %s", this.file);
				if (failOnWriteError(event)) {
					throw new IllegalStateException(message, ex);
				}
				logger.warn(message, ex);
			}
		}
	}

	private void writePidFile(SpringApplicationEvent event) throws IOException {
		File pidFile = this.file;
		String override = getProperty(event, FILE_PROPERTIES);
		if (override != null) {
			pidFile = new File(override);
		}
		new ApplicationPid().write(pidFile);
		pidFile.deleteOnExit();
	}

	private boolean failOnWriteError(SpringApplicationEvent event) {
		String value = getProperty(event, FAIL_ON_WRITE_ERROR_PROPERTIES);
		return Boolean.parseBoolean(value);
	}

	private String getProperty(SpringApplicationEvent event, List<Property> candidates) {
		for (Property candidate : candidates) {
			String value = candidate.getValue(event);
			if (value != null) {
				return value;
			}
		}
		return null;
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
	protected static void reset() {
		created.set(false);
	}

	/**
	 * Provides access to a property value.
	 */
	private interface Property {

		String getValue(SpringApplicationEvent event);

	}

	/**
	 * {@link Property} obtained from Spring's {@link Environment}.
	 */
	private static class SpringProperty implements Property {

		private final String prefix;

		private final String key;

		SpringProperty(String prefix, String key) {
			this.prefix = prefix;
			this.key = key;
		}

		@Override
		public String getValue(SpringApplicationEvent event) {
			Environment environment = getEnvironment(event);
			if (environment == null) {
				return null;
			}
			return environment.getProperty(this.prefix + this.key);
		}

		private Environment getEnvironment(SpringApplicationEvent event) {
			if (event instanceof ApplicationEnvironmentPreparedEvent) {
				return ((ApplicationEnvironmentPreparedEvent) event).getEnvironment();
			}
			if (event instanceof ApplicationPreparedEvent) {
				return ((ApplicationPreparedEvent) event).getApplicationContext().getEnvironment();
			}
			if (event instanceof ApplicationReadyEvent) {
				return ((ApplicationReadyEvent) event).getApplicationContext().getEnvironment();
			}
			return null;
		}

	}

	/**
	 * {@link Property} obtained from {@link SystemProperties}.
	 */
	private static class SystemProperty implements Property {

		private final String[] properties;

		SystemProperty(String name) {
			this.properties = new String[] { name.toUpperCase(Locale.ENGLISH), name.toLowerCase(Locale.ENGLISH) };
		}

		@Override
		public String getValue(SpringApplicationEvent event) {
			return SystemProperties.get(this.properties);
		}

	}

}
