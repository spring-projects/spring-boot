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

package org.springframework.boot.actuate.autoconfigure.metrics;

import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MockClock;
import io.micrometer.core.instrument.histogram.HistogramConfig;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jon Schneider
 */
public class PropertiesMeterFilterTest {
	private MetricsProperties props = new MetricsProperties();

	private HistogramConfig histogramConfig;

	private MeterRegistry registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock()) {
		@Override
		protected DistributionSummary newDistributionSummary(Meter.Id id, HistogramConfig conf) {
			PropertiesMeterFilterTest.this.histogramConfig = conf;
			return super.newDistributionSummary(id, conf);
		}
	};

	@Before
	public void before() {
		this.registry.config().meterFilter(new PropertiesMeterFilter(this.props));
	}

	@Test
	public void disable() {
		this.props.getEnabled().put("my.counter", false);
		this.registry.counter("my.counter");

		assertThat(this.registry.find("my.counter").counter()).isNull();
	}

	@Test
	public void disableAll() {
		this.props.getEnabled().put("all", false);
		this.registry.timer("my.timer");

		assertThat(this.registry.find("my.timer").timer()).isNull();
	}

	@Test
	public void enable() {
		this.props.getEnabled().put("all", false);
		this.props.getEnabled().put("my.timer", true);
		this.registry.timer("my.timer");

		this.registry.mustFind("my.timer").timer();
	}

	@Test
	public void summaryHistogramConfig() {
		this.props.getSummaries().getMaximumExpectedValue().put("my.summary", 100L);
		this.registry.summary("my.summary");

		assertThat(this.histogramConfig.getMaximumExpectedValue()).isEqualTo(100);
	}
}
