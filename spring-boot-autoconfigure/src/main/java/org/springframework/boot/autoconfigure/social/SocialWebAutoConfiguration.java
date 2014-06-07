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

package org.springframework.boot.autoconfigure.social;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurerAdapter;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.social.connect.web.ConnectInterceptor;
import org.springframework.social.connect.web.DisconnectInterceptor;
import org.springframework.social.connect.web.ProviderSignInController;
import org.springframework.social.connect.web.ProviderSignInInterceptor;
import org.springframework.social.connect.web.SignInAdapter;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Social's web connection
 * support.
 * 
 * @author Craig Walls
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ ConnectController.class, SocialConfigurerAdapter.class })
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class SocialWebAutoConfiguration {

	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	protected static class SocialAutoConfigurationAdapter extends SocialConfigurerAdapter {

		@Autowired(required=false)
		private List<ConnectInterceptor<?>> connectInterceptors;

		@Autowired(required=false)
		private List<DisconnectInterceptor<?>> disconnectInterceptors;

		@Autowired(required=false)
		private List<ProviderSignInInterceptor<?>> signInInterceptors;

		@Bean
		@ConditionalOnMissingBean(ConnectController.class)
		public ConnectController connectController(
				ConnectionFactoryLocator connectionFactoryLocator,
				ConnectionRepository connectionRepository) {
			ConnectController connectController = new ConnectController(connectionFactoryLocator, connectionRepository);
			if (connectInterceptors != null && connectInterceptors.size() > 0) {
				connectController.setConnectInterceptors(connectInterceptors);
			}
			if (disconnectInterceptors != null && disconnectInterceptors.size() > 0) {
				connectController.setDisconnectInterceptors(disconnectInterceptors);
			}
			return connectController;
		}

		@Bean
		@ConditionalOnMissingBean(BeanNameViewResolver.class)
		@ConditionalOnProperty(prefix = "spring.social.", value = "auto-connection-views")
		public ViewResolver beanNameViewResolver() {
			BeanNameViewResolver bnvr = new BeanNameViewResolver();
			bnvr.setOrder(Integer.MIN_VALUE);
			return bnvr;
		}
		
		@Bean
		@ConditionalOnBean(SignInAdapter.class)
		@ConditionalOnMissingBean(ProviderSignInController.class)
		public ProviderSignInController signInController(
				ConnectionFactoryLocator connectionFactoryLocator,
				UsersConnectionRepository usersConnectionRepository, 
				SignInAdapter signInAdapter) {
			ProviderSignInController signInController = new ProviderSignInController(connectionFactoryLocator, usersConnectionRepository, signInAdapter);
			if (signInInterceptors != null && signInInterceptors.size() > 0) {
				signInController.setSignInInterceptors(signInInterceptors);
			}
			return signInController;
		}
		
	}
	
	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	@ConditionalOnMissingClass(SecurityContextHolder.class)
	protected static class AnonymousUserIdSourceConfig extends SocialConfigurerAdapter {
		@Override
		public UserIdSource getUserIdSource() {
			return new UserIdSource() {
				@Override
				public String getUserId() {
					return "anonymous";
				}
			};
		}
	}

	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	@ConditionalOnClass(SecurityContextHolder.class)
	protected static class AuthenticationUserIdSourceConfig extends SocialConfigurerAdapter {
		@Override
		public UserIdSource getUserIdSource() {
			return new UserIdSource() {			
				@Override
				public String getUserId() {
					Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
					if (authentication == null) {
						throw new IllegalStateException("Unable to get a ConnectionRepository: no user signed in");
					}
					return authentication.getName();
				}
			};
		}
	}

}
