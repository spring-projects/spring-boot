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

package org.springframework.boot.maven;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to check that our plugin works well with Eclipse m2e.
 *
 * @author Phillip Webb
 */
class EclipseM2eIntegrationTests {

	@Test // gh-21992
	void pluginPomIncludesOptionalShadeDependency() throws Exception {
		SpringBootDependenciesBom bom = new SpringBootDependenciesBom();
		String version = bom.get("version");
		File repository = new File("build/int-test-maven-repository");
		File pluginDirectory = new File(repository, "org/springframework/boot/spring-boot-maven-plugin/" + version);
		File[] pomFiles = pluginDirectory.listFiles(this::isPomFile);
		Arrays.sort(pomFiles, Comparator.comparing(File::getName));
		File pomFile = pomFiles[pomFiles.length - 1];
		String pomContent = new String(FileCopyUtils.copyToByteArray(pomFile), StandardCharsets.UTF_8);
		assertThat(pomContent).contains("maven-shade-plugin");
	}

	private boolean isPomFile(File file) {
		return file.getName().endsWith(".pom");
	}

}
