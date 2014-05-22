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
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.env.Environment;
import org.springframework.social.config.annotation.ConnectionFactoryConfigurer;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurerAdapter;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.GenericConnectionStatusView;
import org.springframework.social.linkedin.api.LinkedIn;
import org.springframework.social.linkedin.connect.LinkedInConnectionFactory;
import org.springframework.web.servlet.View;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Social connectivity
 * with LinkedIn.
 * 
 * @author Craig Walls
 */
@Configuration
@ConditionalOnClass({ LinkedInConnectionFactory.class })
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class LinkedInAutoConfiguration {

	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	protected static class LinkedInAutoConfigurationAdapter extends SocialConfigurerAdapter implements EnvironmentAware {

		private String appId;
		private String appSecret;

		@Override
		public void setEnvironment(Environment env) {
			RelaxedPropertyResolver propertyResolver = new RelaxedPropertyResolver(env, "spring.social.");
			this.appId = propertyResolver.getRequiredProperty("linkedin.appId");
			this.appSecret = propertyResolver.getRequiredProperty("linkedin.appSecret");
		}
		
		@Override
		public void addConnectionFactories(ConnectionFactoryConfigurer cfConfig, Environment env) {
			cfConfig.addConnectionFactory(new LinkedInConnectionFactory(appId, appSecret));
		}

		@Bean
		@ConditionalOnMissingBean(LinkedInConnectionFactory.class)
		@Scope(value="request", proxyMode=ScopedProxyMode.INTERFACES)
		public LinkedIn linkedin(ConnectionRepository repository) {
			Connection<LinkedIn> connection = repository.findPrimaryConnection(LinkedIn.class);
			return connection != null ? connection.getApi() : null;
		}

		@Bean(name={"connect/linkedinConnect", "connect/linkedinConnected"})
		@ConditionalOnExpression("${spring.social.auto_connection_views:false}")
		public View linkedInConnectView() {
			return new GenericConnectionStatusView("linkedin", "LinkedIn");
		}

	}

}
