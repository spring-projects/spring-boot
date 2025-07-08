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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.reactive;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.actuate.reactive.ReactiveManagementWebSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.reactive.ReactiveUserDetailsServiceAutoConfiguration;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.OAuth2ResourceServerProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Reactive OAuth2 resource server
 * support.
 *
 * @author Madhura Bhave
 * @since 4.0.0
 */
@AutoConfiguration(before = { ReactiveManagementWebSecurityAutoConfiguration.class,
		ReactiveSecurityAutoConfiguration.class, ReactiveUserDetailsServiceAutoConfiguration.class })
@EnableConfigurationProperties(OAuth2ResourceServerProperties.class)
@ConditionalOnClass({ EnableWebFluxSecurity.class })
@ConditionalOnWebApplication(type = Type.REACTIVE)
@Import({ ReactiveOAuth2ResourceServerConfiguration.JwtConfiguration.class,
		ReactiveOAuth2ResourceServerConfiguration.OpaqueTokenConfiguration.class })
public class ReactiveOAuth2ResourceServerAutoConfiguration {

}
