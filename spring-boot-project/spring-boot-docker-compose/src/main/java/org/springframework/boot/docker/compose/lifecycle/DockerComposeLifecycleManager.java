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

	/**
     * Constructs a new DockerComposeLifecycleManager with the specified parameters.
     *
     * @param applicationContext the application context
     * @param binder the binder
     * @param shutdownHandlers the shutdown handlers
     * @param properties the Docker Compose properties
     * @param eventListeners the set of event listeners
     */
    DockerComposeLifecycleManager(ApplicationContext applicationContext, Binder binder,
			SpringApplicationShutdownHandlers shutdownHandlers, DockerComposeProperties properties,
			Set<ApplicationListener<?>> eventListeners) {
		this(null, applicationContext, binder, shutdownHandlers, properties, eventListeners,
				new DockerComposeSkipCheck(), null);
	}

	/**
     * Constructs a new DockerComposeLifecycleManager with the specified parameters.
     * 
     * @param workingDirectory the working directory for Docker Compose
     * @param applicationContext the application context for the Spring application
     * @param binder the binder for binding properties
     * @param shutdownHandlers the shutdown handlers for the Spring application
     * @param properties the Docker Compose properties
     * @param eventListeners the event listeners for the Spring application
     * @param skipCheck the skip check for skipping Docker Compose checks
     * @param serviceReadinessChecks the service readiness checks for Docker Compose services
     */
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

	/**
     * Starts the Docker Compose services based on the configuration properties.
     * If AOT processing or native images are enabled, Docker Compose support is disabled.
     * If Docker Compose support is not enabled, the method returns.
     * If Docker Compose support is skipped based on the configured skip conditions, the method returns.
     * Retrieves the Docker Compose file and active profiles.
     * Initializes the DockerCompose instance with the compose file and active profiles.
     * If no services are defined in the Docker Compose file with the active profiles, a warning is logged and the method returns.
     * Retrieves the lifecycle management, start, stop, and readiness configurations from the properties.
     * Retrieves the list of running services from the DockerCompose instance.
     * If lifecycle management requires starting the services and no services are currently running, the start command is applied to the DockerCompose instance.
     * If the readiness configuration is set to ONLY_IF_STARTED, it is changed to ALWAYS.
     * If lifecycle management requires stopping the services, a shutdown handler is added to apply the stop command with the configured timeout.
     * If there are already Docker Compose services running, the method logs a message and skips the startup process.
     * Filters out any ignored services from the list of running services.
     * If the readiness configuration is set to ALWAYS or null, waits until the relevant services are ready.
     * Publishes a DockerComposeServicesReadyEvent with the relevant services.
     */
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
		if (lifecycleManagement.shouldStart()) {
			if (runningServices.isEmpty()) {
				start.getCommand().applyTo(dockerCompose, start.getLogLevel());
				runningServices = dockerCompose.getRunningServices();
				if (wait == Wait.ONLY_IF_STARTED) {
					wait = Wait.ALWAYS;
				}
				if (lifecycleManagement.shouldStop()) {
					this.shutdownHandlers.add(() -> stop.getCommand().applyTo(dockerCompose, stop.getTimeout()));
				}
			}
			else {
				logger.info("There are already Docker Compose services running, skipping startup");
			}
		}
		List<RunningService> relevantServices = new ArrayList<>(runningServices);
		relevantServices.removeIf(this::isIgnored);
		if (wait == Wait.ALWAYS || wait == null) {
			this.serviceReadinessChecks.waitUntilReady(relevantServices);
		}
		publishEvent(new DockerComposeServicesReadyEvent(this.applicationContext, relevantServices));
	}

	/**
     * Retrieves the Docker Compose file to be used for the lifecycle management.
     * If a file path is specified in the properties, it will be used. Otherwise, the method will search for a Docker Compose file in the working directory.
     * If no Docker Compose file is found, an exception will be thrown.
     * 
     * @return The DockerComposeFile object representing the Docker Compose file.
     * @throws IllegalStateException if no Docker Compose file is found.
     */
    protected DockerComposeFile getComposeFile() {
		DockerComposeFile composeFile = (this.properties.getFile() != null)
				? DockerComposeFile.of(this.properties.getFile()) : DockerComposeFile.find(this.workingDirectory);
		Assert.state(composeFile != null, () -> "No Docker Compose file found in directory '%s'".formatted(
				((this.workingDirectory != null) ? this.workingDirectory : new File(".")).toPath().toAbsolutePath()));
		logger.info(LogMessage.format("Using Docker Compose file '%s'", composeFile));
		return composeFile;
	}

	/**
     * Returns a DockerCompose object based on the provided DockerComposeFile and active profiles.
     * 
     * @param composeFile the DockerComposeFile to use for creating the DockerCompose object
     * @param activeProfiles the set of active profiles to use for creating the DockerCompose object
     * @return the DockerCompose object created using the provided DockerComposeFile and active profiles
     */
    protected DockerCompose getDockerCompose(DockerComposeFile composeFile, Set<String> activeProfiles) {
		return DockerCompose.get(composeFile, this.properties.getHost(), activeProfiles);
	}

	/**
     * Checks if a RunningService is ignored based on its labels.
     * 
     * @param service the RunningService to check
     * @return true if the RunningService is ignored, false otherwise
     */
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
