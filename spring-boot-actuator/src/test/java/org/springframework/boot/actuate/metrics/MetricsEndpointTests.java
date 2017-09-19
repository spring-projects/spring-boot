/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.actuate.metrics;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Meter.Id;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleCounter;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricsEndpoint}.
 *
 * @author Andy Wilkinson
 */
public class MetricsEndpointTests {

	private final MeterRegistry registry = mock(MeterRegistry.class);

	private final MetricsEndpoint endpoint = new MetricsEndpoint(this.registry);

	@Test
	public void listNamesHandlesEmptyListOfMeters() {
		given(this.registry.getMeters()).willReturn(Arrays.asList());
		Map<String, List<String>> result = this.endpoint.listNames();
		assertThat(result).containsOnlyKeys("names");
		assertThat(result.get("names")).isEmpty();
	}

	@Test
	public void listNamesProducesListOfUniqueMeterNames() {
		List<Meter> meters = Arrays.asList(createCounter("com.example.foo"),
				createCounter("com.example.bar"), createCounter("com.example.foo"));
		given(this.registry.getMeters()).willReturn(meters);
		Map<String, List<String>> result = this.endpoint.listNames();
		assertThat(result).containsOnlyKeys("names");
		assertThat(result.get("names")).containsOnlyOnce("com.example.foo",
				"com.example.bar");
	}

	private Meter createCounter(String name) {
		return new SimpleCounter(createMeterId(name));
	}

	private Id createMeterId(String name) {
		Id id = mock(Id.class);
		given(id.getName()).willReturn(name);
		return id;
	}

}
