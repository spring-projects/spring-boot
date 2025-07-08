/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.amqp.autoconfigure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test to check that {@link RabbitMetricsAutoConfiguration} does not cause a
 * dependency cycle when used with {@link MeterBinder}.
 *
 * @author Phillip Webb
 * @see <a href="https://github.com/spring-projects/spring-boot/issues/30636">gh-30636</a>
 */
class RabbitMetricsAutoConfigurationMeterBinderCycleIntegrationTests {

	@Test
	void doesNotFormCycle() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(TestConfig.class);
		TestService testService = context.getBean(TestService.class);
		context.close();
		assertThat(testService.bound).isTrue();
	}

	@Configuration
	@Import({ TestService.class, RabbitAutoConfiguration.class, MetricsAutoConfiguration.class,
			RabbitMetricsAutoConfiguration.class })
	static class TestConfig {

		@Bean
		SimpleMeterRegistry meterRegistry() {
			return new SimpleMeterRegistry();
		}

	}

	static class TestService implements MeterBinder {

		private boolean bound;

		TestService(RabbitTemplate rabbitTemplate) {
		}

		@Override
		public void bindTo(MeterRegistry registry) {
			this.bound = true;
		}

	}

}
