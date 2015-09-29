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
package sample.metrics.atsd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.endpoint.MetricsEndpoint;
import org.springframework.boot.actuate.endpoint.MetricsEndpointMetricReader;
import org.springframework.boot.actuate.metrics.atsd.AtsdMetricWriter;
import org.springframework.boot.actuate.metrics.atsd.AtsdNamingStrategy;
import org.springframework.boot.actuate.metrics.atsd.DefaultAtsdNamingStrategy;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableCaching
public class SampleAtsdExportApplication {


	/* Enable public metrics export */
	@Bean
	public MetricsEndpointMetricReader metricsEndpointMetricReader(MetricsEndpoint metricsEndpoint) {
		return new MetricsEndpointMetricReader(metricsEndpoint);
	}

	@Bean
	@ExportMetricWriter
	@ConfigurationProperties("metrics.export")
	public MetricWriter atsdMetricWriter() {
		AtsdMetricWriter writer = new AtsdMetricWriter();
		writer.setNamingStrategy(namingStrategy());
		return writer;
	}

	@Bean
	@ConfigurationProperties("metrics.names")
	public AtsdNamingStrategy namingStrategy() {
		return new DefaultAtsdNamingStrategy();
	}


	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleAtsdExportApplication.class, args);
	}
}
