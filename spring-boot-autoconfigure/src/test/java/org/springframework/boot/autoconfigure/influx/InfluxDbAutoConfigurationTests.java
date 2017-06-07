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

import org.influxdb.InfluxDB;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link InfluxDbAutoConfiguration}.
 *
 * @author Sergey Kuptsov
 * @author Stephane Nicoll
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
		load("spring.influx.url=http://localhost",
				"spring.influx.password:password",
				"spring.influx.user:user");
		assertThat(this.context.getBeansOfType(InfluxDB.class)).hasSize(1);
	}

	@Test
	public void influxDbCanBeCreatedWithoutCredentials() {
		load("spring.influx.url=http://localhost");
		assertThat(this.context.getBeansOfType(InfluxDB.class)).hasSize(1);
	}

	private void load(String... environment) {
		AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
		TestPropertyValues.of(environment).applyTo(ctx);
		ctx.register(InfluxDbAutoConfiguration.class);
		ctx.refresh();
		this.context = ctx;
	}

}
