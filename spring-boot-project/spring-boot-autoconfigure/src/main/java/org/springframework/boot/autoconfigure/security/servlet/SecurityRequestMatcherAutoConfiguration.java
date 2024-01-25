/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.autoconfigure.security.servlet;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionMessage;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.web.servlet.util.matcher.MvcRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.HandlerMappingIntrospector;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for {@link RequestMatcher}.
 *
 * @author Wang Zhiyang
 * @since 3.3.0
 */
@AutoConfiguration(after = SecurityAutoConfiguration.class)
@ConditionalOnWebApplication(type = Type.SERVLET)
public class SecurityRequestMatcherAutoConfiguration {

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(DispatcherServlet.class)
	@EnableConfigurationProperties(WebMvcProperties.class)
	public static class SecurityMvcRequestMatcherConfiguration {

		@Bean
		@ConditionalOnClass({ DispatcherServlet.class, HandlerMappingIntrospector.class })
		@Conditional(ExactlyOneDispatcherServletCondition.class)
		@ConditionalOnBean(HandlerMappingIntrospector.class)
		@ConditionalOnMissingBean
		MvcRequestMatcher.Builder mvcRequestMatcherBuilder(HandlerMappingIntrospector introspector,
				WebMvcProperties webMvcProperties) {
			String servletPath = webMvcProperties.getServlet().getPath();
			MvcRequestMatcher.Builder mvc = new MvcRequestMatcher.Builder(introspector);
			return ("/".equals(servletPath)) ? mvc : mvc.servletPath(servletPath);
		}

	}

	@Order(Ordered.LOWEST_PRECEDENCE - 10)
	private static final class ExactlyOneDispatcherServletCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context, AnnotatedTypeMetadata metadata) {

			ConditionMessage.Builder message = ConditionMessage.forCondition("Exactly One DispatcherServlet");
			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			List<String> dispatchServletBeans = Arrays.asList(beanFactory.getBeanNamesForType(DispatcherServlet.class));
			List<String> dispatchServletSingletonBeans = Arrays
				.asList(beanFactory.getBeanNamesForType(DispatcherServlet.class, false, false));
			if (dispatchServletSingletonBeans.size() < dispatchServletBeans.size()) {
				return ConditionOutcome.noMatch(message.foundExactly("scope not singleton bean"));
			}
			else if (dispatchServletSingletonBeans.size() == 1 && dispatchServletBeans.size() == 1) {
				return ConditionOutcome.match(message.found("single bean").items(dispatchServletBeans.get(0)));
			}
			else if (dispatchServletSingletonBeans.isEmpty()) {
				return ConditionOutcome
					.noMatch(message.foundExactly("non dispatcher servlet bean that scope is singleton"));
			}
			else {
				return ConditionOutcome
					.noMatch(message.found("multiple dispatcher servlet bean that scope is singleton")
						.items(dispatchServletSingletonBeans));
			}
		}

	}

}
