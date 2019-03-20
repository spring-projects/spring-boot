/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.metrics.dropwizard;

import com.codahale.metrics.Reservoir;

/**
 * Factory interface that can be used by {@link DropwizardMetricServices} to create a
 * custom {@link Reservoir}.
 *
 * @author Lucas Saldanha
 * @author Phillip Webb
 * @since 1.5.0
 */
public interface ReservoirFactory {

	/**
	 * Default empty {@link ReservoirFactory} implementation.
	 */
	ReservoirFactory NONE = new ReservoirFactory() {

		@Override
		public Reservoir getReservoir(String name) {
			return null;
		}

	};

	/**
	 * Return the {@link Reservoir} instance to use or {@code null} if a custom reservoir
	 * is not needed.
	 * @param name the name of the metric
	 * @return a reservoir instance or {@code null}
	 */
	Reservoir getReservoir(String name);

}
