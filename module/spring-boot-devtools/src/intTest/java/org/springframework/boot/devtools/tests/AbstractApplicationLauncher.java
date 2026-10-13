/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.devtools.tests;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.util.FileSystemUtils;

/**
 * Base class for all {@link ApplicationLauncher} implementations.
 *
 * @author Andy Wilkinson
 */
abstract class AbstractApplicationLauncher implements ApplicationLauncher {

	private final Directories directories;

	AbstractApplicationLauncher(Directories directories) {
		this.directories = directories;
	}

	protected final void copyApplicationTo(File location) throws IOException {
		FileSystemUtils.deleteRecursively(location);
		location.mkdirs();
		FileSystemUtils.copyRecursively(new File(this.directories.getTestClassesDirectory(), "com"),
				new File(location, "com"));
	}

	protected final List<String> getDependencyJarPaths() {
		return Stream.of(this.directories.getDependenciesDirectory().listFiles()).map(File::getAbsolutePath).toList();
	}

	protected final Directories getDirectories() {
		return this.directories;
	}

}
