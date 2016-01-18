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

package org.springframework.boot.jta.narayana;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;

import com.arjuna.ats.jta.recovery.XAResourceRecoveryHelper;
import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jboss.narayana.jta.jms.JmsXAResourceRecoveryHelper;
import org.jboss.narayana.jta.jms.TransactionHelperImpl;

import org.springframework.boot.jta.XAConnectionFactoryWrapper;

/**
 * {@link XAConnectionFactoryWrapper} that uses {@link ConnectionFactoryProxy} to wrap an {@link XAConnectionFactory}.
 *
 * @author <a href="mailto:gytis@redhat.com">Gytis Trikleris</a>
 */
public class NarayanaXAConnectionFactoryWrapper implements XAConnectionFactoryWrapper {

	private final TransactionManager transactionManager;

	private final NarayanaRecoveryManagerBean narayanaRecoveryManagerBean;

	private final NarayanaProperties narayanaProperties;

	public NarayanaXAConnectionFactoryWrapper(TransactionManager transactionManager,
			NarayanaRecoveryManagerBean narayanaRecoveryManagerBean, NarayanaProperties narayanaProperties) {
		this.transactionManager = transactionManager;
		this.narayanaRecoveryManagerBean = narayanaRecoveryManagerBean;
		this.narayanaProperties = narayanaProperties;
	}

	@Override
	public ConnectionFactory wrapConnectionFactory(XAConnectionFactory xaConnectionFactory) {
		this.narayanaRecoveryManagerBean.registerXAResourceRecoveryHelper(getRecoveryHelper(xaConnectionFactory));

		return new ConnectionFactoryProxy(xaConnectionFactory, new TransactionHelperImpl(this.transactionManager));
	}

	private XAResourceRecoveryHelper getRecoveryHelper(XAConnectionFactory xaConnectionFactory) {
		if (this.narayanaProperties.getRecoveryJmsUser() == null && this.narayanaProperties.getRecoveryJmsPass() == null) {
			return new JmsXAResourceRecoveryHelper(xaConnectionFactory);
		}

		return new JmsXAResourceRecoveryHelper(xaConnectionFactory, this.narayanaProperties.getRecoveryJmsUser(),
				this.narayanaProperties.getRecoveryJmsPass());
	}

}
