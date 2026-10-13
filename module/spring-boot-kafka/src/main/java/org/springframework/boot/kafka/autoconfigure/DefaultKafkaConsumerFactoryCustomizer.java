/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

/**
 * Callback interface for customizing {@code DefaultKafkaConsumerFactory} beans.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface DefaultKafkaConsumerFactoryCustomizer {

	/**
	 * Customize the {@link DefaultKafkaConsumerFactory}.
	 * @param consumerFactory the consumer factory to customize
	 */
	void customize(DefaultKafkaConsumerFactory<?, ?> consumerFactory);

}
