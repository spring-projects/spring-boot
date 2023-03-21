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

package org.springframework.boot.autoconfigure.security.oauth2.server.servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OAuth2 authorization server
 * support.
 *
 * <p>
 * <strong>Note:</strong> This configuration and
 * {@link OAuth2AuthorizationServerJwtAutoConfiguration} work together to ensure that the
 * {@link org.springframework.security.config.annotation.ObjectPostProcessor} is defined
 * <strong>BEFORE</strong> {@link UserDetailsServiceAutoConfiguration} so that a
 * {@link org.springframework.security.core.userdetails.UserDetailsService} can be created
 * if necessary.
 *
 * @author Steve Riesenberg
 * @since 3.1.0
 * @see OAuth2AuthorizationServerJwtAutoConfiguration
 */
@AutoConfiguration(before = { OAuth2ResourceServerAutoConfiguration.class, SecurityAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class })
@ConditionalOnClass(OAuth2Authorization.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({ OAuth2AuthorizationServerConfiguration.class, OAuth2AuthorizationServerWebSecurityConfiguration.class })
public class OAuth2AuthorizationServerAutoConfiguration {

}
