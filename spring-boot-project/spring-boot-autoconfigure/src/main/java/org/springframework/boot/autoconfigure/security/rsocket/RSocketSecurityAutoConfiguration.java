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

package org.springframework.boot.autoconfigure.security.rsocket;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.rsocket.server.ServerRSocketFactoryProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.rsocket.EnableRSocketSecurity;
import org.springframework.security.rsocket.core.SecuritySocketAcceptorInterceptor;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security for an RSocket
 * server.
 *
 * @author Madhura Bhave
 * @since 2.2.0
 */
@Configuration(proxyBeanMethods = false)
@EnableRSocketSecurity
@ConditionalOnClass(SecuritySocketAcceptorInterceptor.class)
public class RSocketSecurityAutoConfiguration {

	@Bean
	ServerRSocketFactoryProcessor springSecurityRSocketSecurity(SecuritySocketAcceptorInterceptor interceptor) {
		return (factory) -> factory.addSocketAcceptorPlugin(interceptor);
	}

}
