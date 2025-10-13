/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.servlet;

import java.io.File;

import jakarta.servlet.Filter;
import org.apache.catalina.Valve;
import org.apache.catalina.valves.AccessLogValve;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.HierarchicalBeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.autoconfigure.condition.SearchStrategy;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.JettyVirtualThreadsWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.JettyWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.TomcatVirtualThreadsWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.TomcatWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.UndertowWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.TomcatServletWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.web.servlet.UndertowServletWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.DelegatingFilterProxyRegistrationBean;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.BeanIds;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.util.StringUtils;

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
	@ConditionalOnClass(name = "io.undertow.Undertow")
	static class UndertowConfiguration {

		@Bean
		UndertowAccessLogCustomizer undertowManagementAccessLogCustomizer() {
			return new UndertowAccessLogCustomizer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.apache.catalina.valves.AccessLogValve")
	static class TomcatConfiguration {

		@Bean
		TomcatAccessLogCustomizer tomcatManagementAccessLogCustomizer() {
			return new TomcatAccessLogCustomizer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(name = "org.eclipse.jetty.server.Server")
	static class JettyConfiguration {

		@Bean
		JettyAccessLogCustomizer jettyManagementAccessLogCustomizer() {
			return new JettyAccessLogCustomizer();
		}

	}

	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
	@ConditionalOnBean(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN, search = SearchStrategy.ANCESTORS)
	static class ServletManagementContextSecurityConfiguration {

		@Bean
		Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			return parent.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN, Filter.class);
		}

		@Bean
		@ConditionalOnBean(name = "securityFilterChainRegistration", search = SearchStrategy.ANCESTORS)
		DelegatingFilterProxyRegistrationBean securityFilterChainRegistration(HierarchicalBeanFactory beanFactory) {
			return beanFactory.getParentBeanFactory()
				.getBean("securityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class);
		}

	}

	static class ServletManagementWebServerFactoryCustomizer
			extends ManagementWebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

		ServletManagementWebServerFactoryCustomizer(ListableBeanFactory beanFactory) {
			super(beanFactory, ServletWebServerFactoryCustomizer.class, TomcatServletWebServerFactoryCustomizer.class,
					TomcatWebServerFactoryCustomizer.class, TomcatVirtualThreadsWebServerFactoryCustomizer.class,
					JettyWebServerFactoryCustomizer.class, JettyVirtualThreadsWebServerFactoryCustomizer.class,
					UndertowServletWebServerFactoryCustomizer.class, UndertowWebServerFactoryCustomizer.class);
		}

		@Override
		protected void customize(ConfigurableServletWebServerFactory webServerFactory,
				ManagementServerProperties managementServerProperties, ServerProperties serverProperties) {
			super.customize(webServerFactory, managementServerProperties, serverProperties);
			webServerFactory.setContextPath(getContextPath(managementServerProperties));
		}

		private String getContextPath(ManagementServerProperties managementServerProperties) {
			String basePath = managementServerProperties.getBasePath();
			return StringUtils.hasText(basePath) ? basePath : "";
		}

	}

	abstract static class AccessLogCustomizer implements Ordered {

		private static final String MANAGEMENT_PREFIX = "management_";

		protected String customizePrefix(String prefix) {
			prefix = (prefix != null) ? prefix : "";
			if (prefix.startsWith(MANAGEMENT_PREFIX)) {
				return prefix;
			}
			return MANAGEMENT_PREFIX + prefix;
		}

		@Override
		public int getOrder() {
			return 1;
		}

	}

	static class TomcatAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

		@Override
		public void customize(TomcatServletWebServerFactory factory) {
			AccessLogValve accessLogValve = findAccessLogValve(factory);
			if (accessLogValve == null) {
				return;
			}
			accessLogValve.setPrefix(customizePrefix(accessLogValve.getPrefix()));
		}

		private AccessLogValve findAccessLogValve(TomcatServletWebServerFactory factory) {
			for (Valve engineValve : factory.getEngineValves()) {
				if (engineValve instanceof AccessLogValve accessLogValve) {
					return accessLogValve;
				}
			}
			return null;
		}

	}

	static class UndertowAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

		@Override
		public void customize(UndertowServletWebServerFactory factory) {
			factory.setAccessLogPrefix(customizePrefix(factory.getAccessLogPrefix()));
		}

	}

	static class JettyAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

		@Override
		public void customize(JettyServletWebServerFactory factory) {
			factory.addServerCustomizers(this::customizeServer);
		}

		private void customizeServer(Server server) {
			RequestLog requestLog = server.getRequestLog();
			if (requestLog instanceof CustomRequestLog customRequestLog) {
				customizeRequestLog(customRequestLog);
			}
		}

		private void customizeRequestLog(CustomRequestLog requestLog) {
			if (requestLog.getWriter() instanceof RequestLogWriter requestLogWriter) {
				customizeRequestLogWriter(requestLogWriter);
			}
		}

		private void customizeRequestLogWriter(RequestLogWriter writer) {
			String filename = writer.getFileName();
			if (StringUtils.hasLength(filename)) {
				File file = new File(filename);
				file = new File(file.getParentFile(), customizePrefix(file.getName()));
				writer.setFilename(file.getPath());
			}
		}

	}

}
