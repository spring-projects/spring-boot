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

package org.springframework.boot.docker.compose.lifecycle;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.docker.compose.core.DockerCompose;
import org.springframework.boot.docker.compose.core.DockerComposeFile;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.readiness.ServiceReadinessChecks;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link DockerComposeLifecycleManager}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeLifecycleManagerTests {

	@TempDir
	File temp;

	private DockerComposeFile dockerComposeFile;

	private DockerCompose dockerCompose;

	private Set<String> activeProfiles;

	private GenericApplicationContext applicationContext;

	private TestSpringApplicationShutdownHandlers shutdownHandlers;

	private ServiceReadinessChecks serviceReadinessChecks;

	private List<RunningService> runningServices;

	private DockerComposeProperties properties;

	private LinkedHashSet<ApplicationListener<?>> eventListeners;

	private DockerComposeLifecycleManager lifecycleManager;

	private DockerComposeSkipCheck skipCheck;

	@BeforeEach
	void setup() throws IOException {
		File file = new File(this.temp, "compose.yml");
		FileCopyUtils.copy(new byte[0], file);
		this.dockerComposeFile = DockerComposeFile.of(file);
		this.dockerCompose = mock(DockerCompose.class);
		File workingDirectory = new File(".");
		this.applicationContext = new GenericApplicationContext();
		this.applicationContext.refresh();
		Binder binder = Binder.get(this.applicationContext.getEnvironment());
		this.shutdownHandlers = new TestSpringApplicationShutdownHandlers();
		this.properties = DockerComposeProperties.get(binder);
		this.eventListeners = new LinkedHashSet<>();
		this.skipCheck = mock(DockerComposeSkipCheck.class);
		this.serviceReadinessChecks = mock(ServiceReadinessChecks.class);
		this.lifecycleManager = new TestDockerComposeLifecycleManager(workingDirectory, this.applicationContext, binder,
				this.shutdownHandlers, this.properties, this.eventListeners, this.skipCheck,
				this.serviceReadinessChecks);
	}

	@Test
	void startupWhenEnabledFalseDoesNotStart() {
		this.properties.setEnabled(false);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setupRunningServices();
		this.lifecycleManager.startup();
		assertThat(listener.getEvent()).isNull();
		then(this.dockerCompose).should(never()).hasDefinedServices();
	}

	@Test
	void startupWhenInTestDoesNotStart() {
		given(this.skipCheck.shouldSkip(any(), any(), any())).willReturn(true);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setupRunningServices();
		this.lifecycleManager.startup();
		assertThat(listener.getEvent()).isNull();
		then(this.dockerCompose).should(never()).hasDefinedServices();
	}

	@Test
	void startupWhenHasNoDefinedServicesDoesNothing() {
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		this.lifecycleManager.startup();
		assertThat(listener.getEvent()).isNull();
		then(this.dockerCompose).should().hasDefinedServices();
		then(this.dockerCompose).should(never()).up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).down(any());
		then(this.dockerCompose).should(never()).stop(any());
	}

	@Test
	void startupWhenLifecycleStartAndStopAndHasNoRunningServicesDoesStartupAndShutdown() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.startup();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should().up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should().down(any());
		then(this.dockerCompose).should(never()).stop(any());
	}

	@Test
	void startupWhenLifecycleStartAndStopAndHasRunningServicesDoesNoStartupOrShutdown() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setupRunningServices();
		this.lifecycleManager.startup();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should(never()).up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).down(any());
		then(this.dockerCompose).should(never()).stop(any());
	}

	@Test
	void startupWhenLifecycleNoneDoesNoStartupOrShutdown() {
		this.properties.setLifecycleManagement(LifecycleManagement.NONE);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setupRunningServices();
		this.lifecycleManager.startup();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should(never()).up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).down(any());
		then(this.dockerCompose).should(never()).stop(any());
	}

	@Test
	void startupWhenLifecycleStartOnlyDoesStartupAndNoShutdown() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_ONLY);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.startup();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should().up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).down(any());
		then(this.dockerCompose).should(never()).stop(any());
		this.shutdownHandlers.assertNoneAdded();
	}

	@Test
	void startupWhenStartupCommandStartDoesStartupUsingStartAndShutdown() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		this.properties.getStartup().setCommand(StartupCommand.START);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.startup();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should(never()).up(any());
		then(this.dockerCompose).should().start(any());
		then(this.dockerCompose).should().down(any());
		then(this.dockerCompose).should(never()).stop(any());
	}

	@Test
	void startupWhenShutdownCommandStopDoesStartupAndShutdownUsingStop() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		this.properties.getShutdown().setCommand(ShutdownCommand.STOP);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.startup();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should().up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).down(any());
		then(this.dockerCompose).should().stop(any());
	}

	@Test
	void startupWhenHasShutdownTimeoutUsesDuration() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		Duration timeout = Duration.ofDays(1);
		this.properties.getShutdown().setTimeout(timeout);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.startup();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should().down(timeout);
	}

	@Test
	void startupWhenHasIgnoreLabelIgnoresService() {
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setupRunningServices(Map.of("org.springframework.boot.ignore", "true"));
		this.lifecycleManager.startup();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		assertThat(listener.getEvent().getRunningServices()).isEmpty();
	}

	@Test
	void startupWaitsUntilReady() {
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setupRunningServices();
		this.lifecycleManager.startup();
		this.shutdownHandlers.run();
		then(this.serviceReadinessChecks).should().waitUntilReady(this.runningServices);
	}

	@Test
	void startupGetsDockerComposeWithActiveProfiles() {
		this.properties.getProfiles().setActive(Set.of("my-profile"));
		setupRunningServices();
		this.lifecycleManager.startup();
		assertThat(this.activeProfiles).containsExactly("my-profile");
	}

	@Test
	void startupPublishesEvent() {
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setupRunningServices();
		this.lifecycleManager.startup();
		DockerComposeServicesReadyEvent event = listener.getEvent();
		assertThat(event).isNotNull();
		assertThat(event.getSource()).isEqualTo(this.applicationContext);
		assertThat(event.getRunningServices()).isEqualTo(this.runningServices);
	}

	private void setupRunningServices() {
		setupRunningServices(Collections.emptyMap());
	}

	private void setupRunningServices(Map<String, String> labels) {
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		given(this.dockerCompose.hasRunningServices()).willReturn(true);
		RunningService runningService = mock(RunningService.class);
		given(runningService.labels()).willReturn(labels);
		this.runningServices = List.of(runningService);
		given(this.dockerCompose.getRunningServices()).willReturn(this.runningServices);
	}

	/**
	 * Testable {@link SpringApplicationShutdownHandlers}.
	 */
	static class TestSpringApplicationShutdownHandlers implements SpringApplicationShutdownHandlers {

		private final List<Runnable> actions = new ArrayList<>();

		@Override
		public void add(Runnable action) {
			this.actions.add(action);
		}

		@Override
		public void remove(Runnable action) {
			this.actions.remove(action);
		}

		void run() {
			this.actions.forEach(Runnable::run);
		}

		void assertNoneAdded() {
			assertThat(this.actions).isEmpty();
		}

	}

	/**
	 * {@link ApplicationListener} to capture the {@link DockerComposeServicesReadyEvent}.
	 */
	static class EventCapturingListener implements ApplicationListener<DockerComposeServicesReadyEvent> {

		private DockerComposeServicesReadyEvent event;

		@Override
		public void onApplicationEvent(DockerComposeServicesReadyEvent event) {
			this.event = event;
		}

		DockerComposeServicesReadyEvent getEvent() {
			return this.event;
		}

	}

	/**
	 * Testable {@link DockerComposeLifecycleManager}.
	 */
	class TestDockerComposeLifecycleManager extends DockerComposeLifecycleManager {

		TestDockerComposeLifecycleManager(File workingDirectory, ApplicationContext applicationContext, Binder binder,
				SpringApplicationShutdownHandlers shutdownHandlers, DockerComposeProperties properties,
				Set<ApplicationListener<?>> eventListeners, DockerComposeSkipCheck skipCheck,
				ServiceReadinessChecks serviceReadinessChecks) {
			super(workingDirectory, applicationContext, binder, shutdownHandlers, properties, eventListeners, skipCheck,
					serviceReadinessChecks);
		}

		@Override
		protected DockerComposeFile getComposeFile() {
			return DockerComposeLifecycleManagerTests.this.dockerComposeFile;
		}

		@Override
		protected DockerCompose getDockerCompose(DockerComposeFile composeFile, Set<String> activeProfiles) {
			DockerComposeLifecycleManagerTests.this.activeProfiles = activeProfiles;
			return DockerComposeLifecycleManagerTests.this.dockerCompose;
		}

	}

}
