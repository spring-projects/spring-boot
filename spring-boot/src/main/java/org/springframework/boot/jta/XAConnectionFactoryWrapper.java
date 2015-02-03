/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.jta;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;

/**
 * Strategy interface used to wrap a JMS {@link XAConnectionFactory} enrolling it with a
 * JTA {@link TransactionManager}.
 *
 * @author Phillip Webb
 * @since 1.2.0
 */
public interface XAConnectionFactoryWrapper {

	/**
	 * Wrap the specific {@link XAConnectionFactory} and enroll it with a JTA
	 * {@link TransactionManager}.
	 * @param connectionFactory the connection factory to wrap
	 * @return the wrapped connection factory
	 * @throws Exception if the connection factory cannot be wrapped
	 */
	ConnectionFactory wrapConnectionFactory(XAConnectionFactory connectionFactory)
			throws Exception;

}
