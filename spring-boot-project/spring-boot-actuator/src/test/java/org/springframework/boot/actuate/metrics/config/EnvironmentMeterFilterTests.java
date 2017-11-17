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

package org.springframework.boot.actuate.metrics.config;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.histogram.HistogramConfig;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.config.EnvironmentMeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
		"spring.metrics.filter.enabled=false", // turn off all metrics by default
		"spring.metrics.filter.my.timer.enabled=true",
		"spring.metrics.filter.my.timer.maximumExpectedValue=PT10S",
		"spring.metrics.filter.my.timer.minimumExpectedValue=1ms",
		"spring.metrics.filter.my.timer.percentiles=0.5,0.95",
		"spring.metrics.filter.my.timer.that.is.misconfigured.enabled=troo",

		"spring.metrics.filter.my.summary.enabled=true",
		"spring.metrics.filter.my.summary.maximumExpectedValue=100",
})
public class EnvironmentMeterFilterTests {
	private HistogramConfig histogramConfig;

	private MeterRegistry registry = new SimpleMeterRegistry(SimpleConfig.DEFAULT, new MockClock()) {
		@Override
		protected Timer newTimer(Meter.Id id, HistogramConfig conf) {
			histogramConfig = conf;
			return super.newTimer(id, conf);
		}

		@Override
		protected DistributionSummary newDistributionSummary(Meter.Id id, HistogramConfig conf) {
			histogramConfig = conf;
			return super.newDistributionSummary(id, conf);
		}
	};

	@Autowired
	private EnvironmentMeterFilter filter;

	@Before
	public void configureRegistry() {
		registry.config().meterFilter(filter);
	}

	@Test
	public void disable() {
		registry.counter("my.counter");
		assertThat(registry.find("my.counter").counter()).isNotPresent();
	}

	@Test
	public void enable() {
		registry.timer("my.timer");
		assertThat(registry.find("my.timer").timer()).isPresent();
	}

	@Test
	public void timerHistogramConfig() {
		registry.timer("my.timer");
		assertThat(histogramConfig.getMaximumExpectedValue()).isEqualTo(Duration.ofSeconds(30).toNanos());
		assertThat(histogramConfig.getMinimumExpectedValue()).isEqualTo(Duration.ofMillis(1).toNanos());
		assertThat(histogramConfig.getPercentiles()).containsExactly(0.5, 0.95);
	}

	@Test
	public void summaryHistogramConfig() {
		registry.summary("my.summary");
		assertThat(histogramConfig.getMaximumExpectedValue()).isEqualTo(100);
	}

	@Test
	public void configErrorMessage(){
		assertThatThrownBy(() -> registry.timer("my.timer.that.is.misconfigured"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Invalid configuration for 'my.timer.that.is.misconfigured.enabled' value 'troo' as class java.lang.Boolean");
	}

	@Configuration
	static class FilterTestConfiguration {
		@Bean
		public EnvironmentMeterFilter filter(Environment environment) {
			return new EnvironmentMeterFilter(environment);
		}
	}
}
