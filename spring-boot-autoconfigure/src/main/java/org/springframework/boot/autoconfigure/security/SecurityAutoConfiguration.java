/*
 * Copyright 2012-2014 the original author or authors.
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

package org.springframework.boot.autoconfigure.security;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfiguration;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Security. Provides an
 * {@link AuthenticationManager} based on configuration bound to a
 * {@link SecurityProperties} bean. There is one user (named "user") whose password is
 * random and printed on the console at INFO level during startup. In a webapp this
 * configuration also secures all web endpoints (except some well-known static resource)
 * locations with HTTP basic security. To replace all the default behaviour in a webapp
 * provide a <code>@Configuration</code> with <code>@EnableWebSecurity</code>. To just add
 * your own layer of application security in front of the defaults, add a
 * <code>@Configuration</code> of type {@link WebSecurityConfigurerAdapter}.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass(AuthenticationManager.class)
@EnableConfigurationProperties
@Import({ SpringBootWebSecurityConfiguration.class,
		AuthenticationManagerConfiguration.class })
public class SecurityAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SecurityProperties securityProperties() {
		return new SecurityProperties();
	}

	/**
	 * If the user explicitly disables the basic security features and forgets to
	 * <code>@EnableWebSecurity</code>, and yet still wants a bean of type
	 * WebSecurityConfigurerAdapter, he is trying to use a custom security setup. The app
	 * would fail in a confusing way without this shim configuration, which just helpfully
	 * defines an empty <code>@EnableWebSecurity</code>.
	 * 
	 * @author Dave Syer
	 */
	@ConditionalOnExpression("!${security.basic.enabled:true}")
	@ConditionalOnBean(WebSecurityConfigurerAdapter.class)
	@ConditionalOnClass(EnableWebSecurity.class)
	@ConditionalOnMissingBean(WebSecurityConfiguration.class)
	@ConditionalOnWebApplication
	@EnableWebSecurity
	protected static class EmptyWebSecurityConfiguration {

	}

}
