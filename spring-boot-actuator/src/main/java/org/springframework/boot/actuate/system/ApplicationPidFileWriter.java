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
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.ApplicationPid;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;

/**
 * An {@link ApplicationListener} that saves application PID into file. This application
 * listener will be triggered exactly once per JVM, and the file name can be overridden at
 * runtime with a System property or environment variable named "PIDFILE" (or "pidfile")
 * or using a {@code spring.pidfile} property in the Spring {@link Environment}.
 *
 * @author Jakub Kubrynski
 * @author Dave Syer
 * @author Phillip Webb
 * @since 1.2.0
 */
public class ApplicationPidFileWriter implements
		ApplicationListener<SpringApplicationEvent>, Ordered {

	private static final Log logger = LogFactory.getLog(ApplicationPidFileWriter.class);

	private static final String DEFAULT_FILE_NAME = "application.pid";

	private static final String[] SYSTEM_PROPERTY_VARIABLES = { "PIDFILE", "pidfile" };

	private static final String SPRING_PROPERTY = "spring.pidfile";

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
	 * {@link ApplicationPreparedEvent} to trigger the write, you will not be able to
	 * specify the PID filename in the Spring {@link Environment}.
	 */
	public void setTriggerEventType(
			Class<? extends SpringApplicationEvent> triggerEventType) {
		Assert.notNull(triggerEventType, "Trigger event type must not be null");
		this.triggerEventType = triggerEventType;
	}

	@Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		if (this.triggerEventType.isInstance(event)) {
			if (created.compareAndSet(false, true)) {
				try {
					writePidFile(event);
				}
				catch (Exception ex) {
					logger.warn(String.format("Cannot create pid file %s", this.file));
				}
			}
		}
	}

	private void writePidFile(SpringApplicationEvent event) throws IOException {
		File pidFile = this.file;
		String override = SystemProperties.get(SYSTEM_PROPERTY_VARIABLES);
		if (override != null) {
			pidFile = new File(override);
		}
		else {
			Environment environment = getEnvironment(event);
			if (environment != null) {
				override = new RelaxedPropertyResolver(environment)
						.getProperty(SPRING_PROPERTY);
				if (override != null) {
					pidFile = new File(override);
				}
			}
		}
		new ApplicationPid().write(pidFile);
		pidFile.deleteOnExit();
	}

	private Environment getEnvironment(SpringApplicationEvent event) {
		if (event instanceof ApplicationEnvironmentPreparedEvent) {
			return ((ApplicationEnvironmentPreparedEvent) event).getEnvironment();
		}
		if (event instanceof ApplicationPreparedEvent) {
			return ((ApplicationPreparedEvent) event).getApplicationContext()
					.getEnvironment();
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
	static void reset() {
		created.set(false);
	}
}
