/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web.reactive;

import java.io.File;
import java.util.Collections;
import java.util.Map;

import org.apache.catalina.Valve;
import org.apache.catalina.valves.AccessLogValve;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.RequestLogWriter;
import org.eclipse.jetty.server.Server;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration;
import org.springframework.boot.actuate.autoconfigure.web.ManagementContextType;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementWebServerFactoryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication.Type;
import org.springframework.boot.web.embedded.jetty.JettyReactiveWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatReactiveWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowReactiveWebServerFactory;
import org.springframework.boot.web.server.ConfigurableWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ContextPathCompositeHandler;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.server.adapter.WebHttpHandlerBuilder;

/**
 * {@link ManagementContextConfiguration @ManagementContextConfiguration} for reactive web
 * infrastructure when a separate management context with a web server running on a
 * different port is required.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Moritz Halbritter
 * @since 2.0.0
 */
@EnableWebFlux
@ManagementContextConfiguration(value = ManagementContextType.CHILD, proxyBeanMethods = false)
@ConditionalOnWebApplication(type = Type.REACTIVE)
public class ReactiveManagementChildContextConfiguration {

	@Bean
	public ManagementWebServerFactoryCustomizer<ConfigurableWebServerFactory> reactiveManagementWebServerFactoryCustomizer(
			ListableBeanFactory beanFactory) {
		return new ManagementWebServerFactoryCustomizer<>(beanFactory);
	}

	@Bean
	public HttpHandler httpHandler(ApplicationContext applicationContext, ManagementServerProperties properties) {
		HttpHandler httpHandler = WebHttpHandlerBuilder.applicationContext(applicationContext).build();
		if (StringUtils.hasText(properties.getBasePath())) {
			Map<String, HttpHandler> handlersMap = Collections.singletonMap(properties.getBasePath(), httpHandler);
			return new ContextPathCompositeHandler(handlersMap);
		}
		return httpHandler;
	}

	@Bean
	@ConditionalOnClass(name = "io.undertow.Undertow")
	UndertowAccessLogCustomizer undertowManagementAccessLogCustomizer(ManagementServerProperties properties) {
		return new UndertowAccessLogCustomizer(properties);
	}

	@Bean
	@ConditionalOnClass(name = "org.apache.catalina.valves.AccessLogValve")
	TomcatAccessLogCustomizer tomcatManagementAccessLogCustomizer(ManagementServerProperties properties) {
		return new TomcatAccessLogCustomizer(properties);
	}

	@Bean
	@ConditionalOnClass(name = "org.eclipse.jetty.server.Server")
	JettyAccessLogCustomizer jettyManagementAccessLogCustomizer(ManagementServerProperties properties) {
		return new JettyAccessLogCustomizer(properties);
	}

	abstract static class AccessLogCustomizer implements Ordered {

		private final String prefix;

		AccessLogCustomizer(String prefix) {
			this.prefix = prefix;
		}

		protected String customizePrefix(String existingPrefix) {
			if (this.prefix == null) {
				return existingPrefix;
			}
			if (existingPrefix == null) {
				return this.prefix;
			}
			if (existingPrefix.startsWith(this.prefix)) {
				return existingPrefix;
			}
			return this.prefix + existingPrefix;
		}

		@Override
		public int getOrder() {
			return 1;
		}

	}

	static class TomcatAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<TomcatReactiveWebServerFactory> {

		TomcatAccessLogCustomizer(ManagementServerProperties properties) {
			super(properties.getTomcat().getAccesslog().getPrefix());
		}

		@Override
		public void customize(TomcatReactiveWebServerFactory factory) {
			AccessLogValve accessLogValve = findAccessLogValve(factory);
			if (accessLogValve == null) {
				return;
			}
			accessLogValve.setPrefix(customizePrefix(accessLogValve.getPrefix()));
		}

		private AccessLogValve findAccessLogValve(TomcatReactiveWebServerFactory factory) {
			for (Valve engineValve : factory.getEngineValves()) {
				if (engineValve instanceof AccessLogValve accessLogValve) {
					return accessLogValve;
				}
			}
			return null;
		}

	}

	static class UndertowAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<UndertowReactiveWebServerFactory> {

		UndertowAccessLogCustomizer(ManagementServerProperties properties) {
			super(properties.getUndertow().getAccesslog().getPrefix());
		}

		@Override
		public void customize(UndertowReactiveWebServerFactory factory) {
			factory.setAccessLogPrefix(customizePrefix(factory.getAccessLogPrefix()));
		}

	}

	static class JettyAccessLogCustomizer extends AccessLogCustomizer
			implements WebServerFactoryCustomizer<JettyReactiveWebServerFactory> {

		JettyAccessLogCustomizer(ManagementServerProperties properties) {
			super(properties.getJetty().getAccesslog().getPrefix());
		}

		@Override
		public void customize(JettyReactiveWebServerFactory factory) {
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
