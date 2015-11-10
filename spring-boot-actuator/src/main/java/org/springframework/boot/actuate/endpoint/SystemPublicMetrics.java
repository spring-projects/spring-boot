/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadMXBean;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;

/**
 * A {@link PublicMetrics} implementation that provides various system-related metrics.
 *
 * @author Dave Syer
 * @author Christian Dupuis
 * @author Stephane Nicoll
 * @author Johannes Edmeier
 * @since 1.2.0
 */
public class SystemPublicMetrics implements PublicMetrics, Ordered {

	private long timestamp;

	public SystemPublicMetrics() {
		this.timestamp = System.currentTimeMillis();
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> result = new LinkedHashSet<Metric<?>>();
		addBasicMetrics(result);
		addManagementMetrics(result);
		return result;
	}

	/**
	 * Add basic system metrics.
	 * @param result the result
	 */
	protected void addBasicMetrics(Collection<Metric<?>> result) {
		// NOTE: ManagementFactory must not be used here since it fails on GAE
		result.add(new Metric<Long>("mem", Runtime.getRuntime().totalMemory() / 1024));
		result.add(
				new Metric<Long>("mem.free", Runtime.getRuntime().freeMemory() / 1024));
		result.add(new Metric<Integer>("processors",
				Runtime.getRuntime().availableProcessors()));
		result.add(new Metric<Long>("instance.uptime",
				System.currentTimeMillis() - this.timestamp));
	}

	/**
	 * Add metrics from ManagementFactory if possible. Note that ManagementFactory is not
	 * available on Google App Engine.
	 * @param result the result
	 */
	private void addManagementMetrics(Collection<Metric<?>> result) {
		try {
			// Add JVM up time in ms
			result.add(new Metric<Long>("uptime",
					ManagementFactory.getRuntimeMXBean().getUptime()));
			result.add(new Metric<Double>("systemload.average",
					ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage()));
			addHeapMetrics(result);
			addThreadMetrics(result);
			addClassLoadingMetrics(result);
			addGarbageCollectionMetrics(result);
		}
		catch (NoClassDefFoundError ex) {
			// Expected on Google App Engine
		}
	}

	/**
	 * Add JVM heap metrics.
	 * @param result the result
	 */
	protected void addHeapMetrics(Collection<Metric<?>> result) {
		MemoryUsage memoryUsage = ManagementFactory.getMemoryMXBean()
				.getHeapMemoryUsage();
		result.add(new Metric<Long>("heap.committed", memoryUsage.getCommitted() / 1024));
		result.add(new Metric<Long>("heap.init", memoryUsage.getInit() / 1024));
		result.add(new Metric<Long>("heap.used", memoryUsage.getUsed() / 1024));
		result.add(new Metric<Long>("heap", memoryUsage.getMax() / 1024));
	}

	/**
	 * Add thread metrics.
	 * @param result the result
	 */
	protected void addThreadMetrics(Collection<Metric<?>> result) {
		ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
		result.add(new Metric<Long>("threads.peak",
				(long) threadMxBean.getPeakThreadCount()));
		result.add(new Metric<Long>("threads.daemon",
				(long) threadMxBean.getDaemonThreadCount()));
		result.add(new Metric<Long>("threads.totalStarted",
				threadMxBean.getTotalStartedThreadCount()));
		result.add(new Metric<Long>("threads", (long) threadMxBean.getThreadCount()));
	}

	/**
	 * Add class loading metrics.
	 * @param result the result
	 */
	protected void addClassLoadingMetrics(Collection<Metric<?>> result) {
		ClassLoadingMXBean classLoadingMxBean = ManagementFactory.getClassLoadingMXBean();
		result.add(new Metric<Long>("classes",
				(long) classLoadingMxBean.getLoadedClassCount()));
		result.add(new Metric<Long>("classes.loaded",
				classLoadingMxBean.getTotalLoadedClassCount()));
		result.add(new Metric<Long>("classes.unloaded",
				classLoadingMxBean.getUnloadedClassCount()));
	}

	/**
	 * Add garbage collection metrics.
	 * @param result the result
	 */
	protected void addGarbageCollectionMetrics(Collection<Metric<?>> result) {
		List<GarbageCollectorMXBean> garbageCollectorMxBeans = ManagementFactory
				.getGarbageCollectorMXBeans();
		for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMxBeans) {
			String name = beautifyGcName(garbageCollectorMXBean.getName());
			result.add(new Metric<Long>("gc." + name + ".count",
					garbageCollectorMXBean.getCollectionCount()));
			result.add(new Metric<Long>("gc." + name + ".time",
					garbageCollectorMXBean.getCollectionTime()));
		}
	}

	/**
	 * Turn GC names like 'PS Scavenge' or 'PS MarkSweep' into something that is more
	 * metrics friendly.
	 * @param name the source name
	 * @return a metric friendly name
	 */
	private String beautifyGcName(String name) {
		return StringUtils.replace(name, " ", "_").toLowerCase();
	}

}
