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

package org.springframework.boot.info;

import java.lang.ProcessHandle.Info;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.PlatformManagedObject;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.Nullable;

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

	private final @Nullable String owner;

	private final @Nullable Instant startTime;

	private final ZoneId timezone;

	private final Locale locale;

	private final String workingDirectory;

	public ProcessInfo() {
		ProcessHandle process = ProcessHandle.current();
		this.pid = process.pid();
		this.parentPid = process.parent().map(ProcessHandle::pid).orElse(-1L);
		this.owner = process.info().user().orElse(null);
		this.startTime = process.info().startInstant().orElse(null);
		this.timezone = ZoneId.systemDefault();
		this.locale = Locale.getDefault();
		this.workingDirectory = Path.of(".").toAbsolutePath().normalize().toString();
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
	public @Nullable VirtualThreadsInfo getVirtualThreads() {
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
		catch (Exception ex) {
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

	public @Nullable String getOwner() {
		return this.owner;
	}

	/**
	 * Uptime of the process. Can be useful to see how long the process has been running
	 * and to check how long ago the last deployment or restart happened.
	 * @return duration since the process started, if available, otherwise {@code null}
	 * @since 4.1.0
	 */
	public @Nullable Duration getUptime() {
		return (this.startTime != null) ? Duration.between(this.startTime, Instant.now()) : null;
	}

	/**
	 * Time at which the process started. Can be useful to see when the process was
	 * started and to check when the last deployment or restart happened.
	 * @return the time when the process started, if available, otherwise {@code null}
	 * @since 4.1.0
	 * @see Info#startInstant()
	 */
	public @Nullable Instant getStartTime() {
		return this.startTime;
	}

	/**
	 * Current time of the process. Can be useful to check if there is any clock-skew
	 * issue and if the current time that the process knows is accurate enough.
	 * @return the current time of the process
	 * @since 4.1.0
	 * @see Instant#now
	 */
	public Instant getCurrentTime() {
		return Instant.now();
	}

	/**
	 * Timezone of the process. Can help to detect time and timezone related issues.
	 * @return the timezone of the process
	 * @since 4.1.0
	 * @see ZoneId#systemDefault()
	 */
	public ZoneId getTimezone() {
		return this.timezone;
	}

	/**
	 * Locale of the process. Can help to detect issues connected to language and country
	 * settings.
	 * @return the locale of the process
	 * @since 4.1.0
	 * @see Locale#getDefault()
	 */
	public Locale getLocale() {
		return this.locale;
	}

	/**
	 * Working directory of the process. Can help to locate files that the process uses.
	 * @return the absolute path of the working directory of the process
	 * @since 4.1.0
	 */
	public String getWorkingDirectory() {
		return this.workingDirectory;
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

		private static final List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory
			.getGarbageCollectorMXBeans();

		private final MemoryUsageInfo heap;

		private final MemoryUsageInfo nonHeap;

		private final List<GarbageCollectorInfo> garbageCollectors;

		MemoryInfo() {
			this.heap = new MemoryUsageInfo(memoryMXBean.getHeapMemoryUsage());
			this.nonHeap = new MemoryUsageInfo(memoryMXBean.getNonHeapMemoryUsage());
			this.garbageCollectors = garbageCollectorMXBeans.stream().map(GarbageCollectorInfo::new).toList();
		}

		public MemoryUsageInfo getHeap() {
			return this.heap;
		}

		public MemoryUsageInfo getNonHeap() {
			return this.nonHeap;
		}

		/**
		 * Garbage Collector information for the process. This list provides details about
		 * the currently used GC algorithms selected by the user or JVM ergonomics. It
		 * might not be trivial to know the used GC algorithms since that usually depends
		 * on the {@link Runtime#availableProcessors()} (see:
		 * {@link ProcessInfo#getCpus()}) and the available memory (see:
		 * {@link MemoryUsageInfo}).
		 * @return {@link List} of {@link GarbageCollectorInfo}.
		 * @since 3.5.0
		 */
		public List<GarbageCollectorInfo> getGarbageCollectors() {
			return this.garbageCollectors;
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

		/**
		 * Garbage collection information.
		 *
		 * @since 3.5.0
		 */
		public static class GarbageCollectorInfo {

			private final String name;

			private final long collectionCount;

			GarbageCollectorInfo(GarbageCollectorMXBean garbageCollectorMXBean) {
				this.name = garbageCollectorMXBean.getName();
				this.collectionCount = garbageCollectorMXBean.getCollectionCount();
			}

			public String getName() {
				return this.name;
			}

			public long getCollectionCount() {
				return this.collectionCount;
			}

		}

	}

}
