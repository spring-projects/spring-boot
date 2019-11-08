/*
 * Copyright 2012-2019 the original author or authors.
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

package io.spring.concourse.releasescripts.bintray;

import io.spring.concourse.releasescripts.ReleaseInfo;
import io.spring.concourse.releasescripts.sonatype.SonatypeProperties;
import io.spring.concourse.releasescripts.sonatype.SonatypeService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.response.DefaultResponseCreator;
import org.springframework.util.Base64Utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link BintrayService}.
 *
 * @author Madhura Bhave
 */
@RestClientTest(BintrayService.class)
@EnableConfigurationProperties({ BintrayProperties.class, SonatypeProperties.class })
class BintrayServiceTests {

	@Autowired
	private BintrayService service;

	@Autowired
	private BintrayProperties properties;

	@Autowired
	private SonatypeProperties sonatypeProperties;

	@MockBean
	private SonatypeService sonatypeService;

	@Autowired
	private MockRestServiceServer server;

	@AfterEach
	void tearDown() {
		this.server.reset();
	}

	@Test
	void isDistributionComplete() throws Exception {
		setupGetPackageFiles(1, "all-package-files.json");
		setupGetPackageFiles(0, "published-files.json");
		setupGetPackageFiles(0, "all-package-files.json");
		assertThat(this.service.isDistributionComplete(getReleaseInfo())).isTrue();
		this.server.verify();
	}

	private void setupGetPackageFiles(int includeUnpublished, String path) {
		this.server
				.expect(requestTo(String.format(
						"https://api.bintray.com/packages/%s/%s/%s/versions/%s/files?include_unpublished=%s",
						this.properties.getSubject(), this.properties.getRepo(), "example", "1.1.0.RELEASE",
						includeUnpublished)))
				.andExpect(method(HttpMethod.GET))
				.andExpect(header("Authorization", "Basic " + Base64Utils.encodeToString(
						String.format("%s:%s", this.properties.getUsername(), this.properties.getApiKey()).getBytes())))
				.andRespond(withJsonFrom(path));
	}

	@Test
	void publishGradlePluginWhenSuccessful() {
		this.server
				.expect(requestTo(String.format("https://api.bintray.com/packages/%s/%s/%s/versions/%s/attributes",
						this.properties.getSubject(), this.properties.getRepo(), "example", "1.1.0.RELEASE")))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json(
						"[ { \"name\": \"gradle-plugin\", \"values\": [\"org.springframework.boot:org.springframework.boot:spring-boot-gradle-plugin\"] } ]"))
				.andExpect(header("Authorization", "Basic " + Base64Utils.encodeToString(
						String.format("%s:%s", this.properties.getUsername(), this.properties.getApiKey()).getBytes())))
				.andExpect(header("Content-Type", MediaType.APPLICATION_JSON.toString())).andRespond(withSuccess());
		this.service.publishGradlePlugin(getReleaseInfo());
		this.server.verify();
	}

	@Test
	void syncToMavenCentralWhenSuccessful() {
		ReleaseInfo releaseInfo = getReleaseInfo();
		given(this.sonatypeService.artifactsPublished(releaseInfo)).willReturn(false);
		this.server
				.expect(requestTo(String.format("https://api.bintray.com/maven_central_sync/%s/%s/%s/versions/%s",
						this.properties.getSubject(), this.properties.getRepo(), "example", "1.1.0.RELEASE")))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json(String.format("{\"username\": \"%s\", \"password\": \"%s\"}",
						this.sonatypeProperties.getUserToken(), this.sonatypeProperties.getPasswordToken())))
				.andExpect(header("Authorization", "Basic " + Base64Utils.encodeToString(
						String.format("%s:%s", this.properties.getUsername(), this.properties.getApiKey()).getBytes())))
				.andExpect(header("Content-Type", MediaType.APPLICATION_JSON.toString())).andRespond(withSuccess());
		this.service.syncToMavenCentral(releaseInfo);
		this.server.verify();
	}

	@Test
	void syncToMavenCentralWhenArtifactsAlreadyPublished() {
		ReleaseInfo releaseInfo = getReleaseInfo();
		given(this.sonatypeService.artifactsPublished(releaseInfo)).willReturn(true);
		this.server.expect(ExpectedCount.never(),
				requestTo(String.format("https://api.bintray.com/maven_central_sync/%s/%s/%s/versions/%s",
						this.properties.getSubject(), this.properties.getRepo(), "example", "1.1.0.RELEASE")));
		this.service.syncToMavenCentral(releaseInfo);
		this.server.verify();
	}

	private ReleaseInfo getReleaseInfo() {
		ReleaseInfo releaseInfo = new ReleaseInfo();
		releaseInfo.setBuildName("example-build");
		releaseInfo.setBuildNumber("example-build-1");
		releaseInfo.setGroupId("example");
		releaseInfo.setVersion("1.1.0.RELEASE");
		return releaseInfo;
	}

	private DefaultResponseCreator withJsonFrom(String path) {
		return withSuccess(getClassPathResource(path), MediaType.APPLICATION_JSON);
	}

	private ClassPathResource getClassPathResource(String path) {
		return new ClassPathResource(path, getClass());
	}

}