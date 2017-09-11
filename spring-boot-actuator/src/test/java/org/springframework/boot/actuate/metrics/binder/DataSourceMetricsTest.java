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
package org.springframework.boot.actuate.metrics.binder;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.sql.SQLException;
import java.util.Collection;

import javax.sql.DataSource;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.SpringMeters;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.metadata.DataSourcePoolMetadataProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * @author Jon Schneider
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.generate-unique-name=true",
    "management.security.enabled=false",
    "metrics.useGlobalRegistry=false"
})
public class DataSourceMetricsTest {
    @Autowired
    DataSource dataSource;

    @Autowired
	MeterRegistry registry;

    @Test
    public void dataSourceIsInstrumented() throws SQLException, InterruptedException {
        dataSource.getConnection().getMetaData();
        assertThat(registry.find("data.source.max.connections").meter()).isPresent();
    }

    @SpringBootApplication(scanBasePackages = "isolated")
    @Import(DataSourceConfig.class)
    static class MetricsApp {
        @Bean
		MeterRegistry registry() {
            return new SimpleMeterRegistry();
        }
    }

    @Configuration
    static class DataSourceConfig {
        public DataSourceConfig(DataSource dataSource,
                                Collection<DataSourcePoolMetadataProvider> metadataProviders,
                                MeterRegistry registry) {
            SpringMeters.monitor(
                registry,
                dataSource,
                metadataProviders,
                "data.source");
        }
    }
}
