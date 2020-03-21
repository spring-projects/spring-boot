/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.maven;

import java.util.function.Supplier;

import org.apache.maven.plugin.logging.Log;

import org.springframework.boot.loader.tools.Packager.MainClassTimeoutWarningListener;

/**
 * {@link MainClassTimeoutWarningListener} backed by a supplied Maven {@link Log}.
 *
 * @author Phillip Webb
 */
class LoggingMainClassTimeoutWarningListener implements MainClassTimeoutWarningListener {

	private final Supplier<Log> log;

	LoggingMainClassTimeoutWarningListener(Supplier<Log> log) {
		this.log = log;
	}

	@Override
	public void handleTimeoutWarning(long duration, String mainMethod) {
		this.log.get().warn("Searching for the main-class is taking some time, "
				+ "consider using the mainClass configuration parameter");
	}

}
