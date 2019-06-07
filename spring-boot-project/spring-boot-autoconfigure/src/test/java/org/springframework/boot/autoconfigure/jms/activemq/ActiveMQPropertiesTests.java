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

package org.springframework.boot.autoconfigure.jms.activemq;

import java.util.Collections;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ActiveMQProperties} and {@link ActiveMQConnectionFactoryFactory}.
 *
 * @author Stephane Nicoll
 * @author Aurélien Leboulanger
 * @author Venil Noronha
 */
public class ActiveMQPropertiesTests {

	private static final String DEFAULT_EMBEDDED_BROKER_URL = "vm://localhost?broker.persistent=false";

	private static final String DEFAULT_NETWORK_BROKER_URL = "tcp://localhost:61616";

	private final ActiveMQProperties properties = new ActiveMQProperties();

	@Test
	public void getBrokerUrlIsInMemoryByDefault() {
		assertThat(createFactory(this.properties).determineBrokerUrl()).isEqualTo(DEFAULT_EMBEDDED_BROKER_URL);
	}

	@Test
	public void getBrokerUrlUseExplicitBrokerUrl() {
		this.properties.setBrokerUrl("vm://foo-bar");
		assertThat(createFactory(this.properties).determineBrokerUrl()).isEqualTo("vm://foo-bar");
	}

	@Test
	public void getBrokerUrlWithInMemorySetToFalse() {
		this.properties.setInMemory(false);
		assertThat(createFactory(this.properties).determineBrokerUrl()).isEqualTo(DEFAULT_NETWORK_BROKER_URL);
	}

	@Test
	public void getExplicitBrokerUrlAlwaysWins() {
		this.properties.setBrokerUrl("vm://foo-bar");
		this.properties.setInMemory(false);
		assertThat(createFactory(this.properties).determineBrokerUrl()).isEqualTo("vm://foo-bar");
	}

	@Test
	public void setTrustAllPackages() {
		this.properties.getPackages().setTrustAll(true);
		assertThat(createFactory(this.properties).createConnectionFactory(ActiveMQConnectionFactory.class)
				.isTrustAllPackages()).isTrue();
	}

	@Test
	public void setTrustedPackages() {
		this.properties.getPackages().setTrustAll(false);
		this.properties.getPackages().getTrusted().add("trusted.package");
		ActiveMQConnectionFactory factory = createFactory(this.properties)
				.createConnectionFactory(ActiveMQConnectionFactory.class);
		assertThat(factory.isTrustAllPackages()).isFalse();
		assertThat(factory.getTrustedPackages().size()).isEqualTo(1);
		assertThat(factory.getTrustedPackages().get(0)).isEqualTo("trusted.package");
	}

	private ActiveMQConnectionFactoryFactory createFactory(ActiveMQProperties properties) {
		return new ActiveMQConnectionFactoryFactory(properties, Collections.emptyList());
	}

}
