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

package org.springframework.boot.autoconfigure.social;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
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
import org.springframework.social.twitter.api.Twitter;
import org.springframework.social.twitter.api.impl.TwitterTemplate;
import org.springframework.social.twitter.connect.TwitterConnectionFactory;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Social connectivity with
 * Twitter.
 *
 * @author Craig Walls
 * @since 1.1.0
 */
@Configuration
@ConditionalOnClass({ SocialConfigurerAdapter.class, TwitterConnectionFactory.class })
@ConditionalOnProperty(prefix = "spring.social.twitter", name = "app-id")
@AutoConfigureBefore(SocialWebAutoConfiguration.class)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class TwitterAutoConfiguration {

	@Configuration
	@EnableSocial
	@EnableConfigurationProperties(TwitterProperties.class)
	@ConditionalOnWebApplication(type = Type.SERVLET)
	protected static class TwitterConfigurerAdapter extends SocialAutoConfigurerAdapter {

		private final TwitterProperties properties;

		protected TwitterConfigurerAdapter(TwitterProperties properties) {
			this.properties = properties;
		}

		@Bean
		@ConditionalOnMissingBean
		@Scope(value = "request", proxyMode = ScopedProxyMode.INTERFACES)
		public Twitter twitter(ConnectionRepository repository) {
			Connection<Twitter> connection = repository
					.findPrimaryConnection(Twitter.class);
			if (connection != null) {
				return connection.getApi();
			}
			return new TwitterTemplate(this.properties.getAppId(),
					this.properties.getAppSecret());
		}

		@Bean(name = { "connect/twitterConnect", "connect/twitterConnected" })
		@ConditionalOnProperty(prefix = "spring.social", name = "auto-connection-views")
		public GenericConnectionStatusView twitterConnectView() {
			return new GenericConnectionStatusView("twitter", "Twitter");
		}

		@Override
		protected ConnectionFactory<?> createConnectionFactory() {
			return new TwitterConnectionFactory(this.properties.getAppId(),
					this.properties.getAppSecret());
		}

	}

}
