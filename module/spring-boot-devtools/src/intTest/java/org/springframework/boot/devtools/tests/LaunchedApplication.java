/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.time.Instant;
import java.util.function.BiFunction;

import org.jspecify.annotations.Nullable;

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

	private @Nullable Process remoteProcess;

	private final Instant launchTime = Instant.now();

	private final @Nullable BiFunction<Integer, File, Process> remoteProcessRestarter;

	LaunchedApplication(File classesDirectory, File standardOut, File standardError, Process localProcess,
			@Nullable Process remoteProcess, @Nullable BiFunction<Integer, File, Process> remoteProcessRestarter) {
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

	private void stop(@Nullable Process process) throws InterruptedException {
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
