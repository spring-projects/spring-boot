/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.cli.command.init;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.boot.cli.json.JSONException;
import org.springframework.boot.cli.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InitializrServiceMetadata}.
 *
 * @author Stephane Nicoll
 */
class InitializrServiceMetadataTests {

	@Test
	void parseDefaults() throws Exception {
		InitializrServiceMetadata metadata = createInstance("2.0.0");
		assertThat(metadata.getDefaults()).containsEntry("bootVersion", "1.1.8.RELEASE");
		assertThat(metadata.getDefaults()).containsEntry("javaVersion", "1.7");
		assertThat(metadata.getDefaults()).containsEntry("groupId", "org.test");
		assertThat(metadata.getDefaults()).containsEntry("name", "demo");
		assertThat(metadata.getDefaults()).containsEntry("description", "Demo project for Spring Boot");
		assertThat(metadata.getDefaults()).containsEntry("packaging", "jar");
		assertThat(metadata.getDefaults()).containsEntry("language", "java");
		assertThat(metadata.getDefaults()).containsEntry("artifactId", "demo");
		assertThat(metadata.getDefaults()).containsEntry("packageName", "demo");
		assertThat(metadata.getDefaults()).containsEntry("type", "maven-project");
		assertThat(metadata.getDefaults()).containsEntry("version", "0.0.1-SNAPSHOT");
		assertThat(metadata.getDefaults()).as("Wrong number of defaults").hasSize(11);
	}

	@Test
	void parseDependencies() throws Exception {
		InitializrServiceMetadata metadata = createInstance("2.0.0");
		assertThat(metadata.getDependencies()).hasSize(5);

		// Security description
		assertThat(metadata.getDependency("aop").getName()).isEqualTo("AOP");
		assertThat(metadata.getDependency("security").getName()).isEqualTo("Security");
		assertThat(metadata.getDependency("security").getDescription()).isEqualTo("Security description");
		assertThat(metadata.getDependency("jdbc").getName()).isEqualTo("JDBC");
		assertThat(metadata.getDependency("data-jpa").getName()).isEqualTo("JPA");
		assertThat(metadata.getDependency("data-mongodb").getName()).isEqualTo("MongoDB");
	}

	@Test
	void parseTypes() throws Exception {
		InitializrServiceMetadata metadata = createInstance("2.0.0");
		ProjectType projectType = metadata.getProjectTypes().get("maven-project");
		assertThat(projectType).isNotNull();
		assertThat(projectType.getTags()).containsEntry("build", "maven");
		assertThat(projectType.getTags()).containsEntry("format", "project");
	}

	private static InitializrServiceMetadata createInstance(String version) throws JSONException {
		try {
			return new InitializrServiceMetadata(readJson(version));
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to read json", ex);
		}
	}

	private static JSONObject readJson(String version) throws IOException, JSONException {
		Resource resource = new ClassPathResource("metadata/service-metadata-" + version + ".json");
		try (InputStream stream = resource.getInputStream()) {
			return new JSONObject(StreamUtils.copyToString(stream, StandardCharsets.UTF_8));
		}
	}

}
