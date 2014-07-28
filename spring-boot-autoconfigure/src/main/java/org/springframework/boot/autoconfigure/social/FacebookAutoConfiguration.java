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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.social.config.annotation.EnableSocial;
import org.springframework.social.config.annotation.SocialConfigurerAdapter;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.web.GenericConnectionStatusView;
import org.springframework.social.facebook.api.Facebook;
import org.springframework.social.facebook.api.impl.FacebookTemplate;
import org.springframework.social.facebook.connect.FacebookConnectionFactory;
import org.springframework.web.servlet.View;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Social connectivity with
 * Facebook.
 *
 * @author Craig Walls
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ SocialConfigurerAdapter.class, FacebookConnectionFactory.class })
@ConditionalOnProperty(prefix = "spring.social.facebook.", value = "app-id")
@AutoConfigureBefore(SocialWebAutoConfiguration.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class FacebookAutoConfiguration {

	@Configuration
	@EnableSocial
	@EnableConfigurationProperties(FacebookProperties.class)
	@ConditionalOnWebApplication
	protected static class FacebookAutoConfigurationAdapter extends
			SocialAutoConfigurerAdapter {

		@Autowired
		private FacebookProperties properties;

		@Bean
		@ConditionalOnMissingBean(Facebook.class)
		@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
		public Facebook facebook(ConnectionRepository repository) {
			Connection<Facebook> connection = repository
					.findPrimaryConnection(Facebook.class);
			return connection != null ? connection.getApi() : new FacebookTemplate();
		}

		@Bean(name = { "connect/facebookConnect", "connect/facebookConnected" })
		@ConditionalOnProperty(prefix = "spring.social.", value = "auto-connection-views")
		public View facebookConnectView() {
			return new GenericConnectionStatusView("facebook", "Facebook");
		}

		@Override
		protected ConnectionFactory<?> createConnectionFactory() {
			return new FacebookConnectionFactory(this.properties.getAppId(),
					this.properties.getAppSecret());
		}

	}

}
