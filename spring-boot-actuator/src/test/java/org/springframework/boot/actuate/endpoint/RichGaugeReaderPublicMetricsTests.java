/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.rich.InMemoryRichGaugeRepository;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link RichGaugeReaderPublicMetrics}.
 *
 * @author Johannes Stelzer
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
		assertTrue(results.containsKey("a.val"));
		assertThat(results.get("a.val").getValue().doubleValue(), equalTo(0.5d));

		assertTrue(results.containsKey("a.avg"));
		assertThat(results.get("a.avg").getValue().doubleValue(), equalTo(0.25d));

		assertTrue(results.containsKey("a.min"));
		assertThat(results.get("a.min").getValue().doubleValue(), equalTo(0.0d));

		assertTrue(results.containsKey("a.max"));
		assertThat(results.get("a.max").getValue().doubleValue(), equalTo(0.5d));

		assertTrue(results.containsKey("a.count"));
		assertThat(results.get("a.count").getValue().longValue(), equalTo(2L));

		assertTrue(results.containsKey("a.alpha"));
		assertThat(results.get("a.alpha").getValue().doubleValue(), equalTo(-1.d));
	}

}
