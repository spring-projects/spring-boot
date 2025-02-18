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
		return new DockerComposeLifecycleManager(applicationContext, binder, this.shutdownHandlers, properties,
				eventListeners);
	}

}
