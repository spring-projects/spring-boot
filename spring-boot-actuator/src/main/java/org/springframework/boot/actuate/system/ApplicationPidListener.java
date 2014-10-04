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
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ProvidesEnvironmentEvent;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.Assert;
import org.springframework.util.SystemPropertyUtils;

/**
 * An {@link org.springframework.context.ApplicationListener} that saves application PID
 * into file. This application listener will be triggered exactly once per JVM, and the file
 * name can be overridden at runtime with a System property or environment variable named
 * "PIDFILE" (or "pidfile").
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
	private static final Class<? extends SpringApplicationEvent> DEFAULT_EVENT = ApplicationStartedEvent.class;

	private static final String CONFIGURED_PID_FILE = "spring.application.pid-file";

	private static final AtomicBoolean created = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE + 13;

	private final Class<? extends SpringApplicationEvent> onEvent;

	private final File file;

	/**
	 * Create a new {@link ApplicationPidListener} instance using the filename
	 * 'application.pid'.
	 */
	public ApplicationPidListener() {
		this.file = new File(DEFAULT_FILE_NAME);
		this.onEvent = DEFAULT_EVENT;
	}

	/**
	 * Create a new {@link ApplicationPidListener} instance with a specified filename.
	 *
	 * @param filename the name of file containing pid
	 */
	public ApplicationPidListener(String filename) {
		Assert.notNull(filename, "Filename must not be null");
		this.file = new File(filename);
		this.onEvent = DEFAULT_EVENT;
	}

	/**
	 * Create a new {@link ApplicationPidListener} instance with a specified file.
	 *
	 * @param file the file containing pid
	 */
	public ApplicationPidListener(File file) {
		Assert.notNull(file, "File must not be null");

    String actual = SystemPropertyUtils.resolvePlaceholders("${PIDFILE}", true);
    actual = !actual.contains("$") ? actual :  SystemPropertyUtils.resolvePlaceholders("${pidfile}", true);
    actual = !actual.contains("$") ? actual :  file.getAbsolutePath();
    file = new File(actual);

		this.file = file;
		this.onEvent = DEFAULT_EVENT;
	}

	/**
	 * Create a new {@link ApplicationPidListener} instance with the PID file location
	 * taken from the {@link Environment}. This causes the listener to wait for
	 * {@link org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent}
	 * before it tries to write the PID file. Normally the listener would react on
	 * {@link #DEFAULT_EVENT} in the first place.
	 *
	 * @param onEvent The class the listener should react to. If the event provides an
	 * {@link org.springframework.core.env.Environment}, the listener tries to resolve the
	 * property '{@value #CONFIGURED_PID_FILE}' in order to write the PID file to the
	 * specified location. If the event does not provide an {@link Environment}, the
	 * defaults are applied.
	 */
	public ApplicationPidListener(Class<? extends SpringApplicationEvent> onEvent) {
    Assert.notNull(onEvent, "The 'onEvent' argument must not be null");

		if (onEvent.equals(ApplicationFailedEvent.class)) {
			throw new IllegalArgumentException(
					String.format(
							"Cannot use '%s' here, as writing the PID file on application boot failure makes no sense.",
							ApplicationFailedEvent.class.getName()));
		}

    this.file = new File (DEFAULT_FILE_NAME);
		this.onEvent = onEvent;
  }

	@Override
	public void onApplicationEvent(SpringApplicationEvent event) {
		if (!event.getClass().equals(onEvent)) {
			return;
		}

		File pidFile = this.file;
		if (event instanceof ProvidesEnvironmentEvent) {
			Environment env = ((ApplicationEnvironmentPreparedEvent) event)
					.getEnvironment();
			if (env.containsProperty(CONFIGURED_PID_FILE)) {
				String location = env.getProperty(CONFIGURED_PID_FILE);
				logger.debug(String.format("Overriding PID file: %s", location));
				pidFile = new File(location);
			}
			else {
				logger.warn(String
						.format("Trying to configure application PID file location from environment, but no property '%s' could be found.",
								CONFIGURED_PID_FILE));
			}
		}

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
