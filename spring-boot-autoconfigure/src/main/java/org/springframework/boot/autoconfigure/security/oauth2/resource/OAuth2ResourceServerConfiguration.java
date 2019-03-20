/*
 * Copyright 2012-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.autoconfigure.security.oauth2.resource;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.autoconfigure.security.oauth2.resource.OAuth2ResourceServerConfiguration.ResourceServerCondition;
import org.springframework.boot.bind.RelaxedPropertyResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ConfigurationCondition;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
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

	@Bean
	public static ResourceServerFilterChainOrderProcessor resourceServerFilterChainOrderProcessor(
			ResourceServerProperties properties) {
		return new ResourceServerFilterChainOrderProcessor(properties);
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

	private static final class ResourceServerFilterChainOrderProcessor
			implements BeanPostProcessor, ApplicationContextAware {

		private final ResourceServerProperties properties;

		private ApplicationContext context;

		private ResourceServerFilterChainOrderProcessor(
				ResourceServerProperties properties) {
			this.properties = properties;
		}

		@Override
		public void setApplicationContext(ApplicationContext context)
				throws BeansException {
			this.context = context;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName)
				throws BeansException {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName)
				throws BeansException {
			if (bean instanceof ResourceServerConfiguration) {
				if (this.context.getBeanNamesForType(ResourceServerConfiguration.class,
						false, false).length == 1) {
					ResourceServerConfiguration config = (ResourceServerConfiguration) bean;
					config.setOrder(this.properties.getFilterOrder());
				}
			}
			return bean;
		}

	}

	protected static class ResourceServerCondition extends SpringBootCondition
			implements ConfigurationCondition {

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
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment,
					"security.oauth2.resource.");
			if (hasOAuthClientId(environment)) {
				return ConditionOutcome.match(message.foundExactly("client-id property"));
			}
			if (!resolver.getSubProperties("jwt").isEmpty()) {
				return ConditionOutcome
						.match(message.foundExactly("JWT resource configuration"));
			}
			if (!resolver.getSubProperties("jwk").isEmpty()) {
				return ConditionOutcome
						.match(message.foundExactly("JWK resource configuration"));
			}
			if (StringUtils.hasText(resolver.getProperty("user-info-uri"))) {
				return ConditionOutcome
						.match(message.foundExactly("user-info-uri property"));
			}
			if (StringUtils.hasText(resolver.getProperty("token-info-uri"))) {
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
					message.didNotFind("client id, JWT resource or authorization server")
							.atAll());
		}

		private boolean hasOAuthClientId(Environment environment) {
			RelaxedPropertyResolver resolver = new RelaxedPropertyResolver(environment,
					"security.oauth2.client.");
			return StringUtils.hasLength(resolver.getProperty("client-id", ""));
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
