/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.util.List;

import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.docker.compose.core.RunningService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;

/**
 * {@link ApplicationEvent} published when Docker Compose {@link RunningService} instances
 * are available. This event is published from the {@link ApplicationPreparedEvent}
 * listener that starts Docker Compose.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
public class DockerComposeServicesReadyEvent extends ApplicationEvent {

	private final List<RunningService> runningServices;

	DockerComposeServicesReadyEvent(ApplicationContext source, List<RunningService> runningServices) {
		super(source);
		this.runningServices = runningServices;
	}

	@Override
	public ApplicationContext getSource() {
		return (ApplicationContext) super.getSource();
	}

	/**
	 * Return the relevant Docker Compose services that are running.
	 * @return the running services
	 */
	public List<RunningService> getRunningServices() {
		return this.runningServices;
	}

}
