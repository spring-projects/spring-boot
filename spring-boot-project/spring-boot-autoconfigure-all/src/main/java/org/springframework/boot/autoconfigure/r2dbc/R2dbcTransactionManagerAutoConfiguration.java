/*
 * Copyright 2012-2022 the original author or authors.
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

import io.r2dbc.spi.ConnectionFactory;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link R2dbcTransactionManager}.
 *
 * @author Mark Paluch
 * @since 2.3.0
 */
@AutoConfiguration(before = TransactionAutoConfiguration.class)
@ConditionalOnClass({ R2dbcTransactionManager.class, ReactiveTransactionManager.class })
@ConditionalOnSingleCandidate(ConnectionFactory.class)
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
public class R2dbcTransactionManagerAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ReactiveTransactionManager.class)
	public R2dbcTransactionManager connectionFactoryTransactionManager(ConnectionFactory connectionFactory) {
		return new R2dbcTransactionManager(connectionFactory);
	}

}
