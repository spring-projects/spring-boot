/*
 * Copyright 2012-2014 the original author or authors.
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
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of {@link PublicMetrics} that exposes all metrics from a
 * {@link MetricReader} along with memory information.
 * 
 * @author Dave Syer
 * @author Christian Dupuis
 */
public class VanillaPublicMetrics implements PublicMetrics {

	private final MetricReader reader;

	public VanillaPublicMetrics(MetricReader reader) {
		Assert.notNull(reader, "MetricReader must not be null");
		this.reader = reader;
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> result = new LinkedHashSet<Metric<?>>();
		for (Metric<?> metric : this.reader.findAll()) {
			result.add(metric);
		}

		addMetrics(result);
		addHeapMetrics(result);
		addThreadMetrics(result);
		addClassLoadingMetrics(result);
		addGarbageCollecitonMetrics(result);

		return result;
	}

	/**
	 * Add basic system metrics.
	 */
	protected void addMetrics(Collection<Metric<?>> result) {
		result.add(new Metric<Long>("mem",
				new Long(Runtime.getRuntime().totalMemory()) / 1024));
		result.add(new Metric<Long>("mem.free", new Long(Runtime.getRuntime()
				.freeMemory()) / 1024));
		result.add(new Metric<Integer>("processors", Runtime.getRuntime()
				.availableProcessors()));
		// Add JVM uptime in ms
		result.add(new Metric<Long>("uptime", new Long(ManagementFactory
				.getRuntimeMXBean().getUptime())));
	}

	/**
	 * Add JVM heap metrics.
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
	 */
	protected void addThreadMetrics(Collection<Metric<?>> result) {
		ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
		result.add(new Metric<Long>("threads.peak", new Long(threadMxBean
				.getPeakThreadCount())));
		result.add(new Metric<Long>("threads.deamon", new Long(threadMxBean
				.getDaemonThreadCount())));
		result.add(new Metric<Long>("threads", new Long(threadMxBean.getThreadCount())));
	}

	/**
	 * Add class loading metrics.
	 */
	protected void addClassLoadingMetrics(Collection<Metric<?>> result) {
		ClassLoadingMXBean classLoadingMxBean = ManagementFactory.getClassLoadingMXBean();
		result.add(new Metric<Long>("classes", new Long(classLoadingMxBean
				.getLoadedClassCount())));
		result.add(new Metric<Long>("classes.loaded", new Long(classLoadingMxBean
				.getTotalLoadedClassCount())));
		result.add(new Metric<Long>("classes.unloaded", new Long(classLoadingMxBean
				.getUnloadedClassCount())));
	}

	/**
	 * Add garbage collection metrics.
	 */
	protected void addGarbageCollecitonMetrics(Collection<Metric<?>> result) {
		List<GarbageCollectorMXBean> garbageCollectorMxBeans = ManagementFactory
				.getGarbageCollectorMXBeans();
		for (int i = 0; i < garbageCollectorMxBeans.size(); i++) {
			GarbageCollectorMXBean garbageCollectorMXBean = garbageCollectorMxBeans
					.get(i);
			String name = beautifyGcName(garbageCollectorMXBean.getName());
			result.add(new Metric<Long>("gc." + name + ".count", new Long(
					garbageCollectorMXBean.getCollectionCount())));
			result.add(new Metric<Long>("gc." + name + ".time", new Long(
					garbageCollectorMXBean.getCollectionTime())));
		}
	}

	/**
	 * Turn GC names like 'PS Scavenge' or 'PS MarkSweep' into something that is more
	 * metrics friendly.
	 */
	private String beautifyGcName(String name) {
		return StringUtils.replace(name, " ", "_").toLowerCase();
	}
}
