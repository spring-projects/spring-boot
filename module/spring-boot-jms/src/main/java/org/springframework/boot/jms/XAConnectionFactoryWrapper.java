/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.jms;

import jakarta.jms.ConnectionFactory;
import jakarta.jms.XAConnectionFactory;
import jakarta.transaction.TransactionManager;

/**
 * Strategy interface used to wrap a JMS {@link XAConnectionFactory} enrolling it with a
 * JTA {@link TransactionManager}.
 *
 * @author Phillip Webb
 * @since 2.0.0
 */
@FunctionalInterface
public interface XAConnectionFactoryWrapper {

	/**
	 * Wrap the specific {@link XAConnectionFactory} and enroll it with a JTA
	 * {@link TransactionManager}.
	 * @param connectionFactory the connection factory to wrap
	 * @return the wrapped connection factory
	 * @throws Exception if the connection factory cannot be wrapped
	 */
	ConnectionFactory wrapConnectionFactory(XAConnectionFactory connectionFactory) throws Exception;

}
