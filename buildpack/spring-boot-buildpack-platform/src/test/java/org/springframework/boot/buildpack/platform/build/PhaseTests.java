/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.buildpack.platform.build;

import org.junit.jupiter.api.Test;

import org.springframework.boot.buildpack.platform.docker.type.Binding;
import org.springframework.boot.buildpack.platform.docker.type.ContainerConfig.Update;
import org.springframework.boot.buildpack.platform.docker.type.VolumeName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link Phase}.
 *
 * @author Phillip Webb
 * @author Scott Frederick
 * @author Jeroen Meijer
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
		then(update).should().withCommand("/cnb/lifecycle/test", NO_ARGS);
		then(update).should().withLabel("author", "spring-boot");
		then(update).shouldHaveNoMoreInteractions();
	}

	@Test
	void applyWhenWithDaemonAccessUpdatesConfigurationWithRootUser() {
		Phase phase = new Phase("test", false);
		phase.withDaemonAccess();
		Update update = mock(Update.class);
		phase.apply(update);
		then(update).should().withUser("root");
		then(update).should().withCommand("/cnb/lifecycle/test", "-daemon");
		then(update).should().withLabel("author", "spring-boot");
		then(update).shouldHaveNoMoreInteractions();
	}

	@Test
	void applyWhenWithLogLevelArgAndVerboseLoggingUpdatesConfigurationWithLogLevel() {
		Phase phase = new Phase("test", true);
		Update update = mock(Update.class);
		phase.apply(update);
		then(update).should().withCommand("/cnb/lifecycle/test", "-log-level", "debug");
		then(update).should().withLabel("author", "spring-boot");
		then(update).shouldHaveNoMoreInteractions();
	}

	@Test
	void applyWhenWithLogLevelArgAndNonVerboseLoggingDoesNotUpdateLogLevel() {
		Phase phase = new Phase("test", false);
		Update update = mock(Update.class);
		phase.apply(update);
		then(update).should().withCommand("/cnb/lifecycle/test");
		then(update).should().withLabel("author", "spring-boot");
		then(update).shouldHaveNoMoreInteractions();
	}

	@Test
	void applyWhenWithArgsUpdatesConfigurationWithArguments() {
		Phase phase = new Phase("test", false);
		phase.withArgs("a", "b", "c");
		Update update = mock(Update.class);
		phase.apply(update);
		then(update).should().withCommand("/cnb/lifecycle/test", "a", "b", "c");
		then(update).should().withLabel("author", "spring-boot");
		then(update).shouldHaveNoMoreInteractions();
	}

	@Test
	void applyWhenWithImageBuildCacheUpdatesConfigurationWithCacheImage() {
		Phase phase = new Phase("test", false);
		phase.withBuildCache("my-cache-image");
		Update update = mock(Update.class);
		phase.apply(update);
		then(update).should().withCommand("/cnb/lifecycle/test", "-cache-image", "my-cache-image");
		then(update).should().withLabel("author", "spring-boot");
		then(update).shouldHaveNoMoreInteractions();
	}

	@Test
	void applyWhenWithBindsUpdatesConfigurationWithBinds() {
		Phase phase = new Phase("test", false);
		VolumeName volumeName = VolumeName.of("test");
		phase.withBinding(Binding.from(volumeName, "/test"));
		Update update = mock(Update.class);
		phase.apply(update);
		then(update).should().withCommand("/cnb/lifecycle/test");
		then(update).should().withLabel("author", "spring-boot");
		then(update).should().withBinding(Binding.from(volumeName, "/test"));
		then(update).shouldHaveNoMoreInteractions();
	}

	@Test
	void applyWhenWithEnvUpdatesConfigurationWithEnv() {
		Phase phase = new Phase("test", false);
		phase.withEnv("name1", "value1");
		phase.withEnv("name2", "value2");
		Update update = mock(Update.class);
		phase.apply(update);
		then(update).should().withCommand("/cnb/lifecycle/test");
		then(update).should().withLabel("author", "spring-boot");
		then(update).should().withEnv("name1", "value1");
		then(update).should().withEnv("name2", "value2");
		then(update).shouldHaveNoMoreInteractions();
	}

	@Test
	void applyWhenWithNetworkModeUpdatesConfigurationWithNetworkMode() {
		Phase phase = new Phase("test", false);
		phase.withNetworkMode("test");
		Update update = mock(Update.class);
		phase.apply(update);
		then(update).should().withCommand("/cnb/lifecycle/test");
		then(update).should().withNetworkMode("test");
		then(update).should().withLabel("author", "spring-boot");
		then(update).shouldHaveNoMoreInteractions();
	}

	@Test
	void applyWhenWithSecurityOptionsUpdatesConfigurationWithSecurityOptions() {
		Phase phase = new Phase("test", false);
		phase.withSecurityOption("option1=value1");
		phase.withSecurityOption("option2=value2");
		Update update = mock(Update.class);
		phase.apply(update);
		then(update).should().withCommand("/cnb/lifecycle/test");
		then(update).should().withLabel("author", "spring-boot");
		then(update).should().withSecurityOption("option1=value1");
		then(update).should().withSecurityOption("option2=value2");
		then(update).shouldHaveNoMoreInteractions();
	}

}
