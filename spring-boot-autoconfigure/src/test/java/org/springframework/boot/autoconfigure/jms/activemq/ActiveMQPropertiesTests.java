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

package org.springframework.boot.autoconfigure.jms.activemq;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ActiveMQProperties} and ActiveMQConnectionFactoryFactory.
 *
 * @author Stephane Nicoll
 */
public class ActiveMQPropertiesTests {

	private static final String DEFAULT_EMBEDDED_BROKER_URL = "vm://localhost?broker.persistent=false";

	private static final String DEFAULT_NETWORK_BROKER_URL = "tcp://localhost:61616";

	private final ActiveMQProperties properties = new ActiveMQProperties();

	@Test
	public void getBrokerUrlIsInMemoryByDefault() {
		assertEquals(DEFAULT_EMBEDDED_BROKER_URL,
				new ActiveMQConnectionFactoryFactory(this.properties)
						.determineBrokerUrl());
	}

	@Test
	public void getBrokerUrlUseExplicitBrokerUrl() {
		this.properties.setBrokerUrl("vm://foo-bar");
		assertEquals("vm://foo-bar", new ActiveMQConnectionFactoryFactory(this.properties)
				.determineBrokerUrl());
	}

	@Test
	public void getBrokerUrlWithInMemorySetToFalse() {
		this.properties.setInMemory(false);
		assertEquals(DEFAULT_NETWORK_BROKER_URL,
				new ActiveMQConnectionFactoryFactory(this.properties)
						.determineBrokerUrl());
	}

	@Test
	public void getExplicitBrokerUrlAlwaysWins() {
		this.properties.setBrokerUrl("vm://foo-bar");
		this.properties.setInMemory(false);
		assertEquals("vm://foo-bar", new ActiveMQConnectionFactoryFactory(this.properties)
				.determineBrokerUrl());
	}

}
