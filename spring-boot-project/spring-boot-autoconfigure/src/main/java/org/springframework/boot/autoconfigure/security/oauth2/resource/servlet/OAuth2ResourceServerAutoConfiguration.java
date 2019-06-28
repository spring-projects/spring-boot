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
package org.springframework.boot.autoconfigure.security.oauth2.resource.servlet;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.OAuth2IntrospectionAuthenticationToken;
import org.springframework.security.oauth2.server.resource.introspection.OAuth2TokenIntrospectionClient;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OAuth2 resource server support.
 *
 * @author Madhura Bhave
 * @since 2.1.0
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore(SecurityAutoConfiguration.class)
@EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)

public class OAuth2ResourceServerAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ JwtAuthenticationToken.class, JwtDecoder.class })
	@Import({ OAuth2ResourceServerJwtConfiguration.JwtDecoderConfiguration.class,
			OAuth2ResourceServerJwtConfiguration.OAuth2WebSecurityConfigurerAdapter.class })
	static class JwtConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ OAuth2IntrospectionAuthenticationToken.class, OAuth2TokenIntrospectionClient.class })
	@Import({ OAuth2ResourceServerOpaqueTokenConfiguration.OpaqueTokenIntrospectionClientConfiguration.class,
			OAuth2ResourceServerOpaqueTokenConfiguration.OAuth2WebSecurityConfigurerAdapter.class })
	static class OpaqueTokenConfiguration {

	}

}
