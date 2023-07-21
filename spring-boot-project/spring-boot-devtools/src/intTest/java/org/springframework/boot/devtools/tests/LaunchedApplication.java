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

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.time.Instant;
import java.util.function.BiFunction;

/**
 * An application launched by {@link ApplicationLauncher}.
 *
 * @author Andy Wilkinson
 */
class LaunchedApplication {

	private final File classesDirectory;

	private final File standardOut;

	private final File standardError;

	private final Process localProcess;

	private Process remoteProcess;

	private final Instant launchTime = Instant.now();

	private final BiFunction<Integer, File, Process> remoteProcessRestarter;

	LaunchedApplication(File classesDirectory, File standardOut, File standardError, Process localProcess,
			Process remoteProcess, BiFunction<Integer, File, Process> remoteProcessRestarter) {
		this.classesDirectory = classesDirectory;
		this.standardOut = standardOut;
		this.standardError = standardError;
		this.localProcess = localProcess;
		this.remoteProcess = remoteProcess;
		this.remoteProcessRestarter = remoteProcessRestarter;
	}

	void restartRemote(int port) throws InterruptedException {
		if (this.remoteProcessRestarter != null) {
			stop(this.remoteProcess);
			this.remoteProcess = this.remoteProcessRestarter.apply(port, this.classesDirectory);
		}
	}

	void stop() throws InterruptedException {
		stop(this.localProcess);
		stop(this.remoteProcess);
	}

	private void stop(Process process) throws InterruptedException {
		if (process != null) {
			process.destroy();
			process.waitFor();
		}
	}

	File getStandardOut() {
		return this.standardOut;
	}

	File getStandardError() {
		return this.standardError;
	}

	File getClassesDirectory() {
		return this.classesDirectory;
	}

	Instant getLaunchTime() {
		return this.launchTime;
	}

}
