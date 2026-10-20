/*
 * Copyright 2012-present the original author or authors.
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
 *
 * @author Challa Srinivasa Reddy
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
