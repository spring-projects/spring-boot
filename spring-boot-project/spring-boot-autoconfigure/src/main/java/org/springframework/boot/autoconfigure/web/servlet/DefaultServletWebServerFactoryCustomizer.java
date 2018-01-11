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

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;

import io.undertow.UndertowOptions;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.NCSARequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.ServerProperties.Session;
import org.springframework.boot.autoconfigure.web.embedded.tomcat.TomcatCustomizer;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.web.embedded.jetty.JettyServerCustomizer;
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

	private static boolean getOrDeduceUseForwardHeaders(ServerProperties serverProperties,
			Environment environment) {
		if (serverProperties.isUseForwardHeaders() != null) {
			return serverProperties.isUseForwardHeaders();
		}
		CloudPlatform platform = CloudPlatform.getActive(environment);
		return platform != null && platform.isUsingForwardHeaders();
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

	private static class UndertowCustomizer {

		protected static void customizeUndertow(ServerProperties serverProperties,
				Environment environment, UndertowServletWebServerFactory factory) {

			ServerProperties.Undertow undertowProperties = serverProperties.getUndertow();
			ServerProperties.Undertow.Accesslog accesslogProperties = undertowProperties
					.getAccesslog();
			if (undertowProperties.getBufferSize() != null) {
				factory.setBufferSize(undertowProperties.getBufferSize());
			}
			if (undertowProperties.getIoThreads() != null) {
				factory.setIoThreads(undertowProperties.getIoThreads());
			}
			if (undertowProperties.getWorkerThreads() != null) {
				factory.setWorkerThreads(undertowProperties.getWorkerThreads());
			}
			if (undertowProperties.getDirectBuffers() != null) {
				factory.setDirectBuffers(undertowProperties.getDirectBuffers());
			}
			if (undertowProperties.getAccesslog().getEnabled() != null) {
				factory.setAccessLogEnabled(accesslogProperties.getEnabled());
			}
			factory.setAccessLogDirectory(accesslogProperties.getDir());
			factory.setAccessLogPattern(accesslogProperties.getPattern());
			factory.setAccessLogPrefix(accesslogProperties.getPrefix());
			factory.setAccessLogSuffix(accesslogProperties.getSuffix());
			factory.setAccessLogRotate(accesslogProperties.isRotate());
			factory.setUseForwardHeaders(
					getOrDeduceUseForwardHeaders(serverProperties, environment));
			if (serverProperties.getMaxHttpHeaderSize() > 0) {
				customizeMaxHttpHeaderSize(factory,
						serverProperties.getMaxHttpHeaderSize());
			}
			if (undertowProperties.getMaxHttpPostSize() > 0) {
				customizeMaxHttpPostSize(factory,
						undertowProperties.getMaxHttpPostSize());
			}
			if (serverProperties.getConnectionTimeout() != null) {
				customizeConnectionTimeout(factory,
						serverProperties.getConnectionTimeout());
			}
			factory.addDeploymentInfoCustomizers((deploymentInfo) -> deploymentInfo
					.setEagerFilterInit(undertowProperties.isEagerFilterInit()));
		}

		private static void customizeConnectionTimeout(
				UndertowServletWebServerFactory factory, Duration connectionTimeout) {
			factory.addBuilderCustomizers((builder) -> builder.setSocketOption(
					UndertowOptions.NO_REQUEST_TIMEOUT,
					(int) connectionTimeout.toMillis()));
		}

		private static void customizeMaxHttpHeaderSize(
				UndertowServletWebServerFactory factory, int maxHttpHeaderSize) {
			factory.addBuilderCustomizers((builder) -> builder
					.setServerOption(UndertowOptions.MAX_HEADER_SIZE, maxHttpHeaderSize));
		}

		private static void customizeMaxHttpPostSize(
				UndertowServletWebServerFactory factory, long maxHttpPostSize) {
			factory.addBuilderCustomizers((builder) -> builder
					.setServerOption(UndertowOptions.MAX_ENTITY_SIZE, maxHttpPostSize));
		}

	}

	private static class JettyCustomizer {

		public static void customizeJetty(ServerProperties serverProperties,
				Environment environment, JettyServletWebServerFactory factory) {
			ServerProperties.Jetty jettyProperties = serverProperties.getJetty();
			factory.setUseForwardHeaders(
					getOrDeduceUseForwardHeaders(serverProperties, environment));
			if (jettyProperties.getAcceptors() != null) {
				factory.setAcceptors(jettyProperties.getAcceptors());
			}
			if (jettyProperties.getSelectors() != null) {
				factory.setSelectors(jettyProperties.getSelectors());
			}
			if (serverProperties.getMaxHttpHeaderSize() > 0) {
				customizeMaxHttpHeaderSize(factory,
						serverProperties.getMaxHttpHeaderSize());
			}
			if (jettyProperties.getMaxHttpPostSize() > 0) {
				customizeMaxHttpPostSize(factory, jettyProperties.getMaxHttpPostSize());
			}

			if (serverProperties.getConnectionTimeout() != null) {
				customizeConnectionTimeout(factory,
						serverProperties.getConnectionTimeout());
			}
			if (jettyProperties.getAccesslog().isEnabled()) {
				customizeAccessLog(factory, jettyProperties.getAccesslog());
			}
		}

		private static void customizeConnectionTimeout(
				JettyServletWebServerFactory factory, Duration connectionTimeout) {
			factory.addServerCustomizers((server) -> {
				for (org.eclipse.jetty.server.Connector connector : server
						.getConnectors()) {
					if (connector instanceof AbstractConnector) {
						((AbstractConnector) connector)
								.setIdleTimeout(connectionTimeout.toMillis());
					}
				}
			});
		}

		private static void customizeMaxHttpHeaderSize(
				JettyServletWebServerFactory factory, int maxHttpHeaderSize) {
			factory.addServerCustomizers(new JettyServerCustomizer() {

				@Override
				public void customize(Server server) {
					for (org.eclipse.jetty.server.Connector connector : server
							.getConnectors()) {
						try {
							for (ConnectionFactory connectionFactory : connector
									.getConnectionFactories()) {
								if (connectionFactory instanceof HttpConfiguration.ConnectionFactory) {
									customize(
											(HttpConfiguration.ConnectionFactory) connectionFactory);
								}
							}
						}
						catch (NoSuchMethodError ex) {
							customizeOnJetty8(connector, maxHttpHeaderSize);
						}
					}

				}

				private void customize(HttpConfiguration.ConnectionFactory factory) {
					HttpConfiguration configuration = factory.getHttpConfiguration();
					configuration.setRequestHeaderSize(maxHttpHeaderSize);
					configuration.setResponseHeaderSize(maxHttpHeaderSize);
				}

				private void customizeOnJetty8(
						org.eclipse.jetty.server.Connector connector,
						int maxHttpHeaderSize) {
					try {
						connector.getClass().getMethod("setRequestHeaderSize", int.class)
								.invoke(connector, maxHttpHeaderSize);
						connector.getClass().getMethod("setResponseHeaderSize", int.class)
								.invoke(connector, maxHttpHeaderSize);
					}
					catch (Exception ex) {
						throw new RuntimeException(ex);
					}
				}

			});
		}

		private static void customizeMaxHttpPostSize(JettyServletWebServerFactory factory,
				int maxHttpPostSize) {
			factory.addServerCustomizers(new JettyServerCustomizer() {

				@Override
				public void customize(Server server) {
					setHandlerMaxHttpPostSize(maxHttpPostSize, server.getHandlers());
				}

				private void setHandlerMaxHttpPostSize(int maxHttpPostSize,
						Handler... handlers) {
					for (Handler handler : handlers) {
						if (handler instanceof ContextHandler) {
							((ContextHandler) handler)
									.setMaxFormContentSize(maxHttpPostSize);
						}
						else if (handler instanceof HandlerWrapper) {
							setHandlerMaxHttpPostSize(maxHttpPostSize,
									((HandlerWrapper) handler).getHandler());
						}
						else if (handler instanceof HandlerCollection) {
							setHandlerMaxHttpPostSize(maxHttpPostSize,
									((HandlerCollection) handler).getHandlers());
						}
					}
				}

			});
		}

		private static void customizeAccessLog(JettyServletWebServerFactory factory,
				ServerProperties.Jetty.Accesslog properties) {
			factory.addServerCustomizers((server) -> {
				NCSARequestLog log = new NCSARequestLog();
				if (properties.getFilename() != null) {
					log.setFilename(properties.getFilename());
				}
				if (properties.getFileDateFormat() != null) {
					log.setFilenameDateFormat(properties.getFileDateFormat());
				}
				log.setRetainDays(properties.getRetentionPeriod());
				log.setAppend(properties.isAppend());
				log.setExtended(properties.isExtendedFormat());
				if (properties.getDateFormat() != null) {
					log.setLogDateFormat(properties.getDateFormat());
				}
				if (properties.getLocale() != null) {
					log.setLogLocale(properties.getLocale());
				}
				if (properties.getTimeZone() != null) {
					log.setLogTimeZone(properties.getTimeZone().getID());
				}
				log.setLogCookies(properties.isLogCookies());
				log.setLogServer(properties.isLogServer());
				log.setLogLatency(properties.isLogLatency());
				server.setRequestLog(log);
			});
		}
	}

}
