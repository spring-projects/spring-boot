/*
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

package org.springframework.boot.autoconfigure.influx;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import org.influxdb.InfluxDB;
import org.junit.After;
import org.junit.Test;
import retrofit2.Retrofit;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfluxDbAutoConfiguration}.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
 * @author Eddú Meléndez
 */
public class InfluxDbAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void tearDown() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void influxDbRequiresUrl() {
		load();
		assertThat(this.context.getBeansOfType(InfluxDB.class)).isEmpty();
	}

	@Test
	public void influxDbCanBeCustomized() {
		load("spring.influx.url=http://localhost", "spring.influx.password:password",
				"spring.influx.user:user");
		assertThat(this.context.getBeansOfType(InfluxDB.class)).hasSize(1);
	}

	@Test
	public void influxDbCanBeCreatedWithoutCredentials() {
		load("spring.influx.url=http://localhost");
		assertThat(this.context.getBeansOfType(InfluxDB.class)).hasSize(1);
		int readTimeout = getReadTimeoutProperty();
		assertThat(readTimeout).isEqualTo(10_000);
	}

	@Test
	public void influxDbWithoutCredentialsAndOkHttpClientBuilder() {
		load(CustomOkHttpClientBuilderConfig.class, "spring.influx.url=http://localhost");
		assertThat(this.context.getBeansOfType(InfluxDB.class)).hasSize(1);
		int readTimeout = getReadTimeoutProperty();
		assertThat(readTimeout).isEqualTo(30_000);
	}

	@Test
	public void influxDbWithOkHttpClientBuilder() {
		load(CustomOkHttpClientBuilderConfig.class, "spring.influx.url=http://localhost",
				"spring.influx.password:password", "spring.influx.user:user");
		assertThat(this.context.getBeansOfType(InfluxDB.class)).hasSize(1);
		int readTimeout = getReadTimeoutProperty();
		assertThat(readTimeout).isEqualTo(30_000);
	}

	private int getReadTimeoutProperty() {
		InfluxDB influxDB = this.context.getBean(InfluxDB.class);
		Retrofit retrofit = (Retrofit) new DirectFieldAccessor(influxDB)
				.getPropertyValue("retrofit");
		OkHttpClient callFactory = (OkHttpClient) new DirectFieldAccessor(retrofit)
				.getPropertyValue("callFactory");
		return callFactory.readTimeoutMillis();
	}

	private void load(Class<?> clazz, String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.register(InfluxDbAutoConfiguration.class);
		if (clazz != null) {
			ctx.register(clazz);
		}
		ctx.refresh();
		this.context = ctx;
	}

	private void load(String... environment) {
		load(null, environment);
	}

	@Configuration
	static class CustomOkHttpClientBuilderConfig {

		@Bean
		public OkHttpClient.Builder builder() {
			return new OkHttpClient.Builder().readTimeout(30, TimeUnit.SECONDS);
		}

	}

}
