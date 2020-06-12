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

package io.spring.concourse.releasescripts.sonatype;

import io.spring.concourse.releasescripts.ReleaseInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link SonatypeService}.
 *
 * @author Madhura Bhave
 */
@RestClientTest(SonatypeService.class)
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
	void artifactsPublishedWhenPublishedShouldReturnTrue() {
		this.server.expect(requestTo(String.format(
				"https://oss.sonatype.org/service/local/repositories/releases/content/org/springframework/boot/spring-boot/%s/spring-boot-%s.jar.sha1",
				"1.1.0.RELEASE", "1.1.0.RELEASE"))).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess().body("ce8d8b6838ecceb68962b975b18682f4237ccf71".getBytes()));
		boolean published = this.service.artifactsPublished(getReleaseInfo());
		assertThat(published).isTrue();
		this.server.verify();
	}

	@Test
	void artifactsPublishedWhenNotPublishedShouldReturnFalse() {
		this.server.expect(requestTo(String.format(
				"https://oss.sonatype.org/service/local/repositories/releases/content/org/springframework/boot/spring-boot/%s/spring-boot-%s.jar.sha1",
				"1.1.0.RELEASE", "1.1.0.RELEASE"))).andExpect(method(HttpMethod.GET))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));
		boolean published = this.service.artifactsPublished(getReleaseInfo());
		assertThat(published).isFalse();
		this.server.verify();
	}

	private ReleaseInfo getReleaseInfo() {
		ReleaseInfo releaseInfo = new ReleaseInfo();
		releaseInfo.setBuildName("example-build");
		releaseInfo.setBuildNumber("example-build-1");
		releaseInfo.setVersion("1.1.0.RELEASE");
		releaseInfo.setGroupId("example");
		return releaseInfo;
	}

}