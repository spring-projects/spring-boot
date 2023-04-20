/*
 * Copyright 2012-2022 the original author or authors.
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

import java.util.List;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationHandler.FirstMatchingCompositeObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;

import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Groups {@link ObservationHandler ObservationHandlers} by type.
 *
 * @author Andy Wilkinson
 */
@SuppressWarnings("rawtypes")
class ObservationHandlerGrouping {

	private final List<Class<? extends ObservationHandler>> categories;

	ObservationHandlerGrouping(Class<? extends ObservationHandler> category) {
		this(List.of(category));
	}

	ObservationHandlerGrouping(List<Class<? extends ObservationHandler>> categories) {
		this.categories = categories;
	}

	void apply(List<ObservationHandler<?>> handlers, ObservationConfig config) {
		MultiValueMap<Class<? extends ObservationHandler>, ObservationHandler<?>> groupings = new LinkedMultiValueMap<>();
		for (ObservationHandler<?> handler : handlers) {
			Class<? extends ObservationHandler> category = findCategory(handler);
			if (category != null) {
				groupings.add(category, handler);
			}
			else {
				config.observationHandler(handler);
			}
		}
		for (Class<? extends ObservationHandler> category : this.categories) {
			List<ObservationHandler<?>> handlerGroup = groupings.get(category);
			if (!CollectionUtils.isEmpty(handlerGroup)) {
				config.observationHandler(new FirstMatchingCompositeObservationHandler(handlerGroup));
			}
		}
	}

	private Class<? extends ObservationHandler> findCategory(ObservationHandler<?> handler) {
		for (Class<? extends ObservationHandler> category : this.categories) {
			if (category.isInstance(handler)) {
				return category;
			}
		}
		return null;
	}

}
