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
import java.util.function.Consumer;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientOptions;
import okhttp3.OkHttpClient;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.influxdb.InfluxDB;
import org.junit.jupiter.api.Test;
import retrofit2.Retrofit;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
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
				.withPropertyValues("spring.influx.url=http://localhost").run((context) -> {
					assertThat(context).hasSingleBean(InfluxDB.class);
					int readTimeout = getReadTimeoutProperty(context);
					assertThat(readTimeout).isEqualTo(40_000);
				});
	}

	@Test
	void influxDbWithCustomizer() {
		this.contextRunner.withBean(InfluxDbCustomizer.class, () -> (influxDb) -> influxDb.setDatabase("test"))
				.withPropertyValues("spring.influx.url=http://localhost").run((context) -> {
					assertThat(context).hasSingleBean(InfluxDB.class);
					InfluxDB influxDb = context.getBean(InfluxDB.class);
					assertThat(influxDb).hasFieldOrPropertyWithValue("database", "test");
				});
	}

	@Test
	void influxDbClientRequiresUrl() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(InfluxDBClient.class));
	}

	@Test
	void influxDbClientCanBeCustomized() {
		this.contextRunner
				.withPropertyValues("spring.influx.url=http://localhost", "spring.influx.user=user",
						"spring.influx.password=password")
				.run((context) -> assertThat(context).hasSingleBean(InfluxDBClient.class));
	}

	@Test
	void influxDbClientCanBeCreatedWithoutCredentials() {
		this.contextRunner.withPropertyValues("spring.influx.url=http://localhost").run(assertInfluxDbClientOptions(
				(options) -> assertThat(options.getOkHttpClient().build().readTimeoutMillis()).isEqualTo(10000)));
	}

	@Test
	void influxDbClientWithOkHttpClientBuilderProvider() {
		this.contextRunner.withUserConfiguration(CustomOkHttpClientBuilderProviderConfig.class)
				.withPropertyValues("spring.influx.url=http://localhost")
				.run(assertInfluxDbClientOptions(
						(options) -> assertThat(options.getOkHttpClient().build().readTimeoutMillis())
								.isEqualTo(40000)));
	}

	@Test
	void influxDbClientWithCustomizer() {
		this.contextRunner
				.withBean(InfluxDbClientOptionsBuilderCustomizer.class, () -> (options) -> options.org("my_org"))
				.withPropertyValues("spring.influx.url=http://localhost")
				.run(assertInfluxDbClientOptions((options) -> assertThat(options.getOrg()).isEqualTo("my_org")));
	}

	private int getReadTimeoutProperty(AssertableApplicationContext context) {
		InfluxDB influxDb = context.getBean(InfluxDB.class);
		Retrofit retrofit = (Retrofit) ReflectionTestUtils.getField(influxDb, "retrofit");
		OkHttpClient callFactory = (OkHttpClient) retrofit.callFactory();
		return callFactory.readTimeoutMillis();
	}

	private ContextConsumer<AssertableApplicationContext> assertInfluxDbClientOptions(
			Consumer<InfluxDBClientOptions> options) {
		return (context) -> {
			assertThat(context).hasSingleBean(InfluxDBClient.class);
			assertThat(context).getBean(InfluxDBClient.class)
					.extracting("options", InstanceOfAssertFactories.type(InfluxDBClientOptions.class))
					.satisfies(options);
		};
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomOkHttpClientBuilderProviderConfig {

		@Bean
		InfluxDbOkHttpClientBuilderProvider influxDbOkHttpClientBuilderProvider() {
			return () -> new OkHttpClient.Builder().readTimeout(40, TimeUnit.SECONDS);
		}

	}

}
