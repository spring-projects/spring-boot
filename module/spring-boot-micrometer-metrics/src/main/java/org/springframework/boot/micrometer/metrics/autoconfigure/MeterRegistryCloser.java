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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

/**
 * Ensures that {@link MeterRegistry meter registries} are closed early in the shutdown
 * process. Also unregisters the registries from the global registry if needed.
 *
 * @author Jon Schneider
 * @author Stephane Nicoll
 * @author Moritz Halbritter
 * @author Michael Berry
 * @author Phillip Webb
 * @author Lordwill Kandiro
 */
class MeterRegistryCloser implements ApplicationListener<ContextClosedEvent> {

	private final ApplicationContext context;

	private final Set<MeterRegistry> registriesToClose = new CopyOnWriteArraySet<>();

	private final Set<MeterRegistry> registriesToRemoveFromGlobalRegistry = new CopyOnWriteArraySet<>();

	MeterRegistryCloser(ApplicationContext context) {
		this.context = context;
	}

	void track(MeterRegistry meterRegistry) {
		this.registriesToClose.add(meterRegistry);
	}

	void trackAddedToGlobalRegistry(MeterRegistry meterRegistry) {
		this.registriesToRemoveFromGlobalRegistry.add(meterRegistry);
	}

	@Override
	public void onApplicationEvent(ContextClosedEvent event) {
		if (!this.context.equals(event.getApplicationContext())) {
			return;
		}
		for (MeterRegistry meterRegistry : this.registriesToRemoveFromGlobalRegistry) {
			Metrics.removeRegistry(meterRegistry);
		}
		for (MeterRegistry meterRegistry : this.registriesToClose) {
			if (!meterRegistry.isClosed()) {
				meterRegistry.close();
			}
		}
	}

}
