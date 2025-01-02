/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.info;

import org.junit.jupiter.api.Test;

import org.springframework.boot.info.ProcessInfo.MemoryInfo.MemoryUsageInfo;
import org.springframework.boot.info.ProcessInfo.VirtualThreadsInfo;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ProcessInfo}.
 *
 * @author Jonatan Ivanov
 * @author Andrey Litvitski
 */
class ProcessInfoTests {

	@Test
	void processInfoIsAvailable() {
		ProcessInfo processInfo = new ProcessInfo();
		assertThat(processInfo.getCpus()).isEqualTo(Runtime.getRuntime().availableProcessors());
		assertThat(processInfo.getOwner()).isEqualTo(ProcessHandle.current().info().user().orElse(null));
		assertThat(processInfo.getPid()).isEqualTo(ProcessHandle.current().pid());
		assertThat(processInfo.getParentPid())
			.isEqualTo(ProcessHandle.current().parent().map(ProcessHandle::pid).orElse(null));
	}

	@Test
	void memoryInfoIsAvailable() {
		ProcessInfo processInfo = new ProcessInfo();
		MemoryUsageInfo heapUsageInfo = processInfo.getMemory().getHeap();
		MemoryUsageInfo nonHeapUsageInfo = processInfo.getMemory().getNonHeap();
		assertThat(heapUsageInfo.getInit()).isPositive().isLessThanOrEqualTo(heapUsageInfo.getMax());
		assertThat(heapUsageInfo.getUsed()).isPositive().isLessThanOrEqualTo(heapUsageInfo.getCommitted());
		assertThat(heapUsageInfo.getCommitted()).isPositive().isLessThanOrEqualTo(heapUsageInfo.getMax());
		assertThat(heapUsageInfo.getMax()).isPositive();
		assertThat(nonHeapUsageInfo.getInit()).isPositive();
		assertThat(nonHeapUsageInfo.getUsed()).isPositive().isLessThanOrEqualTo(heapUsageInfo.getCommitted());
		assertThat(nonHeapUsageInfo.getCommitted()).isPositive();
		assertThat(nonHeapUsageInfo.getMax()).isEqualTo(-1);
	}

	@Test
	void virtualThreadsInfoIsNullWhenMXBeanIsNotAccessible() {
		if (ClassUtils.isPresent("jdk.management.VirtualThreadSchedulerMXBean", null)) {
			ProcessInfo processInfo = new ProcessInfo();

			VirtualThreadsInfo virtualThreadsInfo = processInfo.getVirtualThreads();

			assertThat(virtualThreadsInfo).isNotNull();
			assertThat(virtualThreadsInfo.getMounted()).isGreaterThanOrEqualTo(0);
			assertThat(virtualThreadsInfo.getQueued()).isGreaterThanOrEqualTo(0);
			assertThat(virtualThreadsInfo.getParallelism()).isGreaterThan(0);
			assertThat(virtualThreadsInfo.getPoolSize()).isGreaterThan(0);
		} else {
			ProcessInfo processInfo = new ProcessInfo();

			assertThat(processInfo.getVirtualThreads()).isNull();
		}
	}

}
