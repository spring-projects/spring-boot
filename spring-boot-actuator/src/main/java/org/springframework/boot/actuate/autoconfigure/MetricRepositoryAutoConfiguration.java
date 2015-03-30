/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.buffer.BufferCounterService;
import org.springframework.boot.actuate.metrics.buffer.BufferGaugeService;
import org.springframework.boot.actuate.metrics.buffer.BufferMetricReader;
import org.springframework.boot.actuate.metrics.buffer.CounterBuffers;
import org.springframework.boot.actuate.metrics.buffer.GaugeBuffers;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.export.MetricCopyExporter;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.writer.CompositeMetricWriter;
import org.springframework.boot.actuate.metrics.writer.DefaultCounterService;
import org.springframework.boot.actuate.metrics.writer.DefaultGaugeService;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.JavaVersion;
import org.springframework.boot.autoconfigure.condition.ConditionalOnJava.Range;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.codahale.metrics.MetricRegistry;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics services. Creates
 * user-facing {@link GaugeService} and {@link CounterService} instances, and also back
 * end repositories to catch the data pumped into them.
 * <p>
 * An {@link InMemoryMetricRepository} is always created unless another
 * {@link MetricRepository} is already provided by the user. In general, even if metric
 * data needs to be stored and analysed remotely, it is recommended to use an in-memory
 * repository to buffer metric updates locally. The values can be exported (e.g. on a
 * periodic basis) using an {@link Exporter}, most implementations of which have
 * optimizations for sending data to remote repositories.
 * <p>
 * If Spring Messaging is on the classpath a {@link MessageChannel} called
 * "metricsChannel" is also created (unless one already exists) and all metric update
 * events are published additionally as messages on that channel. Additional analysis or
 * actions can be taken by clients subscribing to that channel.
 * <p>
 * In addition if Codahale's metrics library is on the classpath a {@link MetricRegistry}
 * will be created and wired up to the counter and gauge services in addition to the basic
 * repository. Users can create Codahale metrics by prefixing their metric names with the
 * appropriate type (e.g. "histogram.*", "meter.*") and sending them to the standard
 * <code>GaugeService</code> or <code>CounterService</code>.
 * <p>
 * By default all metric updates go to all {@link MetricWriter} instances in the
 * application context. To change this behaviour define your own metric writer bean called
 * "primaryMetricWriter", mark it <code>@Primary</code>, and this one will receive all
 * updates from the default counter and gauge services. Alternatively you can provide your
 * own counter and gauge services and wire them to whichever writer you choose.
 *
 * @see GaugeService
 * @see CounterService
 * @see MetricWriter
 * @see InMemoryMetricRepository
 * @see Exporter
 *
 * @author Dave Syer
 */
@Configuration
@EnableConfigurationProperties(MetricsProperties.class)
public class MetricRepositoryAutoConfiguration {

	@Configuration
	@ConditionalOnJava(value = JavaVersion.EIGHT, range = Range.OLDER_THAN)
	@ConditionalOnMissingBean(MetricRepository.class)
	static class LegacyMetricServicesConfiguration {

		@Autowired
		private MetricWriter writer;

		@Bean
		@ConditionalOnMissingBean
		public CounterService counterService() {
			return new DefaultCounterService(this.writer);
		}

		@Bean
		@ConditionalOnMissingBean
		public GaugeService gaugeService() {
			return new DefaultGaugeService(this.writer);
		}

	}

	@Configuration
	@ConditionalOnJava(value = JavaVersion.EIGHT)
	@ConditionalOnMissingBean(MetricRepository.class)
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
		@ConditionalOnMissingBean
		public BufferMetricReader metricReader(CounterBuffers counters,
				GaugeBuffers gauges) {
			return new BufferMetricReader(counters, gauges);
		}

		@Bean
		@ConditionalOnMissingBean
		public CounterService counterService(CounterBuffers writer) {
			return new BufferCounterService(writer);
		}

		@Bean
		@ConditionalOnMissingBean
		public GaugeService gaugeService(GaugeBuffers writer) {
			return new BufferGaugeService(writer);
		}
	}

	@Configuration
	@ConditionalOnJava(value = JavaVersion.EIGHT, range = Range.OLDER_THAN)
	@ConditionalOnMissingBean(MetricRepository.class)
	static class LegacyMetricRepositoryConfiguration {

		@Bean
		public InMemoryMetricRepository actuatorMetricRepository() {
			return new InMemoryMetricRepository();
		}

	}

	@Configuration
	@EnableScheduling
	@ConditionalOnProperty(value = "spring.metrics.export.enabled", matchIfMissing = true)
	static class DefaultMetricsExporterConfiguration {

		@Autowired(required = false)
		private List<MetricWriter> writers;

		@Bean
		@ConditionalOnMissingBean
		@ConditionalOnBean(MetricWriter.class)
		public MetricCopyExporter messageChannelMetricExporter(MetricReader reader) {
			return new MetricCopyExporter(reader, new CompositeMetricWriter(this.writers)) {
				@Scheduled(fixedDelayString = "${spring.metrics.export.delayMillis:5000}")
				@Override
				public void export() {
					super.export();
				}
			};
		}
	}

}
