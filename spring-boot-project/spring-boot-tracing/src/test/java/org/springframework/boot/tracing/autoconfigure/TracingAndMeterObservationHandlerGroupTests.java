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
import java.util.Collections;
import java.util.List;

import io.micrometer.core.instrument.observation.MeterObservationHandler;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.handler.TracingAwareMeterObservationHandler;
import io.micrometer.tracing.handler.TracingObservationHandler;
import org.assertj.core.extractor.Extractors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.boot.observation.autoconfigure.ObservationHandlerGroup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

/**
 * Tests for {@link TracingAndMeterObservationHandlerGroup}.
 *
 * @author Phillip Webb
 */
class TracingAndMeterObservationHandlerGroupTests {

	@Test
	void compareToSortsBeforeMeterObservationHandlerGroup() {
		ObservationHandlerGroup meterGroup = ObservationHandlerGroup.of(MeterObservationHandler.class);
		TracingAndMeterObservationHandlerGroup tracingAndMeterGroup = new TracingAndMeterObservationHandlerGroup(
				mock(Tracer.class));
		assertThat(sort(meterGroup, tracingAndMeterGroup)).containsExactly(tracingAndMeterGroup, meterGroup);
		assertThat(sort(tracingAndMeterGroup, meterGroup)).containsExactly(tracingAndMeterGroup, meterGroup);
	}

	@Test
	void isMemberAcceptsMeterObservationHandlerOrTracingObservationHandler() {
		TracingAndMeterObservationHandlerGroup group = new TracingAndMeterObservationHandlerGroup(mock(Tracer.class));
		assertThat(group.isMember(mock(ObservationHandler.class))).isFalse();
		assertThat(group.isMember(mock(MeterObservationHandler.class))).isTrue();
		assertThat(group.isMember(mock(TracingObservationHandler.class))).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	void registerMembersWrapsMeterObservationHandlersAndRegistersDistinctGroups() {
		Tracer tracer = mock(Tracer.class);
		TracingAndMeterObservationHandlerGroup group = new TracingAndMeterObservationHandlerGroup(tracer);
		TracingObservationHandler<?> tracingHandler1 = mock(TracingObservationHandler.class);
		TracingObservationHandler<?> tracingHandler2 = mock(TracingObservationHandler.class);
		MeterObservationHandler<?> meterHandler1 = mock(MeterObservationHandler.class);
		MeterObservationHandler<?> meterHandler2 = mock(MeterObservationHandler.class);
		ObservationConfig config = mock(ObservationConfig.class);
		List<ObservationHandler<?>> members = List.of(tracingHandler1, meterHandler1, tracingHandler2, meterHandler2);
		group.registerMembers(config, members);
		ArgumentCaptor<ObservationHandler<?>> handlerCaptor = ArgumentCaptor.captor();
		then(config).should(times(2)).observationHandler(handlerCaptor.capture());
		List<ObservationHandler<?>> actualComposites = handlerCaptor.getAllValues();
		assertThat(actualComposites).hasSize(2);
		ObservationHandler<?> tracingComposite = actualComposites.get(0);
		assertThat(tracingComposite).isInstanceOf(FirstMatchingCompositeObservationHandler.class);
		List<ObservationHandler<?>> tracingHandlers = (List<ObservationHandler<?>>) Extractors.byName("handlers")
			.apply(tracingComposite);
		assertThat(tracingHandlers).containsExactly(tracingHandler1, tracingHandler2);
		ObservationHandler<?> metricsComposite = actualComposites.get(1);
		assertThat(metricsComposite).isInstanceOf(FirstMatchingCompositeObservationHandler.class);
		List<ObservationHandler<?>> metricsHandlers = (List<ObservationHandler<?>>) Extractors.byName("handlers")
			.apply(metricsComposite);
		assertThat(metricsHandlers).hasSize(2);
		assertThat(metricsHandlers).extracting("delegate").containsExactly(meterHandler1, meterHandler2);
	}

	@Test
	void registerMembersOnlyUsesCompositeWhenMoreThanOneHandler() {
		Tracer tracer = mock(Tracer.class);
		TracingAndMeterObservationHandlerGroup group = new TracingAndMeterObservationHandlerGroup(tracer);
		TracingObservationHandler<?> tracingHandler1 = mock(TracingObservationHandler.class);
		TracingObservationHandler<?> tracingHandler2 = mock(TracingObservationHandler.class);
		MeterObservationHandler<?> meterHandler = mock(MeterObservationHandler.class);
		ObservationConfig config = mock(ObservationConfig.class);
		List<ObservationHandler<?>> members = List.of(tracingHandler1, meterHandler, tracingHandler2);
		group.registerMembers(config, members);
		ArgumentCaptor<ObservationHandler<?>> handlerCaptor = ArgumentCaptor.captor();
		then(config).should(times(2)).observationHandler(handlerCaptor.capture());
		List<ObservationHandler<?>> actualComposites = handlerCaptor.getAllValues();
		assertThat(actualComposites).hasSize(2);
		assertThat(actualComposites.get(0)).isInstanceOf(FirstMatchingCompositeObservationHandler.class);
		assertThat(actualComposites.get(1)).isInstanceOf(TracingAwareMeterObservationHandler.class);
	}

	private List<ObservationHandlerGroup> sort(ObservationHandlerGroup... groups) {
		List<ObservationHandlerGroup> list = new ArrayList<>(List.of(groups));
		Collections.sort(list);
		return list;
	}

}
