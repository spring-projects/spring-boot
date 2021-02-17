/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.influx;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.impl.BatchProcessor;
import org.junit.jupiter.api.Test;
import retrofit2.Retrofit;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfluxDbAutoConfiguration}.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
class InfluxDbAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withConfiguration(AutoConfigurations.of(InfluxDbAutoConfiguration.class));

	@Test
	void influxDbRequiresUrl() {
		this.contextRunner.run((context) -> assertThat(context.getBeansOfType(InfluxDB.class)).isEmpty());
	}

	@Test
	void influxDbCanBeCustomized() {
		this.contextRunner
				.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.password:password",
						"spring.influx.user:user")
				.run(((context) -> assertThat(context.getBeansOfType(InfluxDB.class)).hasSize(1)));
	}

	@Test
	void influxDbCanBeCreatedWithoutCredentials() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost").run((context) -> {
			assertThat(context.getBeansOfType(InfluxDB.class)).hasSize(1);
			int readTimeout = getReadTimeoutProperty(context);
			assertThat(readTimeout).isEqualTo(10_000);
		});
	}

	@Test
	void influxDbWithOkHttpClientBuilderProvider() {
		this.contextRunner.withUserConfiguration(CustomOkHttpClientBuilderProviderConfig.class)
				.withPropertyValues("spring.influx.url=http://localhost").run((context) -> {
					assertThat(context.getBeansOfType(InfluxDB.class)).hasSize(1);
					int readTimeout = getReadTimeoutProperty(context);
					assertThat(readTimeout).isEqualTo(40_000);
				});
	}

	@Test
	void influxDbWithDatabase() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.database:sample-db")
				.run((context) -> {
					assertThat(context.getBeansOfType(InfluxDB.class)).hasSize(1);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					String database = (String) ReflectionTestUtils.getField(influxDb, "database");
					assertThat(database).isEqualTo("sample-db");
				});
	}

	@Test
	void influxDbWithRetentionPolicy() {
		this.contextRunner
				.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.retention-policy:two_hours")
				.run((context) -> {
					assertThat(context.getBeansOfType(InfluxDB.class)).hasSize(1);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					String retentionPolicy = (String) ReflectionTestUtils.getField(influxDb, "retentionPolicy");
					assertThat(retentionPolicy).isEqualTo("two_hours");
				});
	}

	@Test
	void influxDbWithLogLevel() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.log:basic")
				.run((context) -> {
					assertThat(context.getBeansOfType(InfluxDB.class)).hasSize(1);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					InfluxDB.LogLevel log = (InfluxDB.LogLevel) ReflectionTestUtils.getField(influxDb, "logLevel");
					assertThat(log).isEqualTo(InfluxDB.LogLevel.BASIC);
				});
	}

	@Test
	void influxDbWithConsistency() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.consistency:all")
				.run((context) -> {
					assertThat(context.getBeansOfType(InfluxDB.class)).hasSize(1);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					InfluxDB.ConsistencyLevel consistency = (InfluxDB.ConsistencyLevel) ReflectionTestUtils
							.getField(influxDb, "consistency");
					assertThat(consistency).isEqualTo(InfluxDB.ConsistencyLevel.ALL);
				});
	}

	@Test
	void influxDbWithBatchOptions() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.batch.enabled:true",
				"spring.influx.batch.actions:50", "spring.influx.batch.flush-duration:50").run((context) -> {
					assertThat(context.getBeansOfType(InfluxDB.class)).hasSize(1);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					BatchProcessor batchProcessor = (BatchProcessor) ReflectionTestUtils.getField(influxDb,
							"batchProcessor");
					int actions = (int) ReflectionTestUtils.getField(batchProcessor, "actions");
					int flushInterval = (int) ReflectionTestUtils.getField(batchProcessor, "flushInterval");
					assertThat(actions).isEqualTo(50);
					assertThat(flushInterval).isEqualTo(50);
				});
	}

	@Test
	void influxDbWithBatchOptionsCustomizer() {
		this.contextRunner.withUserConfiguration(CustomInfluxDbBatchOptionsCustomizerConfig.class)
				.withPropertyValues("spring.influx.url=http://localhost").run((context) -> {
					assertThat(context.getBeansOfType(InfluxDB.class)).hasSize(1);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					BatchProcessor batchProcessor = (BatchProcessor) ReflectionTestUtils.getField(influxDb,
							"batchProcessor");
					int actions = (int) ReflectionTestUtils.getField(batchProcessor, "actions");
					int flushInterval = (int) ReflectionTestUtils.getField(batchProcessor, "flushInterval");
					int jitterInterval = (int) ReflectionTestUtils.getField(batchProcessor, "jitterInterval");
					assertThat(actions).isEqualTo(20);
					assertThat(flushInterval).isEqualTo(20);
					assertThat(jitterInterval).isEqualTo(20);
				});
	}

	private int getReadTimeoutProperty(AssertableApplicationContext context) {
		InfluxDB influxDB = context.getBean(InfluxDB.class);
		Retrofit retrofit = (Retrofit) ReflectionTestUtils.getField(influxDB, "retrofit");
		OkHttpClient callFactory = (OkHttpClient) retrofit.callFactory();
		return callFactory.readTimeoutMillis();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomOkHttpClientBuilderProviderConfig {

		@Bean
		InfluxDbOkHttpClientBuilderProvider influxDbOkHttpClientBuilderProvider() {
			return () -> new OkHttpClient.Builder().readTimeout(40, TimeUnit.SECONDS);
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomInfluxDbBatchOptionsCustomizerConfig {

		@Bean
		InfluxDbCustomizer influxDbBatchOptionsCustomizer() {
			return (influxDb) -> {
				BatchOptions batchOptions = BatchOptions.DEFAULTS.actions(20).flushDuration(20).jitterDuration(20);
				influxDb.enableBatch(batchOptions);
			};
		}

	}

}
