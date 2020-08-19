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

package org.springframework.boot.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.ContainerConfig.Update;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests for {@link Phase}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 */
class PhaseTests {

	private static final String[] NO_ARGS = {};

	@Test
	void getNameReturnsName() {
		Phase phase = new Phase("test", false);
		assertThat(phase.getName()).isEqualTo("test");
	}

	@Test
	void toStringReturnsName() {
		Phase phase = new Phase("test", false);
		assertThat(phase).hasToString("test");
	}

	@Test
	void applyUpdatesConfiguration() {
		Phase phase = new Phase("test", false);
		Update update = mock(Update.class);
		phase.apply(update);
		verify(update).withCommand("/cnb/lifecycle/test", NO_ARGS);
		verify(update).withLabel("author", "spring-boot");
		verifyNoMoreInteractions(update);
	}

	@Test
	void applyWhenWithDaemonAccessUpdatesConfigurationWithRootUserAndDomainSocketBinding() {
		Phase phase = new Phase("test", false);
		phase.withDaemonAccess();
		Update update = mock(Update.class);
		phase.apply(update);
		verify(update).withUser("root");
		verify(update).withBind("/var/run/docker.sock", "/var/run/docker.sock");
		verify(update).withCommand("/cnb/lifecycle/test", NO_ARGS);
		verify(update).withLabel("author", "spring-boot");
		verifyNoMoreInteractions(update);
	}

	@Test
	void applyWhenWithLogLevelArgAndVerboseLoggingUpdatesConfigurationWithLogLevel() {
		Phase phase = new Phase("test", true);
		phase.withLogLevelArg();
		Update update = mock(Update.class);
		phase.apply(update);
		verify(update).withCommand("/cnb/lifecycle/test", "-log-level", "debug");
		verify(update).withLabel("author", "spring-boot");
		verifyNoMoreInteractions(update);
	}

	@Test
	void applyWhenWithLogLevelArgAndNonVerboseLoggingDoesNotUpdateLogLevel() {
		Phase phase = new Phase("test", false);
		phase.withLogLevelArg();
		Update update = mock(Update.class);
		phase.apply(update);
		verify(update).withCommand("/cnb/lifecycle/test");
		verify(update).withLabel("author", "spring-boot");
		verifyNoMoreInteractions(update);
	}

	@Test
	void applyWhenWithArgsUpdatesConfigurationWithArguments() {
		Phase phase = new Phase("test", false);
		phase.withArgs("a", "b", "c");
		Update update = mock(Update.class);
		phase.apply(update);
		verify(update).withCommand("/cnb/lifecycle/test", "a", "b", "c");
		verify(update).withLabel("author", "spring-boot");
		verifyNoMoreInteractions(update);
	}

	@Test
	void applyWhenWithBindsUpdatesConfigurationWithBinds() {
		Phase phase = new Phase("test", false);
		VolumeName volumeName = VolumeName.of("test");
		phase.withBinds(volumeName, "/test");
		Update update = mock(Update.class);
		phase.apply(update);
		verify(update).withCommand("/cnb/lifecycle/test");
		verify(update).withLabel("author", "spring-boot");
		verify(update).withBind(volumeName, "/test");
		verifyNoMoreInteractions(update);
	}

}
