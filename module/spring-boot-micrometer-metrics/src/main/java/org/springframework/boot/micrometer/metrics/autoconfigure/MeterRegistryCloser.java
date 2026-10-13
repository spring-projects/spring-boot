/*
 * Copyright 2012-present the original author or authors.
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
