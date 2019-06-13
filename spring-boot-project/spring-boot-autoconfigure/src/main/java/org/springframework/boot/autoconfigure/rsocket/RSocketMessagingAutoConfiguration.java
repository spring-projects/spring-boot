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

package org.springframework.boot.autoconfigure.rsocket;

import io.rsocket.RSocketFactory;
import io.rsocket.transport.netty.server.TcpServerTransport;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.rsocket.MessageHandlerAcceptor;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.ClassUtils;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring RSocket support in Spring
 * Messaging.
 *
 * @author Brian Clozel
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({ RSocketRequester.class, RSocketFactory.class, TcpServerTransport.class })
@AutoConfigureAfter(RSocketStrategiesAutoConfiguration.class)
public class RSocketMessagingAutoConfiguration {

	private static final String PATHPATTERN_ROUTEMATCHER_CLASS = "org.springframework.web.util.pattern.PathPatternRouteMatcher";

	@Bean
	@ConditionalOnMissingBean
	public MessageHandlerAcceptor messageHandlerAcceptor(RSocketStrategies rSocketStrategies) {
		MessageHandlerAcceptor acceptor = new MessageHandlerAcceptor();
		acceptor.setRSocketStrategies(rSocketStrategies);
		if (ClassUtils.isPresent(PATHPATTERN_ROUTEMATCHER_CLASS, null)) {
			PathPatternParser parser = new PathPatternParser();
			parser.setSeparator('.');
			acceptor.setRouteMatcher(new PathPatternRouteMatcher(parser));
		}
		return acceptor;
	}

}
