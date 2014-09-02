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
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
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
import org.springframework.social.connect.web.thymeleaf.SpringSocialDialect;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;
import org.thymeleaf.spring4.SpringTemplateEngine;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Social's web connection
 * support.
 *
 * @author Craig Walls
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ ConnectController.class, SocialConfigurerAdapter.class })
@ConditionalOnBean({ ConnectionFactoryLocator.class, UsersConnectionRepository.class })
@AutoConfigureBefore(ThymeleafAutoConfiguration.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class SocialWebAutoConfiguration {

	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	protected static class SocialAutoConfigurationAdapter extends SocialConfigurerAdapter {

		@Autowired(required = false)
		private List<ConnectInterceptor<?>> connectInterceptors;

		@Autowired(required = false)
		private List<DisconnectInterceptor<?>> disconnectInterceptors;

		@Autowired(required = false)
		private List<ProviderSignInInterceptor<?>> signInInterceptors;

		@Bean
		@ConditionalOnMissingBean(ConnectController.class)
		public ConnectController connectController(
				ConnectionFactoryLocator factoryLocator, ConnectionRepository repository) {
			ConnectController controller = new ConnectController(factoryLocator,
					repository);
			if (!CollectionUtils.isEmpty(this.connectInterceptors)) {
				controller.setConnectInterceptors(this.connectInterceptors);
			}
			if (!CollectionUtils.isEmpty(this.disconnectInterceptors)) {
				controller.setDisconnectInterceptors(this.disconnectInterceptors);
			}
			return controller;
		}

		@Bean
		@ConditionalOnMissingBean(BeanNameViewResolver.class)
		@ConditionalOnProperty(prefix = "spring.social", name = "auto-connection-views")
		public ViewResolver beanNameViewResolver() {
			BeanNameViewResolver viewResolver = new BeanNameViewResolver();
			viewResolver.setOrder(Integer.MIN_VALUE);
			return viewResolver;
		}

		@Bean
		@ConditionalOnBean(SignInAdapter.class)
		@ConditionalOnMissingBean(ProviderSignInController.class)
		public ProviderSignInController signInController(
				ConnectionFactoryLocator factoryLocator,
				UsersConnectionRepository usersRepository, SignInAdapter signInAdapter) {
			ProviderSignInController controller = new ProviderSignInController(
					factoryLocator, usersRepository, signInAdapter);
			if (!CollectionUtils.isEmpty(this.signInInterceptors)) {
				controller.setSignInInterceptors(this.signInInterceptors);
			}
			return controller;
		}

	}

	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	@ConditionalOnMissingClass(name = "org.springframework.security.core.context.SecurityContextHolder")
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
	protected static class AuthenticationUserIdSourceConfig extends
			SocialConfigurerAdapter {

		@Override
		public UserIdSource getUserIdSource() {
			return new SecurityContextUserIdSource();
		}

	}

	@Configuration
	@ConditionalOnClass(SpringTemplateEngine.class)
	protected static class SpringSocialThymeleafConfig {

		@Bean
		@ConditionalOnMissingBean
		public SpringSocialDialect springSocialDialect() {
			return new SpringSocialDialect();
		}

	}

	private static class SecurityContextUserIdSource implements UserIdSource {

		@Override
		public String getUserId() {
			SecurityContext context = SecurityContextHolder.getContext();
			Authentication authentication = context.getAuthentication();
			Assert.state(authentication != null, "Unable to get a "
					+ "ConnectionRepository: no user signed in");
			return authentication.getName();
		}

	}
}
