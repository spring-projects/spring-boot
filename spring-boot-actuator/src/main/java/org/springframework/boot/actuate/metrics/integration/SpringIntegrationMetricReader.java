/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.metrics.integration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.integration.support.management.IntegrationManagementConfigurer;
import org.springframework.integration.support.management.MessageChannelMetrics;
import org.springframework.integration.support.management.MessageHandlerMetrics;
import org.springframework.integration.support.management.MessageSourceMetrics;
import org.springframework.integration.support.management.PollableChannelManagement;
import org.springframework.integration.support.management.Statistics;
import org.springframework.lang.UsesJava7;

/**
 * A {@link MetricReader} for Spring Integration metrics (as provided by
 * {@link IntegrationManagementConfigurer}).
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @since 1.3.0
 */
@UsesJava7
public class SpringIntegrationMetricReader implements MetricReader {

	private final IntegrationManagementConfigurer configurer;

	public SpringIntegrationMetricReader(IntegrationManagementConfigurer configurer) {
		this.configurer = configurer;
	}

	@Override
	public Metric<?> findOne(String metricName) {
		return null;
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		List<Metric<?>> result = new ArrayList<Metric<?>>();
		String[] channelNames = this.configurer.getChannelNames();
		String[] handlerNames = this.configurer.getHandlerNames();
		String[] sourceNames = this.configurer.getSourceNames();
		addChannelMetrics(result, channelNames);
		addHandlerMetrics(result, handlerNames);
		addSourceMetrics(result, sourceNames);
		result.add(new Metric<Integer>("integration.handlerCount", handlerNames.length));
		result.add(new Metric<Integer>("integration.channelCount", channelNames.length));
		result.add(new Metric<Integer>("integration.sourceCount", sourceNames.length));
		return result;
	}

	private void addChannelMetrics(List<Metric<?>> result, String[] names) {
		for (String name : names) {
			addChannelMetrics(result, name, this.configurer.getChannelMetrics(name));
		}
	}

	private void addChannelMetrics(List<Metric<?>> result, String name,
			MessageChannelMetrics metrics) {
		String prefix = "integration.channel." + name;
		result.addAll(getStatistics(prefix + ".errorRate", metrics.getErrorRate()));
		result.add(new Metric<Long>(prefix + ".sendCount", metrics.getSendCountLong()));
		result.addAll(getStatistics(prefix + ".sendRate", metrics.getSendRate()));
		if (metrics instanceof PollableChannelManagement) {
			result.add(new Metric<Long>(prefix + ".receiveCount",
					((PollableChannelManagement) metrics).getReceiveCountLong()));
		}
	}

	private void addHandlerMetrics(List<Metric<?>> result, String[] names) {
		for (String name : names) {
			addHandlerMetrics(result, name, this.configurer.getHandlerMetrics(name));
		}
	}

	private void addHandlerMetrics(List<Metric<?>> result, String name,
			MessageHandlerMetrics metrics) {
		String prefix = "integration.handler." + name;
		result.addAll(getStatistics(prefix + ".duration", metrics.getDuration()));
		long activeCount = metrics.getActiveCountLong();
		result.add(new Metric<Long>(prefix + ".activeCount", activeCount));
	}

	private void addSourceMetrics(List<Metric<?>> result, String[] names) {
		for (String name : names) {
			addSourceMetrics(result, name, this.configurer.getSourceMetrics(name));
		}
	}

	private void addSourceMetrics(List<Metric<?>> result, String name,
			MessageSourceMetrics sourceMetrics) {
		String prefix = "integration.source." + name;
		result.add(new Metric<Long>(prefix + ".messageCount",
				sourceMetrics.getMessageCountLong()));
	}

	private Collection<? extends Metric<?>> getStatistics(String name, Statistics stats) {
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		metrics.add(new Metric<Double>(name + ".mean", stats.getMean()));
		metrics.add(new Metric<Double>(name + ".max", stats.getMax()));
		metrics.add(new Metric<Double>(name + ".min", stats.getMin()));
		metrics.add(new Metric<Double>(name + ".stdev", stats.getStandardDeviation()));
		metrics.add(new Metric<Long>(name + ".count", stats.getCountLong()));
		return metrics;
	}

	@Override
	public long count() {
		int totalChannelCount = this.configurer.getChannelNames().length;
		int totalHandlerCount = this.configurer.getHandlerNames().length;
		int totalSourceCount = this.configurer.getSourceNames().length;
		return totalChannelCount + totalHandlerCount + totalSourceCount;
	}

}
