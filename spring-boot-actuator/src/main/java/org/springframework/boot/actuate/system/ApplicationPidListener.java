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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.util.SystemUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * An {@link org.springframework.context.ApplicationListener} that saves application PID
 * into file
 * 
 * @since 1.0.2
 *
 * @author Jakub Kubrynski
 * @author Dave Syer
 */
public class ApplicationPidListener implements
		ApplicationListener<ApplicationStartedEvent>, Ordered {

	private static final String DEFAULT_PID_FILE_NAME = "application.pid";

	private static final AtomicBoolean pidFileCreated = new AtomicBoolean(false);

	private int order = Ordered.HIGHEST_PRECEDENCE + 13;

	private static final Log logger = LogFactory.getLog(ApplicationPidListener.class);

	private String pidFileName = DEFAULT_PID_FILE_NAME;

	/**
	 * Sets the pid file name. This file will contain current process id.
	 *
	 * @param pidFileName the name of file containing pid
	 */
	public ApplicationPidListener(String pidFileName) {
		this.pidFileName = pidFileName;
	}

	public ApplicationPidListener() {
	}

	@Override
	public void onApplicationEvent(ApplicationStartedEvent event) {
		if (pidFileCreated.get()) {
			return;
		}

		String applicationPid;
		try {
			applicationPid = SystemUtils.getApplicationPid();
		}
		catch (IllegalStateException ignore) {
			return;
		}

		if (pidFileCreated.compareAndSet(false, true)) {
			File file = new File(this.pidFileName);
			FileOutputStream fileOutputStream = null;
			try {
				File parent = file.getParentFile();
				if (parent != null) {
					parent.mkdirs();
				}
				fileOutputStream = new FileOutputStream(file);
				fileOutputStream.write(applicationPid.getBytes());
			}
			catch (FileNotFoundException e) {
				logger.warn(String
						.format("Cannot create pid file %s !", this.pidFileName));
			}
			catch (Exception e) {
				logger.warn(String.format("Cannot write to pid file %s!",
						this.pidFileName));
			}
			finally {
				if (fileOutputStream != null) {
					try {
						fileOutputStream.close();
					}
					catch (IOException e) {
						logger.warn(String.format("Cannot close pid file %s!",
								this.pidFileName));
					}
				}
			}
		}
	}

	/**
	 * Allow pid file to be re-written
	 */
	public static void reset() {
		pidFileCreated.set(false);
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int getOrder() {
		return this.order;
	}

}
