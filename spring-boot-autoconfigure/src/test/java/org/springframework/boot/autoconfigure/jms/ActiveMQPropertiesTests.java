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

package org.springframework.boot.autoconfigure.jms;

import org.junit.Test;
import org.springframework.boot.test.EnvironmentTestUtils;
import org.springframework.core.env.StandardEnvironment;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link ActiveMQProperties}.
 * 
 * @author Stephane Nicoll
 */
public class ActiveMQPropertiesTests {

	private final ActiveMQProperties properties = new ActiveMQProperties();

	private final StandardEnvironment environment = new StandardEnvironment();

	@Test
	public void determineBrokerUrlDefault() {
		assertEquals(ActiveMQProperties.DEFAULT_EMBEDDED_BROKER_URL,
				ActiveMQProperties.determineBrokerUrl(this.environment));
	}

	@Test
	public void determineBrokerUrlVmBrokerUrl() {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.activemq.brokerUrl:vm://localhost?persistent=true");
		assertEquals("vm://localhost?persistent=true",
				ActiveMQProperties.determineBrokerUrl(this.environment));
	}

	@Test
	public void determineBrokerUrlInMemoryFlag() {
		EnvironmentTestUtils.addEnvironment(this.environment,
				"spring.activemq.inMemory:false");
		assertEquals(ActiveMQProperties.DEFAULT_NETWORK_BROKER_URL,
				ActiveMQProperties.determineBrokerUrl(this.environment));
	}

	@Test
	public void getBrokerUrlIsInMemoryByDefault() {
		assertEquals(ActiveMQProperties.DEFAULT_EMBEDDED_BROKER_URL,
				this.properties.determineBrokerUrl());
	}

	@Test
	public void getBrokerUrlUseExplicitBrokerUrl() {
		this.properties.setBrokerUrl("vm://foo-bar");
		assertEquals("vm://foo-bar", this.properties.determineBrokerUrl());
	}

	@Test
	public void getBrokerUrlWithInMemorySetToFalse() {
		this.properties.setInMemory(false);
		assertEquals(ActiveMQProperties.DEFAULT_NETWORK_BROKER_URL,
				this.properties.determineBrokerUrl());
	}

	@Test
	public void getExplicitBrokerUrlAlwaysWins() {
		this.properties.setBrokerUrl("vm://foo-bar");
		this.properties.setInMemory(false);
		assertEquals("vm://foo-bar", this.properties.determineBrokerUrl());
	}
}
