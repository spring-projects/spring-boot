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

package io.spring.concourse.releasescripts.command;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.spring.concourse.releasescripts.ReleaseInfo;
import io.spring.concourse.releasescripts.ReleaseType;
import io.spring.concourse.releasescripts.artifactory.ArtifactoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

/**
 * @author Madhura Bhave
 */
class PromoteCommandTests {

	@Mock
	private ArtifactoryService service;

	private PromoteCommand command;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		this.objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		this.command = new PromoteCommand(this.service, this.objectMapper);
	}

	@Test
	void runWhenReleaseTypeNotSpecifiedShouldThrowException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.command.run(new DefaultApplicationArguments("promote")));
	}

	@Test
	void runWhenReleaseTypeMilestoneShouldCallService() throws Exception {
		this.command.run(new DefaultApplicationArguments("promote", "M", getBuildInfoLocation()));
		verify(this.service).promote(eq(ReleaseType.MILESTONE.getRepo()), any(ReleaseInfo.class));
	}

	@Test
	void runWhenReleaseTypeRCShouldCallService() throws Exception {
		this.command.run(new DefaultApplicationArguments("promote", "RC", getBuildInfoLocation()));
		verify(this.service).promote(eq(ReleaseType.RELEASE_CANDIDATE.getRepo()), any(ReleaseInfo.class));
	}

	@Test
	void runWhenReleaseTypeReleaseShouldCallService() throws Exception {
		this.command.run(new DefaultApplicationArguments("promote", "RELEASE", getBuildInfoLocation()));
		verify(this.service).promote(eq(ReleaseType.RELEASE.getRepo()), any(ReleaseInfo.class));
	}

	@Test
	void runWhenBuildInfoNotSpecifiedShouldThrowException() {
		assertThatIllegalStateException()
				.isThrownBy(() -> this.command.run(new DefaultApplicationArguments("promote", "M")));
	}

	@Test
	void runShouldParseBuildInfoProperly() throws Exception {
		ArgumentCaptor<ReleaseInfo> captor = ArgumentCaptor.forClass(ReleaseInfo.class);
		this.command.run(new DefaultApplicationArguments("promote", "RELEASE", getBuildInfoLocation()));
		verify(this.service).promote(eq(ReleaseType.RELEASE.getRepo()), captor.capture());
		ReleaseInfo releaseInfo = captor.getValue();
		assertThat(releaseInfo.getBuildName()).isEqualTo("example");
		assertThat(releaseInfo.getBuildNumber()).isEqualTo("example-build-1");
		assertThat(releaseInfo.getGroupId()).isEqualTo("org.example.demo");
		assertThat(releaseInfo.getVersion()).isEqualTo("2.2.0");
	}

	private String getBuildInfoLocation() throws Exception {
		return new ClassPathResource("build-info-response.json", ArtifactoryService.class).getFile().getAbsolutePath();
	}

}