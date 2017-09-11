/**
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
package org.springframework.boot.actuate.metrics;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.endpoint.Endpoint;
import org.springframework.boot.endpoint.ReadOperation;
import org.springframework.boot.endpoint.Selector;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Statistic;
import io.micrometer.core.instrument.util.HierarchicalNameMapper;

/**
 * @since 2.0.0
 * @author Jon Schneider
 */
@Endpoint(id = "metrics")
public class MetricsEndpoint {
    private final MeterRegistry registry;
    private final HierarchicalNameMapper nameMapper = HierarchicalNameMapper.DEFAULT;

    public MetricsEndpoint(MeterRegistry registry) {
        this.registry = registry;
    }

    @ReadOperation
    public List<String> listNames() {
        return registry.getMeters().stream().map(m -> m.getId().getName()).collect(toList());
    }

    @ReadOperation
    public Map<String, Collection<MeasurementSample>> metric(@Selector String requiredMetricName) {
        return registry.find(requiredMetricName).meters()
            .stream()
            .collect(Collectors.toMap(meter -> nameMapper.toHierarchicalName(meter.getId()),
                meter -> stream(meter.measure().spliterator(), false)
                    .map(ms -> new MeasurementSample(ms.getStatistic(), ms.getValue()))
                    .collect(toList())));
    }

    static class MeasurementSample {
        private Statistic statistic;
        private Double value;

        MeasurementSample() { } // for jackson in test

        MeasurementSample(Statistic statistic, Double value) {
            this.statistic = statistic;
            this.value = value;
        }

        public Statistic getStatistic() {
            return statistic;
        }

        public Double getValue() {
            return value;
        }

        public void setValue(Double value) {
            this.value = value;
        }

        public void setStatistic(Statistic statistic) {
            this.statistic = statistic;
        }

        @Override
        public String toString() {
            return "MeasurementSample{" +
                "statistic=" + statistic +
                ", value=" + value +
                '}';
        }
    }
}
