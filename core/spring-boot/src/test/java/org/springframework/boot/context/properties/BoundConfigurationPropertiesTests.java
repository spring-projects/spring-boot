/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.context.properties;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.boot.context.properties.source.ConfigurationProperty;
import org.springframework.boot.context.properties.source.ConfigurationPropertyName;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link BoundConfigurationProperties}.
 */
class BoundConfigurationPropertiesTests {

	@Test
	void addWhenCalledConcurrentlyRecordsAllProperties() throws Exception {
		BoundConfigurationProperties bound = new BoundConfigurationProperties();
		int threads = 8;
		int propertiesPerThread = 5000;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		try {
			CountDownLatch start = new CountDownLatch(1);
			Future<?>[] futures = new Future<?>[threads];
			for (int thread = 0; thread < threads; thread++) {
				String prefix = "thread" + thread;
				futures[thread] = executor.submit(() -> {
					start.await();
					for (int i = 0; i < propertiesPerThread; i++) {
						ConfigurationPropertyName name = ConfigurationPropertyName.of(prefix + ".property" + i);
						bound.add(new ConfigurationProperty(name, "value", null));
					}
					return null;
				});
			}
			start.countDown();
			for (Future<?> future : futures) {
				future.get(30, TimeUnit.SECONDS);
			}
		}
		finally {
			executor.shutdownNow();
		}
		assertThat(bound.getAll().size()).isEqualTo(threads * propertiesPerThread);
	}

}
