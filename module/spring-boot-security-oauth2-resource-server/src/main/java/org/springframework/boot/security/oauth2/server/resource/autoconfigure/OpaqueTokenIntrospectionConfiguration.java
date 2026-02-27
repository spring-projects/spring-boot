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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;
import org.springframework.util.Assert;

/**
 * Configures an {@link OpaqueTokenIntrospector} when a token introspection endpoint is
 * available.
 *
 * @author Madhura Bhave
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnMissingBean(OpaqueTokenIntrospector.class)
class OpaqueTokenIntrospectionConfiguration {

	@Bean
	@ConditionalOnProperty(name = "spring.security.oauth2.resourceserver.opaquetoken.introspection-uri")
	SpringOpaqueTokenIntrospector opaqueTokenIntrospector(OAuth2ResourceServerProperties properties) {
		OAuth2ResourceServerProperties.Opaquetoken token = properties.getOpaquetoken();
		Assert.state(token.getClientId() != null,
				"No 'spring.security.oauth2.resourceserver.opaquetoken.client-id' property specified");
		Assert.state(token.getClientSecret() != null,
				"No 'spring.security.oauth2.resourceserver.opaquetoken.client-secret' property specified");
		return SpringOpaqueTokenIntrospector.withIntrospectionUri(token.getIntrospectionUri())
			.clientId(token.getClientId())
			.clientSecret(token.getClientSecret())
			.build();
	}

}
