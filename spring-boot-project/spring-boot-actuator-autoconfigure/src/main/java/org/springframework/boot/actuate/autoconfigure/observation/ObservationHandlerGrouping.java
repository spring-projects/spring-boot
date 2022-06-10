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

import java.util.Collection;

import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry.ObservationConfig;

/**
 * Strategy to apply {@link ObservationHandler ObservationHandlers} to an
 * {@link ObservationConfig}.
 *
 * @author Moritz Halbritter
 */
interface ObservationHandlerGrouping {

	/**
	 * Applies the given list of {@code handlers} to the given {@code config}.
	 * @param handlers the list of observation handlers
	 * @param config the config to apply the handlers to
	 */
	void apply(Collection<ObservationHandler<?>> handlers, ObservationConfig config);

}
