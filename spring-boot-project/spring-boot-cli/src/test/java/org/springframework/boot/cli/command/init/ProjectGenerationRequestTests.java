/*
 * Copyright 2012-2017 the original author or authors.
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
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProjectGenerationRequest}.
 *
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
public class ProjectGenerationRequestTests {

	public static final Map<String, String> EMPTY_TAGS = Collections.emptyMap();

	@Rule
	public final ExpectedException thrown = ExpectedException.none();

	private final ProjectGenerationRequest request = new ProjectGenerationRequest();

	@Test
	public void defaultSettings() {
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?type=test-type"));
	}

	@Test
	public void customServer() throws URISyntaxException {
		String customServerUrl = "http://foo:8080/initializr";
		this.request.setServiceUrl(customServerUrl);
		this.request.getDependencies().add("security");
		assertThat(this.request.generateUrl(createDefaultMetadata())).isEqualTo(new URI(
				customServerUrl + "/starter.zip?dependencies=security&type=test-type"));
	}

	@Test
	public void customBootVersion() {
		this.request.setBootVersion("1.2.0.RELEASE");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?type=test-type&bootVersion=1.2.0.RELEASE"));
	}

	@Test
	public void singleDependency() {
		this.request.getDependencies().add("web");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?dependencies=web&type=test-type"));
	}

	@Test
	public void multipleDependencies() {
		this.request.getDependencies().add("web");
		this.request.getDependencies().add("data-jpa");
		assertThat(this.request.generateUrl(createDefaultMetadata())).isEqualTo(
				createDefaultUrl("?dependencies=web%2Cdata-jpa&type=test-type"));
	}

	@Test
	public void customJavaVersion() {
		this.request.setJavaVersion("1.8");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?type=test-type&javaVersion=1.8"));
	}

	@Test
	public void customPackageName() {
		this.request.setPackageName("demo.foo");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?packageName=demo.foo&type=test-type"));
	}

	@Test
	public void customType() throws URISyntaxException {
		ProjectType projectType = new ProjectType("custom", "Custom Type", "/foo", true,
				EMPTY_TAGS);
		InitializrServiceMetadata metadata = new InitializrServiceMetadata(projectType);
		this.request.setType("custom");
		this.request.getDependencies().add("data-rest");
		assertThat(this.request.generateUrl(metadata))
				.isEqualTo(new URI(ProjectGenerationRequest.DEFAULT_SERVICE_URL
						+ "/foo?dependencies=data-rest&type=custom"));
	}

	@Test
	public void customPackaging() {
		this.request.setPackaging("war");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?type=test-type&packaging=war"));
	}

	@Test
	public void customLanguage() {
		this.request.setLanguage("groovy");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?type=test-type&language=groovy"));
	}

	@Test
	public void customProjectInfo() {
		this.request.setGroupId("org.acme");
		this.request.setArtifactId("sample");
		this.request.setVersion("1.0.1-SNAPSHOT");
		this.request.setDescription("Spring Boot Test");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl(
						"?groupId=org.acme&artifactId=sample&version=1.0.1-SNAPSHOT"
								+ "&description=Spring+Boot+Test&type=test-type"));
	}

	@Test
	public void outputCustomizeArtifactId() {
		this.request.setOutput("my-project");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?artifactId=my-project&type=test-type"));
	}

	@Test
	public void outputArchiveCustomizeArtifactId() {
		this.request.setOutput("my-project.zip");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?artifactId=my-project&type=test-type"));
	}

	@Test
	public void outputArchiveWithDotsCustomizeArtifactId() {
		this.request.setOutput("my.nice.project.zip");
		assertThat(this.request.generateUrl(createDefaultMetadata())).isEqualTo(
				createDefaultUrl("?artifactId=my.nice.project&type=test-type"));
	}

	@Test
	public void outputDoesNotOverrideCustomArtifactId() {
		this.request.setOutput("my-project");
		this.request.setArtifactId("my-id");
		assertThat(this.request.generateUrl(createDefaultMetadata()))
				.isEqualTo(createDefaultUrl("?artifactId=my-id&type=test-type"));
	}

	@Test
	public void buildNoMatch() throws Exception {
		InitializrServiceMetadata metadata = readMetadata();
		setBuildAndFormat("does-not-exist", null);
		this.thrown.expect(ReportableException.class);
		this.thrown.expectMessage("does-not-exist");
		this.request.generateUrl(metadata);
	}

	@Test
	public void buildMultipleMatch() throws Exception {
		InitializrServiceMetadata metadata = readMetadata("types-conflict");
		setBuildAndFormat("gradle", null);
		this.thrown.expect(ReportableException.class);
		this.thrown.expectMessage("gradle-project");
		this.thrown.expectMessage("gradle-project-2");
		this.request.generateUrl(metadata);
	}

	@Test
	public void buildOneMatch() throws Exception {
		InitializrServiceMetadata metadata = readMetadata();
		setBuildAndFormat("gradle", null);
		assertThat(this.request.generateUrl(metadata))
				.isEqualTo(createDefaultUrl("?type=gradle-project"));
	}

	@Test
	public void typeAndBuildAndFormat() throws Exception {
		InitializrServiceMetadata metadata = readMetadata();
		setBuildAndFormat("gradle", "project");
		this.request.setType("maven-build");
		assertThat(this.request.generateUrl(metadata))
				.isEqualTo(createUrl("/pom.xml?type=maven-build"));
	}

	@Test
	public void invalidType() {
		this.request.setType("does-not-exist");
		this.thrown.expect(ReportableException.class);
		this.request.generateUrl(createDefaultMetadata());
	}

	@Test
	public void noTypeAndNoDefault() throws Exception {
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

	private static InitializrServiceMetadata readMetadata() throws JSONException {
		return readMetadata("2.0.0");
	}

	private static InitializrServiceMetadata readMetadata(String version)
			throws JSONException {
		try {
			Resource resource = new ClassPathResource(
					"metadata/service-metadata-" + version + ".json");
			String content = StreamUtils.copyToString(resource.getInputStream(),
					StandardCharsets.UTF_8);
			JSONObject json = new JSONObject(content);
			return new InitializrServiceMetadata(json);
		}
		catch (IOException ex) {
			throw new IllegalStateException("Failed to read metadata", ex);
		}
	}

}
