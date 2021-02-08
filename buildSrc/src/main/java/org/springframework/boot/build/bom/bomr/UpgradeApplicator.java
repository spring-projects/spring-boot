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

package org.springframework.boot.build.bom.bomr;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code UpgradeApplicator} is used to apply an {@link Upgrade}. Modifies the bom
 * configuration in the build file or a version property in {@code gradle.properties}.
 *
 * @author Andy Wilkinson
 */
class UpgradeApplicator {

	private final Path buildFile;

	private final Path gradleProperties;

	UpgradeApplicator(Path buildFile, Path gradleProperties) {
		this.buildFile = buildFile;
		this.gradleProperties = gradleProperties;
	}

	Path apply(Upgrade upgrade) throws IOException {
		String buildFileContents = new String(Files.readAllBytes(this.buildFile), StandardCharsets.UTF_8);
		Matcher matcher = Pattern.compile("library\\(\"" + upgrade.getLibrary().getName() + "\", \"(.+)\"\\)")
				.matcher(buildFileContents);
		if (!matcher.find()) {
			throw new IllegalStateException("Failed to find definition for library '" + upgrade.getLibrary().getName()
					+ "' in bom '" + this.buildFile + "'");
		}
		String version = matcher.group(1);
		if (version.startsWith("${") && version.endsWith("}")) {
			updateGradleProperties(upgrade, version);
			return this.gradleProperties;
		}
		else {
			updateBuildFile(upgrade, buildFileContents);
			return this.buildFile;
		}
	}

	private void updateGradleProperties(Upgrade upgrade, String version) throws IOException {
		String property = version.substring(2, version.length() - 1);
		String gradlePropertiesContents = new String(Files.readAllBytes(this.gradleProperties), StandardCharsets.UTF_8);
		String modified = gradlePropertiesContents.replace(property + "=" + upgrade.getLibrary().getVersion(),
				property + "=" + upgrade.getVersion());
		Files.write(this.gradleProperties, modified.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
	}

	private void updateBuildFile(Upgrade upgrade, String buildFileContents) throws IOException {
		String modified = buildFileContents.replace(
				"library(\"" + upgrade.getLibrary().getName() + "\", \"" + upgrade.getLibrary().getVersion() + "\")",
				"library(\"" + upgrade.getLibrary().getName() + "\", \"" + upgrade.getVersion() + "\")");
		Files.write(this.buildFile, modified.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
	}

}
