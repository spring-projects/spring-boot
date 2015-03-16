/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.health;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ConnectionMetaData;
import javax.jms.JMSException;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link JmsHealthIndicator}.
 *
 * @author Stephane Nicoll
 */
public class JmsHealthIndicatorTests {

	@Test
	public void jmsBrokerIsUp() throws JMSException {
		ConnectionMetaData connectionMetaData = mock(ConnectionMetaData.class);
		given(connectionMetaData.getJMSProviderName()).willReturn("JMS test provider");
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(connectionMetaData);
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);
		JmsHealthIndicator indicator = new JmsHealthIndicator(connectionFactory);
		Health health = indicator.health();
		assertEquals(Status.UP, health.getStatus());
		assertEquals("JMS test provider", health.getDetails().get("provider"));
		verify(connection, times(1)).close();
	}

	@Test
	public void jmsBrokerIsDown() throws JMSException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willThrow(
				new JMSException("test", "123"));
		JmsHealthIndicator indicator = new JmsHealthIndicator(connectionFactory);
		Health health = indicator.health();
		assertEquals(Status.DOWN, health.getStatus());
		assertEquals(null, health.getDetails().get("provider"));
	}

	@Test
	public void jmsBrokerCouldNotRetrieveProviderMetadata() throws JMSException {
		ConnectionMetaData connectionMetaData = mock(ConnectionMetaData.class);
		given(connectionMetaData.getJMSProviderName()).willThrow(
				new JMSException("test", "123"));
		Connection connection = mock(Connection.class);
		given(connection.getMetaData()).willReturn(connectionMetaData);
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);
		JmsHealthIndicator indicator = new JmsHealthIndicator(connectionFactory);
		Health health = indicator.health();
		assertEquals(Status.DOWN, health.getStatus());
		assertEquals(null, health.getDetails().get("provider"));
		verify(connection, times(1)).close();
	}

}
