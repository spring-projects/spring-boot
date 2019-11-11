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
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link DistributeCommand}.
 *
 * @author Madhura Bhave
 */
class DistributeCommandTests {

	@Mock
	private ArtifactoryService service;

	private DistributeCommand command;

	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		this.objectMapper = new ObjectMapper().disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		this.command = new DistributeCommand(this.service, objectMapper);
	}

	@Test
	void distributeWhenReleaseTypeNotSpecifiedShouldThrowException() {
		Assertions.assertThatIllegalStateException()
				.isThrownBy(() -> this.command.run(new DefaultApplicationArguments("distribute")));
	}

	@Test
	void distributeWhenReleaseTypeMilestoneShouldDoNothing() throws Exception {
		this.command.run(new DefaultApplicationArguments("distribute", "M", getBuildInfoLocation()));
		verifyNoInteractions(this.service);
	}

	@Test
	void distributeWhenReleaseTypeRCShouldDoNothing() throws Exception {
		this.command.run(new DefaultApplicationArguments("distribute", "RC", getBuildInfoLocation()));
		verifyNoInteractions(this.service);
	}

	@Test
	void distributeWhenReleaseTypeReleaseShouldCallService() throws Exception {
		ArgumentCaptor<ReleaseInfo> captor = ArgumentCaptor.forClass(ReleaseInfo.class);
		this.command.run(new DefaultApplicationArguments("distribute", "RELEASE", getBuildInfoLocation()));
		verify(this.service).distribute(eq(ReleaseType.RELEASE.getRepo()), captor.capture());
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