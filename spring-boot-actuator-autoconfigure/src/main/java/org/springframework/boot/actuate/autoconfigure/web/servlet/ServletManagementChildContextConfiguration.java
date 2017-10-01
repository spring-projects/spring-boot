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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import javax.servlet.Filter;

import org.apache.catalina.Valve;
import org.apache.catalina.valves.AccessLogValve;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerFactoryCustomizer;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.DefaultServletWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * {@link ManagementContextConfiguration} for Servlet web endpoint infrastructure when a
 * separate management context with a web server running on a different port is required.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 * @author Andy Wilkinson
 * @author Eddú Meléndez
 * @author Phillip Webb
 */
@Configuration
@ManagementContextConfiguration(ManagementContextType.CHILD)
@ConditionalOnWebApplication(type = Type.SERVLET)
class ServletManagementChildContextConfiguration {

	@Bean
	public ServletManagementServerFactoryCustomizer serverFactoryCustomizer(
			ListableBeanFactory beanFactory) {
		return new ServletManagementServerFactoryCustomizer(beanFactory);
	}

	@Bean
	public UndertowAccessLogCustomizer undertowAccessLogCustomizer() {
		return new UndertowAccessLogCustomizer();
	}

	@Bean
	@ConditionalOnClass(name = "org.apache.catalina.valves.AccessLogValve")
	public TomcatAccessLogCustomizer tomcatAccessLogCustomizer() {
		return new TomcatAccessLogCustomizer();
	}

	@Configuration
	@ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
	@ConditionalOnBean(name = "springSecurityFilterChain", search = SearchStrategy.ANCESTORS)
	class ServletManagementContextSecurityConfiguration {

		@Bean
		public Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			return parent.getBean("springSecurityFilterChain", Filter.class);
		}

	}

	static class ServletManagementServerFactoryCustomizer extends
			ManagementServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

		ServletManagementServerFactoryCustomizer(ListableBeanFactory beanFactory) {
			super(beanFactory, DefaultServletWebServerFactoryCustomizer.class);
		}

		@Override
		protected void customize(ConfigurableServletWebServerFactory webServerFactory,
				ManagementServerProperties managementServerProperties,
				ServerProperties serverProperties) {
			super.customize(webServerFactory, managementServerProperties,
					serverProperties);
			webServerFactory.setContextPath("");
		}

	}

	static abstract class AccessLogCustomizer implements Ordered {

		protected String customizePrefix(String prefix) {
			return "management_" + prefix;
		}

		@Override
		public int getOrder() {
			return 1;
		}

	}

	static class TomcatAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

		@Override
		public void customize(TomcatServletWebServerFactory serverFactory) {
			AccessLogValve accessLogValve = findAccessLogValve(serverFactory);
			if (accessLogValve == null) {
				return;
			}
			accessLogValve.setPrefix(customizePrefix(accessLogValve.getPrefix()));
		}

		private AccessLogValve findAccessLogValve(
				TomcatServletWebServerFactory serverFactory) {
			for (Valve engineValve : serverFactory.getEngineValves()) {
				if (engineValve instanceof AccessLogValve) {
					return (AccessLogValve) engineValve;
				}
			}
			return null;
		}

	}

	static class UndertowAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

		@Override
		public void customize(UndertowServletWebServerFactory serverFactory) {
			serverFactory.setAccessLogPrefix(
					customizePrefix(serverFactory.getAccessLogPrefix()));
		}

	}

}
