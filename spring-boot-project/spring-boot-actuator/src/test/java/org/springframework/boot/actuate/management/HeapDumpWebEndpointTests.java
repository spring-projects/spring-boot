/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.management;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HeapDumpWebEndpoint}.
 *
 * @author Andy Wilkinson
 */
public class HeapDumpWebEndpointTests {

	@Test
	public void parallelRequestProducesTooManyRequestsResponse()
			throws InterruptedException {
		CountDownLatch dumpingLatch = new CountDownLatch(1);
		CountDownLatch blockingLatch = new CountDownLatch(1);
		HeapDumpWebEndpoint slowEndpoint = new HeapDumpWebEndpoint(2500) {

			@Override
			protected HeapDumper createHeapDumper()
					throws HeapDumperUnavailableException {
				return (file, live) -> {
					dumpingLatch.countDown();
					blockingLatch.await();
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
