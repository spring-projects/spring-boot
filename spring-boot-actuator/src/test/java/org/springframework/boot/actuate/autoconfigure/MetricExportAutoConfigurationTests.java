/*
 * Copyright 2012-2016 the original author or authors.
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

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Matchers;
import org.mockito.Mockito;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.actuate.endpoint.MetricsEndpointMetricReader;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.export.MetricCopyExporter;
import org.springframework.boot.actuate.metrics.export.MetricExporters;
import org.springframework.boot.actuate.metrics.statsd.StatsdMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.util.EnvironmentTestUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricExportAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 * @author Simon Buettner
 */
public class MetricExportAutoConfigurationTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private AnnotationConfigApplicationContext context;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void metricsFlushAutomatically() throws Exception {
		this.context = new AnnotationConfigApplicationContext(WriterConfig.class,
				MetricRepositoryAutoConfiguration.class,
				MetricExportAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		GaugeService gaugeService = this.context.getBean(GaugeService.class);
		assertThat(gaugeService).isNotNull();
		gaugeService.submit("foo", 2.7);
		MetricExporters flusher = this.context.getBean(MetricExporters.class);
		flusher.close(); // this will be called by Spring on shutdown
		MetricWriter writer = this.context.getBean("writer", MetricWriter.class);
		Mockito.verify(writer, Mockito.atLeastOnce()).set(Matchers.any(Metric.class));
	}

	@Test
	public void defaultExporterWhenMessageChannelAvailable() throws Exception {
		this.context = new AnnotationConfigApplicationContext(
				MessageChannelConfiguration.class,
				MetricRepositoryAutoConfiguration.class,
				MetricsChannelAutoConfiguration.class,
				MetricExportAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		MetricExporters exporter = this.context.getBean(MetricExporters.class);
		assertThat(exporter).isNotNull();
		assertThat(exporter.getExporters()).containsKey("messageChannelMetricWriter");
	}

	@Test
	public void provideAdditionalWriter() {
		this.context = new AnnotationConfigApplicationContext(WriterConfig.class,
				MetricRepositoryAutoConfiguration.class,
				MetricExportAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		GaugeService gaugeService = this.context.getBean(GaugeService.class);
		assertThat(gaugeService).isNotNull();
		gaugeService.submit("foo", 2.7);
		MetricExporters exporters = this.context.getBean(MetricExporters.class);
		MetricCopyExporter exporter = (MetricCopyExporter) exporters.getExporters()
				.get("writer");
		exporter.setIgnoreTimestamps(true);
		exporter.export();
		MetricWriter writer = this.context.getBean("writer", MetricWriter.class);
		Mockito.verify(writer, Mockito.atLeastOnce()).set(Matchers.any(Metric.class));
	}

	@Test
	public void exportMetricsEndpoint() {
		this.context = new AnnotationConfigApplicationContext(WriterConfig.class,
				MetricEndpointConfiguration.class, MetricExportAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		MetricExporters exporters = this.context.getBean(MetricExporters.class);
		MetricCopyExporter exporter = (MetricCopyExporter) exporters.getExporters()
				.get("writer");
		exporter.setIgnoreTimestamps(true);
		exporter.export();
		MetricsEndpointMetricReader reader = this.context.getBean("endpointReader",
				MetricsEndpointMetricReader.class);
		Mockito.verify(reader, Mockito.atLeastOnce()).findAll();
	}

	@Test
	public void statsdMissingHost() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		this.context.register(WriterConfig.class, MetricEndpointConfiguration.class,
				MetricExportAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		this.thrown.expect(NoSuchBeanDefinitionException.class);
		this.context.getBean(StatsdMetricWriter.class);
	}

	@Test
	public void statsdWithHost() throws Exception {
		this.context = new AnnotationConfigApplicationContext();
		EnvironmentTestUtils.addEnvironment(this.context,
				"spring.metrics.export.statsd.host=localhost");
		this.context.register(WriterConfig.class, MetricEndpointConfiguration.class,
				MetricExportAutoConfiguration.class,
				PropertyPlaceholderAutoConfiguration.class);
		this.context.refresh();
		assertThat(this.context.getBean(StatsdMetricWriter.class)).isNotNull();
	}

	@Configuration
	public static class MessageChannelConfiguration {

		@Bean
		public SubscribableChannel metricsChannel() {
			return new FixedSubscriberChannel(new MessageHandler() {

				@Override
				public void handleMessage(Message<?> message) throws MessagingException {
				}

			});
		}

	}

	@Configuration
	public static class WriterConfig {

		@Bean
		@ExportMetricWriter
		public MetricWriter writer() {
			return Mockito.mock(MetricWriter.class);
		}

	}

	@Configuration
	public static class MetricEndpointConfiguration {

		@Bean
		@ExportMetricReader
		public MetricsEndpointMetricReader endpointReader() {
			return Mockito.mock(MetricsEndpointMetricReader.class);
		}

	}

}
