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

package org.springframework.boot.actuate.autoconfigure.r2dbc;

import io.micrometer.observation.ObservationRegistry;
import io.r2dbc.proxy.ProxyConnectionFactory;
import io.r2dbc.proxy.observation.ObservationProxyExecutionListener;
import io.r2dbc.proxy.observation.QueryObservationConvention;
import io.r2dbc.proxy.observation.QueryParametersTagProvider;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.autoconfigure.observation.ObservationAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.r2dbc.ConnectionFactoryDecorator;
import org.springframework.boot.r2dbc.OptionsCapableConnectionFactory;
import org.springframework.context.annotation.Bean;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for R2DBC observability support.
 *
 * @author Moritz Halbritter
 * @since 3.2.0
 */
@AutoConfiguration(after = ObservationAutoConfiguration.class)
@ConditionalOnClass({ ConnectionFactory.class, ProxyConnectionFactory.class })
@EnableConfigurationProperties(R2dbcObservationProperties.class)
public class R2dbcObservationAutoConfiguration {

	@Bean
	@ConditionalOnBean(ObservationRegistry.class)
	ConnectionFactoryDecorator connectionFactoryDecorator(R2dbcObservationProperties properties,
			ObservationRegistry observationRegistry,
			ObjectProvider<QueryObservationConvention> queryObservationConvention,
			ObjectProvider<QueryParametersTagProvider> queryParametersTagProvider) {
		return (connectionFactory) -> {
			HostAndPort hostAndPort = extractHostAndPort(connectionFactory);
			ObservationProxyExecutionListener listener = new ObservationProxyExecutionListener(observationRegistry,
					connectionFactory, hostAndPort.host(), hostAndPort.port());
			listener.setIncludeParameterValues(properties.isIncludeParameterValues());
			queryObservationConvention.ifAvailable(listener::setQueryObservationConvention);
			queryParametersTagProvider.ifAvailable(listener::setQueryParametersTagProvider);
			return ProxyConnectionFactory.builder(connectionFactory).listener(listener).build();
		};
	}

	private HostAndPort extractHostAndPort(ConnectionFactory connectionFactory) {
		OptionsCapableConnectionFactory optionsCapableConnectionFactory = OptionsCapableConnectionFactory
			.unwrapFrom(connectionFactory);
		if (optionsCapableConnectionFactory == null) {
			return HostAndPort.empty();
		}
		ConnectionFactoryOptions options = optionsCapableConnectionFactory.getOptions();
		Object host = options.getValue(ConnectionFactoryOptions.HOST);
		Object port = options.getValue(ConnectionFactoryOptions.PORT);
		if (!(host instanceof String hostAsString) || !(port instanceof Integer portAsInt)) {
			return HostAndPort.empty();
		}
		return new HostAndPort(hostAsString, portAsInt);
	}

	private record HostAndPort(String host, Integer port) {
		static HostAndPort empty() {
			return new HostAndPort(null, null);
		}
	}

}
