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

package org.springframework.boot.autoconfigure.security.oauth2;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.oauth2.authserver.SpringSecurityOAuth2AuthorizationServerConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.client.OAuth2RestOperationsConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.method.OAuth2MethodSecurityConfiguration;
import org.springframework.boot.autoconfigure.security.oauth2.resource.ResourceServerProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerConfiguration;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * Spring Security OAuth2 top level auto-configuration beans
 *
 * @author Greg Turnquist
 * @author Dave Syer
 */
@Configuration
@ConditionalOnClass({ OAuth2AccessToken.class, WebMvcConfigurerAdapter.class })
@Import({ SpringSecurityOAuth2AuthorizationServerConfiguration.class,
		OAuth2MethodSecurityConfiguration.class,
		OAuth2ResourceServerConfiguration.class,
		OAuth2RestOperationsConfiguration.class })
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@EnableConfigurationProperties(OAuth2ClientProperties.class)
public class OAuth2AutoConfiguration {

	@Autowired
	private OAuth2ClientProperties credentials;

	@Bean
	public ResourceServerProperties resourceServerProperties() {
		return new ResourceServerProperties(this.credentials.getClientId(),
				this.credentials.getClientSecret());
	}

	@Configuration
	@ConditionalOnWebApplication
	protected static class ResourceServerOrderProcessor implements BeanPostProcessor {

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof ResourceServerConfiguration) {
				ResourceServerConfiguration configuration = (ResourceServerConfiguration) bean;
				configuration.setOrder(getOrder());
			}
			return bean;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

		private int getOrder() {
			// Before the authorization server (default 0)
			return -10;
		}

	}

}
