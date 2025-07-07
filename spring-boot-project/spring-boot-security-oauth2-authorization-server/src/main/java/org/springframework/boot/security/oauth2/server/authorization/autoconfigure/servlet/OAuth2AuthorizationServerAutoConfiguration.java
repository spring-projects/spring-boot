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

package org.springframework.boot.security.oauth2.server.authorization.autoconfigure.servlet;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.security.autoconfigure.servlet.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for OAuth2 authorization server
 * support.
 *
 * <p>
 * <strong>Note:</strong> This configuration and
 * {@link OAuth2AuthorizationServerJwtAutoConfiguration} work together to ensure that the
 * {@link org.springframework.security.config.ObjectPostProcessor} is defined
 * <strong>BEFORE</strong> {@link UserDetailsServiceAutoConfiguration} so that a
 * {@link org.springframework.security.core.userdetails.UserDetailsService} can be created
 * if necessary.
 *
 * @author Steve Riesenberg
 * @since 4.0.0
 * @see OAuth2AuthorizationServerJwtAutoConfiguration
 */
@AutoConfiguration(before = SecurityAutoConfiguration.class,
		beforeName = "org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet.OAuth2ResourceServerAutoConfiguration")
@ConditionalOnClass(OAuth2Authorization.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
@Import({ OAuth2AuthorizationServerConfiguration.class, OAuth2AuthorizationServerWebSecurityConfiguration.class })
public class OAuth2AuthorizationServerAutoConfiguration {

}
