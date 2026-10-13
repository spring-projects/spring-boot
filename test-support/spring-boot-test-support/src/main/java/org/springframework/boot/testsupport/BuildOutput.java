/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport;

import java.io.File;
import java.net.URISyntaxException;

/**
 * Provides access to build output locations in a build system and IDE agnostic manner.
 *
 * @author Andy Wilkinson
 */
public class BuildOutput {

	private final Class<?> testClass;

	public BuildOutput(Class<?> testClass) {
		this.testClass = testClass;
	}

	/**
	 * Returns the location into which test classes have been built.
	 * @return test classes location
	 */
	public File getTestClassesLocation() {
		try {
			File location = new File(this.testClass.getProtectionDomain().getCodeSource().getLocation().toURI());
			if (location.getPath().endsWith(path("bin", "test")) || location.getPath().endsWith(path("bin", "intTest"))
					|| location.getPath().endsWith(path("build", "classes", "java", "test"))
					|| location.getPath().endsWith(path("build", "classes", "java", "intTest"))) {
				return location;
			}
			throw new IllegalStateException("Unexpected test classes location '" + location + "'");
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException("Invalid test class code source location", ex);
		}
	}

	/**
	 * Returns the location into which test resources have been built.
	 * @return test resources location
	 */
	public File getTestResourcesLocation() {
		File testClassesLocation = getTestClassesLocation();
		if (testClassesLocation.getPath().endsWith(path("bin", "test"))
				|| testClassesLocation.getPath().endsWith(path("bin", "intTest"))) {
			return testClassesLocation;
		}
		if (testClassesLocation.getPath().endsWith(path("build", "classes", "java", "test"))) {
			return new File(testClassesLocation.getParentFile().getParentFile().getParentFile(), "resources/test");
		}
		if (testClassesLocation.getPath().endsWith(path("build", "classes", "java", "intTest"))) {
			return new File(testClassesLocation.getParentFile().getParentFile().getParentFile(), "resources/intTest");
		}
		throw new IllegalStateException(
				"Cannot determine test resources location from classes location '" + testClassesLocation + "'");
	}

	/**
	 * Returns the root location into which build output is written.
	 * @return root location
	 */
	public File getRootLocation() {
		return new File("build");
	}

	private String path(String... components) {
		return File.separator + String.join(File.separator, components);
	}

}
