/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.jta.narayana;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jboss.narayana.jta.jms.JmsXAResourceRecoveryHelper;
import org.jboss.narayana.jta.jms.TransactionHelperImpl;

import org.springframework.boot.jta.XAConnectionFactoryWrapper;
import org.springframework.util.Assert;

/**
 * {@link XAConnectionFactoryWrapper} that uses {@link ConnectionFactoryProxy} to wrap an
 * {@link XAConnectionFactory}.
 *
 * @author Gytis Trikleris
 * @since 1.4.0
 */
public class NarayanaXAConnectionFactoryWrapper implements XAConnectionFactoryWrapper {

	private final TransactionManager transactionManager;

	private final NarayanaRecoveryManagerBean recoveryManager;

	private final NarayanaProperties properties;

	/**
	 * Create a new {@link NarayanaXAConnectionFactoryWrapper} instance.
	 * @param transactionManager the underlying transaction manager
	 * @param recoveryManager the underlying recovery manager
	 * @param properties the Narayana properties
	 */
	public NarayanaXAConnectionFactoryWrapper(TransactionManager transactionManager,
			NarayanaRecoveryManagerBean recoveryManager, NarayanaProperties properties) {
		Assert.notNull(transactionManager, "TransactionManager must not be null");
		Assert.notNull(recoveryManager, "RecoveryManager must not be null");
		Assert.notNull(properties, "Properties must not be null");
		this.transactionManager = transactionManager;
		this.recoveryManager = recoveryManager;
		this.properties = properties;
	}

	@Override
	public ConnectionFactory wrapConnectionFactory(
			XAConnectionFactory xaConnectionFactory) {
		XAResourceRecoveryHelper recoveryHelper = getRecoveryHelper(xaConnectionFactory);
		this.recoveryManager.registerXAResourceRecoveryHelper(recoveryHelper);
		return new ConnectionFactoryProxy(xaConnectionFactory,
				new TransactionHelperImpl(this.transactionManager));
	}

	private XAResourceRecoveryHelper getRecoveryHelper(
			XAConnectionFactory xaConnectionFactory) {
		if (this.properties.getRecoveryJmsUser() == null
				&& this.properties.getRecoveryJmsPass() == null) {
			return new JmsXAResourceRecoveryHelper(xaConnectionFactory);
		}
		return new JmsXAResourceRecoveryHelper(xaConnectionFactory,
				this.properties.getRecoveryJmsUser(),
				this.properties.getRecoveryJmsPass());
	}

}
