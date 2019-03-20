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

package org.springframework.boot.actuate.endpoint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.rich.InMemoryRichGaugeRepository;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RichGaugeReaderPublicMetrics}.
 *
 * @author Johannes Edmeier
 */
public class RichGaugeReaderPublicMetricsTests {

	@Test
	public void testMetrics() throws Exception {
		InMemoryRichGaugeRepository repository = new InMemoryRichGaugeRepository();

		repository.set(new Metric<Double>("a", 0.d, new Date()));
		repository.set(new Metric<Double>("a", 0.5d, new Date()));

		RichGaugeReaderPublicMetrics metrics = new RichGaugeReaderPublicMetrics(
				repository);

		Map<String, Metric<?>> results = new HashMap<String, Metric<?>>();
		for (Metric<?> metric : metrics.metrics()) {
			results.put(metric.getName(), metric);
		}
		assertThat(results.containsKey("a.val")).isTrue();
		assertThat(results.get("a.val").getValue().doubleValue()).isEqualTo(0.5d);

		assertThat(results.containsKey("a.avg")).isTrue();
		assertThat(results.get("a.avg").getValue().doubleValue()).isEqualTo(0.25d);

		assertThat(results.containsKey("a.min")).isTrue();
		assertThat(results.get("a.min").getValue().doubleValue()).isEqualTo(0.0d);

		assertThat(results.containsKey("a.max")).isTrue();
		assertThat(results.get("a.max").getValue().doubleValue()).isEqualTo(0.5d);

		assertThat(results.containsKey("a.count")).isTrue();
		assertThat(results.get("a.count").getValue().longValue()).isEqualTo(2L);

		assertThat(results.containsKey("a.alpha")).isTrue();
		assertThat(results.get("a.alpha").getValue().doubleValue()).isEqualTo(-1.d);
	}

}
