/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.build.bom.bomr;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.build.bom.Library;
import org.springframework.boot.build.bom.bomr.version.DependencyVersion;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link UpgradeApplicator}.
 *
 * @author Andy Wilkinson
 */
class UpgradeApplicatorTests {

	@TempDir
	File temp;

	@Test
	void whenUpgradeIsAppliedToLibraryWithVersionThenBomIsUpdated() throws IOException {
		File bom = new File(this.temp, "bom.gradle");
		FileCopyUtils.copy(new File("src/test/resources/bom.gradle"), bom);
		String originalContents = new String(Files.readAllBytes(bom.toPath()), StandardCharsets.UTF_8);
		File gradleProperties = new File(this.temp, "gradle.properties");
		FileCopyUtils.copy(new File("src/test/resources/gradle.properties"), gradleProperties);
		new UpgradeApplicator(bom.toPath(), gradleProperties.toPath())
				.apply(new Upgrade(new Library("ActiveMQ", DependencyVersion.parse("5.15.11"), null, null),
						DependencyVersion.parse("5.16")));
		String bomContents = new String(Files.readAllBytes(bom.toPath()), StandardCharsets.UTF_8);
		assertThat(bomContents.length()).isEqualTo(originalContents.length() - 3);
	}

	@Test
	void whenUpgradeIsAppliedToLibraryWithVersionPropertyThenGradlePropertiesIsUpdated() throws IOException {
		File bom = new File(this.temp, "bom.gradle");
		FileCopyUtils.copy(new File("src/test/resources/bom.gradle"), bom);
		File gradleProperties = new File(this.temp, "gradle.properties");
		FileCopyUtils.copy(new File("src/test/resources/gradle.properties"), gradleProperties);
		new UpgradeApplicator(bom.toPath(), gradleProperties.toPath()).apply(new Upgrade(
				new Library("Kotlin", DependencyVersion.parse("1.3.70"), null, null), DependencyVersion.parse("1.4")));
		Properties properties = new Properties();
		try (InputStream in = new FileInputStream(gradleProperties)) {
			properties.load(in);
		}
		assertThat(properties).containsOnly(entry("a", "alpha"), entry("b", "bravo"), entry("kotlinVersion", "1.4"),
				entry("t", "tango"));
	}

}
