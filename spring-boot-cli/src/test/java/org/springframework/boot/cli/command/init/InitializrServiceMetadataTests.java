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

package org.springframework.boot.cli.command.init;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.json.JSONObject;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link InitializrServiceMetadata}
 *
 * @author Stephane Nicoll
 */
public class InitializrServiceMetadataTests {

	@Test
	public void parseDefaults() {
		InitializrServiceMetadata metadata = createInstance("1.0.0");
		assertEquals("maven-project", metadata.getDefaults().get("type"));
		assertEquals("jar", metadata.getDefaults().get("packaging"));
		assertEquals("java", metadata.getDefaults().get("language"));
	}

	@Test
	public void parseDependencies() {
		InitializrServiceMetadata metadata = createInstance("1.0.0");
		assertEquals(5, metadata.getDependencies().size());

		// Security description
		assertEquals("AOP", metadata.getDependency("aop").getName());
		assertEquals("Security", metadata.getDependency("security").getName());
		assertEquals("Security description", metadata.getDependency("security")
				.getDescription());
		assertEquals("JDBC", metadata.getDependency("jdbc").getName());
		assertEquals("JPA", metadata.getDependency("data-jpa").getName());
		assertEquals("MongoDB", metadata.getDependency("data-mongodb").getName());
	}

	@Test
	public void parseTypesNoTag() {
		InitializrServiceMetadata metadata = createInstance("1.0.0");
		ProjectType projectType = metadata.getProjectTypes().get("maven-project");
		assertNotNull(projectType);
		assertEquals(0, projectType.getTags().size());
	}

	@Test
	public void parseTypesWithTags() {
		InitializrServiceMetadata metadata = createInstance("1.1.0");
		ProjectType projectType = metadata.getProjectTypes().get("maven-project");
		assertNotNull(projectType);
		assertEquals("maven", projectType.getTags().get("build"));
		assertEquals("project", projectType.getTags().get("format"));
	}

	private static InitializrServiceMetadata createInstance(String version) {
		try {
			return new InitializrServiceMetadata(readJson(version));
		}
		catch (IOException e) {
			throw new IllegalStateException("Failed to read json", e);
		}
	}

	private static JSONObject readJson(String version) throws IOException {
		Resource resource = new ClassPathResource("metadata/service-metadata-" + version
				+ ".json");
		InputStream stream = resource.getInputStream();
		try {
			String json = StreamUtils.copyToString(stream, Charset.forName("UTF-8"));
			return new JSONObject(json);
		}
		finally {
			stream.close();
		}
	}

}
