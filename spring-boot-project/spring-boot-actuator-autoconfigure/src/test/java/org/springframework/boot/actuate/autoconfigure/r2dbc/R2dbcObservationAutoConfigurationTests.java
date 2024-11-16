/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.r2dbc;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import io.micrometer.observation.Observation.Context;
import io.micrometer.observation.ObservationHandler;
import io.micrometer.observation.ObservationRegistry;
import io.r2dbc.spi.ConnectionFactory;
import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.r2dbc.ProxyConnectionFactoryCustomizer;
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProxyAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.r2dbc.ConnectionFactoryBuilder;
import org.springframework.boot.r2dbc.ConnectionFactoryDecorator;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link R2dbcObservationAutoConfiguration}.
 *
 * @author Moritz Halbritter
 * @author Tadaya Tsuyukubo
 */
class R2dbcObservationAutoConfigurationTests {

	private final ApplicationContextRunner runnerWithoutObservationRegistry = new ApplicationContextRunner()
		.withConfiguration(
				AutoConfigurations.of(R2dbcProxyAutoConfiguration.class, R2dbcObservationAutoConfiguration.class));

	private final ApplicationContextRunner runner = this.runnerWithoutObservationRegistry
		.withBean(ObservationRegistry.class, ObservationRegistry::create);

	@Test
	void shouldBeRegisteredInAutoConfigurationImports() {
		assertThat(ImportCandidates.load(AutoConfiguration.class, null).getCandidates())
			.contains(R2dbcObservationAutoConfiguration.class.getName());
	}

	@Test
	void shouldNotSupplyBeansIfObservationRegistryIsNotPresent() {
		this.runnerWithoutObservationRegistry
			.run((context) -> assertThat(context).doesNotHaveBean(ProxyConnectionFactoryCustomizer.class));
	}

	@Test
	void decoratorShouldReportObservations() {
		this.runner.run((context) -> {
			CapturingObservationHandler handler = registerCapturingObservationHandler(context);
			ConnectionFactoryDecorator decorator = context.getBean(ConnectionFactoryDecorator.class);
			assertThat(decorator).isNotNull();
			ConnectionFactory connectionFactory = ConnectionFactoryBuilder
				.withUrl("r2dbc:h2:mem:///" + UUID.randomUUID())
				.build();
			ConnectionFactory decorated = decorator.decorate(connectionFactory);
			Mono.from(decorated.create())
				.flatMap((c) -> Mono.from(c.createStatement("SELECT 1;").execute())
					.flatMap((ignore) -> Mono.from(c.close())))
				.block();
			assertThat(handler.awaitContext().getName()).as("context.getName()").isEqualTo("r2dbc.query");
		});
	}

	private static CapturingObservationHandler registerCapturingObservationHandler(
			AssertableApplicationContext context) {
		ObservationRegistry observationRegistry = context.getBean(ObservationRegistry.class);
		assertThat(observationRegistry).isNotNull();
		CapturingObservationHandler handler = new CapturingObservationHandler();
		observationRegistry.observationConfig().observationHandler(handler);
		return handler;
	}

	private static final class CapturingObservationHandler implements ObservationHandler<Context> {

		private final AtomicReference<Context> context = new AtomicReference<>();

		@Override
		public boolean supportsContext(Context context) {
			return true;
		}

		@Override
		public void onStart(Context context) {
			this.context.set(context);
		}

		Context awaitContext() {
			return Awaitility.await().untilAtomic(this.context, Matchers.notNullValue());
		}

	}

}
