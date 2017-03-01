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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryNotificationInfo;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import javax.management.ListenerNotFoundException;
import javax.management.Notification;
import javax.management.NotificationBroadcaster;
import javax.management.NotificationListener;

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;

/**
 * Tests for {@link LowMemoryDetector}.
 *
 * @author Lari Hotari
 */
public class LowMemoryDetectorTests {
	private GarbageCollectorMXBean gcBean;
	private GarbageCollectorMXBean newGenGcBean;
	private MemoryMXBean memoryBean;
	private MemoryPoolMXBean memoryPoolBean;
	private LowMemoryDetector lowMemoryDetector;
	private NotificationListener gcNotificationListener;
	private NotificationListener memoryNotificationListener;
	private long usageMax = 10L * 1000L * 1000L * 1000L; // about 10GB using 10-base

	@Before
	public void setUp() throws Exception {
		this.gcBean = createGcBean("PS Old Gen");
		this.newGenGcBean = createGcBean("New Gen");
		this.memoryBean = createMemoryBean();
		this.memoryPoolBean = createMemoryPoolBean("PS Old Gen", this.usageMax);
	}

	private void createAndStartLowMemoryDetector() {
		this.lowMemoryDetector = new LowMemoryDetector(90,
				Arrays.asList(this.gcBean, this.newGenGcBean),
				Arrays.asList(this.memoryPoolBean), this.memoryBean);
		this.lowMemoryDetector.start();
		extractGcListener();
		extractMemoryListener();
	}

	private void extractMemoryListener() {
		// should register listener to memory bean
		ArgumentCaptor<NotificationListener> memoryListenerCapturer = ArgumentCaptor
				.forClass(NotificationListener.class);
		verify(NotificationBroadcaster.class.cast(this.memoryBean))
				.addNotificationListener(memoryListenerCapturer.capture(), any(), any());

		// extract listener for later tests
		this.memoryNotificationListener = memoryListenerCapturer.getValue();
	}

	private void extractGcListener() {
		// should register listener to tenure collector
		ArgumentCaptor<NotificationListener> gcListenerCapturer = ArgumentCaptor
				.forClass(NotificationListener.class);
		verify(NotificationBroadcaster.class.cast(this.gcBean))
				.addNotificationListener(gcListenerCapturer.capture(), any(), any());

		// extract listener for later tests
		this.gcNotificationListener = gcListenerCapturer.getValue();
	}

	@Test
	public void registersListenerToTenureCollector() {
		createAndStartLowMemoryDetector();
	}

	@Test
	public void doesntRegisterListenerToNewCollector() {
		createAndStartLowMemoryDetector();
		verify(NotificationBroadcaster.class.cast(this.newGenGcBean), never())
				.addNotificationListener(any(), any(), any());
	}

	@Test
	public void setsTheThresholdToTheMemoryPoolBean() {
		createAndStartLowMemoryDetector();
		verify(this.memoryPoolBean).setCollectionUsageThreshold(9000000000L);
	}

	@Test
	public void setsLowMemoryDetectedFlagWhenTheLimitIsExceeded()
			throws ClassNotFoundException {
		createAndStartLowMemoryDetector();
		this.memoryNotificationListener.handleNotification(
				createMemoryNotification(9100000000L, this.usageMax), null);
		assertThat(this.lowMemoryDetector.isHealthy())
				.as("Low memory should be now detected").isFalse();
	}

	@Test
	public void unsetsLowMemoryDetectedFlagWhenMemoryGoesBackUnderThreshold()
			throws ClassNotFoundException {
		setsLowMemoryDetectedFlagWhenTheLimitIsExceeded();

		this.gcNotificationListener.handleNotification(
				createGcNotification(8900000000L, this.usageMax, ""), null);
		assertThat(this.lowMemoryDetector.isHealthy())
				.as("Low memory not be flagged after healthy state").isTrue();
	}

	@Test
	public void unregistersListenersWhenStopped() throws ListenerNotFoundException {
		createAndStartLowMemoryDetector();
		this.lowMemoryDetector.stop();
		verify((NotificationBroadcaster) this.memoryBean)
				.removeNotificationListener(this.memoryNotificationListener);
		verify((NotificationBroadcaster) this.gcBean)
				.removeNotificationListener(this.gcNotificationListener);
	}

	private GarbageCollectorMXBean createGcBean(String memPoolName) {
		GarbageCollectorMXBean gcBean = mock(GarbageCollectorMXBean.class,
				withSettings().extraInterfaces(NotificationBroadcaster.class));
		given(gcBean.getMemoryPoolNames()).willReturn(new String[] { memPoolName });
		return gcBean;
	}

	private MemoryPoolMXBean createMemoryPoolBean(String memPoolName, long usageMax) {
		MemoryPoolMXBean memoryPoolBean = mock(MemoryPoolMXBean.class);
		given(memoryPoolBean.getName()).willReturn(memPoolName);
		given(memoryPoolBean.isCollectionUsageThresholdSupported()).willReturn(true);
		given(memoryPoolBean.getType()).willReturn(MemoryType.HEAP);
		MemoryUsage memoryUsage = mock(MemoryUsage.class);
		given(memoryUsage.getMax()).willReturn(usageMax);
		given(memoryPoolBean.getUsage()).willReturn(memoryUsage);
		return memoryPoolBean;
	}

	private MemoryMXBean createMemoryBean() {
		return mock(MemoryMXBean.class,
				withSettings().extraInterfaces(NotificationBroadcaster.class));
	}

	private Notification createGcNotification(long usage, long usageMax, String gcCause) {
		Notification notification = mock(Notification.class);
		given(notification.getType()).willReturn(
				GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION);
		GcInfo gcInfo = mock(GcInfo.class);
		GarbageCollectionNotificationInfo gcNotificationInfo = new GarbageCollectionNotificationInfo(
				"gcName", "end of major GC", gcCause, gcInfo);
		given(notification.getUserData())
				.willReturn(gcNotificationInfo.toCompositeData(null));
		MemoryUsage memoryUsage = createMemoryUsage(usage, usageMax);
		given(gcInfo.getMemoryUsageAfterGc())
				.willReturn(Collections.singletonMap("PS Old Gen", memoryUsage));
		return notification;
	}

	private Notification createMemoryNotification(long usage, long usageMax)
			throws ClassNotFoundException {
		Notification notification = mock(Notification.class);
		given(notification.getType())
				.willReturn(MemoryNotificationInfo.MEMORY_COLLECTION_THRESHOLD_EXCEEDED);
		MemoryUsage memoryUsage = createMemoryUsage(usage, usageMax);
		MemoryNotificationInfo memoryNotificationInfo = new MemoryNotificationInfo(
				"PS Old Gen", memoryUsage, 1);

		Method toCompositeDataMethod = ReflectionUtils.findMethod(
				Class.forName("sun.management.MemoryNotifInfoCompositeData"),
				"toCompositeData", MemoryNotificationInfo.class);
		Object userData = ReflectionUtils.invokeMethod(toCompositeDataMethod, null,
				memoryNotificationInfo);
		given(notification.getUserData()).willReturn(userData);
		return notification;
	}

	private MemoryUsage createMemoryUsage(long usage, long usageMax) {
		MemoryUsage memoryUsage = mock(MemoryUsage.class);
		given(memoryUsage.getUsed()).willReturn(usage);
		given(memoryUsage.getMax()).willReturn(usageMax);
		return memoryUsage;
	}
}
