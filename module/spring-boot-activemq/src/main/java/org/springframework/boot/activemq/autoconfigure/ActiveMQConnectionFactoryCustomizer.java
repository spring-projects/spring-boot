/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.activemq.autoconfigure;

import org.apache.activemq.ActiveMQConnectionFactory;

/**
 * Callback interface that can be implemented by beans wishing to customize the
 * {@link ActiveMQConnectionFactory} to fine-tune its auto-configuration.
 *
 * @author Stephane Nicoll
 * @since 4.0.0
 */
@FunctionalInterface
public interface ActiveMQConnectionFactoryCustomizer {

	/**
	 * Customize the {@link ActiveMQConnectionFactory}.
	 * @param factory the factory to customize
	 */
	void customize(ActiveMQConnectionFactory factory);

}
