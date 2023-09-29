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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionExecutionListener;
import org.springframework.transaction.TransactionManager;

/**
 * Auto-configuration for the customization of a {@link TransactionManager}.
 *
 * @author Andy Wilkinson
 * @since 3.2.0
 */
@ConditionalOnClass(PlatformTransactionManager.class)
@AutoConfiguration(before = TransactionAutoConfiguration.class)
@EnableConfigurationProperties(TransactionProperties.class)
public class TransactionManagerCustomizationAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	TransactionManagerCustomizers platformTransactionManagerCustomizers(
			ObjectProvider<TransactionManagerCustomizer<?>> customizers) {
		return TransactionManagerCustomizers.of(customizers.orderedStream().toList());
	}

	@Bean
	ExecutionListenersTransactionManagerCustomizer transactionExecutionListeners(
			ObjectProvider<TransactionExecutionListener> listeners) {
		return new ExecutionListenersTransactionManagerCustomizer(listeners.orderedStream().toList());
	}

}
