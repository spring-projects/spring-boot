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
	 * Return the relevant docker compose services that are running.
	 * @return the running services
	 */
	public List<RunningService> getRunningServices() {
		return this.runningServices;
	}

}
