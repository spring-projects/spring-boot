/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.micrometer.metrics.autoconfigure;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextClosedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

/**
 * Tests for {@link MeterRegistryCloser}.
 *
 * @author Lordwill Kandiro
 * @author Moritz Halbritter
 */
class MeterRegistryCloserTests {

	private final ApplicationContext context = mock(ApplicationContext.class);

	@Test
	void trackedRegistryIsRemovedFromGlobalRegistryOnContextClosedEvent() {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		MeterRegistryCloser closer = new MeterRegistryCloser(this.context);
		try {
			Metrics.addRegistry(meterRegistry);
			closer.trackAddedToGlobalRegistry(meterRegistry);
			closer.onApplicationEvent(new ContextClosedEvent(this.context));
			assertThat(Metrics.globalRegistry.getRegistries()).doesNotContain(meterRegistry);
		}
		finally {
			Metrics.removeRegistry(meterRegistry);
		}
	}

	@Test
	void trackedRegistryIsClosedOnContextClosedEvent() {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		MeterRegistryCloser closer = new MeterRegistryCloser(this.context);
		closer.track(meterRegistry);
		closer.onApplicationEvent(new ContextClosedEvent(this.context));
		assertThat(meterRegistry.isClosed()).isTrue();
	}

	@Test
	void trackedRegistriesAreClosedInTheOrderThatTheyWereTracked() {
		MeterRegistry first = mock(MeterRegistry.class);
		MeterRegistry second = mock(MeterRegistry.class);
		MeterRegistry third = mock(MeterRegistry.class);
		MeterRegistryCloser closer = new MeterRegistryCloser(this.context);
		closer.track(first);
		closer.track(second);
		closer.track(third);
		closer.onApplicationEvent(new ContextClosedEvent(this.context));
		InOrder ordered = inOrder(first, second, third);
		then(first).should(ordered).close();
		then(second).should(ordered).close();
		then(third).should(ordered).close();
	}

	@Test
	void alreadyClosedRegistryIsNotClosedAgain() {
		MeterRegistry meterRegistry = mock(MeterRegistry.class);
		given(meterRegistry.isClosed()).willReturn(true);
		MeterRegistryCloser closer = new MeterRegistryCloser(this.context);
		closer.track(meterRegistry);
		closer.onApplicationEvent(new ContextClosedEvent(this.context));
		then(meterRegistry).should(never()).close();
	}

	@Test
	void onApplicationEventIgnoresEventsFromOtherContexts() {
		MeterRegistry meterRegistry = new SimpleMeterRegistry();
		MeterRegistryCloser closer = new MeterRegistryCloser(this.context);
		try {
			Metrics.addRegistry(meterRegistry);
			closer.track(meterRegistry);
			closer.trackAddedToGlobalRegistry(meterRegistry);
			ApplicationContext otherContext = mock(ApplicationContext.class);
			closer.onApplicationEvent(new ContextClosedEvent(otherContext));
			assertThat(Metrics.globalRegistry.getRegistries()).contains(meterRegistry);
			assertThat(meterRegistry.isClosed()).isFalse();
		}
		finally {
			Metrics.removeRegistry(meterRegistry);
		}
	}

}
