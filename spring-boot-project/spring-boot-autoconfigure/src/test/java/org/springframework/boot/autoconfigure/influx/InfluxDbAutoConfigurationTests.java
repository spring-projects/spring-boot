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

package org.springframework.boot.autoconfigure.influx;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import org.influxdb.InfluxDB;
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
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.2.0", forRemoval = true)
class InfluxDbAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(InfluxDbAutoConfiguration.class));

	@Test
	void influxDbRequiresUrl() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(InfluxDB.class));
	}

	@Test
	void influxDbCanBeCustomized() {
		this.contextRunner
			.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.user=user",
					"spring.influx.password=password")
			.run((context) -> assertThat(context).hasSingleBean(InfluxDB.class));
	}

	@Test
	void influxDbCanBeCreatedWithoutCredentials() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost").run((context) -> {
			assertThat(context).hasSingleBean(InfluxDB.class);
			int readTimeout = getReadTimeoutProperty(context);
			assertThat(readTimeout).isEqualTo(10_000);
		});
	}

	@Test
	void influxDbWithOkHttpClientBuilderProvider() {
		this.contextRunner.withUserConfiguration(CustomOkHttpClientBuilderProviderConfig.class)
			.withPropertyValues("spring.influx.url=http://localhost")
			.run((context) -> {
				assertThat(context).hasSingleBean(InfluxDB.class);
				int readTimeout = getReadTimeoutProperty(context);
				assertThat(readTimeout).isEqualTo(40_000);
			});
	}

	@Test
	void influxDbWithCustomizer() {
		this.contextRunner.withBean(InfluxDbCustomizer.class, () -> (influxDb) -> influxDb.setDatabase("test"))
			.withPropertyValues("spring.influx.url=http://localhost")
			.run((context) -> {
				assertThat(context).hasSingleBean(InfluxDB.class);
				InfluxDB influxDb = context.getBean(InfluxDB.class);
				assertThat(influxDb).hasFieldOrPropertyWithValue("database", "test");
			});
	}

	private int getReadTimeoutProperty(AssertableApplicationContext context) {
		InfluxDB influxDb = context.getBean(InfluxDB.class);
		Retrofit retrofit = (Retrofit) ReflectionTestUtils.getField(influxDb, "retrofit");
		OkHttpClient callFactory = (OkHttpClient) retrofit.callFactory();
		return callFactory.readTimeoutMillis();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomOkHttpClientBuilderProviderConfig {

		@Bean
		@SuppressWarnings("removal")
		InfluxDbOkHttpClientBuilderProvider influxDbOkHttpClientBuilderProvider() {
			return () -> new OkHttpClient.Builder().readTimeout(40, TimeUnit.SECONDS);
		}

	}

}
