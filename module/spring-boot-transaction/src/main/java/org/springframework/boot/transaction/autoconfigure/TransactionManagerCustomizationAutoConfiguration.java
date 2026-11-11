/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.transaction.autoconfigure;

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
 * @since 4.0.0
 */
@ConditionalOnClass(PlatformTransactionManager.class)
@AutoConfiguration(before = TransactionAutoConfiguration.class)
@EnableConfigurationProperties(TransactionProperties.class)
public final class TransactionManagerCustomizationAutoConfiguration {

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
