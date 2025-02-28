/*
 * Copyright 2012-2025 the original author or authors.
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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;

import org.springframework.util.ClassUtils;

/**
 * Information about the process of the application.
 *
 * @author Jonatan Ivanov
 * @author Andrey Litvitski
 * @since 3.3.0
 */
public class ProcessInfo {

	private static final String VIRTUAL_THREAD_SCHEDULER_CLASS = "jdk.management.VirtualThreadSchedulerMXBean";

	private static final boolean VIRTUAL_THREAD_SCHEDULER_CLASS_PRESENT = ClassUtils
		.isPresent(VIRTUAL_THREAD_SCHEDULER_CLASS, null);

	private static final Runtime runtime = Runtime.getRuntime();

	private final long pid;

	private final long parentPid;

	private final String owner;

	public ProcessInfo() {
		ProcessHandle process = ProcessHandle.current();
		this.pid = process.pid();
		this.parentPid = process.parent().map(ProcessHandle::pid).orElse(-1L);
		this.owner = process.info().user().orElse(null);
	}

	/**
	 * Number of processors available to the process. This value may change between
	 * invocations especially in (containerized) environments where resource usage can be
	 * isolated (for example using control groups).
	 * @return result of {@link Runtime#availableProcessors()}
	 * @see Runtime#availableProcessors()
	 */
	public int getCpus() {
		return runtime.availableProcessors();
	}

	/**
	 * Memory information for the process. These values can provide details about the
	 * current memory usage and limits selected by the user or JVM ergonomics (init, max,
	 * committed, used for heap and non-heap). If limits not set explicitly, it might not
	 * be trivial to know what these values are runtime; especially in (containerized)
	 * environments where resource usage can be isolated (for example using control
	 * groups) or not necessarily trivial to discover. Other than that, these values can
	 * indicate if the JVM can resize the heap (stop-the-world).
	 * @return heap and non-heap memory information
	 * @since 3.4.0
	 * @see MemoryMXBean#getHeapMemoryUsage()
	 * @see MemoryMXBean#getNonHeapMemoryUsage()
	 * @see MemoryUsage
	 */
	public MemoryInfo getMemory() {
		return new MemoryInfo();
	}

	/**
	 * Virtual threads information for the process. These values provide details about the
	 * current state of virtual threads, including the number of mounted threads, queued
	 * threads, the parallelism level, and the thread pool size.
	 * @return an instance of {@link VirtualThreadsInfo} containing information about
	 * virtual threads, or {@code null} if the VirtualThreadSchedulerMXBean is not
	 * available
	 * @since 3.5.0
	 */
	@SuppressWarnings("unchecked")
	public VirtualThreadsInfo getVirtualThreads() {
		if (!VIRTUAL_THREAD_SCHEDULER_CLASS_PRESENT) {
			return null;
		}
		try {
			Class<PlatformManagedObject> mxbeanClass = (Class<PlatformManagedObject>) ClassUtils
				.forName(VIRTUAL_THREAD_SCHEDULER_CLASS, null);
			PlatformManagedObject mxbean = ManagementFactory.getPlatformMXBean(mxbeanClass);
			int mountedVirtualThreadCount = invokeMethod(mxbeanClass, mxbean, "getMountedVirtualThreadCount");
			long queuedVirtualThreadCount = invokeMethod(mxbeanClass, mxbean, "getQueuedVirtualThreadCount");
			int parallelism = invokeMethod(mxbeanClass, mxbean, "getParallelism");
			int poolSize = invokeMethod(mxbeanClass, mxbean, "getPoolSize");
			return new VirtualThreadsInfo(mountedVirtualThreadCount, queuedVirtualThreadCount, parallelism, poolSize);
		}
		catch (ReflectiveOperationException ex) {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T invokeMethod(Class<?> mxbeanClass, Object mxbean, String name) throws ReflectiveOperationException {
		Method method = mxbeanClass.getMethod(name);
		return (T) method.invoke(mxbean);
	}

	public long getPid() {
		return this.pid;
	}

	public long getParentPid() {
		return this.parentPid;
	}

	public String getOwner() {
		return this.owner;
	}

	/**
	 * Virtual threads information.
	 *
	 * @since 3.5.0
	 */
	public static class VirtualThreadsInfo {

		private final int mounted;

		private final long queued;

		private final int parallelism;

		private final int poolSize;

		VirtualThreadsInfo(int mounted, long queued, int parallelism, int poolSize) {
			this.mounted = mounted;
			this.queued = queued;
			this.parallelism = parallelism;
			this.poolSize = poolSize;
		}

		public int getMounted() {
			return this.mounted;
		}

		public long getQueued() {
			return this.queued;
		}

		public int getParallelism() {
			return this.parallelism;
		}

		public int getPoolSize() {
			return this.poolSize;
		}

	}

	/**
	 * Memory information.
	 *
	 * @since 3.4.0
	 */
	public static class MemoryInfo {

		private static final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

		private final MemoryUsageInfo heap;

		private final MemoryUsageInfo nonHeap;

		MemoryInfo() {
			this.heap = new MemoryUsageInfo(memoryMXBean.getHeapMemoryUsage());
			this.nonHeap = new MemoryUsageInfo(memoryMXBean.getNonHeapMemoryUsage());
		}

		public MemoryUsageInfo getHeap() {
			return this.heap;
		}

		public MemoryUsageInfo getNonHeap() {
			return this.nonHeap;
		}

		public static class MemoryUsageInfo {

			private final MemoryUsage memoryUsage;

			MemoryUsageInfo(MemoryUsage memoryUsage) {
				this.memoryUsage = memoryUsage;
			}

			public long getInit() {
				return this.memoryUsage.getInit();
			}

			public long getUsed() {
				return this.memoryUsage.getUsed();
			}

			public long getCommitted() {
				return this.memoryUsage.getCommitted();
			}

			public long getMax() {
				return this.memoryUsage.getMax();
			}

		}

	}

}
