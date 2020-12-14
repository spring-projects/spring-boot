/*
 * Copyright 2012-2020 the original author or authors.
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

import io.spring.concourse.releasescripts.sdkman.SdkmanService;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Tests for {@link PublishToSdkmanCommand}.
 *
 * @author Madhura Bhave
 */
class PublishToSdkmanCommandTests {

	@Mock
	private SdkmanService service;

	private PublishToSdkmanCommand command;

	@BeforeEach
	void setup() {
		MockitoAnnotations.initMocks(this);
		this.command = new PublishToSdkmanCommand(this.service);
	}

	@Test
	void runWhenReleaseTypeNotSpecifiedShouldThrowException() throws Exception {
		Assertions.assertThatIllegalStateException()
				.isThrownBy(() -> this.command.run(new DefaultApplicationArguments("publishToSdkman")));
	}

	@Test
	void runWhenVersionNotSpecifiedShouldThrowException() throws Exception {
		Assertions.assertThatIllegalStateException()
				.isThrownBy(() -> this.command.run(new DefaultApplicationArguments("publishToSdkman", "RELEASE")));
	}

	@Test
	void runWhenReleaseTypeMilestoneShouldDoNothing() throws Exception {
		this.command.run(new DefaultApplicationArguments("publishToSdkman", "M", "1.2.3"));
		verifyNoInteractions(this.service);
	}

	@Test
	void runWhenReleaseTypeRCShouldDoNothing() throws Exception {
		this.command.run(new DefaultApplicationArguments("publishToSdkman", "RC", "1.2.3"));
		verifyNoInteractions(this.service);
	}

	@Test
	void runWhenLatestGANotSpecifiedShouldCallServiceWithMakeDefaultFalse() throws Exception {
		DefaultApplicationArguments args = new DefaultApplicationArguments("promote", "RELEASE", "1.2.3");
		testRun(args, false);
	}

	@Test
	void runWhenReleaseTypeReleaseShouldCallService() throws Exception {
		DefaultApplicationArguments args = new DefaultApplicationArguments("promote", "RELEASE", "1.2.3", "true");
		testRun(args, true);
	}

	private void testRun(DefaultApplicationArguments args, boolean makeDefault) throws Exception {
		ArgumentCaptor<String> versionCaptor = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<Boolean> makeDefaultCaptor = ArgumentCaptor.forClass(Boolean.class);
		this.command.run(args);
		verify(this.service).publish(versionCaptor.capture(), makeDefaultCaptor.capture());
		String version = versionCaptor.getValue();
		Boolean makeDefaultValue = makeDefaultCaptor.getValue();
		assertThat(version).isEqualTo("1.2.3");
		assertThat(makeDefaultValue).isEqualTo(makeDefault);
	}

}
