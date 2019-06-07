/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.jta.narayana;

import javax.jms.ConnectionFactory;
import javax.jms.XAConnectionFactory;
import javax.transaction.TransactionManager;

import org.jboss.narayana.jta.jms.ConnectionFactoryProxy;
import org.jboss.narayana.jta.jms.JmsXAResourceRecoveryHelper;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link NarayanaXAConnectionFactoryWrapper}.
 *
 * @author Gytis Trikleris
 */
public class NarayanaXAConnectionFactoryWrapperTests {

	private XAConnectionFactory connectionFactory = mock(XAConnectionFactory.class);

	private TransactionManager transactionManager = mock(TransactionManager.class);

	private NarayanaRecoveryManagerBean recoveryManager = mock(NarayanaRecoveryManagerBean.class);

	private NarayanaProperties properties = mock(NarayanaProperties.class);

	private NarayanaXAConnectionFactoryWrapper wrapper = new NarayanaXAConnectionFactoryWrapper(this.transactionManager,
			this.recoveryManager, this.properties);

	@Test
	public void wrap() {
		ConnectionFactory wrapped = this.wrapper.wrapConnectionFactory(this.connectionFactory);
		assertThat(wrapped).isInstanceOf(ConnectionFactoryProxy.class);
		verify(this.recoveryManager, times(1)).registerXAResourceRecoveryHelper(any(JmsXAResourceRecoveryHelper.class));
		verify(this.properties, times(1)).getRecoveryJmsUser();
		verify(this.properties, times(1)).getRecoveryJmsPass();
	}

	@Test
	public void wrapWithCredentials() {
		given(this.properties.getRecoveryJmsUser()).willReturn("userName");
		given(this.properties.getRecoveryJmsPass()).willReturn("password");
		ConnectionFactory wrapped = this.wrapper.wrapConnectionFactory(this.connectionFactory);
		assertThat(wrapped).isInstanceOf(ConnectionFactoryProxy.class);
		verify(this.recoveryManager, times(1)).registerXAResourceRecoveryHelper(any(JmsXAResourceRecoveryHelper.class));
		verify(this.properties, times(2)).getRecoveryJmsUser();
		verify(this.properties, times(1)).getRecoveryJmsPass();
	}

}
