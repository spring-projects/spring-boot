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

package org.springframework.boot.actuate.info;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.util.Assert;


/**
 * Makes the data from all InfoContributors available as the metric "application.info".
 *
 * @author Tiago Bilou, Florian Stumpf
 * @since 2.0.0
 */
public class ApplicationInfoMetrics implements MeterBinder {

	private final List<InfoContributor> infoContributors;
	private final AtomicInteger value = new AtomicInteger(1);

	public ApplicationInfoMetrics(List<InfoContributor> infoContributors) {
		Assert.notNull(infoContributors, "Info contributors must not be null");
		this.infoContributors = infoContributors;
	}

	@Override
	public void bindTo(MeterRegistry registry) {
		Gauge.builder("application.info", this.value, AtomicInteger::get).tags(info()).register(registry);
	}

	private List<Tag> info() {
		Info.Builder builder = new Info.Builder();
		for (InfoContributor contributor : this.infoContributors) {
			contributor.contribute(builder);
		}
		Info build = builder.build();

		final Map<String, Object> details = build.getDetails();
		Map<String, String> flatDetails = new LinkedHashMap<>();

		flattenKeys(details, flatDetails);

		final List<Tag> tags = flatDetails.entrySet().stream()
				.map(e -> Tag.of(e.getKey(), e.getValue()))
				.collect(Collectors.toList());

		return tags;
	}

	private void flattenKeys(Map<String, Object> map, Map<String, String> result) {

		for (Map.Entry<String, Object> entry : map.entrySet()) {
			if (!(entry.getValue() instanceof Map)) {
				result.put(entry.getKey(), String.valueOf(entry.getValue()));
			} else {
				final Map<String, Object> remaining = (Map<String, Object>) entry.getValue();
				remaining.entrySet().stream()
						.map(e -> Collections.singletonMap(entry.getKey().concat(".").concat(e.getKey()), e.getValue()))
						.forEach(m -> flattenKeys(m, result));
			}
		}
	}
}
