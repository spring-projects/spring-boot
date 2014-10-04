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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executor;

import org.junit.Test;
import org.springframework.boot.actuate.endpoint.RichGaugeReaderPublicMetrics;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.rich.RichGaugeReader;
import org.springframework.boot.actuate.metrics.writer.DefaultCounterService;
import org.springframework.boot.actuate.metrics.writer.DefaultGaugeService;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.support.ExecutorSubscribableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link MetricRepositoryAutoConfiguration}.
 *
 * @author Phillip Webb
 * @author Dave Syer
 */
public class MetricRepositoryAutoConfigurationTests {

	@Test
	public void defaultExecutor() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MetricRepositoryAutoConfiguration.class);
		ExecutorSubscribableChannel channel = context
				.getBean(ExecutorSubscribableChannel.class);
		ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) channel.getExecutor();
		context.close();
		assertTrue(executor.getThreadPoolExecutor().isShutdown());
	}

	@Test
	public void createServices() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				SyncTaskExecutorConfiguration.class,
				MetricRepositoryAutoConfiguration.class);
		DefaultGaugeService gaugeService = context.getBean(DefaultGaugeService.class);
		assertNotNull(gaugeService);
		assertNotNull(context.getBean(DefaultCounterService.class));
		gaugeService.submit("foo", 2.7);
		assertEquals(2.7, context.getBean(MetricReader.class).findOne("gauge.foo")
				.getValue());
		context.close();
	}

	@Test
	public void provideAdditionalWriter() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				SyncTaskExecutorConfiguration.class, WriterConfig.class,
				MetricRepositoryAutoConfiguration.class);
		DefaultGaugeService gaugeService = context.getBean(DefaultGaugeService.class);
		assertNotNull(gaugeService);
		gaugeService.submit("foo", 2.7);
		MetricWriter writer = context.getBean("writer", MetricWriter.class);
		verify(writer).set(any(Metric.class));
		context.close();
	}

	@Test
	public void codahaleInstalledIfPresent() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				SyncTaskExecutorConfiguration.class, WriterConfig.class,
				MetricRepositoryAutoConfiguration.class);
		DefaultGaugeService gaugeService = context.getBean(DefaultGaugeService.class);
		assertNotNull(gaugeService);
		gaugeService.submit("foo", 2.7);
		MetricRegistry registry = context.getBean(MetricRegistry.class);
		@SuppressWarnings("unchecked")
		Gauge<Double> gauge = (Gauge<Double>) registry.getMetrics().get("gauge.foo");
		assertEquals(new Double(2.7), gauge.getValue());
		context.close();
	}

	@Test
	public void skipsIfBeansExist() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, MetricRepositoryAutoConfiguration.class);
		assertThat(context.getBeansOfType(DefaultGaugeService.class).size(), equalTo(0));
		assertThat(context.getBeansOfType(DefaultCounterService.class).size(), equalTo(0));
		context.close();
	}

	@Test
	public void richGaugePublicMetrics() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				RichGaugeReaderConfig.class, MetricRepositoryAutoConfiguration.class);

		RichGaugeReader richGaugeReader = context.getBean(RichGaugeReader.class);
		assertNotNull(richGaugeReader);
		when(richGaugeReader.findAll()).thenReturn(
				Collections.singletonList(new RichGauge("bar", 3.7d)));

		RichGaugeReaderPublicMetrics publicMetrics = context
				.getBean(RichGaugeReaderPublicMetrics.class);
		assertNotNull(publicMetrics);

		Collection<Metric<?>> metrics = publicMetrics.metrics();
		assertNotNull(metrics);
		assertEquals(metrics.size(), 6);

		assertHasMetric(metrics, new Metric<Double>("bar.val", 3.7d));
		assertHasMetric(metrics, new Metric<Double>("bar.avg", 3.7d));
		assertHasMetric(metrics, new Metric<Double>("bar.min", 3.7d));
		assertHasMetric(metrics, new Metric<Double>("bar.max", 3.7d));
		assertHasMetric(metrics, new Metric<Double>("bar.alpha", -1.d));
		assertHasMetric(metrics, new Metric<Long>("bar.count", 1L));

		context.close();
	}

	private void assertHasMetric(Collection<Metric<?>> metrics, Metric<?> metric) {
		for (Metric<?> m : metrics) {
			if (m.getValue().equals(metric.getValue())
					&& m.getName().equals(metric.getName())) {
				return;
			}
		}
		fail("Metric " + metric.toString() + " not found in " + metrics.toString());
	}

	@Configuration
	public static class SyncTaskExecutorConfiguration {

		@Bean
		public Executor metricsExecutor() {
			return new SyncTaskExecutor();
		}

	}

	@Configuration
	public static class WriterConfig {

		@Bean
		public MetricWriter writer() {
			return mock(MetricWriter.class);
		}

	}

	@Configuration
	public static class Config {

		@Bean
		public GaugeService gaugeService() {
			return mock(GaugeService.class);
		}

		@Bean
		public CounterService counterService() {
			return mock(CounterService.class);
		}

	}

	@Configuration
	static class RichGaugeReaderConfig {
		@Bean
		public RichGaugeReader richGaugeReader() {
			return mock(RichGaugeReader.class);
		}
	}
}
