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

import java.util.Set;

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
		this.command = new DistributeCommand(this.service, this.objectMapper);
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
	@SuppressWarnings("unchecked")
	void distributeWhenReleaseTypeReleaseShouldCallService() throws Exception {
		ArgumentCaptor<ReleaseInfo> releaseInfoCaptor = ArgumentCaptor.forClass(ReleaseInfo.class);
		ArgumentCaptor<Set<String>> artifactDigestCaptor = ArgumentCaptor.forClass(Set.class);
		this.command.run(new DefaultApplicationArguments("distribute", "RELEASE", getBuildInfoLocation()));
		verify(this.service).distribute(eq(ReleaseType.RELEASE.getRepo()), releaseInfoCaptor.capture(),
				artifactDigestCaptor.capture());
		ReleaseInfo releaseInfo = releaseInfoCaptor.getValue();
		assertThat(releaseInfo.getBuildName()).isEqualTo("example");
		assertThat(releaseInfo.getBuildNumber()).isEqualTo("example-build-1");
		assertThat(releaseInfo.getGroupId()).isEqualTo("org.example.demo");
		assertThat(releaseInfo.getVersion()).isEqualTo("2.2.0");
		Set<String> artifactDigests = artifactDigestCaptor.getValue();
		assertThat(artifactDigests).containsExactly("aaaaaaaaa85f5c5093721f3ed0edda8ff8290yyyyyyyyyy");
	}

	private String getBuildInfoLocation() throws Exception {
		return new ClassPathResource("build-info-response.json", ArtifactoryService.class).getFile().getAbsolutePath();
	}

}