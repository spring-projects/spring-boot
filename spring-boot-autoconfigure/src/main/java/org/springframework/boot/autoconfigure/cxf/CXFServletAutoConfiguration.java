/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.cxf;

import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletRegistration;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.embedded.ServletRegistrationBean;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the {@link CXFServlet}
 * . Should work for a standalone application where an embedded servlet
 * container is already present and also for a deployable application using
 * {@link SpringBootServletInitializer}.
 *
 * @author Elan Thangamani
 */
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
@Configuration
@ConditionalOnWebApplication
@ConditionalOnClass(CXFServlet.class)
@AutoConfigureAfter(EmbeddedServletContainerAutoConfiguration.class)
public class CXFServletAutoConfiguration {

	/*
	 * The bean name for a ServletRegistrationBean for the CXFServlet
	 * "/service/*"
	 */
	public static final String DEFAULT_CXF_SERVLET_BEAN_NAME = "cxfServlet";

	@Configuration
	@Conditional(DefaultCXFServletCondition.class)
	@ConditionalOnClass(ServletRegistration.class)
	protected static class CXFServletConfiguration {

		@Bean(name = DEFAULT_CXF_SERVLET_BEAN_NAME)
		public ServletRegistrationBean cxfServletRegistration() {
			ServletRegistrationBean registration = new ServletRegistrationBean(
					new CXFServlet(), "/service/*");
			registration.setName(DEFAULT_CXF_SERVLET_BEAN_NAME);
			return registration;
		}
	}

	@Order(Ordered.LOWEST_PRECEDENCE - 10)
	private static class DefaultCXFServletCondition extends SpringBootCondition {
		@Override
		public ConditionOutcome getMatchOutcome(ConditionContext context,
				AnnotatedTypeMetadata metadata) {
			ConfigurableListableBeanFactory beanFactory = context
					.getBeanFactory();
			return checkServletRegistrations(beanFactory);
		}

	}

	private static ConditionOutcome checkServletRegistrations(
			ConfigurableListableBeanFactory beanFactory) {

		List<String> registrations = Arrays.asList(beanFactory
				.getBeanNamesForType(ServletRegistrationBean.class, false,
						false));
		boolean containsDispatcherRegistrationBean = beanFactory
				.containsBean(DEFAULT_CXF_SERVLET_BEAN_NAME);

		if (registrations.isEmpty()) {
			if (containsDispatcherRegistrationBean) {
				return ConditionOutcome
						.noMatch("found no ServletRegistrationBean "
								+ "but a non-ServletRegistrationBean named "
								+ DEFAULT_CXF_SERVLET_BEAN_NAME);
			}
			return ConditionOutcome.match("no ServletRegistrationBean found");
		}

		if (registrations.contains(DEFAULT_CXF_SERVLET_BEAN_NAME)) {
			return ConditionOutcome
					.noMatch("found ServletRegistrationBean named "
							+ DEFAULT_CXF_SERVLET_BEAN_NAME);
		}
		if (containsDispatcherRegistrationBean) {
			return ConditionOutcome
					.noMatch("found non-ServletRegistrationBean named "
							+ DEFAULT_CXF_SERVLET_BEAN_NAME);
		}

		return ConditionOutcome
				.match("one or more ServletRegistrationBeans is found and none is named "
						+ DEFAULT_CXF_SERVLET_BEAN_NAME);

	}
}
