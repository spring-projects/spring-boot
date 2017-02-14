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

package org.springframework.boot.autoconfigure.web;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.SessionCookieConfig;

import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.valves.AccessLogValve;
import org.apache.catalina.valves.RemoteIpValve;
import org.apache.coyote.AbstractProtocol;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.AbstractHttp11Protocol;
import org.eclipse.jetty.server.AbstractConnector;
import org.eclipse.jetty.server.ConnectionFactory;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.HandlerWrapper;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizerBeanPostProcessor;
import org.springframework.boot.context.embedded.EmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.InitParameterConfiguringServletContextInitializer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatContextCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Customizer used by an {@link EmbeddedServletContainerFactory} when an
 * {@link EmbeddedServletContainerCustomizerBeanPostProcessor} is active.
 *
 * @author Brian Clozel
 * @since 2.0.0
 */
public class DefaultServletContainerCustomizer
		implements EmbeddedServletContainerCustomizer, EnvironmentAware, Ordered {

	private final ServerProperties serverProperties;

	private Environment environment;

	public DefaultServletContainerCustomizer(ServerProperties serverProperties) {
		this.serverProperties = serverProperties;
	}

	public void setLoader(String value) {
		// no op to support Tomcat running as a traditional container (not embedded)
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
	public void customize(ConfigurableEmbeddedServletContainer container) {
		if (this.serverProperties.getPort() != null) {
			container.setPort(this.serverProperties.getPort());
		}
		if (this.serverProperties.getAddress() != null) {
			container.setAddress(this.serverProperties.getAddress());
		}
		if (this.serverProperties.getServlet().getContextPath() != null) {
			container.setContextPath(this.serverProperties.getServlet().getContextPath());
		}
		if (this.serverProperties.getDisplayName() != null) {
			container.setDisplayName(this.serverProperties.getDisplayName());
		}
		if (this.serverProperties.getSession().getTimeout() != null) {
			container.setSessionTimeout(this.serverProperties.getSession().getTimeout());
		}
		container.setPersistSession(this.serverProperties.getSession().isPersistent());
		container.setSessionStoreDir(this.serverProperties.getSession().getStoreDir());
		if (this.serverProperties.getSsl() != null) {
			container.setSsl(this.serverProperties.getSsl());
		}
		if (this.serverProperties.getServlet() != null) {
			container.setJsp(this.serverProperties.getServlet().getJsp());
		}
		if (this.serverProperties.getCompression() != null) {
			container.setCompression(this.serverProperties.getCompression());
		}
		container.setServerHeader(this.serverProperties.getServerHeader());
		if (container instanceof TomcatEmbeddedServletContainerFactory) {
			TomcatCustomizer.customizeTomcat(this.serverProperties, this.environment,
					(TomcatEmbeddedServletContainerFactory) container);
		}
		if (container instanceof JettyEmbeddedServletContainerFactory) {
			JettyCustomizer.customizeJetty(this.serverProperties, this.environment,
					(JettyEmbeddedServletContainerFactory) container);
		}

		if (container instanceof UndertowEmbeddedServletContainerFactory) {
			UndertowCustomizer.customizeUndertow(this.serverProperties, this.environment,
					(UndertowEmbeddedServletContainerFactory) container);
		}
		container.addInitializers(new SessionConfiguringInitializer(this.serverProperties.getSession()));
		container.addInitializers(new InitParameterConfiguringServletContextInitializer(
				this.serverProperties.getServlet().getContextParameters()));
	}

	private static boolean getOrDeduceUseForwardHeaders(ServerProperties serverProperties,
			Environment environment) {
		if (serverProperties.isUseForwardHeaders() != null) {
			return serverProperties.isUseForwardHeaders();
		}
		CloudPlatform platform = CloudPlatform.getActive(environment);
		return (platform == null ? false : platform.isUsingForwardHeaders());
	}

	/**
	 * {@link ServletContextInitializer} to apply appropriate parts of the {@link ServerProperties.Session}
	 * configuration.
	 */
	private static class SessionConfiguringInitializer
			implements ServletContextInitializer {

		private final ServerProperties.Session session;

		SessionConfiguringInitializer(ServerProperties.Session session) {
			this.session = session;
		}

		@Override
		public void onStartup(ServletContext servletContext) throws ServletException {
			if (this.session.getTrackingModes() != null) {
				servletContext.setSessionTrackingModes(this.session.getTrackingModes());
			}
			configureSessionCookie(servletContext.getSessionCookieConfig());
		}

		private void configureSessionCookie(SessionCookieConfig config) {
			ServerProperties.Session.Cookie cookie = this.session.getCookie();
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
				config.setMaxAge(cookie.getMaxAge());
			}
		}

	}

	private static class TomcatCustomizer {

		public static void customizeTomcat(ServerProperties serverProperties, Environment environment,
				TomcatEmbeddedServletContainerFactory factory) {

			ServerProperties.Tomcat tomcatProperties = serverProperties.getTomcat();
			if (tomcatProperties.getBasedir() != null) {
				factory.setBaseDirectory(tomcatProperties.getBasedir());
			}
			factory.setBackgroundProcessorDelay(tomcatProperties.getBackgroundProcessorDelay());
			customizeRemoteIpValve(serverProperties, environment, factory);
			if (tomcatProperties.getMaxThreads() > 0) {
				customizeMaxThreads(factory, tomcatProperties.getMaxThreads());
			}
			if (tomcatProperties.getMinSpareThreads() > 0) {
				customizeMinThreads(factory, tomcatProperties.getMinSpareThreads());
			}
			int maxHttpHeaderSize = (serverProperties.getMaxHttpHeaderSize() > 0
					? serverProperties.getMaxHttpHeaderSize() : tomcatProperties.getMaxHttpHeaderSize());
			if (maxHttpHeaderSize > 0) {
				customizeMaxHttpHeaderSize(factory, maxHttpHeaderSize);
			}
			if (tomcatProperties.getMaxHttpPostSize() > 0) {
				customizeMaxHttpPostSize(factory, tomcatProperties.getMaxHttpPostSize());
			}
			if (tomcatProperties.getAccesslog().isEnabled()) {
				customizeAccessLog(tomcatProperties, factory);
			}
			if (tomcatProperties.getUriEncoding() != null) {
				factory.setUriEncoding(tomcatProperties.getUriEncoding());
			}
			if (serverProperties.getConnectionTimeout() != null) {
				customizeConnectionTimeout(factory,
						serverProperties.getConnectionTimeout());
			}
			if (tomcatProperties.getRedirectContextRoot() != null) {
				customizeRedirectContextRoot(factory, tomcatProperties.getRedirectContextRoot());
			}
			if (tomcatProperties.getMaxConnections() > 0) {
				customizeMaxConnections(factory, tomcatProperties.getMaxConnections());
			}
			if (tomcatProperties.getAcceptCount() > 0) {
				customizeAcceptCount(factory, tomcatProperties.getAcceptCount());
			}
			if (!ObjectUtils.isEmpty(tomcatProperties.getAdditionalTldSkipPatterns())) {
				factory.getTldSkipPatterns().addAll(tomcatProperties.getAdditionalTldSkipPatterns());
			}
		}

		private static void customizeAcceptCount(TomcatEmbeddedServletContainerFactory factory,
				final int acceptCount) {
			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {

				@Override
				public void customize(Connector connector) {
					ProtocolHandler handler = connector.getProtocolHandler();
					if (handler instanceof AbstractProtocol) {
						AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
						protocol.setBacklog(acceptCount);
					}
				}

			});
		}

		private static void customizeMaxConnections(
				TomcatEmbeddedServletContainerFactory factory, final int maxConnections) {
			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {

				@Override
				public void customize(Connector connector) {
					ProtocolHandler handler = connector.getProtocolHandler();
					if (handler instanceof AbstractProtocol) {
						AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
						protocol.setMaxConnections(maxConnections);
					}
				}

			});
		}

		private static void customizeConnectionTimeout(
				TomcatEmbeddedServletContainerFactory factory, final int connectionTimeout) {
			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {

				@Override
				public void customize(Connector connector) {
					ProtocolHandler handler = connector.getProtocolHandler();
					if (handler instanceof AbstractProtocol) {
						AbstractProtocol<?> protocol = (AbstractProtocol<?>) handler;
						protocol.setConnectionTimeout(connectionTimeout);
					}
				}

			});
		}

		private static void customizeRemoteIpValve(ServerProperties properties, Environment environment,
				TomcatEmbeddedServletContainerFactory factory) {
			String protocolHeader = properties.getTomcat().getProtocolHeader();
			String remoteIpHeader = properties.getTomcat().getRemoteIpHeader();
			// For back compatibility the valve is also enabled if protocol-header is set
			if (StringUtils.hasText(protocolHeader) || StringUtils.hasText(remoteIpHeader)
					|| getOrDeduceUseForwardHeaders(properties, environment)) {
				RemoteIpValve valve = new RemoteIpValve();
				valve.setProtocolHeader(StringUtils.hasLength(protocolHeader)
						? protocolHeader : "X-Forwarded-Proto");
				if (StringUtils.hasLength(remoteIpHeader)) {
					valve.setRemoteIpHeader(remoteIpHeader);
				}
				// The internal proxies default to a white list of "safe" internal IP
				// addresses
				valve.setInternalProxies(properties.getTomcat().getInternalProxies());
				valve.setPortHeader(properties.getTomcat().getPortHeader());
				valve.setProtocolHeaderHttpsValue(properties.getTomcat().getProtocolHeaderHttpsValue());
				// ... so it's safe to add this valve by default.
				factory.addEngineValves(valve);
			}
		}

		@SuppressWarnings("rawtypes")
		private static void customizeMaxThreads(TomcatEmbeddedServletContainerFactory factory,
				final int maxThreads) {
			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
				@Override
				public void customize(Connector connector) {

					ProtocolHandler handler = connector.getProtocolHandler();
					if (handler instanceof AbstractProtocol) {
						AbstractProtocol protocol = (AbstractProtocol) handler;
						protocol.setMaxThreads(maxThreads);
					}

				}
			});
		}

		@SuppressWarnings("rawtypes")
		private static void customizeMinThreads(TomcatEmbeddedServletContainerFactory factory,
				final int minSpareThreads) {
			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
				@Override
				public void customize(Connector connector) {

					ProtocolHandler handler = connector.getProtocolHandler();
					if (handler instanceof AbstractProtocol) {
						AbstractProtocol protocol = (AbstractProtocol) handler;
						protocol.setMinSpareThreads(minSpareThreads);
					}

				}
			});
		}

		@SuppressWarnings("rawtypes")
		private static void customizeMaxHttpHeaderSize(
				TomcatEmbeddedServletContainerFactory factory, final int maxHttpHeaderSize) {
			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {

				@Override
				public void customize(Connector connector) {
					ProtocolHandler handler = connector.getProtocolHandler();
					if (handler instanceof AbstractHttp11Protocol) {
						AbstractHttp11Protocol protocol = (AbstractHttp11Protocol) handler;
						protocol.setMaxHttpHeaderSize(maxHttpHeaderSize);
					}
				}

			});
		}

		private static void customizeMaxHttpPostSize(
				TomcatEmbeddedServletContainerFactory factory, final int maxHttpPostSize) {
			factory.addConnectorCustomizers(new TomcatConnectorCustomizer() {

				@Override
				public void customize(Connector connector) {
					connector.setMaxPostSize(maxHttpPostSize);
				}

			});
		}

		private static void customizeAccessLog(ServerProperties.Tomcat tomcatProperties,
				TomcatEmbeddedServletContainerFactory factory) {

			AccessLogValve valve = new AccessLogValve();
			valve.setPattern(tomcatProperties.getAccesslog().getPattern());
			valve.setDirectory(tomcatProperties.getAccesslog().getDirectory());
			valve.setPrefix(tomcatProperties.getAccesslog().getPrefix());
			valve.setSuffix(tomcatProperties.getAccesslog().getSuffix());
			valve.setRenameOnRotate(tomcatProperties.getAccesslog().isRenameOnRotate());
			valve.setRequestAttributesEnabled(
					tomcatProperties.getAccesslog().isRequestAttributesEnabled());
			valve.setRotatable(tomcatProperties.getAccesslog().isRotate());
			valve.setBuffered(tomcatProperties.getAccesslog().isBuffered());
			factory.addEngineValves(valve);
		}

		private static void customizeRedirectContextRoot(
				TomcatEmbeddedServletContainerFactory factory,
				final boolean redirectContextRoot) {
			factory.addContextCustomizers(new TomcatContextCustomizer() {

				@Override
				public void customize(Context context) {
					context.setMapperContextRootRedirectEnabled(redirectContextRoot);
				}

			});
		}

	}

	private static class UndertowCustomizer {

		protected static void customizeUndertow(final ServerProperties serverProperties,
				Environment environment, UndertowEmbeddedServletContainerFactory factory) {

			ServerProperties.Undertow undertowProperties = serverProperties.getUndertow();
			ServerProperties.Undertow.Accesslog accesslogProperties = undertowProperties.getAccesslog();
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
			factory.setUseForwardHeaders(getOrDeduceUseForwardHeaders(serverProperties, environment));
			if (serverProperties.getMaxHttpHeaderSize() > 0) {
				customizeMaxHttpHeaderSize(factory,
						serverProperties.getMaxHttpHeaderSize());
			}
			if (undertowProperties.getMaxHttpPostSize() > 0) {
				customizeMaxHttpPostSize(factory, undertowProperties.getMaxHttpPostSize());
			}

			if (serverProperties.getConnectionTimeout() != null) {
				customizeConnectionTimeout(factory,
						serverProperties.getConnectionTimeout());
			}
		}

		private static void customizeConnectionTimeout(
				UndertowEmbeddedServletContainerFactory factory,
				final int connectionTimeout) {
			factory.addBuilderCustomizers(new UndertowBuilderCustomizer() {
				@Override
				public void customize(Undertow.Builder builder) {
					builder.setSocketOption(UndertowOptions.NO_REQUEST_TIMEOUT,
							connectionTimeout);
				}
			});
		}

		private static void customizeMaxHttpHeaderSize(
				UndertowEmbeddedServletContainerFactory factory,
				final int maxHttpHeaderSize) {
			factory.addBuilderCustomizers(new UndertowBuilderCustomizer() {

				@Override
				public void customize(Undertow.Builder builder) {
					builder.setServerOption(UndertowOptions.MAX_HEADER_SIZE,
							maxHttpHeaderSize);
				}

			});
		}

		private static void customizeMaxHttpPostSize(
				UndertowEmbeddedServletContainerFactory factory,
				final long maxHttpPostSize) {
			factory.addBuilderCustomizers(new UndertowBuilderCustomizer() {

				@Override
				public void customize(Undertow.Builder builder) {
					builder.setServerOption(UndertowOptions.MAX_ENTITY_SIZE,
							maxHttpPostSize);
				}

			});
		}

	}

	private static class JettyCustomizer {

		public static void customizeJetty(final ServerProperties serverProperties,
				Environment environment, JettyEmbeddedServletContainerFactory factory) {
			ServerProperties.Jetty jettyProperties = serverProperties.getJetty();
			factory.setUseForwardHeaders(getOrDeduceUseForwardHeaders(serverProperties, environment));
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
		}

		private static void customizeConnectionTimeout(
				JettyEmbeddedServletContainerFactory factory,
				final int connectionTimeout) {
			factory.addServerCustomizers(new JettyServerCustomizer() {

				@Override
				public void customize(Server server) {
					for (org.eclipse.jetty.server.Connector connector : server
							.getConnectors()) {
						if (connector instanceof AbstractConnector) {
							((AbstractConnector) connector)
									.setIdleTimeout(connectionTimeout);
						}
					}
				}

			});
		}

		private static void customizeMaxHttpHeaderSize(
				JettyEmbeddedServletContainerFactory factory,
				final int maxHttpHeaderSize) {
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

		private static void customizeMaxHttpPostSize(
				JettyEmbeddedServletContainerFactory factory, final int maxHttpPostSize) {
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
	}

}
