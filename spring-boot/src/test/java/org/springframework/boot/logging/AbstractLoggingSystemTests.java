/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.logging;

import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.util.StringUtils;

/**
 * Base for {@link LoggingSystem} tests.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public abstract class AbstractLoggingSystemTests {

	private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";

	private static String tempDir;

	@BeforeClass
	public static void configureTempdir() {
		tempDir = System.getProperty(JAVA_IO_TMPDIR);
		File newTempDir = new File("target/tmp");
		newTempDir.mkdirs();
		System.setProperty(JAVA_IO_TMPDIR, newTempDir.getAbsolutePath());
	}

	@AfterClass
	public static void reinstateTempDir() {
		System.setProperty(JAVA_IO_TMPDIR, tempDir);
	}

	@Before
	public void deleteTempLog() {
		new File(tmpDir() + "/spring.log").delete();
	}

	@After
	public void clear() {
		System.clearProperty("LOG_FILE");
		System.clearProperty("PID");
	}

	protected final LogFile getLogFile(String file, String path) {
		return new LogFile(file, path);
	}

	protected final String tmpDir() {
		String path = StringUtils.cleanPath(System.getProperty(JAVA_IO_TMPDIR));
		if (path.endsWith("/")) {
			path = path.substring(0, path.length() - 1);
		}
		return path;
	}

}
