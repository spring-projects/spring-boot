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

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.social.UserIdSource;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurerAdapter;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.ConnectController;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.BeanNameViewResolver;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Social's web connection support.
 * 
 * @author Craig Walls
 */
@Configuration
@ConditionalOnClass({ ConnectController.class })
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class SocialWebAutoConfiguration {

	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	protected static class SocialAutoConfigurationAdapter extends SocialConfigurerAdapter {
		@Bean
		@ConditionalOnMissingBean(ConnectController.class)
		public ConnectController connectController(
				ConnectionFactoryLocator connectionFactoryLocator, ConnectionRepository connectionRepository) {
			return new ConnectController(connectionFactoryLocator, connectionRepository);
		}
		
		@Bean
		@ConditionalOnMissingBean(BeanNameViewResolver.class)
		@ConditionalOnExpression("${spring.social.auto_connection_views:false}")
		public ViewResolver beanNameViewResolver() {
			BeanNameViewResolver bnvr = new BeanNameViewResolver();
			bnvr.setOrder(Integer.MIN_VALUE);
			return bnvr;
		}
		
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

}
