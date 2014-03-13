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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;

/**
 * Configuration for a Spring Security in-memory {@link AuthenticationManager}.
 * 
 * @author Dave Syer
 */
@Configuration
@ConditionalOnBean(ObjectPostProcessor.class)
@ConditionalOnMissingBean(AuthenticationManager.class)
@Order(Ordered.LOWEST_PRECEDENCE - 3)
public class AuthenticationManagerConfiguration extends
		GlobalAuthenticationConfigurerAdapter {

	private static Log logger = LogFactory
			.getLog(AuthenticationManagerConfiguration.class);

	@Autowired
	private List<SecurityPrequisite> dependencies;

	@Autowired
	private ObjectPostProcessor<Object> objectPostProcessor;

	@Autowired
	private SecurityProperties security;

	private BootDefaultingAuthenticationConfigurerAdapter configurer = new BootDefaultingAuthenticationConfigurerAdapter();

	@Override
	public void init(AuthenticationManagerBuilder auth) throws Exception {
		auth.apply(this.configurer);
	}

	@Bean
	// avoid issues with scopedTarget (SPR-11548)
	@Primary
	public AuthenticationManager authenticationManager() {
		return lazyAuthenticationManager();
	}

	@Bean
	@Lazy
	@Scope(proxyMode = ScopedProxyMode.INTERFACES)
	protected AuthenticationManager lazyAuthenticationManager() {
		return this.configurer.getAuthenticationManagerBuilder().getOrBuild();
	}

	/**
	 * We must add {@link BootDefaultingAuthenticationConfigurerAdapter} in the init phase
	 * of the last {@link GlobalAuthenticationConfigurerAdapter}. The reason is that the
	 * typical flow is something like:
	 * 
	 * <ul>
	 * <li>A
	 * {@link GlobalAuthenticationConfigurerAdapter#init(AuthenticationManagerBuilder)}
	 * exists that adds a {@link SecurityConfigurer} to the
	 * {@link AuthenticationManagerBuilder}</li>
	 * <li>
	 * {@link AuthenticationManagerConfiguration#init(AuthenticationManagerBuilder)} adds
	 * BootDefaultingAuthenticationConfigurerAdapter so it is after the
	 * {@link SecurityConfigurer} in the first step</li>
	 * <li>We then can default an {@link AuthenticationProvider} if necessary. Note we can
	 * only invoke the
	 * {@link AuthenticationManagerBuilder#authenticationProvider(AuthenticationProvider)}
	 * method since all other methods add a {@link SecurityConfigurer} which is not
	 * allowed in the configure stage. It is not allowed because we guarantee all init
	 * methods are invoked before configure, which cannot be guaranteed at this point.</li>
	 * </ul>
	 * 
	 * @author Rob Winch
	 */
	private class BootDefaultingAuthenticationConfigurerAdapter extends
			GlobalAuthenticationConfigurerAdapter {

		private AuthenticationManagerBuilder defaultAuth;

		public AuthenticationManagerBuilder getAuthenticationManagerBuilder() {
			return this.defaultAuth;
		}

		@Override
		public void configure(AuthenticationManagerBuilder auth) throws Exception {
			if (auth.isConfigured()) {
				this.defaultAuth = auth;
				return;
			}

			User user = AuthenticationManagerConfiguration.this.security.getUser();
			if (user.isDefaultPassword()) {
				logger.info("\n\nUsing default password for application endpoints: "
						+ user.getPassword() + "\n\n");
			}

			this.defaultAuth = new AuthenticationManagerBuilder(
					AuthenticationManagerConfiguration.this.objectPostProcessor);

			Set<String> roles = new LinkedHashSet<String>(user.getRole());

			AuthenticationManager parent = this.defaultAuth.inMemoryAuthentication()
					.withUser(user.getName()).password(user.getPassword())
					.roles(roles.toArray(new String[roles.size()])).and().and().build();

			auth.parentAuthenticationManager(parent);
		}
	}
}
