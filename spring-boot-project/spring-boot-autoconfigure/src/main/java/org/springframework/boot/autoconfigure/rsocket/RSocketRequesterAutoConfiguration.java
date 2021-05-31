/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.autoconfigure.rsocket;

import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.netty.http.server.HttpServer;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.messaging.rsocket.RSocketConnectorConfigurer;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequester.Builder;
import org.springframework.messaging.rsocket.RSocketStrategies;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link org.springframework.messaging.rsocket.RSocketRequester}. This auto-configuration
 * creates {@link org.springframework.messaging.rsocket.RSocketRequester.Builder}
 * prototype beans, as the builders are stateful and should not be reused to build
 * requester instances with different configurations.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RSocketRequester.class, io.rsocket.RSocket.class, HttpServer.class, TcpServerTransport.class })
@AutoConfigureAfter(RSocketStrategiesAutoConfiguration.class)
public class RSocketRequesterAutoConfiguration {

	@Bean
	@Scope("prototype")
	@ConditionalOnMissingBean
	public RSocketRequester.Builder rSocketRequesterBuilder(RSocketStrategies strategies,
			ObjectProvider<RSocketConnectorConfigurer> connectorConfigurers) {
		Builder builder = RSocketRequester.builder().rsocketStrategies(strategies);
		connectorConfigurers.orderedStream().forEach(builder::rsocketConnector);
		return builder;
	}

}
