/*
 * Copyright 2012-2015 the original author or authors.
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

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;

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
 * {@link ApplicationEnvironmentPreparedEvent} or {@link ApplicationPreparedEvent}.
 *
 * @author Jakub Kubrynski
 * @author Dave Syer
 * @author Phillip Webb
 * @author Tomasz Przybyla
 * @since 1.2.0
 * @deprecated as of 1.4 in favor of
 * {@link org.springframework.boot.system.ApplicationPidFileWriter}
 */
@Deprecated
public class ApplicationPidFileWriter
		extends org.springframework.boot.system.ApplicationPidFileWriter {

	/**
	 * Create a new {@link ApplicationPidFileWriter} instance using the filename
	 * 'application.pid'.
	 */
	public ApplicationPidFileWriter() {
		super();
	}

	/**
	 * Create a new {@link ApplicationPidFileWriter} instance with a specified filename.
	 * @param filename the name of file containing pid
	 */
	public ApplicationPidFileWriter(String filename) {
		super(filename);
	}

	/**
	 * Create a new {@link ApplicationPidFileWriter} instance with a specified file.
	 * @param file the file containing pid
	 */
	public ApplicationPidFileWriter(File file) {
		super(file);
	}

}
