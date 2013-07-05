/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.zero.actuate.autoconfigure;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.zero.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.zero.actuate.metrics.CounterService;
import org.springframework.zero.actuate.metrics.DefaultCounterService;
import org.springframework.zero.actuate.metrics.DefaultGaugeService;
import org.springframework.zero.actuate.metrics.GaugeService;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link MetricRepositoryAutoConfiguration}.
 * 
 * @author Phillip Webb
 */
public class MetricRepositoryAutoConfigurationTests {

	@Test
	public void createServices() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				MetricRepositoryAutoConfiguration.class);
		assertNotNull(context.getBean(DefaultGaugeService.class));
		assertNotNull(context.getBean(DefaultCounterService.class));
		context.close();
	}

	@Test
	public void skipsIfBeansExist() throws Exception {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
				Config.class, MetricRepositoryAutoConfiguration.class);
		assertThat(context.getBeansOfType(DefaultGaugeService.class).size(), equalTo(0));
		assertThat(context.getBeansOfType(DefaultCounterService.class).size(), equalTo(0));
		context.close();
	}

	@Configuration
	public static class Config {

		@Bean
		public GaugeService gaugeService() {
			return mock(GaugeService.class);
		}

		@Bean
		public CounterService counterService() {
			return mock(CounterService.class);
		}

	}
}
