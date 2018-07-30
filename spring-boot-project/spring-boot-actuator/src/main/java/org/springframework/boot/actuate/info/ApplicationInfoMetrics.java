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

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.MeterBinder;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;


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
		Map<String, String> flatDetails = flattenKeys(details, "");

		final List<Tag> tags = flatDetails.entrySet().stream()
				.map(e -> Tag.of(e.getKey(), e.getValue()))
				.collect(Collectors.toList());

		return tags;
	}

	private Map<String, String> flattenKeys(Map<String, Object> info, final String parentKey) {

		final Map<String, String> result = new LinkedHashMap<>();
		final String prefix = StringUtils.isEmpty(parentKey) ? "" : parentKey + '.';

		for (Map.Entry<String, Object> entry : info.entrySet()) {
			if (entry.getValue() instanceof Map) {
				result.putAll(flattenKeys((Map<String, Object>) entry.getValue(), prefix + entry.getKey()));
			} else {
				result.put(prefix + entry.getKey(), Objects.toString(entry.getValue()));
			}
		}
		return result;
	}
}
