/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection.influx;

import org.influxdb.InfluxDB;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.influx.InfluxDbAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfluxDbServiceConnection}.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SpringJUnitConfig
@Testcontainers(disabledWithoutDocker = true)
class InfluxDbServiceConnectionTests {

	private static final String INFLUXDB_VERSION = "2.6.1";

	@Container
	@InfluxDbServiceConnection
	static final InfluxDBContainer<?> influxDbService = new InfluxDBContainer<>(
			DockerImageName.parse("influxdb").withTag(INFLUXDB_VERSION));

	@Autowired
	private InfluxDB influxDb;

	@Test
	void connectionCanBeMadeToInfluxDbContainer() {
		assertThat(this.influxDb.version()).isEqualTo("v" + INFLUXDB_VERSION);
	}

	@Configuration(proxyBeanMethods = false)
	@ImportAutoConfiguration(InfluxDbAutoConfiguration.class)
	static class TestConfiguration {

	}

}
