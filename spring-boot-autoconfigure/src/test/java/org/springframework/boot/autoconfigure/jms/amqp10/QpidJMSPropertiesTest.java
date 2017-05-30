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

package org.springframework.boot.autoconfigure.jms.amqp10;

import org.apache.qpid.jms.JmsConnectionFactory;
import org.apache.qpid.jms.policy.JmsDefaultDeserializationPolicy;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for Qpid JMS Properties object.
 *
 * @author Timothy Bish
 */
public class QpidJMSPropertiesTest {

	private static final String DEFAULT_REMOTE_URI = "amqp://localhost:5672";

	private final QpidJMSProperties properties = new QpidJMSProperties();

	@Test
	public void testDefaultURL() {
		assertThat(new QpidJMSConnectionFactoryFactory(this.properties)
				.getRemoteURI()).isEqualTo(DEFAULT_REMOTE_URI);
	}

	@Test
	public void testWhiteListDefaultToEmpty() {
		JmsConnectionFactory factory = new QpidJMSConnectionFactoryFactory(
				this.properties).createConnectionFactory(JmsConnectionFactory.class);

		JmsDefaultDeserializationPolicy policy = (JmsDefaultDeserializationPolicy) factory.getDeserializationPolicy();

		assertThat(policy.getWhiteList().length()).isEqualTo(1);
	}

	@Test
	public void testLocalOnlyReceiveOptions() {
		this.properties.setReceiveLocalOnly(true);
		this.properties.setReceiveNoWaitLocalOnly(true);

		JmsConnectionFactory factory = new QpidJMSConnectionFactoryFactory(
				this.properties).createConnectionFactory(JmsConnectionFactory.class);

		assertThat(factory.isReceiveLocalOnly()).isTrue();
		assertThat(factory.isReceiveNoWaitLocalOnly()).isTrue();
	}

	@Test
	public void testBlackListDefaultToEmpty() {
		JmsConnectionFactory factory = new QpidJMSConnectionFactoryFactory(
				this.properties).createConnectionFactory(JmsConnectionFactory.class);

		JmsDefaultDeserializationPolicy policy = (JmsDefaultDeserializationPolicy) factory.getDeserializationPolicy();

		assertThat(policy.getBlackList().length()).isEqualTo(0);
	}

	@Test
	public void testDeserializationPolicyValuesAreApplied() {
		this.properties.getDeserializationPolicy().setWhiteList("org.apache.qpid.proton.*");
		this.properties.getDeserializationPolicy().setBlackList("org.apache.activemq..*");

		JmsConnectionFactory factory = new QpidJMSConnectionFactoryFactory(
				this.properties).createConnectionFactory(JmsConnectionFactory.class);

		JmsDefaultDeserializationPolicy policy = (JmsDefaultDeserializationPolicy) factory.getDeserializationPolicy();

		assertThat(policy.getWhiteList()).isEqualTo("org.apache.qpid.proton.*");
		assertThat(policy.getBlackList()).isEqualTo("org.apache.activemq..*");
	}
}
