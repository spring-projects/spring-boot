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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.OnBeanCondition;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.ClientCredentialsProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.SpringSecurityOAuth2ResourceServerConfiguration.ResourceServerCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfiguration;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Auto-configure a Spring Security OAuth2 resource server. Back off if another
 * {@link ResourceServerConfigurer} already exists or if resource server not enabled.
 *
 * @author Greg Turnquist
 * @author Dave Syer
 */
@Configuration
@Conditional(ResourceServerCondition.class)
@ConditionalOnClass({ EnableResourceServer.class, SecurityProperties.class })
@ConditionalOnWebApplication
@ConditionalOnBean(ResourceServerConfiguration.class)
@Import(ResourceServerTokenServicesConfiguration.class)
public class SpringSecurityOAuth2ResourceServerConfiguration {

	@Autowired
	private ResourceServerProperties resource;

	@Bean
	@ConditionalOnMissingBean(ResourceServerConfigurer.class)
	public ResourceServerConfigurer resourceServer() {
		return new ResourceSecurityConfigurer(this.resource);
	}

	@Configuration
	protected static class ResourceServerPropertiesConfiguration {

		@Autowired
		private ClientCredentialsProperties credentials;

		@Bean
		public ResourceServerProperties resourceServerProperties() {
			return new ResourceServerProperties(this.credentials.getClientId(),
					this.credentials.getClientSecret());
		}
	}

	protected static class ResourceSecurityConfigurer extends
			ResourceServerConfigurerAdapter {

		private ResourceServerProperties resource;

		@Autowired
		public ResourceSecurityConfigurer(ResourceServerProperties resource) {
			this.resource = resource;
		}

		@Override
		public void configure(ResourceServerSecurityConfigurer resources)
				throws Exception {
			resources.resourceId(this.resource.getResourceId());
		}

		@Override
		public void configure(HttpSecurity http) throws Exception {
			http.authorizeRequests().anyRequest().authenticated();
		}

	}

	@ConditionalOnBean(AuthorizationServerEndpointsConfiguration.class)
	protected static class ResourceServerCondition extends SpringBootCondition implements
			ConfigurationCondition {

		private OnBeanCondition condition = new OnBeanCondition();

		private StandardAnnotationMetadata beanMetaData = new StandardAnnotationMetadata(
				ResourceServerCondition.class);

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			Environment environment = context.getEnvironment();
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment);
			String client = environment
					.resolvePlaceholders("${spring.oauth2.client.clientId:}");
			if (StringUtils.hasText(client)) {
				return ConditionOutcome.match("found client id");
			}
			if (!resolver.getSubProperties("spring.oauth2.resource.jwt").isEmpty()) {
				return ConditionOutcome.match("found JWT resource configuration");
			}
			if (StringUtils.hasText(resolver
					.getProperty("spring.oauth2.resource.userInfoUri"))) {
				return ConditionOutcome
						.match("found UserInfo URI resource configuration");
			}
			if (ClassUtils
					.isPresent(
							"org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerEndpointsConfiguration",
							null)) {
				if (this.condition.matches(context, this.beanMetaData)) {
					return ConditionOutcome
							.match("found authorization server configuration");
				}
			}
			return ConditionOutcome
					.noMatch("found neither client id nor JWT resource nor authorization server");
		}

	}

}
