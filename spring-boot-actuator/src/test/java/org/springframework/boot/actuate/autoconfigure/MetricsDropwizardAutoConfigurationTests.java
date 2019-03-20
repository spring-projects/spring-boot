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

package org.springframework.boot.actuate.autoconfigure;

import com.codahale.metrics.Reservoir;
import com.codahale.metrics.UniformReservoir;
import org.junit.After;
import org.junit.Test;

import org.springframework.boot.actuate.metrics.dropwizard.DropwizardMetricServices;
import org.springframework.boot.actuate.metrics.dropwizard.ReservoirFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MetricsDropwizardAutoConfiguration}.
 *
 * @author Lucas Saldanha
 */
public class MetricsDropwizardAutoConfigurationTests {

	private AnnotationConfigApplicationContext context;

	@After
	public void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	public void dropwizardWithoutCustomReservoirConfigured() {
		this.context = new AnnotationConfigApplicationContext(
				MetricsDropwizardAutoConfiguration.class);
		DropwizardMetricServices dropwizardMetricServices = this.context
				.getBean(DropwizardMetricServices.class);
		ReservoirFactory reservoirFactory = (ReservoirFactory) ReflectionTestUtils
				.getField(dropwizardMetricServices, "reservoirFactory");
		assertThat(reservoirFactory.getReservoir("test")).isNull();
	}

	@Test
	public void dropwizardWithCustomReservoirConfigured() {
		this.context = new AnnotationConfigApplicationContext(
				MetricsDropwizardAutoConfiguration.class, Config.class);
		DropwizardMetricServices dropwizardMetricServices = this.context
				.getBean(DropwizardMetricServices.class);
		ReservoirFactory reservoirFactory = (ReservoirFactory) ReflectionTestUtils
				.getField(dropwizardMetricServices, "reservoirFactory");
		assertThat(reservoirFactory.getReservoir("test"))
				.isInstanceOf(UniformReservoir.class);
	}

	@Configuration
	static class Config {

		@Bean
		public ReservoirFactory reservoirFactory() {
			return new UniformReservoirFactory();
		}

	}

	private static class UniformReservoirFactory implements ReservoirFactory {

		@Override
		public Reservoir getReservoir(String name) {
			return new UniformReservoir();
		}

	}

}
