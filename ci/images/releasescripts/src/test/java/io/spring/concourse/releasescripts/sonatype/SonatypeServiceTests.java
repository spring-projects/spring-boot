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

package io.spring.concourse.releasescripts.sonatype;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.spring.concourse.releasescripts.ReleaseInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link SonatypeService}.
 *
 * @author Madhura Bhave
 */
@RestClientTest(components = SonatypeService.class, properties = "sonatype.url=https://nexus.example.org")
@EnableConfigurationProperties(SonatypeProperties.class)
class SonatypeServiceTests {

	@Autowired
	private SonatypeService service;

	@Autowired
	private MockRestServiceServer server;

	@AfterEach
	void tearDown() {
		this.server.reset();
	}

	@Test
	void publishWhenAlreadyPublishedShouldNotPublish() {
		this.server.expect(requestTo(String.format(
				"/service/local/repositories/releases/content/org/springframework/boot/spring-boot/%s/spring-boot-%s.jar.sha1",
				"1.1.0.RELEASE", "1.1.0.RELEASE"))).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess().body("ce8d8b6838ecceb68962b9150b18682f4237ccf71".getBytes()));
		Path artifactsRoot = new File("src/test/resources/io/spring/concourse/releasescripts/sonatype/artifactory-repo")
				.toPath();
		this.service.publish(getReleaseInfo(), artifactsRoot);
		this.server.verify();
	}

	@Test
	void publishWithSuccessfulClose() throws IOException {
		this.server.expect(requestTo(String.format(
				"/service/local/repositories/releases/content/org/springframework/boot/spring-boot/%s/spring-boot-%s.jar.sha1",
				"1.1.0.RELEASE", "1.1.0.RELEASE"))).andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));
		this.server.expect(requestTo("/service/local/staging/profiles/1a2b3c4d/start"))
				.andExpect(method(HttpMethod.POST)).andExpect(header("Content-Type", "application/json"))
				.andExpect(header("Accept", "application/json, application/*+json"))
				.andExpect(jsonPath("$.data.description").value("example-build-1"))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(
						"{\"data\":{\"stagedRepositoryId\":\"example-6789\", \"description\":\"example-build\"}}"));
		Path artifactsRoot = new File("src/test/resources/io/spring/concourse/releasescripts/sonatype/artifactory-repo")
				.toPath();
		try (Stream<Path> artifacts = Files.walk(artifactsRoot)) {
			Set<RequestMatcher> uploads = artifacts.filter(Files::isRegularFile)
					.map((artifact) -> artifactsRoot.relativize(artifact))
					.filter((artifact) -> !artifact.startsWith("build-info.json"))
					.map((artifact) -> requestTo(
							"/service/local/staging/deployByRepositoryId/example-6789/" + artifact.toString()))
					.collect(Collectors.toSet());
			AnyOfRequestMatcher uploadRequestsMatcher = anyOf(uploads);
			assertThat(uploadRequestsMatcher.candidates).hasSize(150);
			this.server.expect(ExpectedCount.times(150), uploadRequestsMatcher).andExpect(method(HttpMethod.PUT))
					.andRespond(withSuccess());
			this.server.expect(requestTo("/service/local/staging/profiles/1a2b3c4d/finish"))
					.andExpect(method(HttpMethod.POST)).andExpect(header("Content-Type", "application/json"))
					.andExpect(header("Accept", "application/json, application/*+json"))
					.andRespond(withStatus(HttpStatus.CREATED));
			this.server.expect(ExpectedCount.times(2), requestTo("/service/local/staging/repository/example-6789"))
					.andExpect(method(HttpMethod.GET))
					.andExpect(header("Accept", "application/json, application/*+json"))
					.andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON)
							.body("{\"type\":\"open\", \"transitioning\":true}"));
			this.server.expect(requestTo("/service/local/staging/repository/example-6789"))
					.andExpect(method(HttpMethod.GET))
					.andExpect(header("Accept", "application/json, application/*+json"))
					.andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON)
							.body("{\"type\":\"closed\", \"transitioning\":false}"));
			this.server.expect(requestTo("/service/local/staging/bulk/promote")).andExpect(method(HttpMethod.POST))
					.andExpect(header("Content-Type", "application/json"))
					.andExpect(header("Accept", "application/json, application/*+json"))
					.andExpect(jsonPath("$.data.description").value("Releasing example-build-1"))
					.andExpect(jsonPath("$.data.autoDropAfterRelease").value(true))
					.andExpect(jsonPath("$.data.stagedRepositoryIds").value(equalTo(Arrays.asList("example-6789"))))
					.andRespond(withSuccess());
			this.service.publish(getReleaseInfo(), artifactsRoot);
			this.server.verify();
			assertThat(uploadRequestsMatcher.candidates).isEmpty();
		}
	}

	@Test
	void publishWithCloseFailureDueToRuleViolations() throws IOException {
		this.server.expect(requestTo(String.format(
				"/service/local/repositories/releases/content/org/springframework/boot/spring-boot/%s/spring-boot-%s.jar.sha1",
				"1.1.0.RELEASE", "1.1.0.RELEASE"))).andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));
		this.server.expect(requestTo("/service/local/staging/profiles/1a2b3c4d/start"))
				.andExpect(method(HttpMethod.POST)).andExpect(header("Content-Type", "application/json"))
				.andExpect(header("Accept", "application/json, application/*+json"))
				.andExpect(jsonPath("$.data.description").value("example-build-1"))
				.andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(
						"{\"data\":{\"stagedRepositoryId\":\"example-6789\", \"description\":\"example-build\"}}"));
		Path artifactsRoot = new File("src/test/resources/io/spring/concourse/releasescripts/sonatype/artifactory-repo")
				.toPath();
		try (Stream<Path> artifacts = Files.walk(artifactsRoot)) {
			Set<RequestMatcher> uploads = artifacts.filter(Files::isRegularFile)
					.map((artifact) -> artifactsRoot.relativize(artifact))
					.filter((artifact) -> !"build-info.json".equals(artifact.toString()))
					.map((artifact) -> requestTo(
							"/service/local/staging/deployByRepositoryId/example-6789/" + artifact.toString()))
					.collect(Collectors.toSet());
			AnyOfRequestMatcher uploadRequestsMatcher = anyOf(uploads);
			assertThat(uploadRequestsMatcher.candidates).hasSize(150);
			this.server.expect(ExpectedCount.times(150), uploadRequestsMatcher).andExpect(method(HttpMethod.PUT))
					.andRespond(withSuccess());
			this.server.expect(requestTo("/service/local/staging/profiles/1a2b3c4d/finish"))
					.andExpect(method(HttpMethod.POST)).andExpect(header("Content-Type", "application/json"))
					.andExpect(header("Accept", "application/json, application/*+json"))
					.andRespond(withStatus(HttpStatus.CREATED));
			this.server.expect(ExpectedCount.times(2), requestTo("/service/local/staging/repository/example-6789"))
					.andExpect(method(HttpMethod.GET))
					.andExpect(header("Accept", "application/json, application/*+json"))
					.andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON)
							.body("{\"type\":\"open\", \"transitioning\":true}"));
			this.server.expect(requestTo("/service/local/staging/repository/example-6789"))
					.andExpect(method(HttpMethod.GET))
					.andExpect(header("Accept", "application/json, application/*+json"))
					.andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON)
							.body("{\"type\":\"open\", \"transitioning\":false}"));
			this.server.expect(requestTo("/service/local/staging/repository/example-6789/activity"))
					.andExpect(method(HttpMethod.GET))
					.andExpect(header("Accept", "application/json, application/*+json"))
					.andRespond(withSuccess().contentType(MediaType.APPLICATION_JSON).body(new FileSystemResource(
							new File("src/test/resources/io/spring/concourse/releasescripts/sonatype/activity.json"))));
			assertThatExceptionOfType(RuntimeException.class)
					.isThrownBy(() -> this.service.publish(getReleaseInfo(), artifactsRoot))
					.withMessage("Close failed");
			this.server.verify();
			assertThat(uploadRequestsMatcher.candidates).isEmpty();
		}
	}

	private ReleaseInfo getReleaseInfo() {
		ReleaseInfo releaseInfo = new ReleaseInfo();
		releaseInfo.setBuildName("example-build");
		releaseInfo.setBuildNumber("example-build-1");
		releaseInfo.setVersion("1.1.0.RELEASE");
		releaseInfo.setGroupId("example");
		return releaseInfo;
	}

	private AnyOfRequestMatcher anyOf(Set<RequestMatcher> candidates) {
		return new AnyOfRequestMatcher(candidates);
	}

	private static class AnyOfRequestMatcher implements RequestMatcher {

		private final Object monitor = new Object();

		private final Set<RequestMatcher> candidates;

		private AnyOfRequestMatcher(Set<RequestMatcher> candidates) {
			this.candidates = candidates;
		}

		@Override
		public void match(ClientHttpRequest request) throws IOException, AssertionError {
			synchronized (this.monitor) {
				Iterator<RequestMatcher> iterator = this.candidates.iterator();
				while (iterator.hasNext()) {
					try {
						iterator.next().match(request);
						iterator.remove();
						return;
					}
					catch (AssertionError ex) {
						// Continue
					}
				}
				throw new AssertionError(
						"No matching request matcher was found for request to '" + request.getURI() + "'");
			}
		}

	}

}
