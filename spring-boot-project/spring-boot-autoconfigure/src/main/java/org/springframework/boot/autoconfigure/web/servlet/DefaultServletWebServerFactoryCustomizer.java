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
import org.springframework.boot.context.properties.PropertyMapper;
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
 * @author Yunkun Huang
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
		PropertyMapper map = PropertyMapper.get().alwaysApplyingWhenNonNull();
		map.from(this.serverProperties::getPort).to(factory::setPort);
		map.from(this.serverProperties::getAddress).to(factory::setAddress);
		map.from(this.serverProperties.getServlet()::getContextPath)
				.to(factory::setContextPath);
		map.from(this.serverProperties::getDisplayName).to(factory::setDisplayName);
		map.from(this.serverProperties.getServlet()::getSession).to(factory::setSession);
		map.from(this.serverProperties::getSsl).to(factory::setSsl);
		map.from(this.serverProperties::getServlet).as(ServerProperties.Servlet::getJsp)
				.to(factory::setJsp);
		map.from(this.serverProperties::getCompression).to(factory::setCompression);
		map.from(this.serverProperties::getHttp2).to(factory::setHttp2);
		map.from(this.serverProperties::getServerHeader).to(factory::setServerHeader);
		map.from(() -> factory).whenInstanceOf(TomcatServletWebServerFactory.class)
				.to((tomcatFactory) -> {
					TomcatCustomizer.customizeTomcat(this.serverProperties,
							this.environment, tomcatFactory);
					TomcatServletCustomizer.customizeTomcat(this.serverProperties,
							this.environment, tomcatFactory);
				});
		map.from(() -> factory).whenInstanceOf(JettyServletWebServerFactory.class).to(
				(jettyFactory) -> JettyCustomizer.customizeJetty(this.serverProperties,
						this.environment, jettyFactory));
		map.from(() -> factory).whenInstanceOf(UndertowServletWebServerFactory.class)
				.to((undertowFactory) -> UndertowCustomizer.customizeUndertow(
						this.serverProperties, this.environment, undertowFactory));
		map.from(this.serverProperties.getServlet()::getContextParameters)
				.to(factory::setInitParameters);
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
