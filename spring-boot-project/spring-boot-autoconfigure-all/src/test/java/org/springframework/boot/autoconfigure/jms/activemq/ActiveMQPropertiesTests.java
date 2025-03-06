/*
 * Copyright 2012-2024 the original author or authors.
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

import org.apache.activemq.ActiveMQConnectionFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ActiveMQProperties} and {@link ActiveMQConnectionFactoryConfigurer}.
 *
 * @author Stephane Nicoll
 * @author Aurélien Leboulanger
 * @author Venil Noronha
 * @author Eddú Meléndez
 */
class ActiveMQPropertiesTests {

	private static final String DEFAULT_EMBEDDED_BROKER_URL = "vm://localhost?broker.persistent=false";

	private static final String DEFAULT_NETWORK_BROKER_URL = "tcp://localhost:61616";

	private final ActiveMQProperties properties = new ActiveMQProperties();

	@Test
	void getBrokerUrlIsEmbeddedByDefault() {
		assertThat(this.properties.determineBrokerUrl()).isEqualTo(DEFAULT_EMBEDDED_BROKER_URL);
	}

	@Test
	void getBrokerUrlUseExplicitBrokerUrl() {
		this.properties.setBrokerUrl("tcp://activemq.example.com:71717");
		assertThat(this.properties.determineBrokerUrl()).isEqualTo("tcp://activemq.example.com:71717");
	}

	@Test
	void getBrokerUrlWithEmbeddedSetToFalse() {
		this.properties.getEmbedded().setEnabled(false);
		assertThat(this.properties.determineBrokerUrl()).isEqualTo(DEFAULT_NETWORK_BROKER_URL);
	}

	@Test
	void getExplicitBrokerUrlAlwaysWins() {
		this.properties.setBrokerUrl("tcp://activemq.example.com:71717");
		this.properties.getEmbedded().setEnabled(false);
		assertThat(this.properties.determineBrokerUrl()).isEqualTo("tcp://activemq.example.com:71717");
	}

	@Test
	void setTrustAllPackages() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
		this.properties.getPackages().setTrustAll(true);
		new ActiveMQConnectionFactoryConfigurer(this.properties, null).configure(factory);
		assertThat(factory.isTrustAllPackages()).isTrue();
	}

	@Test
	void setTrustedPackages() {
		ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory();
		this.properties.getPackages().setTrustAll(false);
		this.properties.getPackages().getTrusted().add("trusted.package");
		new ActiveMQConnectionFactoryConfigurer(this.properties, null).configure(factory);
		assertThat(factory.isTrustAllPackages()).isFalse();
		assertThat(factory.getTrustedPackages()).hasSize(1);
		assertThat(factory.getTrustedPackages().get(0)).isEqualTo("trusted.package");
	}

}
