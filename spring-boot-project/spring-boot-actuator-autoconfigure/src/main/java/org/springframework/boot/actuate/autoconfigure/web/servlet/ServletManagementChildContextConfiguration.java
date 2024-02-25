/*
 * Copyright 2012-2023 the original author or authors.
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

	/**
	 * Creates a new instance of ServletManagementWebServerFactoryCustomizer with the
	 * given bean factory.
	 * @param beanFactory the ListableBeanFactory used to create the customizer
	 * @return the ServletManagementWebServerFactoryCustomizer instance
	 */
	@Bean
	ServletManagementWebServerFactoryCustomizer servletManagementWebServerFactoryCustomizer(
			ListableBeanFactory beanFactory) {
		return new ServletManagementWebServerFactoryCustomizer(beanFactory);
	}

	/**
	 * Creates an instance of UndertowAccessLogCustomizer if the class
	 * "io.undertow.Undertow" is present. This method is annotated with @Bean
	 * and @ConditionalOnClass to conditionally create the bean based on the presence of
	 * the specified class.
	 * @return an instance of UndertowAccessLogCustomizer
	 */
	@Bean
	@ConditionalOnClass(name = "io.undertow.Undertow")
	UndertowAccessLogCustomizer undertowManagementAccessLogCustomizer() {
		return new UndertowAccessLogCustomizer();
	}

	/**
	 * Creates a TomcatAccessLogCustomizer bean if the class
	 * "org.apache.catalina.valves.AccessLogValve" is present. This bean is used to
	 * customize the access log for Tomcat management.
	 * @return the TomcatAccessLogCustomizer bean
	 */
	@Bean
	@ConditionalOnClass(name = "org.apache.catalina.valves.AccessLogValve")
	TomcatAccessLogCustomizer tomcatManagementAccessLogCustomizer() {
		return new TomcatAccessLogCustomizer();
	}

	/**
	 * Creates a JettyAccessLogCustomizer bean if the class
	 * "org.eclipse.jetty.server.Server" is present in the classpath. This bean is used to
	 * customize the access log for Jetty server.
	 * @return the JettyAccessLogCustomizer bean
	 */
	@Bean
	@ConditionalOnClass(name = "org.eclipse.jetty.server.Server")
	JettyAccessLogCustomizer jettyManagementAccessLogCustomizer() {
		return new JettyAccessLogCustomizer();
	}

	/**
	 * ServletManagementContextSecurityConfiguration class.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass({ EnableWebSecurity.class, Filter.class })
	@ConditionalOnBean(name = BeanIds.SPRING_SECURITY_FILTER_CHAIN, search = SearchStrategy.ANCESTORS)
	static class ServletManagementContextSecurityConfiguration {

		/**
		 * Retrieves the Spring Security filter chain from the parent bean factory.
		 * @param beanFactory the hierarchical bean factory
		 * @return the Spring Security filter chain
		 */
		@Bean
		Filter springSecurityFilterChain(HierarchicalBeanFactory beanFactory) {
			BeanFactory parent = beanFactory.getParentBeanFactory();
			return parent.getBean(BeanIds.SPRING_SECURITY_FILTER_CHAIN, Filter.class);
		}

		/**
		 * Retrieves the DelegatingFilterProxyRegistrationBean for the security filter
		 * chain registration. This method is conditional on the presence of a bean named
		 * "securityFilterChainRegistration" in the ancestor bean factory.
		 * @param beanFactory the HierarchicalBeanFactory used to retrieve the parent bean
		 * factory
		 * @return the DelegatingFilterProxyRegistrationBean for the security filter chain
		 * registration
		 */
		@Bean
		@ConditionalOnBean(name = "securityFilterChainRegistration", search = SearchStrategy.ANCESTORS)
		DelegatingFilterProxyRegistrationBean securityFilterChainRegistration(HierarchicalBeanFactory beanFactory) {
			return beanFactory.getParentBeanFactory()
				.getBean("securityFilterChainRegistration", DelegatingFilterProxyRegistrationBean.class);
		}

	}

	/**
	 * ServletManagementWebServerFactoryCustomizer class.
	 */
	static class ServletManagementWebServerFactoryCustomizer
			extends ManagementWebServerFactoryCustomizer<ConfigurableServletWebServerFactory> {

		/**
		 * Constructs a new ServletManagementWebServerFactoryCustomizer with the specified
		 * ListableBeanFactory.
		 * @param beanFactory the ListableBeanFactory to be used for customization
		 */
		ServletManagementWebServerFactoryCustomizer(ListableBeanFactory beanFactory) {
			super(beanFactory, ServletWebServerFactoryCustomizer.class, TomcatServletWebServerFactoryCustomizer.class,
					TomcatWebServerFactoryCustomizer.class, TomcatVirtualThreadsWebServerFactoryCustomizer.class,
					JettyWebServerFactoryCustomizer.class, JettyVirtualThreadsWebServerFactoryCustomizer.class,
					UndertowServletWebServerFactoryCustomizer.class, UndertowWebServerFactoryCustomizer.class);
		}

		/**
		 * Customizes the ServletWebServerFactory with the provided configuration
		 * properties.
		 * @param webServerFactory the ServletWebServerFactory to be customized
		 * @param managementServerProperties the configuration properties for the
		 * management server
		 * @param serverProperties the configuration properties for the server
		 */
		@Override
		protected void customize(ConfigurableServletWebServerFactory webServerFactory,
				ManagementServerProperties managementServerProperties, ServerProperties serverProperties) {
			super.customize(webServerFactory, managementServerProperties, serverProperties);
			webServerFactory.setContextPath(getContextPath(managementServerProperties));
		}

		/**
		 * Returns the context path for the management server.
		 * @param managementServerProperties the management server properties
		 * @return the context path, or an empty string if not specified
		 */
		private String getContextPath(ManagementServerProperties managementServerProperties) {
			String basePath = managementServerProperties.getBasePath();
			return StringUtils.hasText(basePath) ? basePath : "";
		}

	}

	/**
	 * AccessLogCustomizer class.
	 */
	abstract static class AccessLogCustomizer implements Ordered {

		private static final String MANAGEMENT_PREFIX = "management_";

		/**
		 * Customizes the prefix string by adding a management prefix if it does not
		 * already have one.
		 * @param prefix the prefix string to be customized
		 * @return the customized prefix string
		 */
		protected String customizePrefix(String prefix) {
			prefix = (prefix != null) ? prefix : "";
			if (prefix.startsWith(MANAGEMENT_PREFIX)) {
				return prefix;
			}
			return MANAGEMENT_PREFIX + prefix;
		}

		/**
		 * Returns the order of this AccessLogCustomizer.
		 *
		 * The order determines the position of this customizer in the chain of
		 * customizers. Customizers with lower order values are executed first.
		 * @return the order of this AccessLogCustomizer
		 */
		@Override
		public int getOrder() {
			return 1;
		}

	}

	/**
	 * TomcatAccessLogCustomizer class.
	 */
	static class TomcatAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<TomcatServletWebServerFactory> {

		/**
		 * Customizes the Tomcat servlet web server factory by modifying the access log
		 * valve.
		 * @param factory the Tomcat servlet web server factory to customize
		 */
		@Override
		public void customize(TomcatServletWebServerFactory factory) {
			AccessLogValve accessLogValve = findAccessLogValve(factory);
			if (accessLogValve == null) {
				return;
			}
			accessLogValve.setPrefix(customizePrefix(accessLogValve.getPrefix()));
		}

		/**
		 * Finds the AccessLogValve in the given TomcatServletWebServerFactory.
		 * @param factory the TomcatServletWebServerFactory to search for the
		 * AccessLogValve
		 * @return the found AccessLogValve, or null if not found
		 */
		private AccessLogValve findAccessLogValve(TomcatServletWebServerFactory factory) {
			for (Valve engineValve : factory.getEngineValves()) {
				if (engineValve instanceof AccessLogValve accessLogValve) {
					return accessLogValve;
				}
			}
			return null;
		}

	}

	/**
	 * UndertowAccessLogCustomizer class.
	 */
	static class UndertowAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<UndertowServletWebServerFactory> {

		/**
		 * Customizes the UndertowServletWebServerFactory by setting the access log
		 * prefix.
		 * @param factory the UndertowServletWebServerFactory to be customized
		 * @return the customized access log prefix
		 */
		@Override
		public void customize(UndertowServletWebServerFactory factory) {
			factory.setAccessLogPrefix(customizePrefix(factory.getAccessLogPrefix()));
		}

	}

	/**
	 * JettyAccessLogCustomizer class.
	 */
	static class JettyAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<JettyServletWebServerFactory> {

		/**
		 * Customize the Jetty servlet web server factory by adding server customizers.
		 * @param factory the Jetty servlet web server factory to customize
		 */
		@Override
		public void customize(JettyServletWebServerFactory factory) {
			factory.addServerCustomizers(this::customizeServer);
		}

		/**
		 * Customizes the server by customizing the request log.
		 * @param server the server to be customized
		 */
		private void customizeServer(Server server) {
			RequestLog requestLog = server.getRequestLog();
			if (requestLog instanceof CustomRequestLog customRequestLog) {
				customizeRequestLog(customRequestLog);
			}
		}

		/**
		 * Customizes the request log by checking if the writer is an instance of
		 * RequestLogWriter. If it is, the request log writer is customized using the
		 * customizeRequestLogWriter method.
		 * @param requestLog The request log to be customized.
		 */
		private void customizeRequestLog(CustomRequestLog requestLog) {
			if (requestLog.getWriter() instanceof RequestLogWriter requestLogWriter) {
				customizeRequestLogWriter(requestLogWriter);
			}
		}

		/**
		 * Customizes the request log writer by modifying the filename. If the filename is
		 * not empty, it creates a new file object using the filename, then modifies the
		 * file object by adding a customized prefix to the file name. Finally, it sets
		 * the modified file path as the new filename for the request log writer.
		 * @param writer the request log writer to be customized
		 */
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
