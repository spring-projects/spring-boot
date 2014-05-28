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
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.GenericConnectionStatusView;
import org.springframework.social.linkedin.api.LinkedIn;
import org.springframework.social.linkedin.connect.LinkedInConnectionFactory;
import org.springframework.web.servlet.View;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Social connectivity with
 * LinkedIn.
 * 
 * @author Craig Walls
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ LinkedInConnectionFactory.class })
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class LinkedInAutoConfiguration {

	@Configuration
	@EnableSocial
	@ConditionalOnWebApplication
	protected static class LinkedInAutoConfigurationAdapter extends
			SocialAutoConfigurerAdapter {

		@Override
		protected String getPropertyPrefix() {
			return "spring.social.linkedin.";
		}

		@Override
		protected ConnectionFactory<?> createConnectionFactory(
				RelaxedPropertyResolver properties) {
			return new LinkedInConnectionFactory(
					properties.getRequiredProperty("app-id"),
					properties.getRequiredProperty("app-secret"));
		}

		@Bean
		@ConditionalOnMissingBean(LinkedInConnectionFactory.class)
		@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
		public LinkedIn linkedin(ConnectionRepository repository) {
			Connection<LinkedIn> connection = repository
					.findPrimaryConnection(LinkedIn.class);
			return connection != null ? connection.getApi() : null;
		}

		@Bean(name = { "connect/linkedinConnect", "connect/linkedinConnected" })
		@ConditionalOnProperty(prefix = "spring.social.", value = "auto-connection-views")
		public View linkedInConnectView() {
			return new GenericConnectionStatusView("linkedin", "LinkedIn");
		}

	}

}
