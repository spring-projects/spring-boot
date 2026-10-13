/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.testsupport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link FileUtils}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class FileUtilsTests {

	@TempDir
	File tempDir;

	@Test
	void hash() throws Exception {
		File file = new File(this.tempDir, "file");
		try (OutputStream outputStream = new FileOutputStream(file)) {
			outputStream.write(new byte[] { 1, 2, 3 });
		}
		assertThat(FileUtils.sha1Hash(file)).isEqualTo("7037807198c22a7d2b0807371d763779a84fdfcf");
	}

}
