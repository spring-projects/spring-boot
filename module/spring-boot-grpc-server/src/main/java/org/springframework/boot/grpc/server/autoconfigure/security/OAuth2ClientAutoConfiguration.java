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

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.grpc.server.autoconfigure.ConditionalOnSpringGrpc;
import org.springframework.boot.security.oauth2.client.autoconfigure.ConditionalOnOAuth2ClientRegistrationProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for gRPC OAuth2 security.
 *
 * @author Dave Syer
 * @since 4.0.0
 */
// Copied from Spring Boot (https://github.com/spring-projects/spring-boot/issues/40997, ]
// https://github.com/spring-projects/spring-boot/issues/15877)
@AutoConfiguration(
		afterName = "org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientAutoConfiguration")
@ConditionalOnSpringGrpc
@ConditionalOnClass(InMemoryClientRegistrationRepository.class)
@ConditionalOnOAuth2ClientRegistrationProperties
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public final class OAuth2ClientAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(ClientRegistrationRepository.class)
	InMemoryClientRegistrationRepository clientRegistrationRepository(OAuth2ClientProperties properties) {
		List<ClientRegistration> registrations = new ArrayList<>(
				new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().values());
		return new InMemoryClientRegistrationRepository(registrations);
	}

}
