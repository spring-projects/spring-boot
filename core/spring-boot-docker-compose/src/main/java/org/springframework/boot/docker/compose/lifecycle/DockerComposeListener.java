/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.docker.compose.lifecycle;

import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringApplicationShutdownHandlers;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * {@link ApplicationListener} used to set up a {@link DockerComposeLifecycleManager}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
class DockerComposeListener implements ApplicationListener<ApplicationPreparedEvent> {

	private final SpringApplicationShutdownHandlers shutdownHandlers;

	DockerComposeListener() {
		this(SpringApplication.getShutdownHandlers());
	}

	DockerComposeListener(SpringApplicationShutdownHandlers shutdownHandlers) {
		this.shutdownHandlers = shutdownHandlers;
	}

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		ConfigurableApplicationContext applicationContext = event.getApplicationContext();
		Binder binder = Binder.get(applicationContext.getEnvironment());
		DockerComposeProperties properties = DockerComposeProperties.get(binder);
		Set<ApplicationListener<?>> eventListeners = event.getSpringApplication().getListeners();
		createDockerComposeLifecycleManager(applicationContext, binder, properties, eventListeners).start();
	}

	protected DockerComposeLifecycleManager createDockerComposeLifecycleManager(
			ConfigurableApplicationContext applicationContext, Binder binder, DockerComposeProperties properties,
			Set<ApplicationListener<?>> eventListeners) {
		return new DockerComposeLifecycleManager(applicationContext, this.shutdownHandlers, properties, eventListeners);
	}

}
