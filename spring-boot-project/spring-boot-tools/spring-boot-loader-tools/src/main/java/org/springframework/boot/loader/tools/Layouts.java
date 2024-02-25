/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.loader.tools;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Common {@link Layout layouts}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @author Madhura Bhave
 * @author Scott Frederick
 * @since 1.0.0
 */
public final class Layouts {

	/**
     * Constructs a new instance of the Layouts class.
     */
    private Layouts() {
	}

	/**
	 * Return a layout for the given source file.
	 * @param file the source file
	 * @return a {@link Layout}
	 */
	public static Layout forFile(File file) {
		if (file == null) {
			throw new IllegalArgumentException("File must not be null");
		}
		String lowerCaseFileName = file.getName().toLowerCase(Locale.ENGLISH);
		if (lowerCaseFileName.endsWith(".jar")) {
			return new Jar();
		}
		if (lowerCaseFileName.endsWith(".war")) {
			return new War();
		}
		if (file.isDirectory() || lowerCaseFileName.endsWith(".zip")) {
			return new Expanded();
		}
		throw new IllegalStateException("Unable to deduce layout for '" + file + "'");
	}

	/**
	 * Executable JAR layout.
	 */
	public static class Jar implements RepackagingLayout {

		/**
         * Returns the launcher class name for the Jar.
         * 
         * @return the launcher class name for the Jar
         */
        @Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.launch.JarLauncher";
		}

		/**
         * Returns the location of the library with the specified name and scope.
         * 
         * @param libraryName the name of the library
         * @param scope the scope of the library
         * @return the location of the library
         */
        @Override
		public String getLibraryLocation(String libraryName, LibraryScope scope) {
			return "BOOT-INF/lib/";
		}

		/**
         * Returns the location of the classes in the Jar.
         *
         * @return the location of the classes in the Jar
         */
        @Override
		public String getClassesLocation() {
			return "";
		}

		/**
         * Returns the location of the repackaged classes.
         * 
         * @return the location of the repackaged classes
         */
        @Override
		public String getRepackagedClassesLocation() {
			return "BOOT-INF/classes/";
		}

		/**
         * Returns the location of the classpath index file.
         * 
         * @return the location of the classpath index file
         */
        @Override
		public String getClasspathIndexFileLocation() {
			return "BOOT-INF/classpath.idx";
		}

		/**
         * Returns the location of the layers index file.
         * 
         * @return the location of the layers index file
         */
        @Override
		public String getLayersIndexFileLocation() {
			return "BOOT-INF/layers.idx";
		}

		/**
         * Returns a boolean value indicating whether the method is executable.
         *
         * @return true if the method is executable, false otherwise.
         */
        @Override
		public boolean isExecutable() {
			return true;
		}

	}

	/**
	 * Executable expanded archive layout.
	 */
	public static class Expanded extends Jar {

		/**
         * Returns the launcher class name.
         *
         * @return the launcher class name
         */
        @Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.launch.PropertiesLauncher";
		}

	}

	/**
	 * No layout.
	 */
	public static class None extends Jar {

		/**
         * Returns the launcher class name.
         * 
         * @return the launcher class name, which is null in this case.
         */
        @Override
		public String getLauncherClassName() {
			return null;
		}

		/**
         * Returns whether the method is executable.
         * 
         * @return true if the method is executable, false otherwise.
         */
        @Override
		public boolean isExecutable() {
			return false;
		}

	}

	/**
	 * Executable WAR layout.
	 */
	public static class War implements Layout {

		private static final Map<LibraryScope, String> SCOPE_LOCATION;

		static {
			Map<LibraryScope, String> locations = new HashMap<>();
			locations.put(LibraryScope.COMPILE, "WEB-INF/lib/");
			locations.put(LibraryScope.CUSTOM, "WEB-INF/lib/");
			locations.put(LibraryScope.RUNTIME, "WEB-INF/lib/");
			locations.put(LibraryScope.PROVIDED, "WEB-INF/lib-provided/");
			SCOPE_LOCATION = Collections.unmodifiableMap(locations);
		}

		/**
         * Returns the launcher class name for the War class.
         * 
         * @return the launcher class name for the War class
         */
        @Override
		public String getLauncherClassName() {
			return "org.springframework.boot.loader.launch.WarLauncher";
		}

		/**
         * Returns the location of the specified library based on the given library name and scope.
         *
         * @param libraryName the name of the library
         * @param scope the scope of the library
         * @return the location of the library
         */
        @Override
		public String getLibraryLocation(String libraryName, LibraryScope scope) {
			return SCOPE_LOCATION.get(scope);
		}

		/**
         * Returns the location of the classes directory in the web application.
         * 
         * @return the location of the classes directory in the web application
         */
        @Override
		public String getClassesLocation() {
			return "WEB-INF/classes/";
		}

		/**
         * Returns the location of the classpath index file.
         * 
         * @return the location of the classpath index file
         */
        @Override
		public String getClasspathIndexFileLocation() {
			return "WEB-INF/classpath.idx";
		}

		/**
         * Returns the file location of the layers index file.
         * 
         * @return the file location of the layers index file
         */
        @Override
		public String getLayersIndexFileLocation() {
			return "WEB-INF/layers.idx";
		}

		/**
         * Returns a boolean value indicating whether the method is executable.
         *
         * @return true if the method is executable, false otherwise.
         */
        @Override
		public boolean isExecutable() {
			return true;
		}

	}

}
