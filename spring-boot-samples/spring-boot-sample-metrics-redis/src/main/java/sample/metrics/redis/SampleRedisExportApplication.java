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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.export.MetricExportProperties;
import org.springframework.boot.actuate.metrics.jmx.JmxMetricWriter;
import org.springframework.boot.actuate.metrics.repository.redis.RedisMetricRepository;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jmx.export.MBeanExporter;

@SpringBootApplication
public class SampleRedisExportApplication {

	@Autowired
	private MetricExportProperties export;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleRedisExportApplication.class, args);
	}

	@Bean
	public RedisMetricRepository redisMetricWriter(
			RedisConnectionFactory connectionFactory) {
		return new RedisMetricRepository(connectionFactory, this.export.getRedis().getPrefix(),
				this.export.getRedis().getKey());
	}

	@Bean
	public JmxMetricWriter jmxMetricWriter(
			@Qualifier("mbeanExporter") MBeanExporter exporter) {
		return new JmxMetricWriter(exporter);
	}

}
