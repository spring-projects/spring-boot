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

package sample.metrics.redis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.MetricReaderPublicMetrics;
import org.springframework.boot.actuate.endpoint.PublicMetrics;
import org.springframework.boot.actuate.metrics.aggregate.AggregateMetricReader;
import org.springframework.boot.actuate.metrics.export.MetricExportProperties;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.actuate.metrics.repository.redis.RedisMetricRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

@Configuration
public class AggregateMetricsConfiguration {

	@Autowired
	private MetricExportProperties export;

	@Autowired
	private RedisConnectionFactory connectionFactory;

	@Bean
	public PublicMetrics metricsAggregate() {
		return new MetricReaderPublicMetrics(aggregatesMetricReader());
	}

	private MetricReader globalMetricsForAggregation() {
		return new RedisMetricRepository(this.connectionFactory, this.export.getRedis()
				.getAggregatePrefix(), this.export.getRedis().getKey());
	}

	private MetricReader aggregatesMetricReader() {
		AggregateMetricReader repository = new AggregateMetricReader(
				globalMetricsForAggregation());
		return repository;
	}
}
