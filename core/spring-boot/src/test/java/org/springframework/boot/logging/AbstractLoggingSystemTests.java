/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.MDC;

import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.contentOf;

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
	void configureTempDir(@TempDir Path temp) throws IOException {
		this.originalTempDirectory = System.getProperty(JAVA_IO_TMPDIR);
		Files.delete(Files.createTempFile("prevent", "pollution"));
		File.createTempFile("prevent", "pollution").delete();
		System.setProperty(JAVA_IO_TMPDIR, temp.toAbsolutePath().toString());
		MDC.clear();
	}

	@AfterEach
	void reinstateTempDir() {
		System.setProperty(JAVA_IO_TMPDIR, this.originalTempDirectory);
	}

	@AfterEach
	void clear() {
		for (LoggingSystemProperty property : LoggingSystemProperty.values()) {
			System.getProperties().remove(property.getEnvironmentVariableName());
		}
		MDC.clear();
	}

	protected final String[] getSpringConfigLocations(AbstractLoggingSystem system) {
		return system.getSpringConfigLocations();
	}

	protected final LogFile getLogFile(@Nullable String file, @Nullable String path) {
		return getLogFile(file, path, true);
	}

	protected final LogFile getLogFile(@Nullable String file, @Nullable String path, boolean applyToSystemProperties) {
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

	protected final @Nullable String getLineWithText(File file, CharSequence outputSearch) {
		return getLineWithText(contentOf(file), outputSearch);
	}

	protected final @Nullable String getLineWithText(CharSequence output, CharSequence outputSearch) {
		return Arrays.stream(output.toString().split("\\r?\\n"))
			.filter((line) -> line.contains(outputSearch))
			.findFirst()
			.orElse(null);
	}

}
