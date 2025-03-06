/*
 * Copyright 2012-2025 the original author or authors.
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
import java.util.List;

import org.apache.activemq.ActiveMQConnectionFactory;

import org.springframework.boot.autoconfigure.jms.activemq.ActiveMQProperties.Packages;
import org.springframework.util.Assert;

/**
 * Class to configure an {@link ActiveMQConnectionFactory} instance from properties
 * defined in {@link ActiveMQProperties} and any
 * {@link ActiveMQConnectionFactoryCustomizer customizers}.
 *
 * @author Phillip Webb
 * @author Venil Noronha
 * @author Eddú Meléndez
 */
class ActiveMQConnectionFactoryConfigurer {

	private final ActiveMQProperties properties;

	private final List<ActiveMQConnectionFactoryCustomizer> factoryCustomizers;

	ActiveMQConnectionFactoryConfigurer(ActiveMQProperties properties,
			List<ActiveMQConnectionFactoryCustomizer> factoryCustomizers) {
		Assert.notNull(properties, "'properties' must not be null");
		this.properties = properties;
		this.factoryCustomizers = (factoryCustomizers != null) ? factoryCustomizers : Collections.emptyList();
	}

	void configure(ActiveMQConnectionFactory factory) {
		if (this.properties.getCloseTimeout() != null) {
			factory.setCloseTimeout((int) this.properties.getCloseTimeout().toMillis());
		}
		factory.setNonBlockingRedelivery(this.properties.isNonBlockingRedelivery());
		if (this.properties.getSendTimeout() != null) {
			factory.setSendTimeout((int) this.properties.getSendTimeout().toMillis());
		}
		Packages packages = this.properties.getPackages();
		if (packages.getTrustAll() != null) {
			factory.setTrustAllPackages(packages.getTrustAll());
		}
		if (!packages.getTrusted().isEmpty()) {
			factory.setTrustedPackages(packages.getTrusted());
		}
		customize(factory);
	}

	private void customize(ActiveMQConnectionFactory connectionFactory) {
		for (ActiveMQConnectionFactoryCustomizer factoryCustomizer : this.factoryCustomizers) {
			factoryCustomizer.customize(connectionFactory);
		}
	}

}
