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

package org.springframework.boot.devtools.tests;

import java.io.File;

/**
 * An application launched by {@link ApplicationLauncher}.
 *
 * @author Andy Wilkinson
 */
class LaunchedApplication {

	private final File classesDirectory;

	private final File standardOut;

	private final Process[] processes;

	LaunchedApplication(File classesDirectory, File standardOut, Process... processes) {
		this.classesDirectory = classesDirectory;
		this.standardOut = standardOut;
		this.processes = processes;
	}

	void stop() throws InterruptedException {
		for (Process process : this.processes) {
			process.destroy();
			process.waitFor();
		}
	}

	File getStandardOut() {
		return this.standardOut;
	}

	File getClassesDirectory() {
		return this.classesDirectory;
	}

}
