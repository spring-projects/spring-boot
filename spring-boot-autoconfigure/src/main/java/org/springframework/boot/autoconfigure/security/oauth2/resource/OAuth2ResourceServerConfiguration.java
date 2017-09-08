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

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerConfiguration.ResourceServerCondition;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.ConfigurableEnvironment;
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
 * @author Madhura Bhave
 * @since 1.3.0
 */
@Configuration
@Conditional(ResourceServerCondition.class)
@ConditionalOnClass({ EnableResourceServer.class, SecurityProperties.class })
@ConditionalOnWebApplication
@ConditionalOnBean(ResourceServerConfiguration.class)
@Import(ResourceServerTokenServicesConfiguration.class)
public class OAuth2ResourceServerConfiguration {

	private final ResourceServerProperties resource;

	public OAuth2ResourceServerConfiguration(ResourceServerProperties resource) {
		this.resource = resource;
	}

	@Bean
	@ConditionalOnMissingBean(ResourceServerConfigurer.class)
	public ResourceServerConfigurer resourceServer() {
		return new ResourceSecurityConfigurer(this.resource);
	}

	protected static class ResourceSecurityConfigurer
			extends ResourceServerConfigurerAdapter {

		private ResourceServerProperties resource;

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

	protected static class ResourceServerCondition extends SpringBootCondition
			implements ConfigurationCondition {

		private static final Bindable<Map<String, Object>> STRING_OBJECT_MAP = Bindable
				.mapOf(String.class, Object.class);

		private static final String AUTHORIZATION_ANNOTATION = "org.springframework."
				+ "security.oauth2.config.annotation.web.configuration."
				+ "AuthorizationServerEndpointsConfiguration";

		@Override
		public ConfigurationPhase getConfigurationPhase() {
			return ConfigurationPhase.REGISTER_BEAN;
		}

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConditionMessage.Builder message = ConditionMessage
					.forCondition("OAuth ResourceServer Condition");
			Environment environment = context.getEnvironment();
			if (!(environment instanceof ConfigurableEnvironment)) {
				return ConditionOutcome
						.noMatch(message.didNotFind("A ConfigurableEnvironment").atAll());
			}
			if (hasOAuthClientId(environment)) {
				return ConditionOutcome.match(message.foundExactly("client-id property"));
			}
			Binder binder = Binder.get(environment);
			String prefix = "security.oauth2.resource.";
			if (binder.bind(prefix + "jwt", STRING_OBJECT_MAP).isBound()) {
				return ConditionOutcome
						.match(message.foundExactly("JWT resource configuration"));
			}
			if (binder.bind(prefix + "jwk", STRING_OBJECT_MAP).isBound()) {
				return ConditionOutcome
						.match(message.foundExactly("JWK resource configuration"));
			}
			if (StringUtils.hasText(environment.getProperty(prefix + "user-info-uri"))) {
				return ConditionOutcome
						.match(message.foundExactly("user-info-uri property"));
			}
			if (StringUtils.hasText(environment.getProperty(prefix + "token-info-uri"))) {
				return ConditionOutcome
						.match(message.foundExactly("token-info-uri property"));
			}
			if (ClassUtils.isPresent(AUTHORIZATION_ANNOTATION, null)) {
				if (AuthorizationServerEndpointsConfigurationBeanCondition
						.matches(context)) {
					return ConditionOutcome.match(
							message.found("class").items(AUTHORIZATION_ANNOTATION));
				}
			}
			return ConditionOutcome.noMatch(
					message.didNotFind("client ID, JWT resource or authorization server")
							.atAll());
		}

		private boolean hasOAuthClientId(Environment environment) {
			return StringUtils.hasLength(
					environment.getProperty("security.oauth2.client.client-id"));
		}

	}

	@ConditionalOnBean(AuthorizationServerEndpointsConfiguration.class)
	private static class AuthorizationServerEndpointsConfigurationBeanCondition {

		public static boolean matches(ConditionContext context) {
			Class<AuthorizationServerEndpointsConfigurationBeanCondition> type = AuthorizationServerEndpointsConfigurationBeanCondition.class;
			Conditional conditional = AnnotationUtils.findAnnotation(type,
					Conditional.class);
			StandardAnnotationMetadata metadata = new StandardAnnotationMetadata(type);
			for (Class<? extends Condition> conditionType : conditional.value()) {
				Condition condition = BeanUtils.instantiateClass(conditionType);
				if (condition.matches(context, metadata)) {
					return true;
				}
			}
			return false;
		}

	}

}
