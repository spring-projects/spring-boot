/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.docs.actuate.metrics;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;

/**
 * Example to show injection and use of a {@link MeterRegistry}.
 *
 * @author Andy Wilkinson
 */
public class MetricsMeterRegistryInjectionExample {

	// tag::component[]
	class Dictionary {

		private final List<String> words = new CopyOnWriteArrayList<>();

		Dictionary(MeterRegistry registry) {
			registry.gaugeCollectionSize("dictionary.size", Tags.empty(), this.words);
		}

		// …

	}
	// end::component[]

}
