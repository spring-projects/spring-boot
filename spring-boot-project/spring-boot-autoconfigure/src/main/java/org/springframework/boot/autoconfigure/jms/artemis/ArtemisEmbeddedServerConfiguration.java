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

package org.springframework.boot.autoconfigure.jms.artemis;

import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration used to create the embedded Artemis server.
 *
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Issam El-atif
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(EmbeddedActiveMQ.class)
@ConditionalOnProperty(prefix = "spring.artemis.embedded", name = "enabled",
		havingValue = "true", matchIfMissing = true)
class ArtemisEmbeddedServerConfiguration {

	private final ArtemisProperties properties;

	ArtemisEmbeddedServerConfiguration(ArtemisProperties properties) {
		this.properties = properties;
	}

	@Bean
	@ConditionalOnMissingBean
	public org.apache.activemq.artemis.core.config.Configuration artemisConfiguration() {
		return new ArtemisEmbeddedConfigurationFactory(this.properties)
				.createConfiguration();
	}

	@Bean(initMethod = "start", destroyMethod = "stop")
	@ConditionalOnMissingBean
	public EmbeddedActiveMQ artemisServer(
			org.apache.activemq.artemis.core.config.Configuration configuration,
			ObjectProvider<ArtemisConfigurationCustomizer> configurationCustomizers) {
		EmbeddedActiveMQ server = new EmbeddedActiveMQ();
		configurationCustomizers.orderedStream()
				.forEach((customizer) -> customizer.customize(configuration));
		server.setConfiguration(configuration);
		return server;
	}

}
