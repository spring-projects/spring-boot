/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.management;

import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HeapDumpWebEndpoint}.
 *
 * @author Andy Wilkinson
 */
class HeapDumpWebEndpointTests {

	@Test
	void parallelRequestProducesTooManyRequestsResponse() throws InterruptedException {
		CountDownLatch dumpingLatch = new CountDownLatch(1);
		CountDownLatch blockingLatch = new CountDownLatch(1);
		HeapDumpWebEndpoint slowEndpoint = new HeapDumpWebEndpoint(2500) {

			@Override
			protected HeapDumper createHeapDumper() {
				return (live) -> {
					dumpingLatch.countDown();
					blockingLatch.await();
					return Files.createTempFile("heap-", ".dump").toFile();
				};
			}

		};
		Thread thread = new Thread(() -> slowEndpoint.heapDump(true));
		thread.start();
		dumpingLatch.await();
		assertThat(slowEndpoint.heapDump(true).getStatus()).isEqualTo(429);
		blockingLatch.countDown();
		thread.join();
	}

}
