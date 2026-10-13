/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.tests;

import java.io.File;

import org.springframework.boot.testsupport.BuildOutput;

/**
 * Various directories used by the {@link ApplicationLauncher ApplicationLaunchers}.
 *
 * @author Andy Wilkinson
 */
class Directories {

	private final BuildOutput buildOutput;

	private final File temp;

	Directories(BuildOutput buildOutput, File temp) {
		this.buildOutput = buildOutput;
		this.temp = temp;
	}

	File getTestClassesDirectory() {
		return this.buildOutput.getTestClassesLocation();
	}

	File getRemoteAppDirectory() {
		return new File(this.temp, "remote");
	}

	File getDependenciesDirectory() {
		return new File(this.buildOutput.getRootLocation(), "dependencies");
	}

	File getAppDirectory() {
		return new File(this.temp, "app");
	}

}
