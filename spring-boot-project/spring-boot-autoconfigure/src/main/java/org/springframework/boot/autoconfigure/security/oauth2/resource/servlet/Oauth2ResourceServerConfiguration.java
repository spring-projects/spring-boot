/*
 * Copyright 2012-2020 the original author or authors.
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

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Configuration classes for OAuth2 Resource Server These should be {@code @Import} in a
 * regular auto-configuration class to guarantee their order of execution.
 *
 * @author Madhura Bhave
 */
class Oauth2ResourceServerConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(JwtDecoder.class)
	@Import({ OAuth2ResourceServerJwtConfiguration.JwtDecoderConfiguration.class,
			OAuth2ResourceServerJwtConfiguration.OAuth2WebSecurityConfigurerAdapter.class })
	static class JwtConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	@Import({ OAuth2ResourceServerOpaqueTokenConfiguration.OpaqueTokenIntrospectionClientConfiguration.class,
			OAuth2ResourceServerOpaqueTokenConfiguration.OAuth2WebSecurityConfigurerAdapter.class })
	static class OpaqueTokenConfiguration {

	}

}
