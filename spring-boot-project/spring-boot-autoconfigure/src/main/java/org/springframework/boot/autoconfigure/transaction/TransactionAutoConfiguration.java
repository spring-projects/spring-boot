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

package org.springframework.boot.autoconfigure.transaction;

import org.springframework.boot.LazyInitializationExcludeFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.ReactiveTransactionManager;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.annotation.AbstractTransactionManagementConfiguration;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.aspectj.AbstractTransactionAspect;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * Auto-configuration} for Spring transaction.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 */
@AutoConfiguration
@ConditionalOnClass(PlatformTransactionManager.class)
public class TransactionAutoConfiguration {

	/**
	 * Creates a TransactionalOperator bean if no other bean of the same type is present
	 * in the application context. The bean is conditionally created only if a single
	 * candidate of type ReactiveTransactionManager is available.
	 * @param transactionManager the ReactiveTransactionManager bean to be used for
	 * creating the TransactionalOperator
	 * @return the TransactionalOperator bean created using the provided
	 * ReactiveTransactionManager
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnSingleCandidate(ReactiveTransactionManager.class)
	public TransactionalOperator transactionalOperator(ReactiveTransactionManager transactionManager) {
		return TransactionalOperator.create(transactionManager);
	}

	/**
	 * TransactionTemplateConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnSingleCandidate(PlatformTransactionManager.class)
	public static class TransactionTemplateConfiguration {

		/**
		 * Creates a new TransactionTemplate bean if no other bean of type
		 * TransactionOperations is present.
		 * @param transactionManager the PlatformTransactionManager to be used by the
		 * TransactionTemplate
		 * @return a new TransactionTemplate instance
		 */
		@Bean
		@ConditionalOnMissingBean(TransactionOperations.class)
		public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
			return new TransactionTemplate(transactionManager);
		}

	}

	/**
	 * EnableTransactionManagementConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(TransactionManager.class)
	@ConditionalOnMissingBean(AbstractTransactionManagementConfiguration.class)
	public static class EnableTransactionManagementConfiguration {

		/**
		 * JdkDynamicAutoProxyConfiguration class.
		 */
		@Configuration(proxyBeanMethods = false)
		@EnableTransactionManagement(proxyTargetClass = false)
		@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "false")
		public static class JdkDynamicAutoProxyConfiguration {

		}

		/**
		 * CglibAutoProxyConfiguration class.
		 */
		@Configuration(proxyBeanMethods = false)
		@EnableTransactionManagement(proxyTargetClass = true)
		@ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "true",
				matchIfMissing = true)
		public static class CglibAutoProxyConfiguration {

		}

	}

	/**
	 * AspectJTransactionManagementConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnBean(AbstractTransactionAspect.class)
	static class AspectJTransactionManagementConfiguration {

		/**
		 * Returns a LazyInitializationExcludeFilter for the AbstractTransactionAspect
		 * class. This filter is used to exclude the AbstractTransactionAspect class from
		 * being eagerly initialized.
		 * @return the LazyInitializationExcludeFilter for the AbstractTransactionAspect
		 * class
		 */
		@Bean
		static LazyInitializationExcludeFilter eagerTransactionAspect() {
			return LazyInitializationExcludeFilter.forBeanTypes(AbstractTransactionAspect.class);
		}

	}

}
