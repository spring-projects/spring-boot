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

package org.springframework.boot.actuate.endpoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaOperations;



/**
 * A {@linkplain org.springframework.boot.actuate.endpoint.PublicMetrics} implementation that
 * provides kafka producer statistics.
 *
 * @author Efstathios Souris
 */
public class KafkaPublicMetrics implements PublicMetrics {

	@Autowired
	private ApplicationContext applicationContext;

	private Map<String, KafkaOperations<?, ?>> kafkaOperations;

	@PostConstruct
	public void init() {
		this.kafkaOperations = new HashMap<>();
		for (Map.Entry<String, KafkaOperations> entry : this.applicationContext
				.getBeansOfType(KafkaOperations.class).entrySet()) {
			this.kafkaOperations.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Collection<Metric<?>> metrics() {
		Collection<Metric<?>> result = new ArrayList<>();
		Set<Map.Entry<String, KafkaOperations<?, ?>>> entrySet =
				this.kafkaOperations.entrySet();
		for (Map.Entry<String, KafkaOperations<?, ?>> e : entrySet) {
			result.addAll(convert(e.getKey(), e.getValue().metrics()));
		}
		return result;
	}

	private Collection<Metric<Number>> convert(String prefix,
			Map<MetricName, ? extends org.apache.kafka.common.Metric> metrics) {
		Collection<Metric<Number>> result = new ArrayList<>();
		for (Map.Entry<MetricName, ? extends org.apache.kafka.common.Metric> m : metrics
				.entrySet()) {
			KafkaMetric kafkaMetric = KafkaMetric.class.cast(m.getValue());
			String name = prefix + "." + kafkaMetric.metricName().group() + "." + kafkaMetric.metricName().name();
			result.add(new Metric<>(name, kafkaMetric.value()));
		}
		return result;
	}

}
