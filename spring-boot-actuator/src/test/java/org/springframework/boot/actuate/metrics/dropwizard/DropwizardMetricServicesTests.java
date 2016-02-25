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

package org.springframework.boot.actuate.metrics.dropwizard;

import java.util.ArrayList;
import java.util.List;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DropwizardMetricServices}.
 *
 * @author Dave Syer
 */
public class DropwizardMetricServicesTests {

	private final MetricRegistry registry = new MetricRegistry();
	private final DropwizardMetricServices writer = new DropwizardMetricServices(
			this.registry);

	@Test
	public void incrementCounter() {
		this.writer.increment("foo");
		this.writer.increment("foo");
		this.writer.increment("foo");
		assertThat(this.registry.counter("counter.foo").getCount()).isEqualTo(3);
	}

	@Test
	public void updatePredefinedMeter() {
		this.writer.increment("meter.foo");
		this.writer.increment("meter.foo");
		this.writer.increment("meter.foo");
		assertThat(this.registry.meter("meter.foo").getCount()).isEqualTo(3);
	}

	@Test
	public void updatePredefinedCounter() {
		this.writer.increment("counter.foo");
		this.writer.increment("counter.foo");
		this.writer.increment("counter.foo");
		assertThat(this.registry.counter("counter.foo").getCount()).isEqualTo(3);
	}

	@Test
	public void setGauge() {
		this.writer.submit("foo", 2.1);
		@SuppressWarnings("unchecked")
		Gauge<Double> gauge = (Gauge<Double>) this.registry.getMetrics().get("gauge.foo");
		assertThat(gauge.getValue()).isEqualTo(new Double(2.1));
		this.writer.submit("foo", 2.3);
		assertThat(gauge.getValue()).isEqualTo(new Double(2.3));
	}

	@Test
	public void setPredefinedTimer() {
		this.writer.submit("timer.foo", 200);
		this.writer.submit("timer.foo", 300);
		assertThat(this.registry.timer("timer.foo").getCount()).isEqualTo(2);
	}

	@Test
	public void setPredefinedHistogram() {
		this.writer.submit("histogram.foo", 2.1);
		this.writer.submit("histogram.foo", 2.3);
		assertThat(this.registry.histogram("histogram.foo").getCount()).isEqualTo(2);
	}

	/**
	 * Test the case where a given writer is used amongst several threads where each
	 * thread is updating the same set of metrics. This would be an example case of the
	 * writer being used with the MetricsFilter handling several requests/sec to the same
	 * URL.
	 *
	 * @throws Exception if an error occurs
	 */
	@Test
	public void testParallelism() throws Exception {
		List<WriterThread> threads = new ArrayList<WriterThread>();
		ThreadGroup group = new ThreadGroup("threads");
		for (int i = 0; i < 10; i++) {
			WriterThread thread = new WriterThread(group, i, this.writer);
			threads.add(thread);
			thread.start();
		}

		while (group.activeCount() > 0) {
			Thread.sleep(1000);
		}

		for (WriterThread thread : threads) {
			assertThat(thread.isFailed())
					.as("expected thread caused unexpected exception").isFalse();
		}
	}

	public static class WriterThread extends Thread {

		private int index;

		private boolean failed;

		private DropwizardMetricServices writer;

		public WriterThread(ThreadGroup group, int index,
				DropwizardMetricServices writer) {
			super(group, "Writer-" + index);
			this.index = index;
			this.writer = writer;
		}

		public boolean isFailed() {
			return this.failed;
		}

		@Override
		public void run() {
			for (int i = 0; i < 10000; i++) {
				try {
					this.writer.submit("timer.test.service", this.index);
					this.writer.submit("histogram.test.service", this.index);
					this.writer.submit("gauge.test.service", this.index);
				}
				catch (IllegalArgumentException ex) {
					this.failed = true;
					throw ex;
				}
			}
		}

	}

}
