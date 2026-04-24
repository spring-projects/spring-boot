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

package org.springframework.boot.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import com.sun.management.ThreadMXBean;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Performance smoke tests for {@link WritableJson}.
 *
 * @author Marcus Schiesser
 */
class WritableJsonPerformanceTests {

	private static final int WARMUP_ITERATIONS = 5_000;

	private static final int MEASURED_ITERATIONS = 50_000;

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private static volatile int consumed;

	@AfterEach
	void clearWritableJsonBytes() {
		WritableJsonBytes.clear();
	}

	@Test
	void toByteArrayComparesLegacyAndOptimizedImplementations() {
		for (Payload payload : Payload.all()) {
			WritableJson writable = payload::writeTo;
			assertThat(writable.toByteArray(CHARSET)).isEqualTo(toByteArrayWithLegacyImplementation(writable, CHARSET));
			Result legacy = measure(() -> toByteArrayWithLegacyImplementation(writable, CHARSET));
			WritableJsonBytes.clear();
			Result optimized = measure(() -> writable.toByteArray(CHARSET));
			System.out.printf("%s: legacy=%s/%s allocated, optimized=%s/%s allocated%n", payload.name(),
					legacy.duration(), legacy.allocated(), optimized.duration(), optimized.allocated());
		}
	}

	private static byte[] toByteArrayWithLegacyImplementation(WritableJson writableJson, Charset charset) {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			writableJson.toWriter(new OutputStreamWriter(out, charset));
			return out.toByteArray();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static Result measure(Operation operation) {
		for (int i = 0; i < WARMUP_ITERATIONS; i++) {
			consume(operation.run());
		}
		ThreadAllocation allocation = ThreadAllocation.get();
		long allocatedBefore = allocation.bytes();
		long start = System.nanoTime();
		for (int i = 0; i < MEASURED_ITERATIONS; i++) {
			consume(operation.run());
		}
		Duration duration = Duration.ofNanos(System.nanoTime() - start);
		long allocated = allocation.bytesSince(allocatedBefore);
		return new Result(duration, allocated);
	}

	private static void consume(byte[] bytes) {
		consumed = bytes.length;
	}

	private record Payload(String name, String json) {

		private static List<Payload> all() {
			return List.of(new Payload("small", "{\"message\":\"test\"}"),
					new Payload("structured-log",
							"""
									{"@timestamp":"2026-04-24T10:15:30.123Z","@version":"1","message":"Application started","logger_name":"com.example.Application","thread_name":"main","level":"INFO","level_value":20000}
									"""),
					new Payload("exception",
							"""
									{"@timestamp":"2026-04-24T10:15:30.123Z","message":"Request failed","stack_trace":"java.lang.IllegalStateException: Test failure\\n\\tat com.example.Service.call(Service.java:42)\\n\\tat com.example.Controller.handle(Controller.java:21)"}
									"""));
		}

		private void writeTo(Appendable out) throws IOException {
			out.append(this.json);
		}

	}

	private record Result(Duration duration, long allocatedBytes) {

		private String allocated() {
			return (this.allocatedBytes >= 0) ? this.allocatedBytes + "B" : "unavailable";
		}

	}

	@FunctionalInterface
	private interface Operation {

		byte[] run();

	}

	private static final class ThreadAllocation {

		private static final ThreadAllocation NONE = new ThreadAllocation(null);

		private final @Nullable ThreadMXBean threadBean;

		private ThreadAllocation(@Nullable ThreadMXBean threadBean) {
			this.threadBean = threadBean;
		}

		private static ThreadAllocation get() {
			java.lang.management.ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
			if (threadBean instanceof ThreadMXBean allocatingThreadBean
					&& allocatingThreadBean.isThreadAllocatedMemorySupported()) {
				if (!allocatingThreadBean.isThreadAllocatedMemoryEnabled()) {
					allocatingThreadBean.setThreadAllocatedMemoryEnabled(true);
				}
				return new ThreadAllocation(allocatingThreadBean);
			}
			return NONE;
		}

		private long bytes() {
			return (this.threadBean != null) ? this.threadBean.getCurrentThreadAllocatedBytes() : -1;
		}

		private long bytesSince(long before) {
			long bytes = bytes();
			return (before >= 0 && bytes >= 0) ? bytes - before : -1;
		}

	}

}
