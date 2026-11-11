/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.info;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import org.springframework.boot.info.ProcessInfo.MemoryInfo;
import org.springframework.boot.info.ProcessInfo.MemoryInfo.MemoryUsageInfo;
import org.springframework.boot.info.ProcessInfo.VirtualThreadsInfo;

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
		if (ProcessHandle.current().info().startInstant().isPresent()) {
			assertThat(processInfo.getUptime()).isPositive();
			assertThat(processInfo.getStartTime()).isInThePast();
			assertThat(processInfo.getCurrentTime()).isAfter(processInfo.getStartTime());
		}
		else {
			assertThat(processInfo.getUptime()).isNull();
			assertThat(processInfo.getStartTime()).isNull();
			assertThat(processInfo.getCurrentTime()).isNotNull();
		}
		assertThat(processInfo.getTimezone()).isNotNull();
		assertThat(processInfo.getLocale()).isNotNull();
		assertThat(processInfo.getWorkingDirectory()).isNotBlank();
	}

	@Test
	void memoryInfoIsAvailable() {
		ProcessInfo processInfo = new ProcessInfo();
		MemoryUsageInfo heapUsageInfo = processInfo.getMemory().getHeap();
		assertThat(heapUsageInfo.getInit()).isPositive().isLessThanOrEqualTo(heapUsageInfo.getMax());
		assertThat(heapUsageInfo.getUsed()).isPositive().isLessThanOrEqualTo(heapUsageInfo.getCommitted());
		assertThat(heapUsageInfo.getCommitted()).isPositive().isLessThanOrEqualTo(heapUsageInfo.getMax());
		assertThat(heapUsageInfo.getMax()).isPositive();
		MemoryUsageInfo nonHeapUsageInfo = processInfo.getMemory().getNonHeap();
		assertThat(nonHeapUsageInfo.getInit()).isPositive();
		assertThat(nonHeapUsageInfo.getUsed()).isPositive().isLessThanOrEqualTo(nonHeapUsageInfo.getCommitted());
		assertThat(nonHeapUsageInfo.getCommitted()).isPositive();
		assertThat(nonHeapUsageInfo.getMax()).isEqualTo(-1);
		List<MemoryInfo.GarbageCollectorInfo> garbageCollectors = processInfo.getMemory().getGarbageCollectors();
		assertThat(garbageCollectors).isNotEmpty();
		assertThat(garbageCollectors).allSatisfy((garbageCollector) -> {
			assertThat(garbageCollector.getName()).isNotEmpty();
			assertThat(garbageCollector.getCollectionCount()).isNotNegative();
		});
	}

	@Test
	@EnabledForJreRange(min = JRE.JAVA_24)
	void virtualThreadsInfoIfAvailable() {
		ProcessInfo processInfo = new ProcessInfo();
		VirtualThreadsInfo virtualThreadsInfo = processInfo.getVirtualThreads();
		assertThat(virtualThreadsInfo).isNotNull();
		assertThat(virtualThreadsInfo.getMounted()).isGreaterThanOrEqualTo(0);
		assertThat(virtualThreadsInfo.getQueued()).isGreaterThanOrEqualTo(0);
		assertThat(virtualThreadsInfo.getParallelism()).isGreaterThan(0);
		assertThat(virtualThreadsInfo.getPoolSize()).isGreaterThanOrEqualTo(0);
	}

	@Test
	@EnabledForJreRange(max = JRE.JAVA_23)
	void virtualThreadsInfoIfNotAvailable() {
		ProcessInfo processInfo = new ProcessInfo();
		assertThat(processInfo.getVirtualThreads()).isNull();
	}

}
