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
import java.nio.file.Paths;
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

import org.springframework.aot.AotDetector;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.docker.compose.core.DockerCompose;
import org.springframework.boot.docker.compose.core.DockerComposeFile;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.lifecycle.DockerComposeProperties.Readiness.Wait;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
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
 * @author Scott Frederick
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
	void startWhenEnabledFalseDoesNotStart() {
		this.properties.setEnabled(false);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices();
		this.lifecycleManager.start();
		assertThat(listener.getEvent()).isNull();
		then(this.dockerCompose).should(never()).hasDefinedServices();
	}

	@Test
	void startWhenAotProcessingDoesNotStart() {
		withSystemProperty("spring.aot.processing", "true", () -> {
			EventCapturingListener listener = new EventCapturingListener();
			this.eventListeners.add(listener);
			setUpRunningServices();
			this.lifecycleManager.start();
			assertThat(listener.getEvent()).isNull();
			then(this.dockerCompose).should(never()).hasDefinedServices();
		});
	}

	@Test
	void startWhenUsingAotArtifactsDoesNotStart() {
		withSystemProperty(AotDetector.AOT_ENABLED, "true", () -> {
			EventCapturingListener listener = new EventCapturingListener();
			this.eventListeners.add(listener);
			setUpRunningServices();
			this.lifecycleManager.start();
			assertThat(listener.getEvent()).isNull();
			then(this.dockerCompose).should(never()).hasDefinedServices();
		});
	}

	@Test
	void startWhenComposeFileNotFoundThrowsException() {
		DockerComposeLifecycleManager manager = new DockerComposeLifecycleManager(new File("."),
				this.applicationContext, null, this.shutdownHandlers, this.properties, this.eventListeners,
				this.skipCheck, this.serviceReadinessChecks);
		assertThatIllegalStateException().isThrownBy(manager::start)
			.withMessageContaining(Paths.get(".").toAbsolutePath().toString());
	}

	@Test
	void startWhenComposeFileNotFoundAndWorkingDirectoryNullThrowsException() {
		DockerComposeLifecycleManager manager = new DockerComposeLifecycleManager(null, this.applicationContext, null,
				this.shutdownHandlers, this.properties, this.eventListeners, this.skipCheck,
				this.serviceReadinessChecks);
		assertThatIllegalStateException().isThrownBy(manager::start)
			.withMessageContaining(Paths.get(".").toAbsolutePath().toString());
	}

	@Test
	void startWhenInTestDoesNotStart() {
		given(this.skipCheck.shouldSkip(any(), any())).willReturn(true);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices();
		this.lifecycleManager.start();
		assertThat(listener.getEvent()).isNull();
		then(this.dockerCompose).should(never()).hasDefinedServices();
	}

	@Test
	void startWhenHasNoDefinedServicesDoesNothing() {
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		this.lifecycleManager.start();
		assertThat(listener.getEvent()).isNull();
		then(this.dockerCompose).should().hasDefinedServices();
		then(this.dockerCompose).should(never()).up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).down(any());
		then(this.dockerCompose).should(never()).stop(any());
	}

	@Test
	void startWhenLifecycleStartAndStopAndHasNoRunningServicesDoesUpAndStop() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should().up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should().stop(any());
		then(this.dockerCompose).should(never()).down(any());
	}

	@Test
	void startWhenLifecycleStartAndStopAndHasRunningServicesDoesNothing() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices();
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should(never()).up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).down(any());
		then(this.dockerCompose).should(never()).stop(any());
	}

	@Test
	void startWhenLifecycleNoneDoesNothing() {
		this.properties.setLifecycleManagement(LifecycleManagement.NONE);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices();
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should(never()).up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).down(any());
		then(this.dockerCompose).should(never()).stop(any());
	}

	@Test
	void startWhenLifecycleStartOnlyDoesOnlyStart() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_ONLY);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should().up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).down(any());
		then(this.dockerCompose).should(never()).stop(any());
		this.shutdownHandlers.assertNoneAdded();
	}

	@Test
	void startWhenStartCommandStartDoesStartAndStop() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		this.properties.getStart().setCommand(StartCommand.START);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should(never()).up(any());
		then(this.dockerCompose).should().start(any());
		then(this.dockerCompose).should().stop(any());
		then(this.dockerCompose).should(never()).down(any());
	}

	@Test
	void startWhenStopCommandDownDoesStartAndDown() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		this.properties.getStop().setCommand(StopCommand.DOWN);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should().up(any());
		then(this.dockerCompose).should(never()).start(any());
		then(this.dockerCompose).should(never()).stop(any());
		then(this.dockerCompose).should().down(any());
	}

	@Test
	void startWhenHasStopTimeoutUsesDuration() {
		this.properties.setLifecycleManagement(LifecycleManagement.START_AND_STOP);
		Duration timeout = Duration.ofDays(1);
		this.properties.getStop().setTimeout(timeout);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		then(this.dockerCompose).should().stop(timeout);
	}

	@Test
	void startWhenHasIgnoreLabelIgnoresService() {
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices(true, Map.of("org.springframework.boot.ignore", "true"));
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		assertThat(listener.getEvent()).isNotNull();
		assertThat(listener.getEvent().getRunningServices()).isEmpty();
	}

	@Test
	void startWaitsUntilReady() {
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices();
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		then(this.serviceReadinessChecks).should().waitUntilReady(this.runningServices);
	}

	@Test
	void startWhenWaitNeverDoesNotWaitUntilReady() {
		this.properties.getReadiness().setWait(Wait.NEVER);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices();
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		then(this.serviceReadinessChecks).should(never()).waitUntilReady(this.runningServices);
	}

	@Test
	void startWhenWaitOnlyIfStartedAndNotStartedDoesNotWaitUntilReady() {
		this.properties.getReadiness().setWait(Wait.ONLY_IF_STARTED);
		this.properties.setLifecycleManagement(LifecycleManagement.NONE);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices();
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		then(this.serviceReadinessChecks).should(never()).waitUntilReady(this.runningServices);
	}

	@Test
	void startWhenWaitOnlyIfStartedAndStartedWaitsUntilReady() {
		this.properties.getReadiness().setWait(Wait.ONLY_IF_STARTED);
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices(false);
		this.lifecycleManager.start();
		this.shutdownHandlers.run();
		then(this.serviceReadinessChecks).should().waitUntilReady(this.runningServices);
	}

	@Test
	void startGetsDockerComposeWithActiveProfiles() {
		this.properties.getProfiles().setActive(Set.of("my-profile"));
		setUpRunningServices();
		this.lifecycleManager.start();
		assertThat(this.activeProfiles).containsExactly("my-profile");
	}

	@Test
	void startPublishesEvent() {
		EventCapturingListener listener = new EventCapturingListener();
		this.eventListeners.add(listener);
		setUpRunningServices();
		this.lifecycleManager.start();
		DockerComposeServicesReadyEvent event = listener.getEvent();
		assertThat(event).isNotNull();
		assertThat(event.getSource()).isEqualTo(this.applicationContext);
		assertThat(event.getRunningServices()).isEqualTo(this.runningServices);
	}

	private void setUpRunningServices() {
		setUpRunningServices(true);
	}

	private void setUpRunningServices(boolean started) {
		setUpRunningServices(started, Collections.emptyMap());
	}

	@SuppressWarnings("unchecked")
	private void setUpRunningServices(boolean started, Map<String, String> labels) {
		given(this.dockerCompose.hasDefinedServices()).willReturn(true);
		RunningService runningService = mock(RunningService.class);
		given(runningService.labels()).willReturn(labels);
		this.runningServices = List.of(runningService);
		if (started) {
			given(this.dockerCompose.getRunningServices()).willReturn(this.runningServices);
		}
		else {
			given(this.dockerCompose.getRunningServices()).willReturn(Collections.emptyList(), this.runningServices);
		}
	}

	private void withSystemProperty(String key, String value, Runnable action) {
		String previous = System.getProperty(key);
		try {
			System.setProperty(key, value);
			action.run();
		}
		finally {
			if (previous == null) {
				System.clearProperty(key);
			}
			else {
				System.setProperty(key, previous);
			}
		}
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
