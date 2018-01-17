/*
 * Copyright 2012-2017 the original author or authors.
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

import org.springframework.context.ApplicationEvent;

/**
 * Event fired when an application exit code has been determined from an
 * {@link ExitCodeGenerator}.
 *
 * @author Phillip Webb
 * @since 1.3.2
 */
public class ExitCodeEvent extends ApplicationEvent {

	private final int exitCode;

	/**
	 * Create a new {@link ExitCodeEvent} instance.
	 * @param source the source of the event
	 * @param exitCode the exit code
	 */
	public ExitCodeEvent(Object source, int exitCode) {
		super(source);
		this.exitCode = exitCode;
	}

	/**
	 * Return the exit code that will be used to exit the JVM.
	 * @return the exit code
	 */
	public int getExitCode() {
		return this.exitCode;
	}

}
