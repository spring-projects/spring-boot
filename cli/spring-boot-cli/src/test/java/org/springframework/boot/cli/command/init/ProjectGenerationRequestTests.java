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
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.boot.cli.json.JSONException;
import org.springframework.boot.cli.json.JSONObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for {@link ProjectGenerationRequest}.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class ProjectGenerationRequestTests {

	public static final Map<String, String> EMPTY_TAGS = Collections.emptyMap();

	private final ProjectGenerationRequest request = new ProjectGenerationRequest();

	@Test
	void defaultSettings() {
		assertThat(this.request.generateUrl(createDefaultMetadata())).isEqualTo(createDefaultUrl("?type=test-type"));
	}

	@Test
	void customServer() throws URISyntaxException {
		String customServerUrl = "https://foo:8080/initializr";
		this.request.setServiceUrl(customServerUrl);
		this.request.getDependencies().add("security");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(new URI(customServerUrl + "/starter.zip?dependencies=security&type=test-type"));
	}

	@Test
	void customBootVersion() {
		this.request.setBootVersion("1.2.0.RELEASE");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?type=test-type&bootVersion=1.2.0.RELEASE"));
	}

	@Test
	void singleDependency() {
		this.request.getDependencies().add("web");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?dependencies=web&type=test-type"));
	}

	@Test
	void multipleDependencies() {
		this.request.getDependencies().add("web");
		this.request.getDependencies().add("data-jpa");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?dependencies=web%2Cdata-jpa&type=test-type"));
	}

	@Test
	void customJavaVersion() {
		this.request.setJavaVersion("1.8");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?type=test-type&javaVersion=1.8"));
	}

	@Test
	void customPackageName() {
		this.request.setPackageName("demo.foo");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?packageName=demo.foo&type=test-type"));
	}

	@Test
	void customType() throws URISyntaxException {
		ProjectType projectType = new ProjectType("custom", "Custom Type", "/foo", true, EMPTY_TAGS);
		InitializrServiceMetadata metadata = new InitializrServiceMetadata(projectType);
		this.request.setType("custom");
		this.request.getDependencies().add("data-rest");
		assertThat(this.request.generateUrl(metadata)).isEqualTo(
				new URI(ProjectGenerationRequest.DEFAULT_SERVICE_URL + "/foo?dependencies=data-rest&type=custom"));
	}

	@Test
	void customPackaging() {
		this.request.setPackaging("war");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?type=test-type&packaging=war"));
	}

	@Test
	void customLanguage() {
		this.request.setLanguage("groovy");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?type=test-type&language=groovy"));
	}

	@Test
	void customProjectInfo() {
		this.request.setGroupId("org.acme");
		this.request.setArtifactId("sample");
		this.request.setVersion("1.0.1-SNAPSHOT");
		this.request.setDescription("Spring Boot Test");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?groupId=org.acme&artifactId=sample&version=1.0.1-SNAPSHOT"
					+ "&description=Spring%20Boot%20Test&type=test-type"));
	}

	@Test
	void outputCustomizeArtifactId() {
		this.request.setOutput("my-project");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?artifactId=my-project&type=test-type"));
	}

	@Test
	void outputArchiveCustomizeArtifactId() {
		this.request.setOutput("my-project.zip");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?artifactId=my-project&type=test-type"));
	}

	@Test
	void outputArchiveWithDotsCustomizeArtifactId() {
		this.request.setOutput("my.nice.project.zip");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?artifactId=my.nice.project&type=test-type"));
	}

	@Test
	void outputDoesNotOverrideCustomArtifactId() {
		this.request.setOutput("my-project");
		this.request.setArtifactId("my-id");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
			.isEqualTo(createDefaultUrl("?artifactId=my-id&type=test-type"));
	}

	@Test
	void buildNoMatch() throws Exception {
		InitializrServiceMetadata metadata = readMetadata();
		setBuildAndFormat("does-not-exist", null);
		assertThatExceptionOfType(ReportableException.class).isThrownBy(() -> this.request.generateUrl(metadata))
			.withMessageContaining("does-not-exist");
	}

	@Test
	void buildMultipleMatch() throws Exception {
		InitializrServiceMetadata metadata = readMetadata("types-conflict");
		setBuildAndFormat("gradle", null);
		assertThatExceptionOfType(ReportableException.class).isThrownBy(() -> this.request.generateUrl(metadata))
			.withMessageContaining("gradle-project")
			.withMessageContaining("gradle-project-2");
	}

	@Test
	void buildOneMatch() throws Exception {
		InitializrServiceMetadata metadata = readMetadata();
		setBuildAndFormat("gradle", null);
		assertThat(this.request.generateUrl(metadata)).isEqualTo(createDefaultUrl("?type=gradle-project"));
	}

	@Test
	void typeAndBuildAndFormat() throws Exception {
		InitializrServiceMetadata metadata = readMetadata();
		setBuildAndFormat("gradle", "project");
		this.request.setType("maven-build");
		assertThat(this.request.generateUrl(metadata)).isEqualTo(createUrl("/pom.xml?type=maven-build"));
	}

	@Test
	void invalidType() {
		this.request.setType("does-not-exist");
		assertThatExceptionOfType(ReportableException.class)
			.isThrownBy(() -> this.request.generateUrl(createDefaultMetadata()));
	}

	@Test
	void noTypeAndNoDefault() {
		assertThatExceptionOfType(ReportableException.class)
			.isThrownBy(() -> this.request.generateUrl(readMetadata("types-conflict")))
			.withMessageContaining("no default is defined");
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

	void setBuildAndFormat(String build, String format) {
		this.request.setBuild((build != null) ? build : "maven");
		this.request.setFormat((format != null) ? format : "project");
		this.request.setDetectType(true);
	}

	private static InitializrServiceMetadata createDefaultMetadata() {
		ProjectType projectType = new ProjectType("test-type", "The test type", "/starter.zip", true, EMPTY_TAGS);
		return new InitializrServiceMetadata(projectType);
	}

	private static InitializrServiceMetadata readMetadata() throws JSONException {
		return readMetadata("2.0.0");
	}

	private static InitializrServiceMetadata readMetadata(String version) throws JSONException {
		try {
			Resource resource = new ClassPathResource("metadata/service-metadata-" + version + ".json");
			String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
			JSONObject json = new JSONObject(content);
			return new InitializrServiceMetadata(json);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to read metadata", ex);
		}
	}

}
