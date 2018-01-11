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

import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties.Session;
import org.springframework.boot.autoconfigure.web.embedded.jetty.JettyCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.tomcat.TomcatCustomizer;
import org.springframework.boot.autoconfigure.web.embedded.undertow.UndertowCustomizer;
import org.springframework.boot.web.embedded.jetty.JettyServletWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.ConfigurableTomcatWebServerFactory;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.boot.web.servlet.server.InitParameterConfiguringServletContextInitializer;
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
		if (this.serverProperties.getSession().getTimeout() != null) {
			factory.setSessionTimeout(this.serverProperties.getSession().getTimeout());
		}
		factory.setPersistSession(this.serverProperties.getSession().isPersistent());
		factory.setSessionStoreDir(this.serverProperties.getSession().getStoreDir());
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
			TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment, tomcatFactory);
			TomcatServletCustomizer.customizeTomcat(this.serverProperties, this.environment, tomcatFactory);
		}
		if (factory instanceof JettyServletWebServerFactory) {
			JettyCustomizer.customizeJetty(this.serverProperties, this.environment,
					(JettyServletWebServerFactory) factory);
		}
		if (factory instanceof UndertowServletWebServerFactory) {
			UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment,
					(UndertowServletWebServerFactory) factory);
		}
		factory.addInitializers(
				new SessionConfiguringInitializer(this.serverProperties.getSession()));
		factory.addInitializers(new InitParameterConfiguringServletContextInitializer(
				this.serverProperties.getServlet().getContextParameters()));
	}

	/**
	 * {@link ServletContextInitializer} to apply appropriate parts of the {@link Session}
	 * configuration.
	 */
	private static class SessionConfiguringInitializer
			implements ServletContextInitializer {

		private final Session session;

		SessionConfiguringInitializer(Session session) {
			this.session = session;
		}

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			if (this.session.getTrackingModes() != null) {
				servletContext
						.setSessionTrackingModes(unwrap(this.session.getTrackingModes()));
			}
			configureSessionCookie(servletContext.getSessionCookieConfig());
		}

		private void configureSessionCookie(SessionCookieConfig config) {
			Session.Cookie cookie = this.session.getCookie();
			if (cookie.getName() != null) {
				config.setName(cookie.getName());
			}
			if (cookie.getDomain() != null) {
				config.setDomain(cookie.getDomain());
			}
			if (cookie.getPath() != null) {
				config.setPath(cookie.getPath());
			}
			if (cookie.getComment() != null) {
				config.setComment(cookie.getComment());
			}
			if (cookie.getHttpOnly() != null) {
				config.setHttpOnly(cookie.getHttpOnly());
			}
			if (cookie.getSecure() != null) {
				config.setSecure(cookie.getSecure());
			}
			if (cookie.getMaxAge() != null) {
				config.setMaxAge((int) cookie.getMaxAge().getSeconds());
			}
		}

		private Set<javax.servlet.SessionTrackingMode> unwrap(
				Set<Session.SessionTrackingMode> modes) {
			if (modes == null) {
				return null;
			}
			Set<javax.servlet.SessionTrackingMode> result = new LinkedHashSet<>();
			for (Session.SessionTrackingMode mode : modes) {
				result.add(javax.servlet.SessionTrackingMode.valueOf(mode.name()));
			}
			return result;
		}

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
				ConfigurableTomcatWebServerFactory factory, boolean useRelativeRedirects) {
			factory.addContextCustomizers(
					(context) -> context.setUseRelativeRedirects(useRelativeRedirects));
		}
	}

}
