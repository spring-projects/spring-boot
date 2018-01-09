/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.servlet;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.jetty.JettyCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.tomcat.TomcatCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.undertow.UndertowCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;

/**
 * Default {@link WebServerFactoryCustomizer} for {@link ServerProperties}.
 *
 * @author Brian Clozel
 * @author Stephane Nicoll
 * @author Olivier Lamy
 * @since 2.0.0
 */
public class DefaultServletWebServerFactoryCustomizer
		implements WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>,
		EnvironmentAware, Ordered {

	private final ServerProperties serverProperties;

	private Environment environment;

	public DefaultServletWebServerFactoryCustomizer(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	public void setLoader(String value) {
		// no op to support Tomcat running as a traditional server (not embedded)
	}

	@Override
	public int getOrder() {
		return 0;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void customize(ConfigurableServletWebServerFactory factory) {
		if (this.serverProperties.getPort() != null) {
			factory.setPort(this.serverProperties.getPort());
		}
		if (this.serverProperties.getAddress() != null) {
			factory.setAddress(this.serverProperties.getAddress());
		}
		if (this.serverProperties.getServlet().getContextPath() != null) {
			factory.setContextPath(this.serverProperties.getServlet().getContextPath());
		}
		if (this.serverProperties.getDisplayName() != null) {
			factory.setDisplayName(this.serverProperties.getDisplayName());
		}
		factory.setSession(this.serverProperties.getServlet().getSession());
		if (this.serverProperties.getSsl() != null) {
			factory.setSsl(this.serverProperties.getSsl());
		}
		if (this.serverProperties.getServlet() != null) {
			factory.setJsp(this.serverProperties.getServlet().getJsp());
		}
		if (this.serverProperties.getCompression() != null) {
			factory.setCompression(this.serverProperties.getCompression());
		}
		if (this.serverProperties.getHttp2() != null) {
			factory.setHttp2(this.serverProperties.getHttp2());
		}
		factory.setServerHeader(this.serverProperties.getServerHeader());
		if (factory instanceof TomcatServletWebServerFactory) {
			TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory) factory;
			TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment,
					tomcatFactory);
			TomcatServletCustomizer.customizeTomcat(this.serverProperties,
					this.environment, tomcatFactory);
		}
		if (factory instanceof JettyServletWebServerFactory) {
			JettyCustomizer.customizeJetty(this.serverProperties, this.environment,
					(JettyServletWebServerFactory) factory);
		}
		if (factory instanceof UndertowServletWebServerFactory) {
			UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment,
					(UndertowServletWebServerFactory) factory);
		}
		factory.setInitParameters(
				this.serverProperties.getServlet().getContextParameters());
	}

	private static class TomcatServletCustomizer {

		public static void customizeTomcat(ServerProperties serverProperties,
				Environment environment, TomcatServletWebServerFactory factory) {
			ServerProperties.Tomcat tomcatProperties = serverProperties.getTomcat();
			if (!ObjectUtils.isEmpty(tomcatProperties.getAdditionalTldSkipPatterns())) {
				factory.getTldSkipPatterns()
						.addAll(tomcatProperties.getAdditionalTldSkipPatterns());
			}
			if (tomcatProperties.getRedirectContextRoot() != null) {
				customizeRedirectContextRoot(factory,
						tomcatProperties.getRedirectContextRoot());
			}
			if (tomcatProperties.getUseRelativeRedirects() != null) {
				customizeUseRelativeRedirects(factory,
						tomcatProperties.getUseRelativeRedirects());
			}
		}

		private static void customizeRedirectContextRoot(
				ConfigurableTomcatWebServerFactory factory, boolean redirectContextRoot) {
			factory.addContextCustomizers((context) -> context
					.setMapperContextRootRedirectEnabled(redirectContextRoot));
		}

		private static void customizeUseRelativeRedirects(
				ConfigurableTomcatWebServerFactory factory,
				boolean useRelativeRedirects) {
			factory.addContextCustomizers(
					(context) -> context.setUseRelativeRedirects(useRelativeRedirects));
		}
	}

}
