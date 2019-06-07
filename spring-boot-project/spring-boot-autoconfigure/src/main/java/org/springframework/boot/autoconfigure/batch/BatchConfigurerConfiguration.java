/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.batch;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.transaction.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Provide a {@link BatchConfigurer} according to the current environment.
 *
 * @author Stephane Nicoll
 */
@ConditionalOnClass(PlatformTransactionManager.class)
@ConditionalOnMissingBean(BatchConfigurer.class)
@Configuration
class BatchConfigurerConfiguration {

	@Configuration
	@ConditionalOnMissingBean(name = "entityManagerFactory")
	static class JdbcBatchConfiguration {

		@Bean
		public BasicBatchConfigurer batchConfigurer(BatchProperties properties, DataSource dataSource,
				ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
			return new BasicBatchConfigurer(properties, dataSource, transactionManagerCustomizers.getIfAvailable());
		}

	}

	@Configuration
	@ConditionalOnClass(name = "javax.persistence.EntityManagerFactory")
	@ConditionalOnBean(name = "entityManagerFactory")
	static class JpaBatchConfiguration {

		@Bean
		public JpaBatchConfigurer batchConfigurer(BatchProperties properties, DataSource dataSource,
				ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers,
				EntityManagerFactory entityManagerFactory) {
			return new JpaBatchConfigurer(properties, dataSource, transactionManagerCustomizers.getIfAvailable(),
					entityManagerFactory);
		}

	}

}
