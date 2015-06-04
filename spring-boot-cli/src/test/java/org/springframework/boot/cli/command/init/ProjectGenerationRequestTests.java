/*
 * Copyright 2012-2015 the original author or authors.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ProjectGenerationRequest}
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
public class ProjectGenerationRequestTests {

	public static final Map<String, String> EMPTY_TAGS = Collections
			.<String, String> emptyMap();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final ProjectGenerationRequest request = new ProjectGenerationRequest();

	@Test
	public void defaultSettings() {
		assertEquals(createDefaultUrl("?type=test-type"),
				this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customServer() throws URISyntaxException {
		String customServerUrl = "http://foo:8080/initializr";
		this.request.setServiceUrl(customServerUrl);
		this.request.getDependencies().add("security");
		assertEquals(new URI(customServerUrl
				+ "/starter.zip?dependencies=security&type=test-type"),
				this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customBootVersion() {
		this.request.setBootVersion("1.2.0.RELEASE");
		assertEquals(createDefaultUrl("?type=test-type&bootVersion=1.2.0.RELEASE"),
				this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void singleDependency() {
		this.request.getDependencies().add("web");
		assertEquals(createDefaultUrl("?dependencies=web&type=test-type"),
				this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void multipleDependencies() {
		this.request.getDependencies().add("web");
		this.request.getDependencies().add("data-jpa");
		assertEquals(createDefaultUrl("?dependencies=web%2Cdata-jpa&type=test-type"),
				this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customJavaVersion() {
		this.request.setJavaVersion("1.8");
		assertEquals(createDefaultUrl("?type=test-type&javaVersion=1.8"),
				this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customPackaging() {
		this.request.setPackaging("war");
		assertEquals(createDefaultUrl("?packaging=war&type=test-type"),
				this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customType() throws URISyntaxException {
		ProjectType projectType = new ProjectType("custom", "Custom Type", "/foo", true,
				EMPTY_TAGS);
		InitializrServiceMetadata metadata = new InitializrServiceMetadata(projectType);
		this.request.setType("custom");
		this.request.getDependencies().add("data-rest");
		assertEquals(new URI(ProjectGenerationRequest.DEFAULT_SERVICE_URL
				+ "/foo?dependencies=data-rest&type=custom"),
				this.request.generateUrl(metadata));
	}

	@Test
	public void customLanguage() {
		this.request.setLanguage("groovy");
		assertEquals(createDefaultUrl("?type=test-type&language=groovy"),
				this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void customProjectInfo() {
		this.request.setGroupId("org.acme");
		this.request.setArtifactId("sample");
		this.request.setVersion("1.0.1-SNAPSHOT");
		this.request.setDescription("Spring Boot Test");
		assertEquals(
				createDefaultUrl("?groupId=org.acme&artifactId=sample&version=1.0.1-SNAPSHOT"
						+ "&description=Spring+Boot+Test&type=test-type"),
				this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	public void buildNoMatch() {
		InitializrServiceMetadata metadata = readMetadata();
		setBuildAndFormat("does-not-exist", null);
		this.thrown.expect(ReportableException.class);
		this.thrown.expectMessage("does-not-exist");
		this.request.generateUrl(metadata);
	}

	@Test
	public void buildMultipleMatch() {
		InitializrServiceMetadata metadata = readMetadata("types-conflict");
		setBuildAndFormat("gradle", null);
		this.thrown.expect(ReportableException.class);
		this.thrown.expectMessage("gradle-project");
		this.thrown.expectMessage("gradle-project-2");
		this.request.generateUrl(metadata);
	}

	@Test
	public void buildOneMatch() {
		InitializrServiceMetadata metadata = readMetadata();
		setBuildAndFormat("gradle", null);
		assertEquals(createDefaultUrl("?type=gradle-project"),
				this.request.generateUrl(metadata));
	}

	@Test
	public void typeAndBuildAndFormat() {
		InitializrServiceMetadata metadata = readMetadata();
		setBuildAndFormat("gradle", "project");
		this.request.setType("maven-build");
		assertEquals(createUrl("/pom.xml?type=maven-build"),
				this.request.generateUrl(metadata));
	}

	@Test
	public void invalidType() throws URISyntaxException {
		this.request.setType("does-not-exist");
		this.thrown.expect(ReportableException.class);
		this.request.generateUrl(createDefaultMetadata());
	}

	@Test
	public void noTypeAndNoDefault() throws URISyntaxException {
		this.thrown.expect(ReportableException.class);
		this.thrown.expectMessage("no default is defined");
		this.request.generateUrl(readMetadata("types-conflict"));
	}

	private static URI createUrl(String actionAndParam) {
		try {
			return new URI(ProjectGenerationRequest.DEFAULT_SERVICE_URL + actionAndParam);
		}
		catch (URISyntaxException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static URI createDefaultUrl(String param) {
		return createUrl("/starter.zip" + param);
	}

	public void setBuildAndFormat(String build, String format) {
		this.request.setBuild(build != null ? build : "maven");
		this.request.setFormat(format != null ? format : "project");
		this.request.setDetectType(true);
	}

	private static InitializrServiceMetadata createDefaultMetadata() {
		ProjectType projectType = new ProjectType("test-type", "The test type",
				"/starter.zip", true, EMPTY_TAGS);
		return new InitializrServiceMetadata(projectType);
	}

	private static InitializrServiceMetadata readMetadata() {
		return readMetadata("2.0.0");
	}

	private static InitializrServiceMetadata readMetadata(String version) {
		try {
			Resource resource = new ClassPathResource("metadata/service-metadata-"
					+ version + ".json");
			String content = StreamUtils.copyToString(resource.getInputStream(),
					Charset.forName("UTF-8"));
			JSONObject json = new JSONObject(content);
			return new InitializrServiceMetadata(json);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to read metadata", ex);
		}
	}

}
