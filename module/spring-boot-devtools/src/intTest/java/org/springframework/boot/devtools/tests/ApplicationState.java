/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.time.Instant;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.devtools.tests.JvmLauncher.LaunchedJvm;
import org.springframework.util.Assert;

/**
 * State of an application.
 *
 * @author Andy Wilkinson
 */
final class ApplicationState {

	private final Instant launchTime;

	private final @Nullable Integer serverPort;

	private final FileContents out;

	private final FileContents err;

	ApplicationState(File serverPortFile, LaunchedJvm jvm) {
		this(serverPortFile, jvm.getStandardOut(), jvm.getStandardError(), jvm.getLaunchTime());
	}

	ApplicationState(File serverPortFile, LaunchedApplication application) {
		this(serverPortFile, application.getStandardOut(), application.getStandardError(), application.getLaunchTime());
	}

	private ApplicationState(File serverPortFile, File out, File err, Instant launchTime) {
		this.serverPort = new FileContents(serverPortFile).get(Integer::parseInt);
		this.out = new FileContents(out);
		this.err = new FileContents(err);
		this.launchTime = launchTime;
	}

	boolean hasServerPort() {
		return this.serverPort != null;
	}

	int getServerPort() {
		Assert.notNull(this.serverPort, "No server port is available");
		return this.serverPort;
	}

	@Override
	public String toString() {
		return String.format("Application launched at %s produced output:%n%s%n%s", this.launchTime, this.out,
				this.err);
	}

}
