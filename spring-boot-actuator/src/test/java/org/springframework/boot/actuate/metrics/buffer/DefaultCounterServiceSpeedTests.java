/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.metrics.buffer;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;

import org.junit.BeforeClass;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.repository.InMemoryMetricRepository;
import org.springframework.boot.actuate.metrics.writer.DefaultCounterService;
import org.springframework.lang.UsesJava8;
import org.springframework.util.StopWatch;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Speed tests for {@link DefaultCounterService}.
 *
 * @author Dave Syer
 */
@RunWith(Theories.class)
@UsesJava8
public class DefaultCounterServiceSpeedTests {

	@DataPoints
	public static String[] values = new String[10];

	public static String[] names = new String[] { "foo", "bar", "spam", "bucket" };

	public static String[] sample = new String[1000];

	private InMemoryMetricRepository repository = new InMemoryMetricRepository();

	private CounterService counterService = new DefaultCounterService(this.repository);

	private MetricReader reader = this.repository;

	private static int threadCount = 2;

	private static final int number = (Boolean.getBoolean("performance.test") ? 2000000
			: 1000000);

	private static int count;

	private static StopWatch watch = new StopWatch("count");

	private static PrintWriter err;

	@BeforeClass
	public static void prime() throws FileNotFoundException {
		err = new NullPrintWriter();
		final Random random = new Random();
		for (int i = 0; i < 1000; i++) {
			sample[i] = names[random.nextInt(names.length)];
		}
	}

	@Theory
	public void counters(String input) throws Exception {
		watch.start("counters" + count++);
		ExecutorService pool = Executors.newFixedThreadPool(threadCount);
		Runnable task = new Runnable() {
			@Override
			public void run() {
				for (int i = 0; i < number; i++) {
					String name = sample[i % sample.length];
					DefaultCounterServiceSpeedTests.this.counterService.increment(name);
				}
			}
		};
		Collection<Future<?>> futures = new HashSet<Future<?>>();
		for (int i = 0; i < threadCount; i++) {
			futures.add(pool.submit(task));
		}
		for (Future<?> future : futures) {
			future.get();
		}
		watch.stop();
		double rate = number / watch.getLastTaskTimeMillis() * 1000;
		System.err.println("Counters rate(" + count + ")=" + rate + ", " + watch);
		watch.start("read" + count);
		this.reader.findAll().forEach(new Consumer<Metric<?>>() {
			@Override
			public void accept(Metric<?> metric) {
				err.println(metric);
			}
		});
		final LongAdder total = new LongAdder();
		this.reader.findAll().forEach(new Consumer<Metric<?>>() {
			@Override
			public void accept(Metric<?> value) {
				total.add(value.getValue().intValue());
			}
		});
		watch.stop();
		System.err.println("Read(" + count + ")=" + watch.getLastTaskTimeMillis() + "ms");
		assertThat(total.longValue()).isEqualTo(number * threadCount);
	}

}
