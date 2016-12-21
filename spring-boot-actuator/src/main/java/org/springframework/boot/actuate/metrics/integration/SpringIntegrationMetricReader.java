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

	private final IntegrationManagementConfigurer managementConfigurer;

	public SpringIntegrationMetricReader(IntegrationManagementConfigurer managementConfigurer) {
		this.managementConfigurer = managementConfigurer;
	}

	@Override
	public Metric<?> findOne(String metricName) {
		return null;
	}

	@Override
	public Iterable<Metric<?>> findAll() {
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();

		for (String name : this.managementConfigurer.getChannelNames()) {
			MessageChannelMetrics channelMetrics = this.managementConfigurer.getChannelMetrics(name);
			String prefix = "integration.channel." + name;
			metrics.addAll(getStatistics(prefix + ".errorRate", channelMetrics.getErrorRate()));
			metrics.add(new Metric<Long>(prefix + ".sendCount", channelMetrics.getSendCountLong()));
			metrics.addAll(getStatistics(prefix + ".sendRate", channelMetrics.getSendRate()));
			if (channelMetrics instanceof PollableChannelManagement) {
				metrics.add(new Metric<Long>(prefix + ".receiveCount",
						((PollableChannelManagement) channelMetrics).getReceiveCountLong()));
			}
		}

		for (String name : this.managementConfigurer.getHandlerNames()) {
			MessageHandlerMetrics handlerMetrics = this.managementConfigurer.getHandlerMetrics(name);
			String prefix = "integration.handler." + name;
			metrics.addAll(getStatistics(prefix + ".duration", handlerMetrics.getDuration()));
			metrics.add(new Metric<Long>(prefix + ".activeCount", handlerMetrics.getActiveCountLong()));
		}

		for (String name : this.managementConfigurer.getSourceNames()) {
			MessageSourceMetrics sourceMetrics = this.managementConfigurer.getSourceMetrics(name);
			String prefix = "integration.source." + name;
			metrics.add(new Metric<Long>(prefix + ".messageCount", sourceMetrics.getMessageCountLong()));
		}

		metrics.add(new Metric<Integer>("integration.handlerCount",
				this.managementConfigurer.getHandlerNames().length));
		metrics.add(new Metric<Integer>("integration.channelCount",
				this.managementConfigurer.getChannelNames().length));
		metrics.add(new Metric<Integer>("integration.sourceCount",
				this.managementConfigurer.getSourceNames().length));

		return metrics;
	}

	private Collection<? extends Metric<?>> getStatistics(String name,
			Statistics statistic) {
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		metrics.add(new Metric<Double>(name + ".mean", statistic.getMean()));
		metrics.add(new Metric<Double>(name + ".max", statistic.getMax()));
		metrics.add(new Metric<Double>(name + ".min", statistic.getMin()));
		metrics.add(
				new Metric<Double>(name + ".stdev", statistic.getStandardDeviation()));
		metrics.add(new Metric<Long>(name + ".count", statistic.getCountLong()));
		return metrics;
	}

	@Override
	public long count() {
		int totalChannelCount = this.managementConfigurer.getChannelNames().length;
		int totalHandlerCount = this.managementConfigurer.getHandlerNames().length;
		int totalSourceCount = this.managementConfigurer.getSourceNames().length;
		return totalChannelCount + totalHandlerCount + totalSourceCount;
	}

}
