/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet.autoconfigure.actuate.web;

import jakarta.servlet.Filter;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.util.Assert;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for Servlet web
 * endpoint infrastructure when a separate management context with a web server running on
 * a different port is required.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Phillip Webb
 * @author Moritz Halbritter
 */
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.SERVLET)
class ServletManagementChildContextConfiguration {

	@Bean
	ServletManagementWebServerFactoryCustomizer servletManagementWebServerFactoryCustomizer(
			ListableBeanFactory beanFactory) {
		return new ServletManagementWebServerFactoryCustomizer(beanFactory);
	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
	@ConditionalOnBean(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN, search = SearchStrategy.ANCESTORS)
	static class ServletManagementContextSecurityConfiguration {

		@Bean
		Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			Assert.state(parent != null, "'parent' must not be null");
			return parent.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN, Filter.class);
		}

		@Bean
		@ConditionalOnBean(name = "securityFilterChainRegistration", search = SearchStrategy.ANCESTORS)
		DelegatingFilterProxyRegistrationBean securityFilterChainRegistration(HierarchicalBeanFactory beanFactory) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			Assert.state(parent != null, "'parent' must not be null");
			return parent.getBean("securityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class);
		}

	}

}
