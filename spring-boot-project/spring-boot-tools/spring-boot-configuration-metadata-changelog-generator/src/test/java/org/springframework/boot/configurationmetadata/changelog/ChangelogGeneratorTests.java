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

package org.springframework.boot.configurationmetadata.changelog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ChangelogGenerator}.
 *
 * @author Phillip Webb
 */
class ChangelogGeneratorTests {

	@TempDir
	File temp;

	@Test
	void generateChangeLog() throws IOException {
		File oldJars = new File(this.temp, "1.0");
		addJar(oldJars, "sample-1.0.json");
		File newJars = new File(this.temp, "2.0");
		addJar(newJars, "sample-2.0.json");
		File out = new File(this.temp, "changes.adoc");
		String[] args = new String[] { oldJars.getAbsolutePath(), newJars.getAbsolutePath(), out.getAbsolutePath() };
		ChangelogGenerator.main(args);
		assertThat(out).usingCharset(StandardCharsets.UTF_8)
			.hasSameTextualContentAs(new File("src/test/resources/sample.adoc"));
	}

	private void addJar(File directory, String filename) throws IOException {
		directory.mkdirs();
		try (JarOutputStream out = new JarOutputStream(new FileOutputStream(new File(directory, "sample.jar")))) {
			out.putNextEntry(new ZipEntry("META-INF/spring-configuration-metadata.json"));
			try (InputStream in = new FileInputStream("src/test/resources/" + filename)) {
				in.transferTo(out);
				out.closeEntry();
			}
		}

	}

}
