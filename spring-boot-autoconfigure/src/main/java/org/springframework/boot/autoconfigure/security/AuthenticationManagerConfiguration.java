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
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

/**
 * Configuration for a Spring Security in-memory {@link AuthenticationManager}. Can be
 * disabled by providing a bean of type AuthenticationManager. The value provided by this
 * configuration will become the "global" authentication manager (from Spring Security),
 * or the parent of the global instance. Thus it acts as a fallback when no others are
 * provided, is used by method security if enabled, and as a parent authentication manager
 * for "local" authentication managers in individual filter chains.
 *
 * @author Dave Syer
 * @author Rob Winch
 */
@Configuration
@ConditionalOnBean(ObjectPostProcessor.class)
@ConditionalOnMissingBean({ AuthenticationManager.class })
@Order(0)
public class AuthenticationManagerConfiguration extends
		GlobalAuthenticationConfigurerAdapter {

	/*
	 * Yes, this class is a GlobalAuthenticationConfigurerAdapter, even though none of
	 * those methods are overridden: we want Spring Security to instantiate us early, so
	 * we can in turn force the SecurityPrequisites to be instantiated. This will prevent
	 * ordering issues between Spring Boot modules when they need to influence the default
	 * security configuration.
	 */

	private static Log logger = LogFactory
			.getLog(AuthenticationManagerConfiguration.class);

	@Autowired
	private List<SecurityPrequisite> dependencies;

	@Autowired
	private SecurityProperties security;

	@Autowired
	private ObjectPostProcessor<Object> objectPostProcessor;

	@Bean
	@Primary
	public AuthenticationManager authenticationManager(AuthenticationManagerBuilder auth,
			ApplicationContext context) throws Exception {

		if (isAuthenticationManagerAlreadyConfigured(context)) {
			return new LazyAuthenticationManager(auth);
		}

		/*
		 * This AuthenticationManagerBuilder is for the global AuthenticationManager
		 */
		BootDefaultingAuthenticationConfigurerAdapter configurer = new BootDefaultingAuthenticationConfigurerAdapter();
		configurer.configure(auth);
		AuthenticationManager manager = configurer.getAuthenticationManagerBuilder()
				.getOrBuild();
		configurer.configureParent(auth);
		return manager;

	}

	private boolean isAuthenticationManagerAlreadyConfigured(ApplicationContext context) {
		return context.getBeanNamesForType(GlobalAuthenticationConfigurerAdapter.class).length > 2;
	}

	@Component
	protected static class AuthenticationManagerConfigurationListener implements
			SmartInitializingSingleton {

		@Autowired
		private AuthenticationEventPublisher authenticationEventPublisher;

		@Autowired
		private ApplicationContext context;

		@Override
		public void afterSingletonsInstantiated() {
			if (this.context.getBeanNamesForType(AuthenticationManager.class).length == 0) {
				return;
			}
			AuthenticationManager manager = this.context
					.getBean(AuthenticationManager.class);
			if (manager instanceof ProviderManager) {
				((ProviderManager) manager)
						.setAuthenticationEventPublisher(this.authenticationEventPublisher);
			}
		}

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
	 */
	private class BootDefaultingAuthenticationConfigurerAdapter {

		private AuthenticationManagerBuilder defaultAuth;

		private AuthenticationManager parent;

		public void configureParent(AuthenticationManagerBuilder auth) {
			if (!auth.isConfigured() && this.parent != null) {
				auth.parentAuthenticationManager(this.parent);
			}
		}

		public AuthenticationManagerBuilder getAuthenticationManagerBuilder() {
			return this.defaultAuth;
		}

		public void configure(AuthenticationManagerBuilder auth) throws Exception {
			if (auth.isConfigured()) {
				this.defaultAuth = auth;
				return;
			}

			User user = AuthenticationManagerConfiguration.this.security.getUser();
			if (user.isDefaultPassword()) {
				logger.info("\n\nUsing default security password: " + user.getPassword()
						+ "\n\n");
			}

			this.defaultAuth = new AuthenticationManagerBuilder(
					AuthenticationManagerConfiguration.this.objectPostProcessor);

			Set<String> roles = new LinkedHashSet<String>(user.getRole());

			this.parent = this.defaultAuth.inMemoryAuthentication()
					.withUser(user.getName()).password(user.getPassword())
					.roles(roles.toArray(new String[roles.size()])).and().and().build();

			// Defer actually setting the parent on the AuthenticationManagerBuilder
			// because it makes it "configured" and we are only in the init() phase
			// here.

		}
	}

	private static class LazyAuthenticationManager implements AuthenticationManager {

		private AuthenticationManagerBuilder builder;

		public LazyAuthenticationManager(AuthenticationManagerBuilder builder) {
			this.builder = builder;
		}

		@Override
		public Authentication authenticate(Authentication authentication)
				throws AuthenticationException {
			return this.builder.getOrBuild().authenticate(authentication);
		}

	}

}
