/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.kafka.autoconfigure;

import org.springframework.kafka.core.DefaultKafkaProducerFactory;

/**
 * Callback interface for customizing {@code DefaultKafkaProducerFactory} beans.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface DefaultKafkaProducerFactoryCustomizer {

	/**
	 * Customize the {@link DefaultKafkaProducerFactory}.
	 * @param producerFactory the producer factory to customize
	 */
	void customize(DefaultKafkaProducerFactory<?, ?> producerFactory);

}
