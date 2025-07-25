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

package org.springframework.boot.tracing.autoconfigure;

import java.util.ArrayList;
import java.util.List;

import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;

import org.springframework.boot.observation.autoconfigure.ObservationHandlerGroup;

/**
 * {@link ObservationHandlerGroup} that considers both {@link TracingObservationHandler}
 * and {@link MeterObservationHandler} types as members. This group takes precedence over
 * any regular {@link MeterObservationHandler} group in order to ensure
 * {@link TracingAwareMeterObservationHandler} wrapping is applied during registration.
 *
 * @author Phillip Webb
 */
class TracingAndMeterObservationHandlerGroup implements ObservationHandlerGroup {

	private final Tracer tracer;

	TracingAndMeterObservationHandlerGroup(Tracer tracer) {
		this.tracer = tracer;
	}

	@Override
	public boolean isMember(ObservationHandler<?> handler) {
		return MeterObservationHandler.class.isInstance(handler) || TracingObservationHandler.class.isInstance(handler);
	}

	@Override
	public int compareTo(ObservationHandlerGroup other) {
		if (other instanceof TracingAndMeterObservationHandlerGroup) {
			return 0;
		}
		return MeterObservationHandler.class.isAssignableFrom(other.handlerType()) ? -1 : 1;
	}

	@Override
	public void registerMembers(ObservationConfig config, List<ObservationHandler<?>> members) {
		List<ObservationHandler<?>> tracingHandlers = new ArrayList<>(members.size());
		List<ObservationHandler<?>> metricsHandlers = new ArrayList<>(members.size());
		for (ObservationHandler<?> member : members) {
			if (member instanceof MeterObservationHandler<?> meterObservationHandler
					&& !(member instanceof TracingAwareMeterObservationHandler<?>)) {
				metricsHandlers.add(new TracingAwareMeterObservationHandler<>(meterObservationHandler, this.tracer));
			}
			else {
				tracingHandlers.add(member);
			}
		}
		registerHandlers(config, tracingHandlers);
		registerHandlers(config, metricsHandlers);
	}

	private void registerHandlers(ObservationConfig config, List<ObservationHandler<?>> handlers) {
		if (handlers.size() == 1) {
			config.observationHandler(handlers.get(0));
		}
		else if (!handlers.isEmpty()) {
			config.observationHandler(new FirstMatchingCompositeObservationHandler(handlers));
		}
	}

	@Override
	public Class<?> handlerType() {
		return TracingObservationHandler.class;
	}

}
