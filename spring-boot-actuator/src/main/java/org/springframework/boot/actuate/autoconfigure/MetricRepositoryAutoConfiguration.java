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

package org.springframework.boot.actuate.autoconfigure;

import java.util.List;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.endpoint.RichGaugeReaderPublicMetrics;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.rich.RichGaugeReader;
import org.springframework.boot.actuate.metrics.writer.CodahaleMetricWriter;
import org.springframework.boot.actuate.metrics.writer.CompositeMetricWriter;
import org.springframework.boot.actuate.metrics.writer.DefaultCounterService;
import org.springframework.boot.actuate.metrics.writer.DefaultGaugeService;
import org.springframework.boot.actuate.metrics.writer.MessageChannelMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriterMessageHandler;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.codahale.metrics.MetricRegistry;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for metrics services. Creates
 * user-facing {@link GaugeService} and {@link CounterService} instances, and also back
 * end repositories to catch the data pumped into them. </p>
 * <p>
 * An {@link InMemoryMetricRepository} is always created unless another
 * {@link MetricRepository} is already provided by the user. In general, even if metric
 * data needs to be stored and analysed remotely, it is recommended to use an in-memory
 * repository to buffer metric updates locally. The values can be exported (e.g. on a
 * periodic basis) using an {@link Exporter}, most implementations of which have
 * optimizations for sending data to remote repositories.
 * </p>
 * <p>
 * If Spring Messaging is on the classpath a {@link MessageChannel} called
 * "metricsChannel" is also created (unless one already exists) and all metric update
 * events are published additionally as messages on that channel. Additional analysis or
 * actions can be taken by clients subscribing to that channel.
 * </p>
 * <p>
 * In addition if Codahale's metrics library is on the classpath a {@link MetricRegistry}
 * will be created and wired up to the counter and gauge services in addition to the basic
 * repository. Users can create Codahale metrics by prefixing their metric names with the
 * appropriate type (e.g. "histogram.*", "meter.*") and sending them to the standard
 * <code>GaugeService</code> or <code>CounterService</code>.
 * </p>
 * <p>
 * By default all metric updates go to all {@link MetricWriter} instances in the
 * application context. To change this behaviour define your own metric writer bean called
 * "primaryMetricWriter", mark it <code>@Primary</code>, and this one will receive all
 * updates from the default counter and gauge services. Alternatively you can provide your
 * own counter and gauge services and wire them to whichever writer you choose.
 * </p>
 *
 * @see GaugeService
 * @see CounterService
 * @see MetricWriter
 * @see InMemoryMetricRepository
 * @see CodahaleMetricWriter
 * @see Exporter
 *
 * @author Dave Syer
 */
@Configuration
public class MetricRepositoryAutoConfiguration {

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

	@Configuration
	@ConditionalOnMissingBean(MetricRepository.class)
	static class MetricRepositoryConfiguration {

		@Bean
		public InMemoryMetricRepository metricRepository() {
			return new InMemoryMetricRepository();
		}

	}

	@Bean
	@ConditionalOnBean(RichGaugeReader.class)
	public PublicMetrics richGaugePublicMetrics(RichGaugeReader richGaugeReader) {
		return new RichGaugeReaderPublicMetrics(richGaugeReader);
	}

	@Configuration
	@ConditionalOnClass(MessageChannel.class)
	static class MetricsChannelConfiguration {

		@Autowired
		@Qualifier("metricsExecutor")
		private Executor executor;

		@Bean
		@ConditionalOnMissingBean(name = "metricsChannel")
		public SubscribableChannel metricsChannel() {
			return new ExecutorSubscribableChannel(this.executor);
		}

		@Bean
		@ConditionalOnMissingBean(name = "metricsExecutor")
		public Executor metricsExecutor() {
			ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
			return executor;
		}

		@Bean
		@Primary
		@ConditionalOnMissingBean(name = "primaryMetricWriter")
		public MetricWriter primaryMetricWriter(
				@Qualifier("metricsChannel") SubscribableChannel channel,
				List<MetricWriter> writers) {
			final MetricWriter observer = new CompositeMetricWriter(writers);
			channel.subscribe(new MetricWriterMessageHandler(observer));
			return new MessageChannelMetricWriter(channel);
		}

	}

	@Configuration
	@ConditionalOnClass(MetricRegistry.class)
	static class CodahaleMetricRegistryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		public MetricRegistry metricRegistry() {
			return new MetricRegistry();
		}

		@Bean
		public CodahaleMetricWriter codahaleMetricWriter(MetricRegistry metricRegistry) {
			return new CodahaleMetricWriter(metricRegistry);
		}

		@Bean
		@Primary
		@ConditionalOnMissingClass(name = "org.springframework.messaging.MessageChannel")
		@ConditionalOnMissingBean(name = "primaryMetricWriter")
		public MetricWriter primaryMetricWriter(List<MetricWriter> writers) {
			return new CompositeMetricWriter(writers);
		}

	}

}
