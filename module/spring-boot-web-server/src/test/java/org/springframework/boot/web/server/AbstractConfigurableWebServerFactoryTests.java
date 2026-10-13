/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory.TempDirs;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AbstractConfigurableWebServerFactory}.
 *
 * @author Moritz Halbritter
 */
class AbstractConfigurableWebServerFactoryTests {

	@Test
	void shouldCleanUpTempDirs() throws IOException {
		TempDirs tempDirs = new TempDirs(1234);
		Path tempDir1 = tempDirs.createTempDir(getClass().getSimpleName());
		Path tempDir2 = tempDirs.createTempDir(getClass().getSimpleName());
		assertThat(tempDir1).isNotEqualTo(tempDir2);
		assertThat(tempDir1).exists();
		assertThat(tempDir2).exists();
		Files.writeString(tempDir1.resolve("test.txt"), "test");
		tempDirs.cleanup();
		assertThat(tempDir1).doesNotExist();
		assertThat(tempDir2).doesNotExist();
	}

}
