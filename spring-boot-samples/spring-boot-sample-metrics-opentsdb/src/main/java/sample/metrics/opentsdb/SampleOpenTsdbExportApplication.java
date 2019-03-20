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

package sample.metrics.opentsdb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.ExportMetricWriter;
import org.springframework.boot.actuate.metrics.opentsdb.DefaultOpenTsdbNamingStrategy;
import org.springframework.boot.actuate.metrics.opentsdb.OpenTsdbGaugeWriter;
import org.springframework.boot.actuate.metrics.writer.GaugeWriter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(HelloWorldProperties.class)
public class SampleOpenTsdbExportApplication {

	@Bean
	@ConfigurationProperties("metrics.export")
	@ExportMetricWriter
	public GaugeWriter openTsdbMetricWriter() {
		OpenTsdbGaugeWriter writer = new OpenTsdbGaugeWriter();
		writer.setNamingStrategy(namingStrategy());
		return writer;
	}

	@Bean
	@ConfigurationProperties("metrics.names")
	public DefaultOpenTsdbNamingStrategy namingStrategy() {
		return new DefaultOpenTsdbNamingStrategy();
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleOpenTsdbExportApplication.class, args);
	}

}
