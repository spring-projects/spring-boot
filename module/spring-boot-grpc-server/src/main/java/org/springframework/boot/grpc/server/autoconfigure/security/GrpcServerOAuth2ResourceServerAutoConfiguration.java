/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.grpc.server.autoconfigure.security;

import io.grpc.BindableService;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.grpc.server.autoconfigure.GrpcServerAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.grpc.server.GlobalServerInterceptor;
import org.springframework.grpc.server.GrpcServerFactory;
import org.springframework.grpc.server.security.AuthenticationProcessInterceptor;
import org.springframework.grpc.server.security.GrpcSecurity;
import org.springframework.security.config.ObjectPostProcessor;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC OAuth2 resource server.
 *
 * @author Dave Syer
 * @author Andrey Litvitski
 * @author Phillip Webb
 * @since 4.1.0
 */
@AutoConfiguration(beforeName = "org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration",
		afterName = { "org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration",
				"org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerAutoConfiguration" },
		after = { GrpcServerSecurityAutoConfiguration.class, GrpcServerAutoConfiguration.class })
@ConditionalOnBooleanProperty(name = "spring.grpc.server.enabled", matchIfMissing = true)
@ConditionalOnClass({ BindableService.class, GrpcServerFactory.class, ObjectPostProcessor.class })
@ConditionalOnMissingBean(AuthenticationProcessInterceptor.class)
@ConditionalOnBean(GrpcSecurity.class)
public final class GrpcServerOAuth2ResourceServerAutoConfiguration {

	@Bean
	@ConditionalOnBean(OpaqueTokenIntrospector.class)
	@GlobalServerInterceptor
	AuthenticationProcessInterceptor opaqueTokenAuthenticationProcessInterceptor(GrpcSecurity grpcSecurity)
			throws Exception {
		grpcSecurity.authorizeRequests((requests) -> requests.allRequests().authenticated());
		grpcSecurity.oauth2ResourceServer((resourceServer) -> resourceServer.opaqueToken(withDefaults()));
		return grpcSecurity.build();
	}

	@Bean
	@ConditionalOnBean(JwtDecoder.class)
	@GlobalServerInterceptor
	AuthenticationProcessInterceptor jwtAuthenticationProcessInterceptor(GrpcSecurity grpcSecurity) throws Exception {
		grpcSecurity.authorizeRequests((requests) -> requests.allRequests().authenticated());
		grpcSecurity.oauth2ResourceServer((resourceServer) -> resourceServer.jwt(withDefaults()));
		return grpcSecurity.build();
	}

}
