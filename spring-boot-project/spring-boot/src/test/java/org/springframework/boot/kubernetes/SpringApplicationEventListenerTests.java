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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.support.StaticApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link SpringApplicationEventListener}
 *
 * @author Brian Clozel
 */
class SpringApplicationEventListenerTests {

	@Test
	void shouldReactToApplicationStartedEvent() {
		ApplicationEvent event = publishAndReceiveApplicationEvent(
				new ApplicationStartedEvent(new SpringApplication(), null, null));

		assertThat(event).isInstanceOf(LivenessStateChangedEvent.class);
		LivenessStateChangedEvent livenessEvent = (LivenessStateChangedEvent) event;
		assertThat(livenessEvent.getLivenessState()).isEqualTo(LivenessState.live());
	}

	@Test
	void shouldReactToApplicationReadyEvent() {
		ApplicationEvent event = publishAndReceiveApplicationEvent(
				new ApplicationReadyEvent(new SpringApplication(), null, null));

		assertThat(event).isInstanceOf(ReadinessStateChangedEvent.class);
		ReadinessStateChangedEvent readinessEvent = (ReadinessStateChangedEvent) event;
		assertThat(readinessEvent.getReadinessState()).isEqualTo(ReadinessState.ready());
	}

	@Test
	void shouldReactToContextClosedEvent() {
		ApplicationEvent event = publishAndReceiveApplicationEvent(
				new ContextClosedEvent(new StaticApplicationContext()));

		assertThat(event).isInstanceOf(ReadinessStateChangedEvent.class);
		ReadinessStateChangedEvent readinessEvent = (ReadinessStateChangedEvent) event;
		assertThat(readinessEvent.getReadinessState()).isEqualTo(ReadinessState.busy());
	}

	private ApplicationEvent publishAndReceiveApplicationEvent(ApplicationEvent eventToSend) {
		ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
		SpringApplicationEventListener eventListener = new SpringApplicationEventListener();
		eventListener.setApplicationEventPublisher(eventPublisher);

		eventListener.onApplicationEvent(eventToSend);
		ArgumentCaptor<ApplicationEvent> event = ArgumentCaptor.forClass(ApplicationEvent.class);
		verify(eventPublisher).publishEvent(event.capture());

		return event.getValue();
	}

}
