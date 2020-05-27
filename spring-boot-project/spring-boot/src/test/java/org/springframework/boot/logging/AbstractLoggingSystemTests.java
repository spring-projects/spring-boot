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

package org.springframework.boot.logging;

import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.util.StringUtils;

/**
 * Base for {@link LoggingSystem} tests.
 *
 * @author Ilya Lukyanovich
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public abstract class AbstractLoggingSystemTests {

	private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

	private String originalTempDirectory;

	@BeforeEach
	void configureTempDir(@TempDir Path temp) {
		this.originalTempDirectory = System.getProperty(JAVA_IO_TMPDIR);
		System.setProperty(JAVA_IO_TMPDIR, temp.toAbsolutePath().toString());
	}

	@AfterEach
	void reinstateTempDir() {
		System.setProperty(JAVA_IO_TMPDIR, this.originalTempDirectory);
	}

	@AfterEach
	void clear() {
		System.clearProperty(LoggingSystemProperties.LOG_FILE);
		System.clearProperty(LoggingSystemProperties.PID_KEY);
	}

	protected final String[] getSpringConfigLocations(AbstractLoggingSystem system) {
		return system.getSpringConfigLocations();
	}

	protected final LogFile getLogFile(String file, String path) {
		return getLogFile(file, path, true);
	}

	protected final LogFile getLogFile(String file, String path, boolean applyToSystemProperties) {
		LogFile logFile = new LogFile(file, path);
		if (applyToSystemProperties) {
			logFile.applyToSystemProperties();
		}
		return logFile;
	}

	protected final String tmpDir() {
		String path = StringUtils.cleanPath(System.getProperty(JAVA_IO_TMPDIR));
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

}
