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

package org.springframework.boot.actuate.health;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;
import javax.management.openmbean.CompositeData;

import com.sun.management.GarbageCollectionNotificationInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Low-level implementation of low memory detection used by {@link MemoryHealthIndicator}.
 *
 * <p>
 * Detects low memory condition in the tenured space by using JMX API for consuming memory
 * usage and garbage collection notification events.
 * </p>
 * <p>
 * Uses <a href=
 * "http://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryPoolMXBean.html#CollectionThreshold">MemoryPoolMXBean's
 * JMX API</a> to set collection usage threshold. The listener is registered to <a href=
 * "http://docs.oracle.com/javase/8/docs/api/java/lang/management/MemoryMXBean.html">MemoryMXBean</a>.
 * </p>
 * <p>
 * The Oracle / OpenJDK specific <a href=
 * "https://docs.oracle.com/javase/8/docs/jre/api/management/extension/com/sun/management/GarbageCollectionNotificationInfo.html">Garbage
 * collection notification</a> API is also used. This is used to detect if memory usage
 * drops below the threshold after crossing the threshold. On JVMs where this is not
 * available, the memory usage is polled each time the health check is requested to check
 * if the memory usage has returned back to healthy.
 * </p>
 *
 * @author Lari Hotari
 */
class LowMemoryDetector {
	private final int occupiedHeapPercentageThreshold;
	private final List<GarbageCollectorMXBean> tenuredSpaceGcBeans;
	private final MemoryPoolMXBean tenuredSpaceMemoryPoolBean;
	private final MemoryMXBean memoryBean;
	private AtomicReference<MemoryUsage> lowMemoryStateUsage = new AtomicReference<>(
			null);

	private static final Log logger = LogFactory.getLog(LowMemoryDetector.class);

	private final AtomicBoolean lowMemoryDetectedFlag = new AtomicBoolean(false);
	private final AtomicBoolean started = new AtomicBoolean(false);

	private NotificationListener gcListener;

	private final NotificationListener memoryListener = (notification, handback) -> {
		if (MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED == notification
				.getType()) {
			handleMemoryCollectionThresholdExceeded(MemoryNotificationInfo
					.from(CompositeData.class.cast(notification.getUserData())));
		}
	};

	LowMemoryDetector(int occupiedHeapPercentageThreshold) {
		this(occupiedHeapPercentageThreshold,
				ManagementFactory.getGarbageCollectorMXBeans(),
				ManagementFactory.getMemoryPoolMXBeans(),
				ManagementFactory.getMemoryMXBean());
	}

	LowMemoryDetector(int occupiedHeapPercentageThreshold,
			List<GarbageCollectorMXBean> gcBeans, List<MemoryPoolMXBean> memoryPoolBeans,
			MemoryMXBean memoryBean) {
		this.occupiedHeapPercentageThreshold = occupiedHeapPercentageThreshold;
		this.tenuredSpaceGcBeans = findTenuredSpaceGcBeans(gcBeans);
		this.tenuredSpaceMemoryPoolBean = findTenuredSpaceMemoryPoolBean(memoryPoolBeans);
		this.memoryBean = memoryBean;
		try {
			this.gcListener = OracleOpenJdkGcNotificationListenerFactory
					.createListener(this);
		}
		catch (LinkageError linkageError) {
			this.gcListener = null;
		}
	}

	private MemoryPoolMXBean findTenuredSpaceMemoryPoolBean(
			List<MemoryPoolMXBean> memoryPoolBeans) {
		List<MemoryPoolMXBean> filtered = memoryPoolBeans.stream().filter(
				memoryPoolBean -> memoryPoolBean.isCollectionUsageThresholdSupported()
						&& memoryPoolBean.getType() == MemoryType.HEAP
						&& this.isTenuredSpace.test(memoryPoolBean.getName()))
				.collect(Collectors.toList());
		Assert.isTrue(filtered.size() == 1,
				"Expecting a single tenured space memory pool bean.");
		return filtered.get(0);
	}

	private List<GarbageCollectorMXBean> findTenuredSpaceGcBeans(
			List<GarbageCollectorMXBean> gcBeans) {
		return gcBeans.stream().filter(gcBean -> Arrays
				.stream(gcBean.getMemoryPoolNames()).anyMatch(this.isTenuredSpace))
				.collect(Collectors.toList());
	}

	private Predicate<String> isTenuredSpace = (Predicate<String>) name -> name
			.endsWith("Old Gen") || name.endsWith("Tenured Gen");

	void start() {
		if (this.started.compareAndSet(false, true)) {
			registerGcListeners();
			applyTenuredSpaceUsageThreshold();
			registerMemoryBeanListener();
		}
	}

	void stop() {
		if (this.started.compareAndSet(true, false)) {
			unregisterGcListeners();
			unregisterMemoryBeanListener();
		}
	}

	private void registerGcListeners() {
		if (this.gcListener != null) {
			boolean listenerAdded = false;
			for (GarbageCollectorMXBean gcBean : this.tenuredSpaceGcBeans) {
				NotificationBroadcaster.class.cast(gcBean)
						.addNotificationListener(this.gcListener, null, null);
				listenerAdded = true;
			}
			if (!listenerAdded) {
				logger.warn("Cannot find GarbageCollectorMXBean for tenured space.");
			}
		}
	}

	private void unregisterGcListeners() {
		if (this.gcListener != null) {
			for (GarbageCollectorMXBean gcBean : this.tenuredSpaceGcBeans) {
				try {
					NotificationBroadcaster.class.cast(gcBean)
							.removeNotificationListener(this.gcListener);
				}
				catch (ListenerNotFoundException e) {
					// ignore
				}
			}
		}
	}

	private void applyTenuredSpaceUsageThreshold() {
		long usageThreshold = ((long) occupiedHeapPercentageThreshold)
				* this.tenuredSpaceMemoryPoolBean.getUsage().getMax() / 100L;
		if (logger.isInfoEnabled()) {
			logger.info(String.format("Setting threshold for %s to %s of %s (%s%%)",
					this.tenuredSpaceMemoryPoolBean.getName(), usageThreshold,
					this.tenuredSpaceMemoryPoolBean.getUsage().getMax(),
					this.occupiedHeapPercentageThreshold));
		}
		this.tenuredSpaceMemoryPoolBean.setCollectionUsageThreshold(usageThreshold);
	}

	private void registerMemoryBeanListener() {
		NotificationBroadcaster.class.cast(this.memoryBean)
				.addNotificationListener(this.memoryListener, null, null);
	}

	private void unregisterMemoryBeanListener() {
		try {
			NotificationBroadcaster.class.cast(this.memoryBean)
					.removeNotificationListener(this.memoryListener);
		}
		catch (ListenerNotFoundException e) {
			// ignore
		}
	}

	protected void handleGcNotification(Map<String, MemoryUsage> memoryUsageAfterGc) {
		if (this.lowMemoryDetectedFlag.get()) {
			for (Map.Entry<String, MemoryUsage> entry : memoryUsageAfterGc.entrySet()) {
				String spaceName = entry.getKey();
				if (this.isTenuredSpace.test(spaceName)) {
					MemoryUsage space = entry.getValue();
					handleMemoryUsageUpdate(space);
				}
			}
		}
	}

	private void handleMemoryUsageUpdate(MemoryUsage space) {
		if (isMemoryUsageBelowLimit(space)
				&& this.lowMemoryDetectedFlag.compareAndSet(true, false)) {
			exitedLowMemoryState(space);
		}
	}

	private boolean isMemoryUsageBelowLimit(MemoryUsage space) {
		if (space.getMax() > 0) {
			long percentUsed = 100L * space.getUsed() / space.getMax();
			if (percentUsed < occupiedHeapPercentageThreshold) {
				return true;
			}
		}
		return false;
	}

	protected void handleMemoryCollectionThresholdExceeded(MemoryNotificationInfo info) {
		if (this.lowMemoryDetectedFlag.compareAndSet(false, true)) {
			enteredLowMemoryState(info.getUsage());
		}
	}

	protected void enteredLowMemoryState(MemoryUsage space) {
		this.lowMemoryStateUsage.set(space);
		logMemoryStateChange("Low memory state detected.", space);
	}

	protected void exitedLowMemoryState(MemoryUsage space) {
		this.lowMemoryStateUsage.set(null);
		logMemoryStateChange("Memory state back to healthy.", space);
	}

	private void logMemoryStateChange(String description, MemoryUsage space) {
		String msg = String.format("%s tenured space usage %s / %s", description,
				space.getUsed(), space.getMax());
		logErrorMessage(msg);
	}

	protected void logErrorMessage(String message) {
		logger.warn(message);
	}

	boolean isHealthy() {
		start();
		nonOracleOpenJDKFallbackCheckForHealthyAfterExceedingLimit();
		return !this.lowMemoryDetectedFlag.get();
	}

	private void nonOracleOpenJDKFallbackCheckForHealthyAfterExceedingLimit() {
		// poll memory usage each time when GC notifications aren't available
		if (this.gcListener == null && this.lowMemoryDetectedFlag.get()) {
			handleMemoryUsageUpdate(getCurrentUsage());
		}
	}

	MemoryUsage getLowMemoryStateUsage() {
		return this.lowMemoryStateUsage.get();
	}

	MemoryUsage getCurrentUsage() {
		return this.tenuredSpaceMemoryPoolBean.getUsage();
	}

	public int getOccupiedHeapPercentageThreshold() {
		return this.occupiedHeapPercentageThreshold;
	}

	/**
	 * This class loads the Oracle & OpenJDK classes
	 * (com.sun.management.GarbageCollectionNotificationInfo) which might not be available
	 * in all environments.
	 */
	private static class OracleOpenJdkGcNotificationListenerFactory {
		static NotificationListener createListener(LowMemoryDetector lowMemoryDetector) {
			NotificationListener gcListener = (notification, handback) -> {
				if (GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION == notification
						.getType()) {
					lowMemoryDetector
							.handleGcNotification(convertNotification(notification)
									.getGcInfo().getMemoryUsageAfterGc());
				}
			};
			return gcListener;
		}

		private static GarbageCollectionNotificationInfo convertNotification(
				Notification notification) {
			return GarbageCollectionNotificationInfo
					.from(CompositeData.class.cast(notification.getUserData()));
		}
	}
}
