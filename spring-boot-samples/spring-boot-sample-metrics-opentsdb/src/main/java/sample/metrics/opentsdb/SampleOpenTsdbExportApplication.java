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

package sample.metrics.opentsdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.opentsdb.DefaultOpenTsdbNamingStrategy;
import org.springframework.boot.actuate.metrics.opentsdb.OpenTsdbMetricWriter;
import org.springframework.boot.actuate.metrics.opentsdb.OpenTsdbNamingStrategy;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SampleOpenTsdbExportApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleOpenTsdbExportApplication.class, args);
	}

	@Bean
	@ConfigurationProperties("metrics.export")
	public MetricWriter openTsdbMetricWriter() {
		OpenTsdbMetricWriter writer = new OpenTsdbMetricWriter();
		writer.setNamingStrategy(namingStrategy());
		return writer;
	}

	@Bean
	@ConfigurationProperties("metrics.names")
	public OpenTsdbNamingStrategy namingStrategy() {
		return new DefaultOpenTsdbNamingStrategy();
	}

}
