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

package org.springframework.boot.kubernetes;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * {@link ApplicationListener} that listens for application lifecycle events such as
 * {@link ApplicationStartedEvent}, {@link ApplicationReadyEvent},
 * {@link ContextClosedEvent}. Those events are then translated and published into events
 * consumed by {@link ApplicationStateProvider} to update the application state.
 *
 * @author Brian Clozel
 * @since 2.3.0
 */
public class SpringApplicationEventListener
		implements ApplicationListener<ApplicationEvent>, ApplicationEventPublisherAware {

	private ApplicationEventPublisher applicationEventPublisher;

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ApplicationStartedEvent) {
			LivenessState livenessState = LivenessState.live();
			this.applicationEventPublisher
					.publishEvent(new LivenessStateChangedEvent(livenessState, "Application has started"));
		}
		else if (event instanceof ApplicationReadyEvent) {
			this.applicationEventPublisher.publishEvent(ReadinessStateChangedEvent.ready());
		}
		else if (event instanceof ContextClosedEvent) {
			this.applicationEventPublisher.publishEvent(ReadinessStateChangedEvent.busy());
		}
	}

}
