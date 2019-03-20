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

package org.springframework.boot.actuate.autoconfigure;

import com.codahale.metrics.MetricRegistry;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.buffer.BufferCounterService;
import org.springframework.boot.actuate.metrics.buffer.BufferGaugeService;
import org.springframework.boot.actuate.metrics.buffer.BufferMetricReader;
import org.springframework.boot.actuate.metrics.buffer.CounterBuffers;
import org.springframework.boot.actuate.metrics.buffer.GaugeBuffers;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.export.MetricCopyExporter;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.repository.InMemoryMultiMetricRepository;
import org.springframework.boot.actuate.metrics.writer.DefaultCounterService;
import org.springframework.boot.actuate.metrics.writer.DefaultGaugeService;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.JavaVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.Range;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics services. Creates
 * user-facing {@link GaugeService} and {@link CounterService} instances, and also back
 * end repositories to catch the data pumped into them.
 * <p>
 * In general, even if metric data needs to be stored and analysed remotely, it is
 * recommended to use in-memory storage to buffer metric updates locally as is done by the
 * default {@link InMemoryMetricRepository} (Java 7) or {@link CounterBuffers} and
 * {@link GaugeBuffers} (Java 8). The values can be exported (e.g. on a periodic basis)
 * using an {@link Exporter}, most implementations of which have optimizations for sending
 * data to remote repositories.
 * <p>
 * If Spring Messaging is on the classpath and a {@link MessageChannel} called
 * "metricsChannel" is also available, all metric update events are published additionally
 * as messages on that channel. Additional analysis or actions can be taken by clients
 * subscribing to that channel.
 * <p>
 * In addition if Dropwizard's metrics library is on the classpath a
 * {@link MetricRegistry} will be created and the default counter and gauge services will
 * switch to using it instead of the default repository. Users can create "special"
 * Dropwizard metrics by prefixing their metric names with the appropriate type (e.g.
 * "histogram.*", "meter.*". "timer.*") and sending them to the {@code GaugeService} or
 * {@code CounterService}.
 * <p>
 * By default all metric updates go to all {@link MetricWriter} instances in the
 * application context via a {@link MetricCopyExporter} firing every 5 seconds (disable
 * this by setting {@code spring.metrics.export.enabled=false}).
 *
 * @see GaugeService
 * @see CounterService
 * @see MetricWriter
 * @see InMemoryMetricRepository
 * @see Exporter
 * @author Dave Syer
 */
@Configuration
public class MetricRepositoryAutoConfiguration {

	@Configuration
	@ConditionalOnJava(value = JavaVersion.EIGHT, range = Range.OLDER_THAN)
	@ConditionalOnMissingBean(GaugeService.class)
	static class LegacyMetricServicesConfiguration {

		private final MetricWriter writer;

		LegacyMetricServicesConfiguration(@ActuatorMetricWriter MetricWriter writer) {
			this.writer = writer;
		}

		@Bean
		@ConditionalOnMissingBean(CounterService.class)
		public DefaultCounterService counterService() {
			return new DefaultCounterService(this.writer);
		}

		@Bean
		@ConditionalOnMissingBean(GaugeService.class)
		public DefaultGaugeService gaugeService() {
			return new DefaultGaugeService(this.writer);
		}

	}

	@Configuration
	@ConditionalOnJava(JavaVersion.EIGHT)
	@ConditionalOnMissingBean(GaugeService.class)
	static class FastMetricServicesConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public CounterBuffers counterBuffers() {
			return new CounterBuffers();
		}

		@Bean
		@ConditionalOnMissingBean
		public GaugeBuffers gaugeBuffers() {
			return new GaugeBuffers();
		}

		@Bean
		@ExportMetricReader
		@ConditionalOnMissingBean
		public BufferMetricReader actuatorMetricReader(CounterBuffers counters,
				GaugeBuffers gauges) {
			return new BufferMetricReader(counters, gauges);
		}

		@Bean
		@ConditionalOnMissingBean(CounterService.class)
		public BufferCounterService counterService(CounterBuffers writer) {
			return new BufferCounterService(writer);
		}

		@Bean
		@ConditionalOnMissingBean(GaugeService.class)
		public BufferGaugeService gaugeService(GaugeBuffers writer) {
			return new BufferGaugeService(writer);
		}

	}

	@Configuration
	@ConditionalOnJava(value = JavaVersion.EIGHT, range = Range.OLDER_THAN)
	@ConditionalOnMissingBean(name = "actuatorMetricRepository")
	static class LegacyMetricRepositoryConfiguration {

		@Bean
		@ExportMetricReader
		@ActuatorMetricWriter
		public InMemoryMetricRepository actuatorMetricRepository() {
			return new InMemoryMetricRepository();
		}

		@Bean
		@ExportMetricReader
		@ActuatorMetricWriter
		public InMemoryMultiMetricRepository actuatorMultiMetricRepository(
				InMemoryMetricRepository actuatorMetricRepository) {
			return new InMemoryMultiMetricRepository(actuatorMetricRepository);
		}

	}

}
