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

package org.springframework.boot.actuate.autoconfigure.observation;

import java.lang.reflect.Method;
import java.util.List;

import io.micrometer.observation.Observation;
import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ObservationHandlerGrouping}.
 *
 * @author Moritz Halbritter
 */
class ObservationHandlerGroupingTests {

	@Test
	void shouldGroupCategoriesIntoFirstMatchingHandlerAndRespectsCategoryOrder() {
		ObservationHandlerGrouping grouping = new ObservationHandlerGrouping(
				List.of(ObservationHandlerA.class, ObservationHandlerB.class));
		ObservationConfig config = new ObservationConfig();
		ObservationHandlerA handlerA1 = new ObservationHandlerA("a1");
		ObservationHandlerA handlerA2 = new ObservationHandlerA("a2");
		ObservationHandlerB handlerB1 = new ObservationHandlerB("b1");
		ObservationHandlerB handlerB2 = new ObservationHandlerB("b2");
		grouping.apply(List.of(handlerB1, handlerB2, handlerA1, handlerA2), config);
		List<ObservationHandler<?>> handlers = getObservationHandlers(config);
		assertThat(handlers).hasSize(2);
		// Category A is first
		assertThat(handlers.get(0)).isInstanceOf(FirstMatchingCompositeObservationHandler.class);
		FirstMatchingCompositeObservationHandler firstMatching0 = (FirstMatchingCompositeObservationHandler) handlers
			.get(0);
		assertThat(firstMatching0.getHandlers()).containsExactly(handlerA1, handlerA2);
		// Category B is second
		assertThat(handlers.get(1)).isInstanceOf(FirstMatchingCompositeObservationHandler.class);
		FirstMatchingCompositeObservationHandler firstMatching1 = (FirstMatchingCompositeObservationHandler) handlers
			.get(1);
		assertThat(firstMatching1.getHandlers()).containsExactly(handlerB1, handlerB2);
	}

	@Test
	void uncategorizedHandlersShouldBeOrderedAfterCategories() {
		ObservationHandlerGrouping grouping = new ObservationHandlerGrouping(ObservationHandlerA.class);
		ObservationConfig config = new ObservationConfig();
		ObservationHandlerA handlerA1 = new ObservationHandlerA("a1");
		ObservationHandlerA handlerA2 = new ObservationHandlerA("a2");
		ObservationHandlerB handlerB1 = new ObservationHandlerB("b1");
		grouping.apply(List.of(handlerB1, handlerA1, handlerA2), config);
		List<ObservationHandler<?>> handlers = getObservationHandlers(config);
		assertThat(handlers).hasSize(2);
		// Category A is first
		assertThat(handlers.get(0)).isInstanceOf(FirstMatchingCompositeObservationHandler.class);
		FirstMatchingCompositeObservationHandler firstMatching0 = (FirstMatchingCompositeObservationHandler) handlers
			.get(0);
		// Uncategorized handlers follow
		assertThat(firstMatching0.getHandlers()).containsExactly(handlerA1, handlerA2);
		assertThat(handlers.get(1)).isEqualTo(handlerB1);
	}

	@SuppressWarnings("unchecked")
	private static List<ObservationHandler<?>> getObservationHandlers(ObservationConfig config) {
		Method method = ReflectionUtils.findMethod(ObservationConfig.class, "getObservationHandlers");
		ReflectionUtils.makeAccessible(method);
		return (List<ObservationHandler<?>>) ReflectionUtils.invokeMethod(method, config);
	}

	private static class NamedObservationHandler implements ObservationHandler<Observation.Context> {

		private final String name;

		NamedObservationHandler(String name) {
			this.name = name;
		}

		@Override
		public boolean supportsContext(Context context) {
			return true;
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "{name='" + this.name + "'}";
		}

	}

	private static class ObservationHandlerA extends NamedObservationHandler {

		ObservationHandlerA(String name) {
			super(name);
		}

	}

	private static class ObservationHandlerB extends NamedObservationHandler {

		ObservationHandlerB(String name) {
			super(name);
		}

	}

}
