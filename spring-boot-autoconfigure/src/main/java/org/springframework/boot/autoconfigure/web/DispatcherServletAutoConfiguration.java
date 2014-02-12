/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.servlet.DispatcherServlet;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the Spring
 * {@link DispatcherServlet}. Should work for a standalone application where an embedded
 * servlet container is already present and also for a deployable application using
 * {@link SpringBootServletInitializer}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(DispatcherServlet.class)
@ConditionalOnBean(EmbeddedServletContainerFactory.class)
@AutoConfigureAfter(EmbeddedServletContainerAutoConfiguration.class)
public class DispatcherServletAutoConfiguration {

	/*
	 * The bean name for a DispatcherServlet that will be mapped to the root URL "/"
	 */
	public static final String DEFAULT_DISPATCHER_SERVLET_BEAN_NAME = "dispatcherServlet";

	@Bean(name = DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)
	@Conditional(DefaultDispatcherServletCondition.class)
	public DispatcherServlet dispatcherServlet() {
		return new DispatcherServlet();
	}

	private static class DefaultDispatcherServletCondition extends SpringBootCondition {

		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {

			ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
			List<String> servlets = Arrays.asList(beanFactory.getBeanNamesForType(
					DispatcherServlet.class, false, false));
			boolean containsDispatcherBean = beanFactory
					.containsBean(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
			if (servlets.isEmpty()) {
				if (containsDispatcherBean) {
					return ConditionOutcome
							.noMatch("found no DispatcherServlet but a non-DispatcherServlet named "
									+ DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
				}
				return ConditionOutcome.match("no DispatcherServlet found");
			}
			if (servlets.contains(DEFAULT_DISPATCHER_SERVLET_BEAN_NAME)) {
				return ConditionOutcome.noMatch("found DispatcherServlet named "
						+ DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
			}
			if (containsDispatcherBean) {
				return ConditionOutcome.noMatch("found non-DispatcherServlet named "
						+ DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
			}
			return ConditionOutcome
					.match("one or more DispatcherServlets found and none is named "
							+ DEFAULT_DISPATCHER_SERVLET_BEAN_NAME);
		}

	}

}
