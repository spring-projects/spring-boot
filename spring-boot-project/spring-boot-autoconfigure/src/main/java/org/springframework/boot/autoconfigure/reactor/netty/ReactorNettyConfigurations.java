/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.reactor.netty;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorResourceFactory;

/**
 * Configurations for Reactor Netty. Those should be {@code @Import} in a regular
 * auto-configuration class.
 *
 * @author Moritz Halbritter
 * @since 2.7.9
 */
public final class ReactorNettyConfigurations {

	private ReactorNettyConfigurations() {
	}

	@Configuration(proxyBeanMethods = false)
	@EnableConfigurationProperties(ReactorNettyProperties.class)
	public static class ReactorResourceFactoryConfiguration {

		@Bean
		@ConditionalOnMissingBean
		ReactorResourceFactory reactorResourceFactory(ReactorNettyProperties configurationProperties) {
			ReactorResourceFactory reactorResourceFactory = new ReactorResourceFactory();
			if (configurationProperties.getShutdownQuietPeriod() != null) {
				reactorResourceFactory.setShutdownQuietPeriod(configurationProperties.getShutdownQuietPeriod());
			}
			return reactorResourceFactory;
		}

	}

}
