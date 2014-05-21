/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.actuate.metrics.rich;

import org.junit.Test;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.export.RichGaugeExporter;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Dave Syer
 */
public class MultiMetricRichGaugeReaderTests {

	private InMemoryMetricRepository repository = new InMemoryMetricRepository();
	private MultiMetricRichGaugeReader reader = new MultiMetricRichGaugeReader(
			this.repository);
	private InMemoryRichGaugeRepository data = new InMemoryRichGaugeRepository();
	private RichGaugeExporter exporter = new RichGaugeExporter(this.data, this.repository);

	@Test
	public void countOne() {
		this.data.set(new Metric<Integer>("foo", 1));
		this.data.set(new Metric<Integer>("foo", 1));
		this.exporter.export();
		// Check the exporter worked
		assertEquals(6, this.repository.count());
		assertEquals(1, this.reader.count());
		RichGauge one = this.reader.findOne("foo");
		assertNotNull(one);
		assertEquals(2, one.getCount());
	}

	@Test
	public void countTwo() {
		this.data.set(new Metric<Integer>("foo", 1));
		this.data.set(new Metric<Integer>("bar", 1));
		this.exporter.export();
		assertEquals(2, this.reader.count());
		RichGauge one = this.reader.findOne("foo");
		assertNotNull(one);
		assertEquals(1, one.getCount());
	}

}
