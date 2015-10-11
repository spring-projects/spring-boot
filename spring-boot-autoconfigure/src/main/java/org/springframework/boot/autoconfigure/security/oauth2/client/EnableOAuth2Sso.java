/*
 * Copyright 2012-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.client;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerTokenServicesConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;

/**
 * Enable OAuth2 Single Sign On (SSO). If there is an existing
 * {@link WebSecurityConfigurerAdapter} provided by the user and annotated with
 * {@code @EnableOAuth2Sso}, it is enhanced by adding an authentication filter and an
 * authentication entry point. If the user only has {@code @EnableOAuth2Sso} but not on a
 * WebSecurityConfigurerAdapter then one is added with all paths secured and with an order
 * that puts it ahead of the default HTTP Basic security chain in Spring Boot.
 *
 * @author Dave Syer
 * @since 1.3.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@EnableOAuth2Client
@EnableConfigurationProperties(OAuth2SsoProperties.class)
@Import({ OAuth2SsoDefaultConfiguration.class, OAuth2SsoCustomConfiguration.class,
		ResourceServerTokenServicesConfiguration.class })
public @interface EnableOAuth2Sso {

}
