/*
 * Copyright 2012-2017 the original author or authors.
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

import java.lang.reflect.Field;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationEventPublisher;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.ObjectPostProcessor;
import org.springframework.security.config.annotation.SecurityConfigurer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.authentication.configurers.GlobalAuthenticationConfigurerAdapter;
import org.springframework.security.config.annotation.authentication.configurers.provisioning.InMemoryUserDetailsManagerConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.util.ReflectionUtils;

/**
 * Configuration for a Spring Security in-memory {@link AuthenticationManager}. Can be
 * disabled by providing a bean of type {@link AuthenticationManager},
 * {@link AuthenticationProvider} or {@link UserDetailsService}. The value provided by
 * this configuration will become the "global" authentication manager (from Spring
 * Security), or the parent of the global instance. Thus it acts as a fallback when no
 * others are provided, is used by method security if enabled, and as a parent
 * authentication manager for "local" authentication managers in individual filter chains.
 *
 * @author Dave Syer
 * @author Rob Winch
 * @author Madhura Bhave
 */
@Configuration
@ConditionalOnBean(ObjectPostProcessor.class)
@ConditionalOnMissingBean({ AuthenticationManager.class, AuthenticationProvider.class,
		UserDetailsService.class })
@Order(0)
public class AuthenticationManagerConfiguration {

	private static final Log logger = LogFactory
			.getLog(AuthenticationManagerConfiguration.class);

	@Bean
	@Primary
	public AuthenticationManager authenticationManager(
			AuthenticationConfiguration configuration) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public static SpringBootAuthenticationConfigurerAdapter springBootAuthenticationConfigurerAdapter() {
		return new SpringBootAuthenticationConfigurerAdapter();
	}

	@Bean
	public AuthenticationManagerConfigurationListener authenticationManagerConfigurationListener() {
		return new AuthenticationManagerConfigurationListener();
	}

	/**
	 * {@link GlobalAuthenticationConfigurerAdapter} to apply
	 * {@link DefaultInMemoryUserDetailsManagerConfigurer}. We must apply
	 * {@link DefaultInMemoryUserDetailsManagerConfigurer} in the init phase of the last
	 * {@link GlobalAuthenticationConfigurerAdapter}. The reason is that the typical flow
	 * is something like:
	 *
	 * <ul>
	 * <li>A
	 * {@link GlobalAuthenticationConfigurerAdapter#init(AuthenticationManagerBuilder)}
	 * exists that adds a {@link SecurityConfigurer} to the
	 * {@link AuthenticationManagerBuilder}.</li>
	 * <li>{@link AuthenticationManagerConfiguration} adds
	 * {@link SpringBootAuthenticationConfigurerAdapter} so it is after the
	 * {@link SecurityConfigurer} in the first step.</li>
	 * <li>We then can default an {@link AuthenticationProvider} if necessary. Note we can
	 * only invoke the
	 * {@link AuthenticationManagerBuilder#authenticationProvider(AuthenticationProvider)}
	 * method since all other methods add a {@link SecurityConfigurer} which is not
	 * allowed in the configure stage. It is not allowed because we guarantee all init
	 * methods are invoked before configure, which cannot be guaranteed at this point.
	 * </li>
	 * </ul>
	 */
	@Order(Ordered.LOWEST_PRECEDENCE - 100)
	private static class SpringBootAuthenticationConfigurerAdapter
			extends GlobalAuthenticationConfigurerAdapter {

		@Override
		public void init(AuthenticationManagerBuilder auth) throws Exception {
			auth.apply(new DefaultInMemoryUserDetailsManagerConfigurer());
		}

	}

	/**
	 * {@link InMemoryUserDetailsManagerConfigurer} to add user details from
	 * {@link SecurityProperties}. This is necessary to delay adding the default user.
	 *
	 * <ul>
	 * <li>A {@link GlobalAuthenticationConfigurerAdapter} will initialize the
	 * {@link AuthenticationManagerBuilder} with a Configurer which will be after any
	 * {@link GlobalAuthenticationConfigurerAdapter}.</li>
	 * <li>{@link SpringBootAuthenticationConfigurerAdapter} will be invoked after all
	 * {@link GlobalAuthenticationConfigurerAdapter}, but before the Configurers that were
	 * added by other {@link GlobalAuthenticationConfigurerAdapter} instances.</li>
	 * <li>A {@link SpringBootAuthenticationConfigurerAdapter} will add
	 * {@link DefaultInMemoryUserDetailsManagerConfigurer} after all Configurer instances.
	 * </li>
	 * <li>All init methods will be invoked.</li>
	 * <li>All configure methods will be invoked which is where the
	 * {@link AuthenticationProvider} instances are setup.</li>
	 * <li>If no AuthenticationProviders were provided,
	 * {@link DefaultInMemoryUserDetailsManagerConfigurer} will default the value.</li>
	 * </ul>
	 */
	private static class DefaultInMemoryUserDetailsManagerConfigurer
			extends InMemoryUserDetailsManagerConfigurer<AuthenticationManagerBuilder> {

		@Override
		public void configure(AuthenticationManagerBuilder auth) throws Exception {
			if (auth.isConfigured()) {
				return;
			}
			String password = UUID.randomUUID().toString();
			logger.info(
					String.format("%n%nUsing default security password: %s%n", password));
			withUser("user").password(password).roles();
			setField(auth, "defaultUserDetailsService", getUserDetailsService());
			super.configure(auth);
		}

		private void setField(Object target, String name, Object value) {
			try {
				Field field = ReflectionUtils.findField(target.getClass(), name);
				ReflectionUtils.makeAccessible(field);
				ReflectionUtils.setField(field, target, value);
			}
			catch (Exception ex) {
				logger.info("Could not set " + name);
			}
		}

	}

	/**
	 * {@link ApplicationListener} to autowire the {@link AuthenticationEventPublisher}
	 * into the {@link AuthenticationManager}.
	 */
	protected static class AuthenticationManagerConfigurationListener
			implements SmartInitializingSingleton {

		@Autowired
		private AuthenticationEventPublisher eventPublisher;

		@Autowired
		private ApplicationContext context;

		@Override
		public void afterSingletonsInstantiated() {
			try {
				configureAuthenticationManager(
						this.context.getBean(AuthenticationManager.class));
			}
			catch (NoSuchBeanDefinitionException ex) {
				// Ignore
			}
		}

		private void configureAuthenticationManager(AuthenticationManager manager) {
			if (manager instanceof ProviderManager) {
				((ProviderManager) manager)
						.setAuthenticationEventPublisher(this.eventPublisher);
			}
		}

	}

}
