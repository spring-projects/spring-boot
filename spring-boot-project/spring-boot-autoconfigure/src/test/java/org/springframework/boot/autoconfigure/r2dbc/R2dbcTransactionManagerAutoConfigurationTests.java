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

package org.springframework.boot.autoconfigure.r2dbc;

import java.time.Duration;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.TransactionDefinition;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import org.springframework.transaction.reactive.TransactionalOperator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link R2dbcTransactionManagerAutoConfiguration}.
 *
 * @author Mark Paluch
 * @author Oliver Drotbohm
 */
class R2dbcTransactionManagerAutoConfigurationTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner().withConfiguration(
			AutoConfigurations.of(R2dbcTransactionManagerAutoConfiguration.class, TransactionAutoConfiguration.class));

	@Test
	void noTransactionManager() {
		this.contextRunner.run((context) -> assertThat(context).doesNotHaveBean(ReactiveTransactionManager.class));
	}

	@Test
	void singleTransactionManager() {
		this.contextRunner.withUserConfiguration(SingleConnectionFactoryConfiguration.class)
			.run((context) -> assertThat(context).hasSingleBean(TransactionalOperator.class)
				.hasSingleBean(ReactiveTransactionManager.class));
	}

	@Test
	void coexistenceOfCustomTransactionManagerWithDefaultOne() {
		this.contextRunner
				.withUserConfiguration(SingleConnectionFactoryConfiguration.class)
				.withConfiguration(AutoConfigurations.of(AlwaysReadOnlyTransactionManagerAutoConfiguration.class))
			.run((context) -> {
				ReactiveTransactionManager defaultTm = context.getBean(ReactiveTransactionManager.class);
				ReactiveTransactionManager customTm = context.getBean("customTransactionManager", CustomAlwaysReadOnlyTransactionManager.class);
				assertThat(defaultTm).isNotSameAs(customTm);  // verifying different instances
				assertThat(defaultTm).isInstanceOf(R2dbcTransactionManager.class);
				assertThat(customTm).isInstanceOf(CustomAlwaysReadOnlyTransactionManager.class);
			});
	}

	@Test
	void transactionManagerEnabled() {
		this.contextRunner.withUserConfiguration(SingleConnectionFactoryConfiguration.class, BaseConfiguration.class)
			.run((context) -> {
				TransactionalService bean = context.getBean(TransactionalService.class);
				bean.isTransactionActive()
					.as(StepVerifier::create)
					.expectNext(true)
					.expectComplete()
					.verify(Duration.ofSeconds(30));
			});
	}

	@Configuration(proxyBeanMethods = false)
	static class SingleConnectionFactoryConfiguration {

		@Bean
		ConnectionFactory connectionFactory() {
			ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
			Connection connection = mock(Connection.class);
			given(connectionFactory.create()).willAnswer((invocation) -> Mono.just(connection));
			given(connection.beginTransaction(any(TransactionDefinition.class))).willReturn(Mono.empty());
			given(connection.commitTransaction()).willReturn(Mono.empty());
			given(connection.close()).willReturn(Mono.empty());
			return connectionFactory;
		}

	}

	@Configuration(proxyBeanMethods = false)
	@EnableTransactionManagement
	static class BaseConfiguration {

		@Bean
		TransactionalService transactionalService() {
			return new TransactionalServiceImpl();
		}

	}

	interface TransactionalService {

		@Transactional
		Mono<Boolean> isTransactionActive();

	}

	static class TransactionalServiceImpl implements TransactionalService {

		@Override
		public Mono<Boolean> isTransactionActive() {
			return TransactionSynchronizationManager.forCurrentTransaction()
				.map(TransactionSynchronizationManager::isActualTransactionActive);
		}

	}

	@AutoConfiguration(after = R2dbcTransactionManagerAutoConfiguration.class)
	static class AlwaysReadOnlyTransactionManagerAutoConfiguration {

		@Bean
		CustomAlwaysReadOnlyTransactionManager customTransactionManager(ConnectionFactory connectionFactory) {
			return new CustomAlwaysReadOnlyTransactionManager(connectionFactory);
		}

	}
	static class CustomAlwaysReadOnlyTransactionManager extends R2dbcTransactionManager {
		public CustomAlwaysReadOnlyTransactionManager(ConnectionFactory connectionFactory) {
			super(connectionFactory);
		}

		@Override
		public boolean isEnforceReadOnly() {
			return true;
		}
	}

}
