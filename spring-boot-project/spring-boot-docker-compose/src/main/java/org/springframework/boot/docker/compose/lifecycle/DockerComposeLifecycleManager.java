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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aot.AotDetector;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.docker.compose.core.DockerCompose;
import org.springframework.boot.docker.compose.core.DockerComposeFile;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.boot.docker.compose.lifecycle.DockerComposeProperties.Readiness.Wait;
import org.springframework.boot.docker.compose.lifecycle.DockerComposeProperties.Start;
import org.springframework.boot.docker.compose.lifecycle.DockerComposeProperties.Stop;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.core.log.LogMessage;
import org.springframework.util.Assert;

/**
 * Manages the lifecycle for docker compose services.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Scott Frederick
 * @see DockerComposeListener
 */
class DockerComposeLifecycleManager {

	private static final Log logger = LogFactory.getLog(DockerComposeLifecycleManager.class);

	private static final String IGNORE_LABEL = "org.springframework.boot.ignore";

	private final File workingDirectory;

	private final ApplicationContext applicationContext;

	private final ClassLoader classLoader;

	private final SpringApplicationShutdownHandlers shutdownHandlers;

	private final DockerComposeProperties properties;

	private final Set<ApplicationListener<?>> eventListeners;

	private final DockerComposeSkipCheck skipCheck;

	private final ServiceReadinessChecks serviceReadinessChecks;

	DockerComposeLifecycleManager(ApplicationContext applicationContext, Binder binder,
			SpringApplicationShutdownHandlers shutdownHandlers, DockerComposeProperties properties,
			Set<ApplicationListener<?>> eventListeners) {
		this(null, applicationContext, binder, shutdownHandlers, properties, eventListeners,
				new DockerComposeSkipCheck(), null);
	}

	DockerComposeLifecycleManager(File workingDirectory, ApplicationContext applicationContext, Binder binder,
			SpringApplicationShutdownHandlers shutdownHandlers, DockerComposeProperties properties,
			Set<ApplicationListener<?>> eventListeners, DockerComposeSkipCheck skipCheck,
			ServiceReadinessChecks serviceReadinessChecks) {
		this.workingDirectory = workingDirectory;
		this.applicationContext = applicationContext;
		this.classLoader = applicationContext.getClassLoader();
		this.shutdownHandlers = shutdownHandlers;
		this.properties = properties;
		this.eventListeners = eventListeners;
		this.skipCheck = skipCheck;
		this.serviceReadinessChecks = (serviceReadinessChecks != null) ? serviceReadinessChecks
				: new ServiceReadinessChecks(properties.getReadiness());
	}

	void start() {
		if (Boolean.getBoolean("spring.aot.processing") || AotDetector.useGeneratedArtifacts()) {
			logger.trace("Docker Compose support disabled with AOT and native images");
			return;
		}
		if (!this.properties.isEnabled()) {
			logger.trace("Docker Compose support not enabled");
			return;
		}
		if (this.skipCheck.shouldSkip(this.classLoader, this.properties.getSkip())) {
			logger.trace("Docker Compose support skipped");
			return;
		}
		DockerComposeFile composeFile = getComposeFile();
		Set<String> activeProfiles = this.properties.getProfiles().getActive();
		DockerCompose dockerCompose = getDockerCompose(composeFile, activeProfiles);
		if (!dockerCompose.hasDefinedServices()) {
			logger.warn(LogMessage.format("No services defined in Docker Compose file '%s' with active profiles %s",
					composeFile, activeProfiles));
			return;
		}
		LifecycleManagement lifecycleManagement = this.properties.getLifecycleManagement();
		Start start = this.properties.getStart();
		Stop stop = this.properties.getStop();
		Wait wait = this.properties.getReadiness().getWait();
		List<RunningService> runningServices = dockerCompose.getRunningServices();
		if (lifecycleManagement.shouldStart() && runningServices.isEmpty()) {
			start.getCommand().applyTo(dockerCompose, start.getLogLevel());
			runningServices = dockerCompose.getRunningServices();
			wait = (wait != Wait.ONLY_IF_STARTED) ? wait : Wait.ALWAYS;
			if (lifecycleManagement.shouldStop()) {
				this.shutdownHandlers.add(() -> stop.getCommand().applyTo(dockerCompose, stop.getTimeout()));
			}
		}
		List<RunningService> relevantServices = new ArrayList<>(runningServices);
		relevantServices.removeIf(this::isIgnored);
		if (wait == Wait.ALWAYS || wait == null) {
			this.serviceReadinessChecks.waitUntilReady(relevantServices);
		}
		publishEvent(new DockerComposeServicesReadyEvent(this.applicationContext, relevantServices));
	}

	protected DockerComposeFile getComposeFile() {
		DockerComposeFile composeFile = (this.properties.getFile() != null)
				? DockerComposeFile.of(this.properties.getFile()) : DockerComposeFile.find(this.workingDirectory);
		Assert.state(composeFile != null, () -> "No Docker Compose file found in directory '%s'".formatted(
				((this.workingDirectory != null) ? this.workingDirectory : new File(".")).toPath().toAbsolutePath()));
		logger.info(LogMessage.format("Using Docker Compose file '%s'", composeFile));
		return composeFile;
	}

	protected DockerCompose getDockerCompose(DockerComposeFile composeFile, Set<String> activeProfiles) {
		return DockerCompose.get(composeFile, this.properties.getHost(), activeProfiles);
	}

	private boolean isIgnored(RunningService service) {
		return service.labels().containsKey(IGNORE_LABEL);
	}

	/**
	 * Publish a {@link DockerComposeServicesReadyEvent} directly to the event listeners
	 * since we cannot call {@link ApplicationContext#publishEvent} this early.
	 * @param event the event to publish
	 */
	private void publishEvent(DockerComposeServicesReadyEvent event) {
		SimpleApplicationEventMulticaster multicaster = new SimpleApplicationEventMulticaster();
		this.eventListeners.forEach(multicaster::addApplicationListener);
		multicaster.multicastEvent(event);
	}

}
